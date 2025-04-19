package com.darkrockstudios.app.securecamera

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
	private var capturePhoto = mutableStateOf<Boolean?>(null)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
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

	override fun onDestroy() {
		super.onDestroy()
		clearShareDirectory(this)
	}
}
