package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository

class InvalidateSessionUseCase(
	private val imageManager: SecureImageRepository,
	private val authManager: AuthorizationRepository,
) {
	fun invalidateSession() {
		imageManager.evictKey()
		imageManager.thumbnailCache.clear()
		authManager.revokeAuthorization()
	}
}