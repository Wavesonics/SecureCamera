package com.darkrockstudios.app.securecamera.auth

import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.preferences.HashedPin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

/**
 * Manages user authorization state, including PIN verification and session expiration.
 */
class AuthorizationRepository(
	private val preferencesManager: AppPreferencesDataSource
) {
	companion object {
		const val MAX_FAILED_ATTEMPTS = 10
	}

	private val _isAuthorized = MutableStateFlow(false)
	val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

	private var lastAuthTimeMs: Long = 0

	var securityPin: SecurityPin? = null
		private set

	suspend fun securityFailureReset() {
		preferencesManager.securityFailureReset()
	}

	suspend fun activatePoisonPill() {
		preferencesManager.activatePoisonPill()
	}

	/**
	 * Gets the current number of failed PIN attempts
	 * @return The number of failed attempts
	 */
	suspend fun getFailedAttempts(): Int {
		return preferencesManager.getFailedPinAttempts()
	}

	/**
	 * Sets the number of failed PIN attempts
	 * @param count The number of failed attempts to set
	 */
	suspend fun setFailedAttempts(count: Int) {
		preferencesManager.setFailedPinAttempts(count)
	}

	/**
	 * Increments the failed PIN attempts counter, stores the current timestamp, and returns the new count
	 * @return The updated number of failed attempts
	 */
	suspend fun incrementFailedAttempts(): Int {
		val currentCount = getFailedAttempts()
		val newCount = currentCount + 1
		setFailedAttempts(newCount)

		// Store the current timestamp as the last failed attempt time
		preferencesManager.setLastFailedAttemptTimestamp(System.currentTimeMillis())

		return newCount
	}

	/**
	 * Gets the timestamp of the last failed PIN attempt
	 * @return The timestamp of the last failed attempt
	 */
	suspend fun getLastFailedAttemptTimestamp(): Long {
		return preferencesManager.getLastFailedAttemptTimestamp()
	}

	/**
	 * Calculates the remaining backoff time in seconds based on the number of failed attempts and the last failed attempt timestamp
	 * @return The remaining backoff time in seconds, or 0 if no backoff is needed
	 */
	suspend fun calculateRemainingBackoffSeconds(): Int {
		val failedAttempts = getFailedAttempts()
		if (failedAttempts <= 0) {
			return 0
		}

		val lastFailedTimestamp = getLastFailedAttemptTimestamp()
		if (lastFailedTimestamp <= 0) {
			return 0
		}

		val backoffTime = (2 * Math.pow(2.0, failedAttempts - 1.0)).toInt()
		val elapsedSeconds = ((System.currentTimeMillis() - lastFailedTimestamp) / 1000).toInt()
		val remainingSeconds = backoffTime - elapsedSeconds

		return if (remainingSeconds > 0) remainingSeconds else 0
	}

	/**
	 * Resets the failed PIN attempts counter to zero and clears the last failed attempt timestamp
	 */
	suspend fun resetFailedAttempts() {
		setFailedAttempts(0)
		preferencesManager.setLastFailedAttemptTimestamp(0)
	}

	/**
	 * Verifies the PIN and updates the authorization state if successful.
	 * @param pin The PIN entered by the user
	 * @return True if the PIN is correct, false otherwise
	 */
	suspend fun verifyPin(pin: String): Boolean {
		val hashedPin = preferencesManager.getHashedPin()
		val isValid = preferencesManager.verifySecurityPin(pin)
		if (isValid && hashedPin != null) {
			authorizeSession()
			securityPin = SecurityPin(
				plainPin = pin,
				hashedPin = hashedPin,
			)
			// Reset failed attempts counter on successful verification
			resetFailedAttempts()
		}
		return isValid
	}

	/**
	 * Marks the current session as authorized and updates the last authentication time.
	 */
	private fun authorizeSession() {
		lastAuthTimeMs = System.currentTimeMillis()
		_isAuthorized.value = true
	}

	/**
	 * Checks if the current session is still valid or has expired.
	 * @return True if the session is valid, false if it has expired
	 */
	fun checkSessionValidity(): Boolean = runBlocking {
		if (!_isAuthorized.value) {
			return@runBlocking false
		}

		val sessionTimeoutMs = preferencesManager.getSessionTimeout()
		val currentTime = System.currentTimeMillis()
		val sessionValid = (currentTime - lastAuthTimeMs) < sessionTimeoutMs

		if (!sessionValid) {
			_isAuthorized.value = false
		}

		return@runBlocking sessionValid
	}

	/**
	 * Explicitly revokes the current authorization session.
	 */
	fun revokeAuthorization() {
		_isAuthorized.value = false
		lastAuthTimeMs = 0
	}

	data class SecurityPin(
		val plainPin: String,
		val hashedPin: HashedPin,
	)
}
