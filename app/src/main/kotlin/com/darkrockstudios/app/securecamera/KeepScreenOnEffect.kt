package com.darkrockstudios.app.securecamera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun KeepScreenOnEffect() {
	val view = LocalView.current

	DisposableEffect(view) {
		view.keepScreenOn = true
		onDispose { view.keepScreenOn = false }
	}
}
