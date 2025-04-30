package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager

class VerifyPinUseCase(
	private val authManager: AuthorizationManager,
	private val imageManager: SecureImageRepository,
	private val preferencesManager: AppPreferencesManager,
) {
	suspend fun verifyPin(pin: String): Boolean {
		if (preferencesManager.hasPoisonPillPin() && preferencesManager.verifyPoisonPillPin(pin)) {
			imageManager.activatePoisonPill()
			authManager.activatePoisonPill()
		}

		return authManager.verifyPin(pin)
	}
}
