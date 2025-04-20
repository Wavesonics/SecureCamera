package com.darkrockstudios.app.securecamera

import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.darkrockstudios.app.securecamera.navigation.AppNavHost
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import com.darkrockstudios.app.securecamera.ui.theme.SecureCameraTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App(capturePhoto: MutableState<Boolean?>) {
	KoinContext {
		SecureCameraTheme {
			val snackbarHostState = remember { SnackbarHostState() }
			val navController = rememberNavController()
			val preferencesManager = koinInject<AppPreferencesManager>()
			val authorizationManager = koinInject<AuthorizationManager>()

			val hasCompletedIntro by preferencesManager.hasCompletedIntro.collectAsState(initial = false)
			val startDestination = rememberSaveable(hasCompletedIntro) {
				if (hasCompletedIntro) {
					if (authorizationManager.checkSessionValidity()) {
						AppDestinations.CAMERA_ROUTE
					} else {
						AppDestinations.PIN_VERIFICATION_ROUTE
					}
				} else {
					AppDestinations.INTRODUCTION_ROUTE
				}
			}

			Scaffold(
				snackbarHost = { SnackbarHost(snackbarHostState) },
				modifier = Modifier.imePadding()
			) { paddingValues ->
					AppNavHost(
						navController = navController,
						capturePhoto = capturePhoto,
						modifier = Modifier,
						startDestination = startDestination,
						paddingValues = paddingValues
					)
			}
		}
	}
}
