package com.darkrockstudios.app.securecamera

import android.net.Uri
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.darkrockstudios.app.securecamera.navigation.AppNavHost
import com.darkrockstudios.app.securecamera.navigation.enforceAuth
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.ui.theme.SecureCameraTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App(
	capturePhoto: MutableState<Boolean?>,
	photosToImport: List<Uri>
) {
	KoinContext {
		SecureCameraTheme {
			val snackbarHostState = remember { SnackbarHostState() }
			val navController = rememberNavController()
			val preferencesManager = koinInject<AppPreferencesDataSource>()
			val authorizationRepository = koinInject<AuthorizationRepository>()

			val hasCompletedIntro by preferencesManager.hasCompletedIntro.collectAsState(initial = null)
			val startDestination = rememberSaveable(hasCompletedIntro) {
				if (hasCompletedIntro == true) {
					val targetDestination = if (photosToImport.isNotEmpty()) {
						AppDestinations.IMPORT_PHOTOS_ROUTE
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
			}

			VerifySessionOnResume(navController, hasCompletedIntro, authorizationRepository)

			if (hasCompletedIntro != null) {
				Scaffold(
					snackbarHost = { SnackbarHost(snackbarHostState) },
					modifier = Modifier.imePadding()
				) { paddingValues ->
					// Create a mutable state to hold the photos to import
					val photosToImportState = remember { mutableStateOf(photosToImport) }

					AppNavHost(
						navController = navController,
						capturePhoto = capturePhoto,
						modifier = Modifier,
						snackbarHostState = snackbarHostState,
						startDestination = startDestination,
						paddingValues = paddingValues,
						photosToImport = photosToImportState
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
	authorizationRepository: AuthorizationRepository
) {
	var requireAuthCheck = remember { false }
	LifecycleResumeEffect(hasCompletedIntro) {
		if (hasCompletedIntro == true && requireAuthCheck) {
			enforceAuth(authorizationRepository, navController.currentDestination, navController)
		}
		onPauseOrDispose {
			requireAuthCheck = true
		}
	}
}
