package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager

class VerifyPinUseCase(
	private val authManager: AuthorizationManager,
	private val imageManager: SecureImageManager,
	private val preferencesManager: AppPreferencesManager,
) {
	suspend fun verifyPin(pin: String): Boolean {
		if (preferencesManager.hasPoisonPillPin() && preferencesManager.verifyPoisonPillPin(pin)) {
			authManager.activatePoisonPill()
			imageManager.activatePoisonPill()
		}

		return authManager.verifyPin(pin)
	}
}
