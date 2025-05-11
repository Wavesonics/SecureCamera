package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.security.pin.PinRepository

class RemovePoisonPillIUseCase(
	private val pinRepository: PinRepository,
	private val imageManager: SecureImageRepository,
) {
	suspend fun removePoisonPill() {
		pinRepository.removePoisonPillPin()
		imageManager.removeAllDecoyPhotos()
	}
}