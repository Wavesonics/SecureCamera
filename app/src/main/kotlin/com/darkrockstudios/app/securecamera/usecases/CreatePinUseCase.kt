package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.security.SchemeConfig

class CreatePinUseCase(
	private val preferencesDataSource: AppPreferencesDataSource,
	private val authorizationRepository: AuthorizationRepository,
) {
	suspend fun createPin(pin: String, schemeConfig: SchemeConfig): Boolean {
		preferencesDataSource.setAppPin(pin, schemeConfig)
		authorizationRepository.createKey()
		authorizationRepository.verifyPin(pin)

		return true
	}
}