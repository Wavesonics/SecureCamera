package com.darkrockstudios.app.securecamera

// Import the Gallery icon

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.darkrockstudios.app.securecamera.navigation.AppNavHost
import com.darkrockstudios.app.securecamera.ui.theme.SecureCameraTheme
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.permissions.Permissions
import com.kashif.cameraK.permissions.providePermissions

@Composable
fun App() {
	SecureCameraTheme {
		val permissions: Permissions = providePermissions()
		val snackbarHostState = remember { SnackbarHostState() }
		val navController = rememberNavController()

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
					modifier = Modifier,
					paddingValues = paddingValues
				)
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
