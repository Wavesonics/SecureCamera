package com.darkrockstudios.app.securecamera.security

import com.darkrockstudios.app.securecamera.preferences.HashedPin
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Implementation of EncryptionScheme that uses AES encryption with keys derived from a PIN.
 */
open class SoftwareEncryptionScheme(
	protected val deviceInfo: DeviceInfo,
) : EncryptionScheme {
	private val provider = CryptographyProvider.Default
	protected var key: ShardedKey? = null
	protected val keyMutex = Mutex()
	protected val defaultKeyParams = KeyParams()

	override fun evictKey() {
		// Zero the mem before releasing it
		key?.evict()
		key = null
	}

	override suspend fun createKey() {
	}

	override suspend fun encryptToFile(plain: ByteArray, targetFile: File) {
		val keyBytes = getDerivedKey()
		encryptToFile(plain = plain, keyBytes = keyBytes, targetFile = targetFile)
	}

	override suspend fun encryptToFile(plain: ByteArray, keyBytes: ByteArray, targetFile: File) {
		val aesKey = provider
			.get(AES.GCM)
			.keyDecoder()
			.decodeFromByteArray(AES.Key.Format.RAW, keyBytes)
		val encryptedBytes = aesKey.cipher().encrypt(plain)
		targetFile.writeBytes(encryptedBytes)
	}

	/**
	 * Derives the encryption key from the user's PIN, then decrypts encryptedFile and returns the plainText bytes
	 */
	override suspend fun decryptFile(encryptedFile: File): ByteArray {
		val encryptedBytes = encryptedFile.readBytes()
		val keyBytes = getDerivedKey()

		val aesKey = provider.get(AES.GCM).keyDecoder()
			.decodeFromByteArray(AES.Key.Format.RAW, keyBytes)

		return aesKey.cipher().decrypt(encryptedBytes)
	}

	override suspend fun deriveAndCacheKey(plainPin: String, hashedPin: HashedPin) {
		keyMutex.withLock {
			// Early out for race conditions
			key?.let { return@withLock }

			val derivedKey = deriveKey(plainPin, hashedPin)
			key = ShardedKey(derivedKey)
		}
	}

	override suspend fun getDerivedKey(): ByteArray {
		return keyMutex.withLock {
			key?.reconstructKey() ?: error("Key has not been derived!")
		}
	}

	@OptIn(ExperimentalEncodingApi::class)
	override suspend fun deriveKey(
		plainPin: String,
		hashedPin: HashedPin,
	): ByteArray {
		val secretDeriver = provider.get(PBKDF2).secretDerivation(
			digest = SHA256,
			iterations = defaultKeyParams.iterations,
			outputSize = defaultKeyParams.outputSize,
			salt = hashedPin.salt.toByteArray()
		)

		val deviceId = deviceInfo.getDeviceIdentifier()
		val encodedDeviceId = Base64.Default.encode(deviceId)

		val dekInput = plainPin.toByteArray(Charsets.UTF_8) + encodedDeviceId.toByteArray(Charsets.UTF_8)

		return secretDeriver.deriveSecret(dekInput).toByteArray()
	}
}