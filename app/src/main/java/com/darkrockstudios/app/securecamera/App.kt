package com.darkrockstudios.app.securecamera

// Import the Gallery icon
import com.darkrockstudios.app.securecamera.Gallery

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

	// Define navigation items
	val items = listOf(
		NavigationItem(
			route = AppDestinations.CAMERA_ROUTE,
			icon = { Icon(imageVector = Camera, contentDescription = "Camera") },
			label = "Camera"
		),
		NavigationItem(
			route = AppDestinations.GALLERY_ROUTE,
			icon = { Icon(imageVector = Gallery, contentDescription = "Gallery") },
			label = "Gallery"
		)
	)

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) },
		modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
		bottomBar = {
			NavigationBar {
				val navBackStackEntry by navController.currentBackStackEntryAsState()
				val currentDestination = navBackStackEntry?.destination

				items.forEach { item ->
					NavigationBarItem(
						icon = { item.icon() },
						label = { Text(item.label) },
						selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
						onClick = {
							navController.navigate(item.route) {
								// Pop up to the start destination of the graph to
								// avoid building up a large stack of destinations
								popUpTo(navController.graph.findStartDestination().id) {
									saveState = true
								}
								// Avoid multiple copies of the same destination when
								// reselecting the same item
								launchSingleTop = true
								// Restore state when reselecting a previously selected item
								restoreState = true
							}
						}
					)
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
				modifier = Modifier.padding(paddingValues)
			)
		}
	}
}

/**
 * Data class for navigation items
 */
private data class NavigationItem(
	val route: String,
	val icon: @Composable () -> Unit,
	val label: String
)

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
