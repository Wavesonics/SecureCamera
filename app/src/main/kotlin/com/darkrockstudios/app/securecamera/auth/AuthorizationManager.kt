package com.darkrockstudios.app.securecamera.auth

import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import com.darkrockstudios.app.securecamera.preferences.HashedPin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * Manages user authorization state, including PIN verification and session expiration.
 */
class AuthorizationManager(
	private val preferencesManager: AppPreferencesManager
) {
	companion object {
		private val DEFAULT_SESSION_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5)
	}

	private val _isAuthorized = MutableStateFlow(false)
	val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

	private var sessionTimeoutMs: Long = DEFAULT_SESSION_TIMEOUT_MS
	private var lastAuthTimeMs: Long = 0

	var securityPin: SecurityPin? = null
		private set

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
	fun checkSessionValidity(): Boolean {
		if (!_isAuthorized.value) {
			return false
		}

		val currentTime = System.currentTimeMillis()
		val sessionValid = (currentTime - lastAuthTimeMs) < sessionTimeoutMs

		if (!sessionValid) {
			_isAuthorized.value = false
		}

		return sessionValid
	}

	/**
	 * Sets a custom session timeout duration.
	 * @param timeoutMs The timeout duration in milliseconds
	 */
	fun setSessionTimeout(timeoutMs: Long) {
		sessionTimeoutMs = timeoutMs
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