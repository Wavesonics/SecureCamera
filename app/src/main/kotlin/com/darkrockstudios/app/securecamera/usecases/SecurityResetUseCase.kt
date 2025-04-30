package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository

class SecurityResetUseCase(
	private val authManager: AuthorizationRepository,
	private val imageManager: SecureImageRepository,
) {
	suspend fun reset() {
		authManager.securityFailureReset()
		imageManager.securityFailureReset()
	}
}