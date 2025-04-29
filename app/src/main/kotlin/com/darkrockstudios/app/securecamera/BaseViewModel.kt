package com.darkrockstudios.app.securecamera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkrockstudios.app.securecamera.ui.UiEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BaseViewModel<S : Any> : ViewModel() {
	protected val _uiState = MutableStateFlow(createState())
	val uiState: StateFlow<S> = _uiState.asStateFlow()

	protected abstract fun createState(): S

	private val _events = MutableSharedFlow<UiEvent>()
	val events = _events.asSharedFlow()

	fun showMessage(message: String) {
		viewModelScope.launch {
			_events.emit(UiEvent.ShowSnack(message))
		}
	}
}