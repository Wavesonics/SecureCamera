package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.security.schemes.EncryptionScheme

class VerifyPinUseCase(
	private val authManager: AuthorizationRepository,
	private val imageManager: SecureImageRepository,
	private val preferencesManager: AppPreferencesDataSource,
	private val encryptionScheme: EncryptionScheme,
) {
	suspend fun verifyPin(pin: String): Boolean {
		if (preferencesManager.hasPoisonPillPin() && preferencesManager.verifyPoisonPillPin(pin)) {
			encryptionScheme.activatePoisonPill(oldPin = preferencesManager.getHashedPin())
			imageManager.activatePoisonPill()
			authManager.activatePoisonPill()
		}

		val hashedPin = authManager.verifyPin(pin)
		return if (hashedPin != null) {
			encryptionScheme.deriveAndCacheKey(pin, hashedPin)
			true
		} else {
			false
		}
	}
}
