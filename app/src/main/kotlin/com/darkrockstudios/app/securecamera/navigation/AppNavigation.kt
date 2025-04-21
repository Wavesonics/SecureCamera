package com.darkrockstudios.app.securecamera.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.darkrockstudios.app.securecamera.LocationRepository
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.about.AboutContent
import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.auth.PinVerificationContent
import com.darkrockstudios.app.securecamera.camera.CameraContent
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.gallery.GalleryContent
import com.darkrockstudios.app.securecamera.introduction.IntroductionContent
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import com.darkrockstudios.app.securecamera.settings.SettingsContent
import com.darkrockstudios.app.securecamera.viewphoto.ViewPhotoContent
import kotlinx.coroutines.flow.combine
import org.koin.compose.koinInject

/**
 * Navigation destinations for the app
 */
object AppDestinations {
	const val INTRODUCTION_ROUTE = "introduction"
	const val CAMERA_ROUTE = "camera"
	const val GALLERY_ROUTE = "gallery"
	const val VIEW_PHOTO_ROUTE = "viewphoto/{photoName}"
	const val SETTINGS_ROUTE = "settings"
	const val ABOUT_ROUTE = "about"
	const val PIN_VERIFICATION_ROUTE = "pin_verification/{returnRoute}"

	fun createViewPhotoRoute(photoName: String): String {
		return "viewphoto/$photoName"
	}

	fun createPinVerificationRoute(returnRoute: String): String {
		return "pin_verification/$returnRoute"
	}
}

/**
 * Main navigation component for the app
 */
@Composable
fun AppNavHost(
	navController: NavHostController,
	capturePhoto: MutableState<Boolean?>,
	modifier: Modifier = Modifier,
	snackbarHostState: SnackbarHostState,
	startDestination: String = AppDestinations.CAMERA_ROUTE,
	paddingValues: PaddingValues,
) {
	val imageManager = koinInject<SecureImageManager>()
	val authManager = koinInject<AuthorizationManager>()
	val preferencesManager = koinInject<AppPreferencesManager>()
	val locationRepository = koinInject<LocationRepository>()

	LaunchedEffect(Unit) {
		authManager.checkSessionValidity()

		authManager.isAuthorized
			.combine(navController.currentBackStackEntryFlow) { isAuthorized, destination ->
				Pair(
					isAuthorized,
					destination
				)
			}
			.collect { (isAuthorized, destination) ->
				when (destination.destination.route) {
					AppDestinations.GALLERY_ROUTE -> {
						if (authManager.checkSessionValidity().not()) {
							navController.navigate(AppDestinations.createPinVerificationRoute(AppDestinations.GALLERY_ROUTE)) {
								popUpTo(AppDestinations.CAMERA_ROUTE)
								launchSingleTop = true
							}
						}
					}

					AppDestinations.VIEW_PHOTO_ROUTE -> {
						if (authManager.checkSessionValidity().not()) {
							val photoName = destination.destination.arguments["photoName"]?.toString() ?: ""
							navController.navigate(
								AppDestinations.createPinVerificationRoute(
									AppDestinations.createViewPhotoRoute(photoName)
								)
							) {
								popUpTo(AppDestinations.CAMERA_ROUTE)
								launchSingleTop = true
							}
						}
					}
				}
			}
	}

	NavHost(
		navController = navController,
		startDestination = startDestination,
		modifier = modifier
	) {
		composable(AppDestinations.INTRODUCTION_ROUTE) {
			IntroductionContent(
				navController = navController,
				modifier = Modifier.fillMaxSize()
			)
		}

		composable(AppDestinations.CAMERA_ROUTE) {
			CameraContent(
				capturePhoto = capturePhoto,
				navController = navController,
				modifier = Modifier.fillMaxSize(),
				paddingValues = paddingValues
			)
		}

		composable(AppDestinations.GALLERY_ROUTE) {
			val isAuthorized by authManager.isAuthorized.collectAsState()

			if (isAuthorized) {
				GalleryContent(
					navController = navController,
					modifier = Modifier.fillMaxSize(),
					paddingValues = paddingValues
				)
			} else {
				Box(modifier = Modifier.fillMaxSize()) {
					Text(
						text = stringResource(R.string.unauthorized),
						modifier = Modifier.align(Alignment.Center)
					)
				}
			}
		}

		composable(
			route = AppDestinations.VIEW_PHOTO_ROUTE,
			arguments = listOf(navArgument("photoName") { defaultValue = "" })
		) { backStackEntry ->
			val photoName = backStackEntry.arguments?.getString("photoName") ?: ""

			if (authManager.checkSessionValidity()) {
				val photo = imageManager.getPhotoByName(photoName)
				if (photo != null) {
					ViewPhotoContent(
						photo = photo,
						navController = navController,
						modifier = Modifier.fillMaxSize(),
						paddingValues = paddingValues
					)
				} else {
					Text(text = stringResource(R.string.photo_content_none_selected))
				}
			} else {
				Box(modifier = Modifier.fillMaxSize()) {
					Text(
						text = stringResource(R.string.unauthorized),
						modifier = Modifier.align(Alignment.Center)
					)
				}
			}
		}

		composable(
			route = AppDestinations.PIN_VERIFICATION_ROUTE,
			arguments = listOf(navArgument("returnRoute") { defaultValue = AppDestinations.CAMERA_ROUTE })
		) { backStackEntry ->
			val returnRoute = backStackEntry.arguments?.getString("returnRoute") ?: AppDestinations.CAMERA_ROUTE

			PinVerificationContent(
				navController = navController,
				returnRoute = returnRoute,
				snackbarHostState = snackbarHostState,
				modifier = Modifier.fillMaxSize()
			)
		}

		composable(AppDestinations.SETTINGS_ROUTE) {
			SettingsContent(
				navController = navController,
				modifier = Modifier.fillMaxSize(),
				paddingValues = paddingValues,
				preferencesManager = preferencesManager,
				locationRepository = locationRepository
			)
		}

		composable(AppDestinations.ABOUT_ROUTE) {
			AboutContent(
				navController = navController,
				modifier = Modifier.fillMaxSize(),
			)
		}
	}
}
