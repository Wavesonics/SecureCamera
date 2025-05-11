package com.darkrockstudios.app.securecamera.security.pin

import com.darkrockstudios.app.securecamera.preferences.*
import com.darkrockstudios.app.securecamera.security.SchemeConfig
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import com.lambdapioneer.argon2kt.Argon2Mode
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.ExperimentalEncodingApi

class PinRepositorySoftware(
	private val dataSource: AppPreferencesDataSource,
) : PinRepository {
	private val argon2Kt = Argon2Kt()

	override suspend fun setAppPin(pin: String, schemeConfig: SchemeConfig) {
		val hashedPin: HashedPin = hashPin(pin)
		val key = dataSource.getCipherKey()

		val cipheredHash = XorCipher.encrypt(Json.Default.encodeToString(hashedPin), key)
		val config = Json.Default.encodeToString(schemeConfig)
		dataSource.setAppPin(cipheredHash, config)
	}

	override suspend fun getHashedPin(): HashedPin? {
		val key = dataSource.getCipherKey()
		val storedPinJson = dataSource.getCipheredPin()?.let { XorCipher.decrypt(it, key) } ?: return null
		return Json.decodeFromString(HashedPin.serializer(), storedPinJson)
	}

	@OptIn(ExperimentalStdlibApi::class)
	override fun hashPin(pin: String): HashedPin {
		val salt = CryptographyRandom.nextBytes(16)
		val hashResult: Argon2KtResult = argon2Kt.hash(
			mode = Argon2Mode.ARGON2_I,
			password = pin.toByteArray(Charsets.UTF_8),
			salt = salt,
			tCostInIterations = ARGON_ITTERATIONS,
			mCostInKibibyte = ARGON_COST,
		)

		return HashedPin(
			hashResult.encodedOutputAsString().toByteArray().base64EncodeUrlSafe(),
			salt.base64EncodeUrlSafe()
		)
	}

	@OptIn(ExperimentalStdlibApi::class)
	override fun verifyPin(inputPin: String, storedHash: HashedPin): Boolean {
		return argon2Kt.verify(
			mode = Argon2Mode.ARGON2_I,
			encoded = String(storedHash.hash.base64DecodeUrlSafe()),
			password = inputPin.toByteArray(),
		)
	}

	/**
	 * Set the Poison Pill PIN
	 */
	@OptIn(ExperimentalEncodingApi::class)
	override suspend fun setPoisonPillPin(pin: String) {
		val hashedPin: HashedPin = hashPin(pin)
		val cipherKey = dataSource.getCipherKey()

		val cipheredHashedPpp = XorCipher.encrypt(Json.encodeToString(hashedPin), cipherKey)
		val cipheredPlainPpp = XorCipher.encrypt(pin, cipherKey)

		dataSource.setPoisonPillPin(cipheredHashedPpp, cipheredPlainPpp)
	}

	@OptIn(ExperimentalEncodingApi::class)
	override suspend fun getPlainPoisonPillPin(): String? {
		val encryptedStoredPin = dataSource.getPlainPoisonPillPin() ?: return null
		return XorCipher.decrypt(encryptedStoredPin, dataSource.getCipherKey())
	}

	/**
	 * Get the hashed Poison Pill PIN
	 */
	override suspend fun getHashedPoisonPillPin(): HashedPin? {
		val cipherKey = dataSource.getCipherKey()
		val storedPinJson = dataSource.getHashedPoisonPillPin()?.let { XorCipher.decrypt(it, cipherKey) } ?: return null
		return Json.decodeFromString(storedPinJson)
	}

	/**
	 * Activate the Poison Pill - replaces the regular PIN with the Poison Pill PIN
	 */
	override suspend fun activatePoisonPill() {
		val poisonPillPin = getHashedPoisonPillPin() ?: error("No PPP")
		val cipherKey = dataSource.getCipherKey()
		val ciphered = XorCipher.encrypt(Json.encodeToString(poisonPillPin), cipherKey)

		dataSource.activatePoisonPill(ciphered)

		removePoisonPillPin()
	}

	/**
	 * Remove the Poison Pill PIN
	 */
	override suspend fun removePoisonPillPin() {
		dataSource.removePoisonPillPin()
	}

	companion object {
		private const val ARGON_ITTERATIONS = 5
		private const val ARGON_COST = 65536
	}
}