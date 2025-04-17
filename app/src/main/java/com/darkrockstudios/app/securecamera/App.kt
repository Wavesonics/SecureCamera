package com.darkrockstudios.app.securecamera

// Import the Gallery icon

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.darkrockstudios.app.securecamera.camera.CameraBottomBar
import com.darkrockstudios.app.securecamera.gallery.GalleryBottomNav
import com.darkrockstudios.app.securecamera.gallery.GalleryTopNav
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.darkrockstudios.app.securecamera.navigation.AppNavHost
import com.darkrockstudios.app.securecamera.ui.theme.SecureCameraTheme
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.permissions.Permissions
import com.kashif.cameraK.permissions.providePermissions
import com.kashif.imagesaverplugin.ImageSaverConfig
import com.kashif.imagesaverplugin.rememberImageSaverPlugin

@Composable
fun App() = SecureCameraTheme {
	val permissions: Permissions = providePermissions()
	val snackbarHostState = remember { SnackbarHostState() }
	val navController = rememberNavController()

	val navBackStackEntry by navController.currentBackStackEntryAsState()
	val currentDestination = navBackStackEntry?.destination

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) },
		modifier = Modifier,
		topBar = {
			when (currentDestination?.route) {
				AppDestinations.CAMERA_ROUTE -> {
				}

				AppDestinations.GALLERY_ROUTE -> {
					GalleryTopNav(navController)
				}
			}
		},
		bottomBar = {
			when (currentDestination?.route) {
				AppDestinations.CAMERA_ROUTE -> {
					CameraBottomBar(navController)
				}

				AppDestinations.GALLERY_ROUTE -> {
					GalleryBottomNav(navController)
				}
			}
		}
	) { paddingValues ->
		val cameraPermissionState = remember { mutableStateOf(permissions.hasCameraPermission()) }
		val storagePermissionState = remember { mutableStateOf(permissions.hasStoragePermission()) }

		val cameraController = remember { mutableStateOf<CameraController?>(null) }
		val imageSaverPlugin = rememberImageSaverPlugin(
			config = ImageSaverConfig(
				isAutoSave = false,
				prefix = "MyApp",
				directory = Directory.PICTURES,
				customFolderName = "CustomFolder"
			)
		)

		PermissionsHandler(
			permissions = permissions,
			cameraPermissionState = cameraPermissionState,
			storagePermissionState = storagePermissionState
		)

		if (cameraPermissionState.value && storagePermissionState.value) {
			AppNavHost(
				navController = navController,
				cameraController = cameraController,
				imageSaverPlugin = imageSaverPlugin,
				modifier = Modifier
			)
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
