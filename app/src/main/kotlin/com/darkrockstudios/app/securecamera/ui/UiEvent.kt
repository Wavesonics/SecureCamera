package com.darkrockstudios.app.securecamera.ui

sealed interface UiEvent {
	data class ShowSnack(
		val text: String,
		val action: String? = null
	) : UiEvent
}