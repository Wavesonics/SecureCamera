package com.darkrockstudios.app.securecamera

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
	private var capturePhoto = mutableStateOf<Boolean?>(null)
	private val locationRepository: LocationRepository by inject()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		clearShareDirectory(this)

		window.setFlags(
			WindowManager.LayoutParams.FLAG_SECURE,
			WindowManager.LayoutParams.FLAG_SECURE
		)

		enableEdgeToEdge()
		setContent {
			App(capturePhoto)
		}
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		return when (keyCode) {
			KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
				val curValue = capturePhoto.value
				capturePhoto.value = if (curValue != null) {
					!curValue
				} else {
					true
				}
				true
			}

			else -> super.onKeyDown(keyCode, event)
		}
	}

	override fun onResume() {
		super.onResume()
		// Refresh permission status when the app comes back to the foreground
		locationRepository.refreshPermissionStatus()
	}
}
