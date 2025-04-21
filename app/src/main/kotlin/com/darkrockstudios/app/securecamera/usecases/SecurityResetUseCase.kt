package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.camera.SecureImageManager

class SecurityResetUseCase(
	private val authManager: AuthorizationManager,
	private val imageManager: SecureImageManager,
) {
	suspend fun reset() {
		authManager.securityFailureReset()
		imageManager.securityFailureReset()
	}
}