package com.darkrockstudios.app.securecamera.security.pin

import com.darkrockstudios.app.securecamera.preferences.*
import com.darkrockstudios.app.securecamera.security.SchemeConfig
import com.darkrockstudios.app.securecamera.security.schemes.EncryptionScheme
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import com.lambdapioneer.argon2kt.Argon2Mode
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PinRepositoryHardware(
	private val dataSource: AppPreferencesDataSource,
	private val encryptionScheme: EncryptionScheme,
) : PinRepository {
	private val argon2Kt = Argon2Kt()

	override suspend fun setAppPin(
		pin: String,
		schemeConfig: SchemeConfig
	) {
		val hashedPin: HashedPin = hashPin(pin)
		val hashedPinJson = Json.Default.encodeToString(hashedPin)

		val cipheredHash =
			encryptionScheme.encryptWithKeyAlias(hashedPinJson.toByteArray(), PIN_KEY_ALIAS).base64Encode()

		val config = Json.Default.encodeToString(schemeConfig)
		dataSource.setAppPin(cipheredHash, config)
	}

	override suspend fun getHashedPin(): HashedPin? {
		val cipheredPinBytes = dataSource.getCipheredPin()?.base64Decode() ?: return null
		val hashedPinJson = String(encryptionScheme.decryptWithKeyAlias(cipheredPinBytes, PIN_KEY_ALIAS))
		return Json.decodeFromString(HashedPin.serializer(), hashedPinJson)
	}

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

	override fun verifyPin(
		inputPin: String,
		storedHash: HashedPin
	): Boolean {
		return argon2Kt.verify(
			mode = Argon2Mode.ARGON2_I,
			encoded = String(storedHash.hash.base64DecodeUrlSafe()),
			password = inputPin.toByteArray(),
		)
	}

	override suspend fun setPoisonPillPin(pin: String) {
		val hashedPin: HashedPin = hashPin(pin)

		val json = Json.encodeToString(hashedPin)
		val cipheredHashedPpp = encryptionScheme.encryptWithKeyAlias(json.toByteArray(), PIN_KEY_ALIAS).base64Encode()
		val cipheredPlainPpp = encryptionScheme.encryptWithKeyAlias(pin.toByteArray(), PIN_KEY_ALIAS).base64Encode()

		dataSource.setPoisonPillPin(cipheredHashedPpp, cipheredPlainPpp)
	}

	override suspend fun getPlainPoisonPillPin(): String? {
		val encryptedStoredPin = dataSource.getPlainPoisonPillPin()?.base64Decode() ?: return null
		return String(encryptionScheme.decryptWithKeyAlias(encryptedStoredPin, PIN_KEY_ALIAS))
	}

	override suspend fun getHashedPoisonPillPin(): HashedPin? {
		val encryptedPinBytes = dataSource.getHashedPoisonPillPin()?.base64Decode() ?: return null
		val storedPinJson = String(encryptionScheme.decryptWithKeyAlias(encryptedPinBytes, PIN_KEY_ALIAS))
		return Json.decodeFromString(storedPinJson)
	}

	/**
	 * Activate the Poison Pill - replaces the regular PIN with the Poison Pill PIN
	 */
	override suspend fun activatePoisonPill() {
		val poisonPillPin = getHashedPoisonPillPin() ?: error("No PPP")
		val poisonPillPinJson = Json.encodeToString(poisonPillPin)

		val ciphered =
			encryptionScheme.encryptWithKeyAlias(poisonPillPinJson.toByteArray(), PIN_KEY_ALIAS).base64Encode()
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
		const val PIN_KEY_ALIAS = "pin_key"
		const val ARGON_ITTERATIONS = 5
		const val ARGON_COST = 65536
	}
}