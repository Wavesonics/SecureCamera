package com.darkrockstudios.app.securecamera.auth

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.darkrockstudios.app.securecamera.BaseViewModel
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.gallery.vibrateDevice
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.darkrockstudios.app.securecamera.usecases.SecurityResetUseCase
import com.darkrockstudios.app.securecamera.usecases.VerifyPinUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PinVerificationViewModel(
	private val appContext: Context,
	private val authManager: AuthorizationRepository,
	private val imageManager: SecureImageRepository,
	private val securityResetUseCase: SecurityResetUseCase,
	private val verifyPinUseCase: VerifyPinUseCase
) : BaseViewModel<PinVerificationUiState>() {

	override fun createState() = PinVerificationUiState()

	init {
		loadInitialState()
	}

	private fun loadInitialState() {
		viewModelScope.launch {
			val failedAttempts = authManager.getFailedAttempts()

			val remainingBackoff = authManager.calculateRemainingBackoffSeconds()
			val isBackoffActive = remainingBackoff > 0

			_uiState.update {
				it.copy(
					failedAttempts = failedAttempts,
					remainingBackoffSeconds = remainingBackoff,
					isBackoffActive = isBackoffActive,
					error = if (isBackoffActive) PinVerificationError.INVALID_PIN else PinVerificationError.NONE
				)
			}

			if (isBackoffActive) {
				startBackoffCountdown()
			}
		}
	}

	private fun startBackoffCountdown() {
		viewModelScope.launch {
			while (uiState.value.remainingBackoffSeconds > 0) {
				delay(1000)
				_uiState.update {
					it.copy(
						remainingBackoffSeconds = it.remainingBackoffSeconds - 1
					)
				}
			}

			_uiState.update {
				it.copy(
					isBackoffActive = false
				)
			}
		}
	}

	fun validatePin(newPin: String): Boolean {
		return if (newPin.length <= pinSize.max() && newPin.all { char -> char.isDigit() }) {
			clearError()
			true
		} else {
			false
		}
	}

	fun clearError() {
		_uiState.update {
			it.copy(
				error = PinVerificationError.NONE
			)
		}
	}

	fun verify(pin: String, returnRoute: String, onNavigate: (String) -> Unit, onFailure: () -> Unit) {
		val currentState = uiState.value

		if (pin.isBlank()) {
			_uiState.update { it.copy(error = PinVerificationError.EMPTY_PIN) }
			return
		}

		if (currentState.isBackoffActive) {
			return
		}

		_uiState.update { it.copy(isVerifying = true) }

		viewModelScope.launch(Dispatchers.Default) {
			val isValid = verifyPinUseCase.verifyPin(pin)

			if (isValid) {
				authManager.resetFailedAttempts()

				withContext(Dispatchers.Main) {
					_uiState.update {
						it.copy(
							isVerifying = false,
							failedAttempts = 0
						)
					}

					onNavigate(returnRoute)
				}
			} else {
				val newFailedAttempts = authManager.incrementFailedAttempts()
				val remainingBackoff = authManager.calculateRemainingBackoffSeconds()
				val isBackoffActive = remainingBackoff > 0

				withContext(Dispatchers.Main) {
					vibrateDevice(appContext)

					_uiState.update {
						it.copy(
							isVerifying = false,
							failedAttempts = newFailedAttempts,
							remainingBackoffSeconds = remainingBackoff,
							isBackoffActive = isBackoffActive,
							error = PinVerificationError.INVALID_PIN
						)
					}

					if (isBackoffActive) {
						startBackoffCountdown()
					}

					if (newFailedAttempts >= AuthorizationRepository.MAX_FAILED_ATTEMPTS) {
						// Nuke it all
						securityResetUseCase.reset()
						showMessage(appContext.getString(R.string.pin_verification_all_data_deleted))
						onNavigate(AppDestinations.INTRODUCTION_ROUTE)
					}

					onFailure()
				}
			}
		}
	}

	fun invalidateSession() {
		imageManager.evictKey()
		imageManager.thumbnailCache.clear()
		authManager.revokeAuthorization()
	}
}

enum class PinVerificationError {
	EMPTY_PIN,
	INVALID_PIN,
	NONE
}

data class PinVerificationUiState(
	val error: PinVerificationError = PinVerificationError.NONE,
	val isVerifying: Boolean = false,
	val failedAttempts: Int = 0,
	val isBackoffActive: Boolean = false,
	val remainingBackoffSeconds: Int = 0
)
