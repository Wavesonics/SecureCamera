package com.darkrockstudios.app.securecamera

import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.darkrockstudios.app.securecamera.navigation.AppNavHost
import com.darkrockstudios.app.securecamera.navigation.enforceAuth
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

			val hasCompletedIntro by preferencesManager.hasCompletedIntro.collectAsState(initial = null)
			val startDestination = rememberSaveable(hasCompletedIntro) {
				if (hasCompletedIntro == true) {
					if (authorizationManager.checkSessionValidity()) {
						AppDestinations.CAMERA_ROUTE
					} else {
						AppDestinations.PIN_VERIFICATION_ROUTE
					}
				} else {
					AppDestinations.INTRODUCTION_ROUTE
				}
			}

			VerifySessionOnResume(navController, hasCompletedIntro, authorizationManager)

			if (hasCompletedIntro != null) {
				Scaffold(
					snackbarHost = { SnackbarHost(snackbarHostState) },
					modifier = Modifier.imePadding()
				) { paddingValues ->
					AppNavHost(
						navController = navController,
						capturePhoto = capturePhoto,
						modifier = Modifier,
						snackbarHostState = snackbarHostState,
						startDestination = startDestination,
						paddingValues = paddingValues
					)
				}
			}
		}
	}
}

@Composable
fun VerifySessionOnResume(
	navController: NavHostController,
	hasCompletedIntro: Boolean?,
	authorizationManager: AuthorizationManager
) {
	var requireAuthCheck = remember { false }
	LifecycleResumeEffect(hasCompletedIntro) {
		if (hasCompletedIntro == true && requireAuthCheck) {
			enforceAuth(authorizationManager, navController.currentDestination, navController)
		}
		onPauseOrDispose {
			requireAuthCheck = true
		}
	}
}