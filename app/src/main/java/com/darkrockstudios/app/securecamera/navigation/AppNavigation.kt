package com.darkrockstudios.app.securecamera.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.darkrockstudios.app.securecamera.camera.CameraContent
import com.darkrockstudios.app.securecamera.gallery.GalleryContent
import com.kashif.cameraK.controller.CameraController
import com.kashif.imagesaverplugin.ImageSaverPlugin

/**
 * Navigation destinations for the app
 */
object AppDestinations {
    const val CAMERA_ROUTE = "camera"
    const val GALLERY_ROUTE = "gallery"
}

/**
 * Main navigation component for the app
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    cameraController: MutableState<CameraController?>,
    imageSaverPlugin: ImageSaverPlugin,
    modifier: Modifier = Modifier,
    startDestination: String = AppDestinations.CAMERA_ROUTE,
    paddingValues: androidx.compose.foundation.layout.PaddingValues? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppDestinations.CAMERA_ROUTE) {
            CameraContent(
                cameraController = cameraController,
                imageSaverPlugin = imageSaverPlugin,
                navController = navController,
                modifier = Modifier.fillMaxSize(),
                paddingValues = paddingValues
            )
        }

        composable(AppDestinations.GALLERY_ROUTE) {
            GalleryContent(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
