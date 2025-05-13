package com.darkrockstudios.app.securecamera.introduction

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.viewModelScope
import com.darkrockstudios.app.securecamera.BaseViewModel
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.security.HardwareSchemeConfig
import com.darkrockstudios.app.securecamera.security.SecurityLevel
import com.darkrockstudios.app.securecamera.security.SecurityLevelDetector
import com.darkrockstudios.app.securecamera.security.SoftwareSchemeConfig
import com.darkrockstudios.app.securecamera.usecases.CreatePinUseCase
import com.darkrockstudios.app.securecamera.usecases.PinSizeUseCase
import com.darkrockstudios.app.securecamera.usecases.PinStrengthCheckUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class IntroductionViewModel(
	private val appContext: Context,
	private val preferencesDataSource: AppPreferencesDataSource,
	private val securityLevelDetector: SecurityLevelDetector,
	private val pinStrengthCheck: PinStrengthCheckUseCase,
	private val createPinUseCase: CreatePinUseCase,
	private val pinSizeUseCase: PinSizeUseCase,
) : BaseViewModel<IntroductionUiState>() {

	override fun createState() = IntroductionUiState(
		securityLevel = securityLevelDetector.detectSecurityLevel(),
		slides = createSlides(),
		pinSize = pinSizeUseCase.getPinSizeRange()
	)

	private val _skipToPage = MutableSharedFlow<Int>()
	val skipToPage = _skipToPage.asSharedFlow()

	private fun createSlides(): List<IntroductionSlide> {
		return listOf(
			IntroductionSlide(
				icon = Icons.Filled.Camera,
				title = appContext.getString(R.string.intro_slide0_title),
				description = appContext.getString(R.string.intro_slide0_description)
			),
			IntroductionSlide(
				icon = Icons.Filled.PrivacyTip,
				title = appContext.getString(R.string.intro_slide1_title),
				description = appContext.getString(R.string.intro_slide1_description)
			),
			IntroductionSlide(
				icon = Icons.Filled.Lock,
				title = appContext.getString(R.string.intro_slide2_title),
				description = appContext.getString(R.string.intro_slide2_description)
			),
			IntroductionSlide(
				icon = Icons.AutoMirrored.Filled.Send,
				title = appContext.getString(R.string.intro_slide3_title),
				description = appContext.getString(R.string.intro_slide3_description),
			),
			IntroductionSlide(
				icon = Icons.Filled.LocationOff,
				title = appContext.getString(R.string.intro_slide4_title),
				description = appContext.getString(R.string.intro_slide4_description),
			),
			IntroductionSlide(
				icon = Icons.Filled.MyLocation,
				title = appContext.getString(R.string.intro_slide5_title),
				description = appContext.getString(R.string.intro_slide5_description),
			),
		)
	}

	fun setPage(page: Int) {
		_uiState.update { it.copy(currentPage = page) }
	}

	suspend fun navigateToNextPage() {
		val currentPage = uiState.value.currentPage
		val totalPages = uiState.value.slides.size + 2
		if (currentPage < totalPages - 1) {
			_skipToPage.emit(currentPage + 1)
		}
	}

	suspend fun navigateToSecurity() {
		_skipToPage.emit(uiState.value.slides.size)
	}

	fun createPin(pin: String, confirmPin: String) {
		val config = when (uiState.value.securityLevel) {
			SecurityLevel.TEE, SecurityLevel.STRONGBOX -> HardwareSchemeConfig(
				requireBiometricAttestation = uiState.value.requireBiometrics,
				authTimeout = 5.minutes, // Hard coded for now
				ephemeralKey = uiState.value.ephemeralKey
			)

			SecurityLevel.SOFTWARE -> SoftwareSchemeConfig
		}

		val pinSize = pinSizeUseCase.getPinSizeRange()
		if (pin != confirmPin || (pin.length in pinSize).not()) {
			_uiState.update { it.copy(errorMessage = appContext.getString(R.string.pin_creation_error)) }
			return
		}

		val strongPin = pinStrengthCheck.isPinStrongEnough(pin)
		if (strongPin.not()) {
			_uiState.update { it.copy(errorMessage = appContext.getString(R.string.pin_creation_error_weak_pin)) }
			return
		}

		// Set loading state to true before starting PIN creation
		_uiState.update { it.copy(isCreatingPin = true, errorMessage = null) }

		viewModelScope.launch(Dispatchers.Default) {
			if (createPinUseCase.createPin(pin, config)) {
				preferencesDataSource.setIntroCompleted(true)
				_uiState.update { it.copy(pinCreated = true, isCreatingPin = false) }
			} else {
				_uiState.update { it.copy(isCreatingPin = false) }
			}
		}
	}

	fun toggleBiometricsRequired() {
		_uiState.update { it.copy(requireBiometrics = it.requireBiometrics.not()) }
	}

	fun toggleEphemeralKey() {
		_uiState.update { it.copy(ephemeralKey = it.ephemeralKey.not()) }
	}
}

data class IntroductionUiState(
	val slides: List<IntroductionSlide> = emptyList(),
	val errorMessage: String? = null,
	val pinCreated: Boolean = false,
	val securityLevel: SecurityLevel,
	val requireBiometrics: Boolean = false,
	val ephemeralKey: Boolean = false,
	val currentPage: Int = 0,
	val isCreatingPin: Boolean = false,
	val pinSize: IntRange,
)
