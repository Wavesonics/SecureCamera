package com.darkrockstudios.app.securecamera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
	private var capturePhoto = mutableStateOf<Boolean?>(null)
	private val locationRepository: LocationRepository by inject()
	private val preferences: AppPreferencesDataSource by inject()
	private val authorizationRepository: AuthorizationRepository by inject()
	lateinit var navController: NavHostController

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		if (BuildConfig.DEBUG.not()) {
			window.setFlags(
				WindowManager.LayoutParams.FLAG_SECURE,
				WindowManager.LayoutParams.FLAG_SECURE
			)
		}

		enableEdgeToEdge()

		val startDestination = determineStartRoute()
		setContent {
			navController = rememberNavController()
			App(capturePhoto, startDestination, navController)
		}
	}

	private fun determineStartRoute(): String {
		val photosToImport = receiveFiles()
		val hasCompletedIntro = runBlocking { preferences.hasCompletedIntro.firstOrNull() ?: false }
		val startDestination = if (hasCompletedIntro) {
			val targetDestination = if (photosToImport.isNotEmpty()) {
				AppDestinations.createImportPhotosRoute(photosToImport)
			} else {
				AppDestinations.CAMERA_ROUTE
			}
			if (authorizationRepository.checkSessionValidity()) {
				targetDestination
			} else {
				AppDestinations.createPinVerificationRoute(targetDestination)
			}
		} else {
			AppDestinations.INTRODUCTION_ROUTE
		}
		return startDestination
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
		locationRepository.refreshPermissionStatus()
	}

	private fun receiveFiles(): List<Uri> {
		val intent = getIntent()

		return if (Intent.ACTION_SEND == intent.action && intent.type != null) {
			if (intent.type?.startsWith("image/jpeg") == true) {
				handleSingleImage(intent)
			} else {
				emptyList()
			}
		} else if (Intent.ACTION_SEND_MULTIPLE == intent.action && intent.type != null) {
			if (intent.type?.startsWith("image/jpeg") == true) {
				handleMultipleImages(intent)
			} else {
				emptyList()
			}
		} else {
			emptyList()
		}
	}

	private fun handleSingleImage(intent: Intent): List<Uri> {
		return intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)?.let { imageUri ->
			listOf(imageUri)
		} ?: emptyList()
	}

	private fun handleMultipleImages(intent: Intent): List<Uri> {
		val imageUris = intent.getParcelableArrayListExtra<Uri?>(Intent.EXTRA_STREAM)
		return imageUris?.filterNotNull() ?: emptyList()
	}
}
