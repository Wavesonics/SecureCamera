package com.darkrockstudios.app.securecamera.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.darkrockstudios.app.securecamera.camera.CameraContent
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.gallery.GalleryContent
import com.darkrockstudios.app.securecamera.viewphoto.ViewPhotoContent
import com.kashif.cameraK.controller.CameraController
import org.koin.compose.koinInject

/**
 * Navigation destinations for the app
 */
object AppDestinations {
	const val CAMERA_ROUTE = "camera"
	const val GALLERY_ROUTE = "gallery"
	const val VIEW_PHOTO_ROUTE = "viewphoto/{photoName}"

	fun createViewPhotoRoute(photoName: String): String {
		return "viewphoto/$photoName"
	}
}

/**
 * Main navigation component for the app
 */
@Composable
fun AppNavHost(
	navController: NavHostController,
	cameraController: MutableState<CameraController?>,
	modifier: Modifier = Modifier,
	startDestination: String = AppDestinations.CAMERA_ROUTE,
	paddingValues: PaddingValues
) {
	val imageManager = koinInject<SecureImageManager>()

	NavHost(
		navController = navController,
		startDestination = startDestination,
		modifier = modifier
	) {
		composable(AppDestinations.CAMERA_ROUTE) {
			CameraContent(
				cameraController = cameraController,
				navController = navController,
				modifier = Modifier.fillMaxSize(),
				paddingValues = paddingValues
			)
		}

		composable(AppDestinations.GALLERY_ROUTE) {
			GalleryContent(
				navController = navController,
				modifier = Modifier.fillMaxSize(),
				paddingValues = paddingValues
			)
		}

		composable(
			route = AppDestinations.VIEW_PHOTO_ROUTE,
			arguments = listOf(navArgument("photoName") { defaultValue = "" })
		) { backStackEntry ->
			val photoName = backStackEntry.arguments?.getString("photoName") ?: ""
			val photo = imageManager.getPhotoByName(photoName)

			if (photo != null) {
				ViewPhotoContent(
					photo = photo,
					navController = navController,
					modifier = Modifier.fillMaxSize(),
					paddingValues = paddingValues
				)
			} else {
				Text(text = "No photo selected")
			}
		}
	}
}
