package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.security.schemes.EncryptionScheme
import timber.log.Timber

class SecurityResetUseCase(
	private val authManager: AuthorizationRepository,
	private val imageManager: SecureImageRepository,
	private val encryptionScheme: EncryptionScheme,
) {
	suspend fun reset() {
		authManager.securityFailureReset()
		imageManager.securityFailureReset()
		encryptionScheme.securityFailureReset()
		Timber.d("Security Reset Complete!")
	}
}