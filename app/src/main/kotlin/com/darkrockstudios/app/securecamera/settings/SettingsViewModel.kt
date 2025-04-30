package com.darkrockstudios.app.securecamera.settings

import androidx.lifecycle.viewModelScope
import com.darkrockstudios.app.securecamera.BaseViewModel
import com.darkrockstudios.app.securecamera.LocationPermissionStatus
import com.darkrockstudios.app.securecamera.LocationRepository
import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource.Companion.SESSION_TIMEOUT_DEFAULT
import com.darkrockstudios.app.securecamera.usecases.SecurityResetUseCase
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
	private val preferencesManager: AppPreferencesDataSource,
	private val locationRepository: LocationRepository,
	private val securityResetUseCase: SecurityResetUseCase,
	private val authorizationRepository: AuthorizationRepository,
	private val imageManager: SecureImageRepository
) : BaseViewModel<SettingsUiState>() {

	override fun createState() = SettingsUiState()

	init {
		observePreferences()
		checkPoisonPillStatus()
		refreshLocationPermissionStatus()
	}

	private fun observePreferences() {
		viewModelScope.launch {
			preferencesManager.sanitizeFileName.collect { sanitizeFileName ->
				_uiState.update { it.copy(sanitizeFileName = sanitizeFileName) }
			}
		}

		viewModelScope.launch {
			preferencesManager.sanitizeMetadata.collect { sanitizeMetadata ->
				_uiState.update { it.copy(sanitizeMetadata = sanitizeMetadata) }
			}
		}

		viewModelScope.launch {
			preferencesManager.sessionTimeout.collect { sessionTimeout ->
				_uiState.update { it.copy(sessionTimeout = sessionTimeout) }
			}
		}

		viewModelScope.launch {
			locationRepository.locationPermissionStatus.collect { status ->
				_uiState.update { it.copy(locationPermissionStatus = status) }
			}
		}
	}

	private fun checkPoisonPillStatus() {
		viewModelScope.launch {
			val hasPoisonPillPin = preferencesManager.hasPoisonPillPin()
			_uiState.update { it.copy(hasPoisonPillPin = hasPoisonPillPin) }
		}
	}

	fun refreshLocationPermissionStatus() {
		viewModelScope.launch {
			locationRepository.refreshPermissionStatus()
		}
	}

	fun setSanitizeFileName(checked: Boolean) {
		viewModelScope.launch {
			preferencesManager.setSanitizeFileName(checked)
		}
	}

	fun setSanitizeMetadata(checked: Boolean) {
		viewModelScope.launch {
			preferencesManager.setSanitizeMetadata(checked)
		}
	}

	fun setSessionTimeout(timeout: Long) {
		viewModelScope.launch {
			preferencesManager.setSessionTimeout(timeout)
		}
	}

	fun showLocationDialog() {
		_uiState.update { it.copy(showLocationDialog = true) }
	}

	fun dismissLocationDialog() {
		_uiState.update { it.copy(showLocationDialog = false) }
	}

	fun showSecurityResetDialog() {
		_uiState.update { it.copy(showSecurityResetDialog = true) }
	}

	fun dismissSecurityResetDialog() {
		_uiState.update { it.copy(showSecurityResetDialog = false) }
	}

	fun performSecurityReset() {
		viewModelScope.launch {
			securityResetUseCase.reset()
			_uiState.update { it.copy(securityResetComplete = true) }
		}
	}

	fun showPoisonPillDialog() {
		_uiState.update { it.copy(showPoisonPillDialog = true) }
	}

	fun dismissPoisonPillDialog() {
		_uiState.update { it.copy(showPoisonPillDialog = false) }
	}

	fun showPoisonPillPinCreationDialog() {
		_uiState.update {
			it.copy(
				showPoisonPillDialog = false,
				showPoisonPillPinCreationDialog = true
			)
		}
	}

	fun dismissPoisonPillPinCreationDialog() {
		_uiState.update { it.copy(showPoisonPillPinCreationDialog = false) }
	}

	fun setPoisonPillPin(pin: String) {
		viewModelScope.launch {
			preferencesManager.setPoisonPillPin(pin)
			_uiState.update {
				it.copy(
					showPoisonPillPinCreationDialog = false,
					hasPoisonPillPin = true,
					showDecoyPhotoExplanationDialog = true
				)
			}
		}
	}

	fun dismissDecoyPhotoExplanationDialog() {
		_uiState.update { it.copy(showDecoyPhotoExplanationDialog = false) }
	}

	fun showRemovePoisonPillDialog() {
		_uiState.update { it.copy(showRemovePoisonPillDialog = true) }
	}

	fun dismissRemovePoisonPillDialog() {
		_uiState.update { it.copy(showRemovePoisonPillDialog = false) }
	}

	fun removePoisonPillPin() {
		viewModelScope.launch {
			preferencesManager.removePoisonPillPin()
			imageManager.removeAllDecoyPhotos()
			_uiState.update {
				it.copy(
					showRemovePoisonPillDialog = false,
					hasPoisonPillPin = false,
					poisonPillRemoved = true
				)
			}
		}
	}

	fun getCurrentPin(): String {
		return authorizationRepository.securityPin?.plainPin ?: ""
	}
}

data class SettingsUiState(
	val sanitizeFileName: Boolean = true,
	val sanitizeMetadata: Boolean = true,
	val sessionTimeout: Long = SESSION_TIMEOUT_DEFAULT,
	val locationPermissionStatus: LocationPermissionStatus = LocationPermissionStatus.DENIED,
	val hasPoisonPillPin: Boolean = false,
	val showLocationDialog: Boolean = false,
	val showSecurityResetDialog: Boolean = false,
	val showPoisonPillDialog: Boolean = false,
	val showPoisonPillPinCreationDialog: Boolean = false,
	val showDecoyPhotoExplanationDialog: Boolean = false,
	val showRemovePoisonPillDialog: Boolean = false,
	val securityResetComplete: Boolean = false,
	val poisonPillRemoved: Boolean = false
)