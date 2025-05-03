package com.darkrockstudios.app.securecamera.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.*
import android.security.keystore.StrongBoxUnavailableException
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.preferences.HashedPin
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


class HardwareBackedEncryptionScheme(
	private val appContext: Context,
	deviceInfo: DeviceInfo,
	private val appPreferencesDataSource: AppPreferencesDataSource,
) : SoftwareEncryptionScheme(deviceInfo) {
	private val provider = CryptographyProvider.Default
	private val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

	@OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)
	override suspend fun deriveKeyRaw(
		plainPin: String,
		hashedPin: HashedPin,
	): ByteArray {
		val dSaltFile = dSaltFile()
		if (dSaltFile.exists().not()) error("dSalt not found!")

		val cipheredDsalt = dSaltFile.readBytes()
		val plainDsalt = decryptWithHardwareBackedKey(cipheredDsalt)
		val encodedDsalt = Base64.Default.encode(plainDsalt)

		val deviceId = deviceInfo.getDeviceIdentifier()
		val encodedDeviceId = Base64.Default.encode(deviceId)

		val dekInput =
			plainPin.toByteArray(Charsets.UTF_8) + encodedDsalt.toByteArray(Charsets.UTF_8) + encodedDeviceId.toByteArray(
				Charsets.UTF_8
			)

		val secretDerivation = provider.get(PBKDF2).secretDerivation(
			digest = SHA256,
			iterations = defaultKeyParams.iterations,
			outputSize = defaultKeyParams.outputSize,
			salt = hashedPin.salt.toByteArray()
		)

		return secretDerivation.deriveSecret(dekInput).toByteArray()
	}

	/**
	 * Creates the Hardware backed key, and creates and encrypts the dSalt
	 * then stores it to disk.
	 */
	override suspend fun createKey() {
		// Sanity checks
		if (ks.containsAlias(KEY_ALIAS)) error("KEK already exists!")

		val dSaltFile = dSaltFile()
		if (dSaltFile.exists()) error("dSalt already exists!")

		val config = appPreferencesDataSource.getSchemeConfig() as HardwareSchemeConfig

		keyMutex.withLock {
			// Create hardware backed KEK
			generateHardwareBackedKey(config)

			// Create the dSalt
			val dSalt = ByteArray(DSALT_SIZE)
			SecureRandom.getInstanceStrong().nextBytes(dSalt)

			// Write it to disk
			val cipheredDSalt = encryptWithHardwareBackedKey(dSalt)
			dSaltFile.writeBytes(cipheredDSalt)
		}
	}

	private fun generateHardwareBackedKey(config: HardwareSchemeConfig) {
		// 1. Build the key-generation parameters
		val spec = KeyGenParameterSpec.Builder(
			KEY_ALIAS,
			PURPOSE_ENCRYPT or PURPOSE_DECRYPT
		).apply {
			setKeySize(256)
			setBlockModes(BLOCK_MODE_GCM)
			setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
			setUnlockedDeviceRequired(true)

			// Require user authentication for every use
//			if (config.requireBiometricAttestation) {
//				setUserAuthenticationRequired(true)
//				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//					setUserAuthenticationParameters(config.authTimeout.inWholeSeconds.toInt(), AUTH_BIOMETRIC_STRONG)
//				}
//			}

			// Ask for StrongBox backing
			setIsStrongBoxBacked(true)
		}.build()

		try {
			// 2. Generate the key
			val keyGenerator = KeyGenerator.getInstance(
				KEY_ALGORITHM_AES,
				"AndroidKeyStore"
			)
			keyGenerator.init(spec)
			keyGenerator.generateKey()    // This creates (or replaces) the key in Keystore
		} catch (_: StrongBoxUnavailableException) {
			// Device lacks StrongBox; retry without StrongBox request
			val fallbackSpec = KeyGenParameterSpec.Builder(
				KEY_ALIAS,
				PURPOSE_ENCRYPT or PURPOSE_DECRYPT
			).apply {
				setKeySize(256)
				setBlockModes(BLOCK_MODE_GCM)
				setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
				setIsStrongBoxBacked(false)
			}.build()

			val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, "AndroidKeyStore")
			keyGenerator.init(fallbackSpec)
			keyGenerator.generateKey()
		}
	}

	private fun getKeyEncryptionKey(): SecretKey {
		return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
	}

	/**
	 * Encrypts plain using the StrongBox-backed AES key.
	 *
	 * @param plain      The plain text byte array to be encrypted
	 * @throws SecurityException if authentication is required but missing/expired
	 */
	private fun encryptWithHardwareBackedKey(plain: ByteArray): ByteArray {
		val secretKey = getKeyEncryptionKey()

		// Prepare cipher
		val cipher = Cipher.getInstance(AES_GCM_MODE)
		cipher.init(Cipher.ENCRYPT_MODE, secretKey)

		// returns cipherText || authTag (tag at the end)
		val cipherText = cipher.doFinal(plain)

		// Concatenate IV + cipherText(+tag)
		return ByteArray(cipher.iv.size + cipherText.size).apply {
			System.arraycopy(cipher.iv, 0, this, 0, cipher.iv.size)
			System.arraycopy(cipherText, 0, this, cipher.iv.size, cipherText.size)
		}
	}

	/**
	 * Decrypts data produced by [encryptWithHardwareBackedKey].
	 *
	 * @param encrypted  The byte array formatted as IV ∥ cipherText ∥ authTag
	 * @return           The original plaintext bytes
	 * @throws SecurityException if authentication is required but missing/expired
	 * @throws javax.crypto.AEADBadTagException on tampering or wrong key
	 */
	private fun decryptWithHardwareBackedKey(encrypted: ByteArray): ByteArray {
		require(encrypted.size > IV_LENGTH_BYTES) { "Ciphertext too short" }

		val secretKey = getKeyEncryptionKey()

		// Split encrypted bytes into parts
		val iv = encrypted.copyOfRange(0, IV_LENGTH_BYTES)
		val cipherAndTag = encrypted.copyOfRange(IV_LENGTH_BYTES, encrypted.size)

		// Init cipher for decryption
		val cipher = Cipher.getInstance(AES_GCM_MODE)
		val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
		cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

		// returns plaintext, or throws if auth fails
		return cipher.doFinal(cipherAndTag)
	}

	private fun dSaltFile(): File = File(appContext.filesDir, DSALT_FILENAME)

	companion object {
		private const val KEY_ALIAS = "snapsafe_kek"
		private const val AES_GCM_MODE = "AES/GCM/NoPadding"
		private const val IV_LENGTH_BYTES = 12            // 96-bit IV recommended for GCM
		private const val TAG_LENGTH_BITS = 128            // 16-byte tag appended automatically
		private const val DSALT_SIZE = 64
		private const val DSALT_FILENAME = "dSalt"
	}
}
