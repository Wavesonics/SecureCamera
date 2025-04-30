package com.darkrockstudios.app.securecamera.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA512
import dev.whyoleg.cryptography.operations.Hasher
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
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

		val SESSION_TIMEOUT_1_MIN = 1.minutes.inWholeMilliseconds
		val SESSION_TIMEOUT_5_MIN = 5.minutes.inWholeMilliseconds
		val SESSION_TIMEOUT_10_MIN = 10.minutes.inWholeMilliseconds
		val SESSION_TIMEOUT_DEFAULT = SESSION_TIMEOUT_5_MIN
	}

	private val hasher: Hasher = CryptographyProvider.Default.get(SHA512).hasher()

	@OptIn(ExperimentalStdlibApi::class)
	private suspend fun getCipherKey(): String {
		val preferences = dataStore.data.first()

		return preferences[SYMMETRIC_CIPHER_KEY] ?: run {
			val newKey = CryptographyRandom.nextBytes(128).toHexString()
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
	 * Get the app PIN
	 */
	val appPin: Flow<String?> = dataStore.data
		.map { preferences ->
			preferences[APP_PIN]
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
	suspend fun setAppPin(pin: String) {
		val hashedPin = hashPin(pin)
		dataStore.edit { preferences ->
			preferences[APP_PIN] = Json.encodeToString(HashedPin.serializer(), hashedPin)
		}
	}

	suspend fun getHashedPin(): HashedPin? {
		val preferences = dataStore.data.firstOrNull() ?: return null
		val storedPinJson = preferences[APP_PIN] ?: return null
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
	suspend fun hashPin(pin: String): HashedPin {
		val salt = CryptographyRandom.nextBytes(16).toHexString()
		val saltedPin = pin + salt
		val hash = hasher.hash(saltedPin.encodeToByteArray()).toHexString()
		return HashedPin(hash, salt)
	}

	@OptIn(ExperimentalStdlibApi::class)
	suspend fun verifyPin(inputPin: String, storedHash: HashedPin): Boolean {
		val saltedInputPin = inputPin + storedHash.salt
		val hashedInputPin = hasher.hash(saltedInputPin.encodeToByteArray()).toHexString()
		return hashedInputPin == storedHash.hash
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
	 * Get the failed PIN attempts count
	 */
	val failedPinAttempts: Flow<Int> = dataStore.data
		.map { preferences ->
			preferences[FAILED_PIN_ATTEMPTS]?.toIntOrNull() ?: 0
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
	 * Get the timestamp of the last failed PIN attempt
	 */
	val lastFailedAttemptTimestamp: Flow<Long> = dataStore.data
		.map { preferences ->
			preferences[LAST_FAILED_ATTEMPT_TIMESTAMP]?.toLongOrNull() ?: 0L
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

	/**
	 * Set the Poison Pill PIN
	 */
	@OptIn(ExperimentalEncodingApi::class)
	suspend fun setPoisonPillPin(pin: String) {
		val hashedPin = hashPin(pin)
		val cipherKey = getCipherKey()
		dataStore.edit { preferences ->
			preferences[POISON_PILL_PIN] = Json.encodeToString(HashedPin.serializer(), hashedPin)
			preferences[POISON_PILL_PIN_PLAIN] = XorCipher.encrypt(pin, cipherKey)
		}
	}

	@OptIn(ExperimentalEncodingApi::class)
	suspend fun getPlainPoisonPillPin(): String? {
		val preferences = dataStore.data.firstOrNull() ?: return null
		val storedPin = preferences[POISON_PILL_PIN_PLAIN] ?: return null
		val cipherKey = getCipherKey()
		return XorCipher.decrypt(storedPin, cipherKey)
	}

	/**
	 * Get the hashed Poison Pill PIN
	 */
	suspend fun getHashedPoisonPillPin(): HashedPin? {
		val preferences = dataStore.data.firstOrNull() ?: return null
		val storedPinJson = preferences[POISON_PILL_PIN] ?: return null
		return try {
			Json.decodeFromString(HashedPin.serializer(), storedPinJson)
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
		val poisonPillPin = getHashedPoisonPillPin() ?: return
		dataStore.edit { preferences ->
			preferences[APP_PIN] = Json.encodeToString(HashedPin.serializer(), poisonPillPin)
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
			preferences.remove(SYMMETRIC_CIPHER_KEY)
		}
	}
}

@Serializable
data class HashedPin(val hash: String, val salt: String)
