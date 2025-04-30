package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource

class VerifyPinUseCase(
	private val authManager: AuthorizationRepository,
	private val imageManager: SecureImageRepository,
	private val preferencesManager: AppPreferencesDataSource,
) {
	suspend fun verifyPin(pin: String): Boolean {
		if (preferencesManager.hasPoisonPillPin() && preferencesManager.verifyPoisonPillPin(pin)) {
			imageManager.activatePoisonPill()
			authManager.activatePoisonPill()
		}

		return authManager.verifyPin(pin)
	}
}
