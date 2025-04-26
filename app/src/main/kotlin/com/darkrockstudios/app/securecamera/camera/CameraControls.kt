package com.darkrockstudios.app.securecamera.camera

import android.content.Context
import android.location.Location
import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ashampoo.kim.model.GpsCoordinates
import com.darkrockstudios.app.securecamera.LocationRepository
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.RequestLocationPermission
import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.gallery.vibrateDevice
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import timber.log.Timber
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun CameraControls(
	cameraController: CameraState,
	capturePhoto: MutableState<Boolean?>,
	navController: NavHostController,
	paddingValues: PaddingValues? = null,
) {
	val scope = rememberCoroutineScope()
	var isFlashOn by rememberSaveable(cameraController.flashMode) { mutableStateOf(cameraController.flashMode == ImageCapture.FLASH_MODE_ON) }
	var isTopControlsVisible by rememberSaveable { mutableStateOf(false) }
	var activeJobs by remember { mutableStateOf(mutableListOf<kotlinx.coroutines.Job>()) }
	val isLoading by remember { derivedStateOf { activeJobs.isNotEmpty() } }
	var isFlashing by rememberSaveable { mutableStateOf(false) }
	val imageSaver = koinInject<SecureImageManager>()
	val authManager = koinInject<AuthorizationManager>()
	val locationRepository = koinInject<LocationRepository>()
	val context = LocalContext.current

	var locationPermissionState by rememberSaveable { mutableStateOf(false) }
	RequestLocationPermission {
		locationPermissionState = true
	}

	fun doCapturePhoto() {
		if (authManager.checkSessionValidity()) {
			isFlashing = true

			val job = scope.launch(Dispatchers.IO) {
				val location = if (locationPermissionState) {
					locationRepository.currentLocation()
				} else {
					null
				}

				try {
					handleImageCapture(
						cameraController = cameraController,
						imageSaver = imageSaver,
						location = location,
						isFlashOn = isFlashOn,
						context = context,
					)
				} finally {
					activeJobs =
						activeJobs
							.filterNot { it === this }
							.filter { it.isCompleted.not() && it.isCancelled.not() }
							.toMutableList()
				}
			}
			activeJobs = (activeJobs + job).toMutableList()
		} else {
			navController.navigate(AppDestinations.createPinVerificationRoute(AppDestinations.CAMERA_ROUTE))
		}
	}

	LaunchedEffect(capturePhoto.value) {
		if (capturePhoto.value != null) {
			doCapturePhoto()
		}
	}

	LaunchedEffect(isFlashing) {
		if (isFlashing) {
			delay(250)
			isFlashing = false
		}
	}

	Box(modifier = Modifier.fillMaxSize()) {
		FlashEffect(isFlashing = isFlashing)

		ZoomMeter(
			cameraController,
			modifier = Modifier
				.align(Alignment.TopCenter)
				.padding(top = 64.dp)
		)

		LevelIndicator(
			modifier = Modifier
				.align(Alignment.Center)
				.padding(top = paddingValues?.calculateTopPadding()?.plus(16.dp) ?: 16.dp)
		)

		if (!isTopControlsVisible) {
			ElevatedButton(
				onClick = { isTopControlsVisible = true },
				modifier = Modifier
					.align(Alignment.TopEnd)
					.padding(
						top = paddingValues?.calculateTopPadding()?.plus(16.dp) ?: 16.dp,
						end = 16.dp
					)
			) {
				Icon(
					imageVector = Icons.Filled.MoreVert,
					contentDescription = stringResource(id = R.string.camera_more_options_content_description),
				)
			}
		}

		if (activeJobs.isNotEmpty()) {
			CircularProgressIndicator(
				modifier = Modifier
					.padding(start = 16.dp, top = paddingValues?.calculateTopPadding()?.plus(16.dp) ?: 16.dp)
					.size(40.dp)
					.align(Alignment.TopStart),
				color = MaterialTheme.colorScheme.primary
			)
		}

		TopCameraControlsBar(
			isFlashOn = isFlashOn,
			isVisible = isTopControlsVisible,
			onFlashToggle = {
				isFlashOn = !isFlashOn
			},
			onLensToggle = { cameraController.toggleLens() },
			onClose = { isTopControlsVisible = false },
			paddingValues = paddingValues
		)

		BottomCameraControls(
			modifier = Modifier.align(Alignment.BottomCenter),
			isLoading = isLoading,
			navController = navController,
			onCapture = { doCapturePhoto() }
		)
	}
}

@OptIn(ExperimentalUuidApi::class)
private suspend fun handleImageCapture(
	cameraController: CameraState,
	imageSaver: SecureImageManager,
	context: Context,
	location: Location?,
	isFlashOn: Boolean,
) {
	val gpsCoordinates = location?.let {
		GpsCoordinates(
			latitude = it.latitude,
			longitude = it.longitude,
		)
	}

	cameraController.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
	val result = cameraController.capturePhoto()

	val image = result.getOrNull()
	if (result.isSuccess && image != null) {
		vibrateDevice(context)
		imageSaver.saveImage(
			image = image,
			applyRotation = true,
			latLng = gpsCoordinates,
		).let { path ->
			Timber.i("Image saved at: $path")
		}
	} else {
		Timber.e(result.exceptionOrNull(), "Image Capture Error")
	}
}

@Composable
private fun FlashEffect(isFlashing: Boolean) {
	AnimatedVisibility(
		visible = isFlashing,
		enter = fadeIn(animationSpec = tween(durationMillis = 50)),
		exit = fadeOut(animationSpec = tween(durationMillis = 100))
	) {
		Surface(
			modifier = Modifier.fillMaxSize(),
			color = Color.White
		) {}
	}
}
