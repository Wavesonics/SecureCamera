package com.darkrockstudios.app.securecamera

import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavHostController
import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.navigation.AppNavHost
import com.darkrockstudios.app.securecamera.navigation.enforceAuth
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.ui.theme.SecureCameraTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App(
	capturePhoto: MutableState<Boolean?>,
	startDestination: String,
	navController: NavHostController
) {
	KoinContext {
		SecureCameraTheme {
			val snackbarHostState = remember { SnackbarHostState() }
			val preferencesManager = koinInject<AppPreferencesDataSource>()
			val authorizationRepository = koinInject<AuthorizationRepository>()

			val hasCompletedIntro by preferencesManager.hasCompletedIntro.collectAsState(initial = null)

			VerifySessionOnResume(navController, hasCompletedIntro, authorizationRepository)

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
						paddingValues = paddingValues,
					)
				}
			}
		}
	}
}

@Composable
private fun VerifySessionOnResume(
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
