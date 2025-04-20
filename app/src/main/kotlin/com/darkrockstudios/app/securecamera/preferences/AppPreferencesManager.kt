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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * Manages app preferences using DataStore
 */
class AppPreferencesManager(private val context: Context) {
	companion object {
		private val HAS_COMPLETED_INTRO = booleanPreferencesKey("has_completed_intro")
		private val APP_PIN = stringPreferencesKey("app_pin")
		private val SANITIZE_FILE_NAME = booleanPreferencesKey("sanitize_file_name")
		private val SANITIZE_METADATA = booleanPreferencesKey("sanitize_metadata")
	}

	private val hasher: Hasher = CryptographyProvider.Default.get(SHA512).hasher()

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
	val appPin: Flow<String?> = context.dataStore.data
		.map { preferences ->
			preferences[APP_PIN]
		}

	/**
	 * Get the sanitize file name preference
	 */
	val sanitizeFileName: Flow<Boolean> = context.dataStore.data
		.map { preferences ->
			preferences[SANITIZE_FILE_NAME] ?: sanitizeFileNameDefault
		}
	val sanitizeFileNameDefault = true

	/**
	 * Get the sanitize metadata preference
	 */
	val sanitizeMetadata: Flow<Boolean> = context.dataStore.data
		.map { preferences ->
			preferences[SANITIZE_METADATA] ?: sanitizeMetadataDefault
		}
	val sanitizeMetadataDefault = true

	/**
	 * Set the introduction completion status
	 */
	suspend fun setIntroCompleted(completed: Boolean) {
		context.dataStore.edit { preferences ->
			preferences[HAS_COMPLETED_INTRO] = completed
		}
	}

	/**
	 * Set the app PIN
	 */
	suspend fun setAppPin(pin: String) {
		val hashedPin = hashPin(pin)
		context.dataStore.edit { preferences ->
			preferences[APP_PIN] = Json.encodeToString(HashedPin.serializer(), hashedPin)
		}
	}

	suspend fun getHashedPin(): HashedPin? {
		val preferences = context.dataStore.data.firstOrNull() ?: return null
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
		context.dataStore.edit { preferences ->
			preferences[SANITIZE_FILE_NAME] = sanitize
		}
	}

	/**
	 * Set the sanitize metadata preference
	 */
	suspend fun setSanitizeMetadata(sanitize: Boolean) {
		context.dataStore.edit { preferences ->
			preferences[SANITIZE_METADATA] = sanitize
		}
	}
}

@Serializable
data class HashedPin(val hash: String, val salt: String)
