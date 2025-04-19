package com.darkrockstudios.app.securecamera

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.darkrockstudios.app.securecamera.navigation.AppNavHost
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import com.darkrockstudios.app.securecamera.ui.theme.SecureCameraTheme
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.permissions.Permissions
import com.kashif.cameraK.permissions.providePermissions
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App(capturePhoto: MutableState<Boolean?>) {
	KoinContext {
		SecureCameraTheme {
			val permissions: Permissions = providePermissions()
			val snackbarHostState = remember { SnackbarHostState() }
			val navController = rememberNavController()
			val preferencesManager = koinInject<AppPreferencesManager>()
			val authorizationManager = koinInject<AuthorizationManager>()

			val hasCompletedIntro by preferencesManager.hasCompletedIntro.collectAsState(initial = true)
			val startDestination = if (hasCompletedIntro) {
				if (authorizationManager.checkSessionValidity()) {
					AppDestinations.CAMERA_ROUTE
				} else {
					AppDestinations.PIN_VERIFICATION_ROUTE
				}
			} else {
				AppDestinations.INTRODUCTION_ROUTE
			}

			Scaffold(
				snackbarHost = { SnackbarHost(snackbarHostState) },
				modifier = Modifier
			) { paddingValues ->
				val cameraPermissionState = remember { mutableStateOf(permissions.hasCameraPermission()) }
				val storagePermissionState = remember { mutableStateOf(permissions.hasStoragePermission()) }
				val cameraController = remember { mutableStateOf<CameraController?>(null) }

				PermissionsHandler(
					permissions = permissions,
					cameraPermissionState = cameraPermissionState,
					storagePermissionState = storagePermissionState
				)

				if (cameraPermissionState.value && storagePermissionState.value) {
					AppNavHost(
						navController = navController,
						cameraController = cameraController,
						capturePhoto = capturePhoto,
						modifier = Modifier,
						startDestination = startDestination,
						paddingValues = paddingValues
					)
				}
			}
		}
	}
}

@Composable
private fun PermissionsHandler(
	permissions: Permissions,
	cameraPermissionState: MutableState<Boolean>,
	storagePermissionState: MutableState<Boolean>
) {
	if (!cameraPermissionState.value) {
		permissions.RequestCameraPermission(
			onGranted = { cameraPermissionState.value = true },
			onDenied = { println("Camera Permission Denied") }
		)
	}

	if (!storagePermissionState.value) {
		permissions.RequestStoragePermission(
			onGranted = { storagePermissionState.value = true },
			onDenied = { println("Storage Permission Denied") }
		)
	}
}
