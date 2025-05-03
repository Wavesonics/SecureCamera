package com.darkrockstudios.app.securecamera.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.about.AboutContent
import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.auth.PinVerificationContent
import com.darkrockstudios.app.securecamera.camera.CameraContent
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.gallery.GalleryContent
import com.darkrockstudios.app.securecamera.import.ImportPhotosContent
import com.darkrockstudios.app.securecamera.introduction.IntroductionContent
import com.darkrockstudios.app.securecamera.obfuscation.ObfuscatePhotoContent
import com.darkrockstudios.app.securecamera.settings.SettingsContent
import com.darkrockstudios.app.securecamera.viewphoto.ViewPhotoContent
import kotlinx.coroutines.flow.combine
import org.koin.compose.koinInject
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Main navigation component for the app
 */
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun AppNavHost(
	navController: NavHostController,
	capturePhoto: MutableState<Boolean?>,
	modifier: Modifier = Modifier,
	snackbarHostState: SnackbarHostState,
	startDestination: String = AppDestinations.CAMERA_ROUTE,
	paddingValues: PaddingValues,
	photosToImport: MutableState<List<Uri>> = mutableStateOf(emptyList()),
) {
	val imageManager = koinInject<SecureImageRepository>()
	val authManager = koinInject<AuthorizationRepository>()

	val scope = rememberCoroutineScope()

	/**
	 * Continually enforce auth as the user navigates around the app
	 */
	LaunchedEffect(Unit) {
		authManager.checkSessionValidity()

		authManager.isAuthorized
			.combine(navController.currentBackStackEntryFlow) { isAuthorized, backStackEntry ->
				Pair(
					isAuthorized,
					backStackEntry
				)
			}
			.collect { (_, backStackEntry) ->
				enforceAuth(authManager, backStackEntry.destination, navController)
			}
	}

	NavHost(
		navController = navController,
		startDestination = startDestination,
		modifier = modifier
	) {
		defaultAnimatedComposable(
			route = AppDestinations.INTRODUCTION_ROUTE,
		) {
			IntroductionContent(
				navController = navController,
				modifier = Modifier.fillMaxSize()
			)
		}

		defaultAnimatedComposable(
			AppDestinations.CAMERA_ROUTE,
		) {
			CameraContent(
				capturePhoto = capturePhoto,
				navController = navController,
				modifier = Modifier.fillMaxSize(),
				paddingValues = paddingValues
			)
		}

		defaultAnimatedComposable(
			AppDestinations.GALLERY_ROUTE,
		) {
			val isAuthorized by authManager.isAuthorized.collectAsState()

			if (isAuthorized) {
				GalleryContent(
					navController = navController,
					modifier = Modifier.fillMaxSize(),
					paddingValues = paddingValues,
					snackbarHostState = snackbarHostState
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

		defaultAnimatedComposable(
			route = AppDestinations.VIEW_PHOTO_ROUTE,
			arguments = listOf(navArgument("photoName") { defaultValue = "" }),
		) { backStackEntry ->
			val photoName = backStackEntry.arguments?.getString("photoName") ?: ""

			if (authManager.checkSessionValidity()) {
				val photo = imageManager.getPhotoByName(photoName)
				if (photo != null) {
					ViewPhotoContent(
						initialPhoto = photo,
						navController = navController,
						modifier = Modifier.fillMaxSize(),
						paddingValues = paddingValues,
						snackbarHostState = snackbarHostState,
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

		defaultAnimatedComposable(
			route = AppDestinations.PIN_VERIFICATION_ROUTE,
			arguments = listOf(navArgument("returnRoute") {
				defaultValue = AppDestinations.encodeReturnRoute(AppDestinations.CAMERA_ROUTE)
			}),
		) { backStackEntry ->
			val returnRoute = backStackEntry.arguments?.getString("returnRoute")?.let { encodedRoute ->
				AppDestinations.decodeReturnRoute(encodedRoute)
			} ?: AppDestinations.CAMERA_ROUTE

			PinVerificationContent(
				navController = navController,
				returnRoute = returnRoute,
				snackbarHostState = snackbarHostState,
				modifier = Modifier.fillMaxSize()
			)
		}

		defaultAnimatedComposable(
			AppDestinations.SETTINGS_ROUTE,
		) {
			SettingsContent(
				navController = navController,
				modifier = Modifier.fillMaxSize(),
				paddingValues = paddingValues,
				snackbarHostState = snackbarHostState,
			)
		}

		defaultAnimatedComposable(
			AppDestinations.ABOUT_ROUTE,
		) {
			AboutContent(
				navController = navController,
				modifier = Modifier.fillMaxSize(),
			)
		}

		defaultAnimatedComposable(
			route = AppDestinations.OBFUSCATE_PHOTO_ROUTE,
			arguments = listOf(navArgument("photoName") { defaultValue = "" }),
		) { backStackEntry ->
			val photoName = backStackEntry.arguments?.getString("photoName") ?: ""

			if (authManager.checkSessionValidity()) {
				ObfuscatePhotoContent(
					photoName = photoName,
					navController = navController,
					snackbarHostState = snackbarHostState,
					outerScope = scope,
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

		defaultAnimatedComposable(
			route = AppDestinations.IMPORT_PHOTOS_ROUTE,
		) {
			if (authManager.checkSessionValidity()) {
				ImportPhotosContent(
					photosToImport = photosToImport,
					navController = navController
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
	}
}

/**
 * Check for session validity, send user to PinVerification
 * if needed.
 *
 * This also calculates the return path for successful
 * PinVerification.
 */
fun enforceAuth(
	authManager: AuthorizationRepository,
	destination: NavDestination?,
	navController: NavHostController
) {
	if (
		authManager.checkSessionValidity().not()
		&& AppDestinations.isPinVerificationRoute(destination).not()
		&& destination?.route != AppDestinations.INTRODUCTION_ROUTE
	) {
		val returnRoute = when (destination?.route) {
			AppDestinations.VIEW_PHOTO_ROUTE -> {
				navController.currentBackStackEntry?.arguments?.getString("photoName")
					?.let { photoName ->
						AppDestinations.createViewPhotoRoute(photoName)
					} ?: AppDestinations.CAMERA_ROUTE
			}

			AppDestinations.OBFUSCATE_PHOTO_ROUTE -> {
				navController.currentBackStackEntry?.arguments?.getString("photoName")
					?.let { photoName ->
						AppDestinations.createObfuscatePhotoRoute(photoName)
					} ?: AppDestinations.CAMERA_ROUTE
			}

			else -> {
				destination?.route ?: AppDestinations.CAMERA_ROUTE
			}
		}

		navController.navigate(AppDestinations.createPinVerificationRoute(returnRoute)) {
			launchSingleTop = true
		}
	}
}
