package com.darkrockstudios.app.securecamera.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import at.favre.lib.crypto.bcrypt.BCrypt
import com.darkrockstudios.app.securecamera.security.SchemeConfig
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.minutes

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * Manages app preferences using DataStore
 */
class AppPreferencesDataSource(
	private val context: Context,
	private val dataStore: DataStore<Preferences> = context.dataStore,
) {
	companion object {
		private val HAS_COMPLETED_INTRO = booleanPreferencesKey("has_completed_intro")
		private val APP_PIN = stringPreferencesKey("app_pin")
		private val POISON_PILL_PIN = stringPreferencesKey("poison_pill_pin")
		private val POISON_PILL_PIN_PLAIN = stringPreferencesKey("poison_pill_pin_plain")
		private val SANITIZE_FILE_NAME = booleanPreferencesKey("sanitize_file_name")
		private val SANITIZE_METADATA = booleanPreferencesKey("sanitize_metadata")
		private val FAILED_PIN_ATTEMPTS = stringPreferencesKey("failed_pin_attempts")
		private val LAST_FAILED_ATTEMPT_TIMESTAMP = stringPreferencesKey("last_failed_attempt_timestamp")
		private val SESSION_TIMEOUT = stringPreferencesKey("session_timeout")
		private val SYMMETRIC_CIPHER_KEY = stringPreferencesKey("symmetric_cipher_key")
		private val SCHEME_CONFIG_KEY = stringPreferencesKey("scheme_config")

		val SESSION_TIMEOUT_1_MIN = 1.minutes.inWholeMilliseconds
		val SESSION_TIMEOUT_5_MIN = 5.minutes.inWholeMilliseconds
		val SESSION_TIMEOUT_10_MIN = 10.minutes.inWholeMilliseconds
		val SESSION_TIMEOUT_DEFAULT = SESSION_TIMEOUT_5_MIN

		private const val BCRYPT_COST = 12
	}

	private val bcrypt = BCrypt.withDefaults()

	@OptIn(ExperimentalStdlibApi::class)
	private suspend fun getCipherKey(): String {
		val preferences = dataStore.data.first()

		return preferences[SYMMETRIC_CIPHER_KEY] ?: run {
			val newKey = CryptographyRandom.nextBytes(128).base64Encode()
			dataStore.edit { preferences ->
				preferences[SYMMETRIC_CIPHER_KEY] = newKey
			}
			newKey
		}
	}

	/**
	 * Check if the user has completed the introduction
	 */
	val hasCompletedIntro: Flow<Boolean?> = context.dataStore.data
		.map { preferences ->
			preferences[HAS_COMPLETED_INTRO] ?: false
		}

	/**
	 * Get the sanitize file name preference
	 */
	val sanitizeFileName: Flow<Boolean> = dataStore.data
		.map { preferences ->
			preferences[SANITIZE_FILE_NAME] ?: sanitizeFileNameDefault
		}
	val sanitizeFileNameDefault = true

	/**
	 * Get the sanitize metadata preference
	 */
	val sanitizeMetadata: Flow<Boolean> = dataStore.data
		.map { preferences ->
			preferences[SANITIZE_METADATA] ?: sanitizeMetadataDefault
		}
	val sanitizeMetadataDefault = true

	/**
	 * Get the session timeout preference
	 */
	val sessionTimeout: Flow<Long> = dataStore.data
		.map { preferences ->
			preferences[SESSION_TIMEOUT]?.toLongOrNull() ?: SESSION_TIMEOUT_DEFAULT
		}

	/**
	 * Set the introduction completion status
	 */
	suspend fun setIntroCompleted(completed: Boolean) {
		dataStore.edit { preferences ->
			preferences[HAS_COMPLETED_INTRO] = completed
		}
	}

	/**
	 * Set the app PIN
	 */
	suspend fun setAppPin(pin: String, schemeConfig: SchemeConfig) {
		val hashedPin: HashedPin = hashPin(pin)
		val key = getCipherKey()
		dataStore.edit { preferences ->
			preferences[APP_PIN] = XorCipher.encrypt(Json.encodeToString(hashedPin), key)
			preferences[SCHEME_CONFIG_KEY] = Json.encodeToString(schemeConfig)
		}
	}

	suspend fun getHashedPin(): HashedPin? {
		val key = getCipherKey()
		val preferences = dataStore.data.firstOrNull() ?: return null
		val storedPinJson = preferences[APP_PIN]?.let { XorCipher.decrypt(it, key) } ?: return null
		return try {
			Json.decodeFromString(HashedPin.serializer(), storedPinJson)
		} catch (e: Exception) {
			Timber.w(e, "verifySecurityPin failed to get hashed PIN")
			null
		}
	}

	suspend fun verifySecurityPin(pin: String): Boolean {
		val storedHashedPin = getHashedPin() ?: return false
		return verifyPin(pin, storedHashedPin)
	}

	@OptIn(ExperimentalStdlibApi::class)
	fun hashPin(pin: String): HashedPin {
		val salt = CryptographyRandom.nextBytes(16)
		val hash = bcrypt.hash(BCRYPT_COST, salt, pin.toByteArray(Charsets.UTF_8))
		return HashedPin(hash.base64Encode(), salt.base64Encode())
	}

	@OptIn(ExperimentalStdlibApi::class)
	fun verifyPin(inputPin: String, storedHash: HashedPin): Boolean {
		val result = BCrypt.verifyer().verify(inputPin.toCharArray(), storedHash.hash.base64Decode())
		return result.verified
	}

	/**
	 * Set the sanitize file name preference
	 */
	suspend fun setSanitizeFileName(sanitize: Boolean) {
		dataStore.edit { preferences ->
			preferences[SANITIZE_FILE_NAME] = sanitize
		}
	}

	/**
	 * Set the sanitize metadata preference
	 */
	suspend fun setSanitizeMetadata(sanitize: Boolean) {
		dataStore.edit { preferences ->
			preferences[SANITIZE_METADATA] = sanitize
		}
	}

	/**
	 * Get the current failed PIN attempts count
	 */
	suspend fun getFailedPinAttempts(): Int {
		return dataStore.data.firstOrNull()?.get(FAILED_PIN_ATTEMPTS)?.toIntOrNull() ?: 0
	}

	/**
	 * Set the failed PIN attempts count
	 */
	suspend fun setFailedPinAttempts(count: Int) {
		dataStore.edit { preferences ->
			preferences[FAILED_PIN_ATTEMPTS] = count.toString()
		}
	}

	/**
	 * Get the current timestamp of the last failed PIN attempt
	 */
	suspend fun getLastFailedAttemptTimestamp(): Long {
		return dataStore.data.firstOrNull()?.get(LAST_FAILED_ATTEMPT_TIMESTAMP)?.toLongOrNull() ?: 0L
	}

	/**
	 * Set the timestamp of the last failed PIN attempt
	 */
	suspend fun setLastFailedAttemptTimestamp(timestamp: Long) {
		dataStore.edit { preferences ->
			preferences[LAST_FAILED_ATTEMPT_TIMESTAMP] = timestamp.toString()
		}
	}

	/**
	 * Resets all user data and preferences when a security failure occurs.
	 * This deletes all stored preferences including PIN, intro completion status, and security settings.
	 */
	suspend fun securityFailureReset() {
		dataStore.edit { preferences ->
			preferences.clear()
		}
	}

	/**
	 * Get the current session timeout value
	 */
	suspend fun getSessionTimeout(): Long {
		val timeout =
			dataStore.data.firstOrNull()?.get(SESSION_TIMEOUT)?.toLongOrNull() ?: SESSION_TIMEOUT_DEFAULT
		return timeout
	}

	/**
	 * Set the session timeout value
	 */
	suspend fun setSessionTimeout(timeoutMs: Long) {
		dataStore.edit { preferences ->
			preferences[SESSION_TIMEOUT] = timeoutMs.toString()
		}
	}

	suspend fun getSchemeConfig(): SchemeConfig {
		val schemeJson = dataStore.data.firstOrNull()?.get(SCHEME_CONFIG_KEY) ?: error("No encryption scheme")
		return Json.decodeFromString<SchemeConfig>(schemeJson)
	}

	/**
	 * Set the Poison Pill PIN
	 */
	@OptIn(ExperimentalEncodingApi::class)
	suspend fun setPoisonPillPin(pin: String) {
		val hashedPin: HashedPin = hashPin(pin)
		val cipherKey = getCipherKey()
		dataStore.edit { preferences ->
			preferences[POISON_PILL_PIN] = XorCipher.encrypt(Json.encodeToString(hashedPin), cipherKey)
			preferences[POISON_PILL_PIN_PLAIN] = XorCipher.encrypt(pin, cipherKey)
		}
	}

	@OptIn(ExperimentalEncodingApi::class)
	suspend fun getPlainPoisonPillPin(): String? {
		val preferences = dataStore.data.firstOrNull() ?: return null
		val encryptedStoredPin = preferences[POISON_PILL_PIN_PLAIN] ?: return null
		return XorCipher.decrypt(encryptedStoredPin, getCipherKey())
	}

	/**
	 * Get the hashed Poison Pill PIN
	 */
	suspend fun getHashedPoisonPillPin(): HashedPin? {
		val preferences = dataStore.data.firstOrNull() ?: return null
		val cipherKey = getCipherKey()
		val storedPinJson = preferences[POISON_PILL_PIN]?.let { XorCipher.decrypt(it, cipherKey) } ?: return null
		return try {
			Json.decodeFromString(storedPinJson)
		} catch (e: Exception) {
			Timber.w(e, "getHashedPoisonPillPin failed to get hashed PIN")
			null
		}
	}

	/**
	 * Check if a Poison Pill PIN is set
	 */
	suspend fun hasPoisonPillPin(): Boolean {
		return getHashedPoisonPillPin() != null
	}

	/**
	 * Verify if the input PIN matches the Poison Pill PIN
	 */
	suspend fun verifyPoisonPillPin(pin: String): Boolean {
		val storedHashedPin = getHashedPoisonPillPin() ?: return false
		return verifyPin(pin, storedHashedPin)
	}

	/**
	 * Activate the Poison Pill - replaces the regular PIN with the Poison Pill PIN
	 */
	suspend fun activatePoisonPill() {
		val poisonPillPin = getHashedPoisonPillPin() ?: error("No PPP")
		val key = getCipherKey()
		dataStore.edit { preferences ->
			preferences[APP_PIN] = XorCipher.encrypt(Json.encodeToString(poisonPillPin), key)
		}
		removePoisonPillPin()
	}

	/**
	 * Remove the Poison Pill PIN
	 */
	suspend fun removePoisonPillPin() {
		dataStore.edit { preferences ->
			preferences.remove(POISON_PILL_PIN)
			preferences.remove(POISON_PILL_PIN_PLAIN)
		}
	}
}

@Serializable
data class HashedPin(val hash: String, val salt: String)

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.base64Encode(): String = Base64.Default.encode(this)
@OptIn(ExperimentalEncodingApi::class)
private fun String.base64Decode(): ByteArray = Base64.Default.decode(this)