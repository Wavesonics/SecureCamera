package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.security.SchemeConfig
import com.darkrockstudios.app.securecamera.security.schemes.EncryptionScheme

class CreatePinUseCase(
	private val preferencesDataSource: AppPreferencesDataSource,
	private val authorizationRepository: AuthorizationRepository,
	private val encryptionScheme: EncryptionScheme,
) {
	suspend fun createPin(pin: String, schemeConfig: SchemeConfig): Boolean {
		preferencesDataSource.setAppPin(pin, schemeConfig)
		val hashedPin = authorizationRepository.verifyPin(pin)
		return if (hashedPin != null) {
			authorizationRepository.createKey(pin, hashedPin)
			encryptionScheme.deriveAndCacheKey(pin, hashedPin)
			true
		} else {
			false
		}
	}
}