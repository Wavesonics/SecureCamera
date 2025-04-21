package com.darkrockstudios.app.securecamera.camera

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ashampoo.kim.model.GpsCoordinates
import com.ashampoo.kim.model.TiffOrientation
import com.darkrockstudios.app.securecamera.*
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.gallery.vibrateDevice
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.*
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.ui.CameraPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun CameraContent(
	capturePhoto: MutableState<Boolean?>,
	navController: NavHostController,
	modifier: Modifier,
	paddingValues: PaddingValues,
) {
	KeepScreenOnEffect()

	val cameraController = remember { mutableStateOf<CameraController?>(null) }

	val permissionsState = rememberMultiplePermissionsState(
		permissions = listOf(
			Manifest.permission.CAMERA,
		)
	)

	var showRationaleDialog by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		if (!permissionsState.allPermissionsGranted && permissionsState.shouldShowRationale) {
			showRationaleDialog = true
		} else {
			permissionsState.launchMultiplePermissionRequest()
		}
	}

	if (showRationaleDialog) {
		CameraPermissionRationaleDialog(
			onContinue = {
				showRationaleDialog = false
				permissionsState.launchMultiplePermissionRequest()
			},
			onDismiss = { showRationaleDialog = false }
		)
	}

	Box(
		modifier = modifier
			.fillMaxSize()
	) {
		if (permissionsState.allPermissionsGranted) {
			CameraPreview(
				modifier = Modifier.fillMaxSize(),
				cameraConfiguration = {
					setCameraLens(CameraLens.BACK)
					setFlashMode(FlashMode.OFF)
					setImageFormat(ImageFormat.JPEG)
					setDirectory(Directory.PICTURES)
					setTorchMode(TorchMode.OFF)
				},
				onCameraControllerReady = {
					cameraController.value = it
				}
			)

			cameraController.value?.let { controller ->
				EnhancedCameraScreen(
					cameraController = controller,
					capturePhoto = capturePhoto,
					navController = navController,
					paddingValues = paddingValues,
				)
			}
		} else {
			NoCameraPermission(navController, permissionsState)
		}
	}
}

@Composable
fun EnhancedCameraScreen(
	cameraController: CameraController,
	capturePhoto: MutableState<Boolean?>,
	navController: NavHostController,
	paddingValues: PaddingValues? = null,
) {
	val scope = rememberCoroutineScope()
	var isFlashOn by rememberSaveable(cameraController.getFlashMode()) { mutableStateOf(cameraController.getFlashMode() == FlashMode.ON) }
	var isTopControlsVisible by rememberSaveable { mutableStateOf(false) }
	var activeJobs by remember { mutableStateOf(0) }
	val isLoading by derivedStateOf { activeJobs > 0 }
	var isFlashing by rememberSaveable { mutableStateOf(false) }
	val imageSaver = koinInject<SecureImageManager>()
	val authManager = koinInject<AuthorizationManager>()
	val locationRepository = koinInject<LocationRepository>()
	val context = LocalContext.current
	val orientation = rememberCurrentTiffOrientation()

	var locationPermissionState by rememberSaveable { mutableStateOf(false) }
	RequestLocationPermission {
		locationPermissionState = true
	}

	fun doCapturePhoto() {
		if (authManager.checkSessionValidity()) {
			isFlashing = true

			activeJobs++
			scope.launch(Dispatchers.IO) {
				val location = if (locationPermissionState) {
					locationRepository.currentLocation()
				} else {
					null
				}

				try {
					handleImageCapture(
						cameraController = cameraController,
						imageSaver = imageSaver,
						orientation = orientation,
						location = location,
						isFlashOn = isFlashOn,
						context = context,
					)
				} finally {
					activeJobs--
				}
			}
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

		TopControlsBar(
			isFlashOn = isFlashOn,
			isVisible = isTopControlsVisible,
			onFlashToggle = {
				isFlashOn = !isFlashOn
			},
			onLensToggle = { cameraController.toggleCameraLens() },
			onClose = { isTopControlsVisible = false },
			paddingValues = paddingValues
		)

		if (activeJobs > 0) {
			CircularProgressIndicator(
				modifier = Modifier
					.padding(start = 16.dp, top = paddingValues?.calculateTopPadding()?.plus(16.dp) ?: 16.dp)
					.size(40.dp)
					.align(Alignment.TopStart),
				color = MaterialTheme.colorScheme.primary
			)
		}

		BottomControls(
			modifier = Modifier.align(Alignment.BottomCenter),
			isLoading = isLoading,
			navController = navController,
			onCapture = { doCapturePhoto() }
		)
	}
}

@Composable
private fun TopControlsBar(
	isFlashOn: Boolean,
	isVisible: Boolean,
	onFlashToggle: (Boolean) -> Unit,
	onLensToggle: () -> Unit,
	onClose: () -> Unit,
	paddingValues: PaddingValues? = null
) {
	AnimatedVisibility(
		visible = isVisible,
		enter = slideInHorizontally(
			initialOffsetX = { fullWidth -> fullWidth },
			animationSpec = tween(durationMillis = 300)
		) + fadeIn(animationSpec = tween(durationMillis = 300)),
		exit = slideOutHorizontally(
			targetOffsetX = { fullWidth -> fullWidth },
			animationSpec = tween(durationMillis = 300)
		) + fadeOut(animationSpec = tween(durationMillis = 300))
	) {
		Surface(
			modifier = Modifier
				.fillMaxWidth()
				.padding(
					start = 16.dp,
					end = 16.dp,
					top = paddingValues?.calculateTopPadding()?.plus(16.dp) ?: 16.dp,
					bottom = 16.dp
				),
			color = Color.Black.copy(alpha = 0.6f),
			shape = RoundedCornerShape(16.dp)
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Column(
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					CameraControlSwitch(
						icon = if (isFlashOn) Flashlight else FlashlightOff,
						checked = isFlashOn,
						onCheckedChange = onFlashToggle
					)
				}

				Row(
					horizontalArrangement = Arrangement.spacedBy(16.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					FilledTonalButton(
						onClick = onLensToggle,
						modifier = Modifier
							.background(MaterialTheme.colorScheme.primary, CircleShape),
						colors = ButtonDefaults.filledTonalButtonColors(
							containerColor = MaterialTheme.colorScheme.primary
						)
					) {
						Icon(
							imageVector = Icons.Filled.Cameraswitch,
							contentDescription = stringResource(id = R.string.camera_toggle_content_description),
							tint = MaterialTheme.colorScheme.onPrimary,
						)
					}

					FilledTonalButton(
						onClick = onClose,
						modifier = Modifier
							.background(MaterialTheme.colorScheme.primary, CircleShape),
						colors = ButtonDefaults.filledTonalButtonColors(
							containerColor = MaterialTheme.colorScheme.primary
						)
					) {
						Icon(
							imageVector = Icons.Filled.Close,
							contentDescription = stringResource(id = R.string.camera_close_controls_content_description),
							tint = MaterialTheme.colorScheme.onPrimary,
						)
					}
				}
			}
		}
	}
}

@Composable
private fun CameraControlSwitch(
	icon: ImageVector,
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit
) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier.padding(horizontal = 8.dp)
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = Color.White,
			modifier = Modifier.size(24.dp)
		)
		Spacer(modifier = Modifier.width(8.dp))
		Text(
			text = stringResource(id = R.string.camera_flash_text),
			color = Color.White,
			style = MaterialTheme.typography.bodyMedium
		)
		Spacer(modifier = Modifier.width(8.dp))
		Switch(
			checked = checked,
			onCheckedChange = onCheckedChange,
			colors = SwitchDefaults.colors(
				checkedThumbColor = MaterialTheme.colorScheme.primary,
				checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
			)
		)
	}
}

@Composable
fun BottomControls(
	modifier: Modifier = Modifier,
	onCapture: (() -> Unit)?,
	isLoading: Boolean,
	navController: NavHostController,
) {
	Box(
		modifier = modifier
			.fillMaxWidth()
			.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
	) {
		ElevatedButton(
			onClick = { navController.navigate(AppDestinations.SETTINGS_ROUTE) },
			enabled = isLoading.not(),
			modifier = Modifier.align(Alignment.BottomStart),
		) {
			Icon(
				imageVector = Icons.Filled.Settings,
				contentDescription = stringResource(R.string.camera_settings_button),
				modifier = Modifier.size(32.dp),
			)
		}

		if (onCapture != null) {
			FilledTonalButton(
				onClick = onCapture,
				modifier = Modifier
					.size(80.dp)
					.clip(CircleShape)
					.align(Alignment.BottomCenter),
				colors = ButtonDefaults.filledTonalButtonColors(
					containerColor = MaterialTheme.colorScheme.primary,
				),
			) {
				Icon(
					imageVector = Icons.Filled.Camera,
					contentDescription = stringResource(id = R.string.camera_capture_content_description),
					tint = MaterialTheme.colorScheme.onPrimary,
					modifier = Modifier.size(32.dp),
				)
			}
		}

		ElevatedButton(
			onClick = { navController.navigate(AppDestinations.GALLERY_ROUTE) },
			enabled = isLoading.not(),
			modifier = Modifier.align(Alignment.BottomEnd),
		) {
			Icon(
				imageVector = Icons.Filled.PhotoLibrary,
				contentDescription = stringResource(id = R.string.camera_gallery_content_description),
				modifier = Modifier.size(32.dp),
			)
		}
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

@OptIn(ExperimentalUuidApi::class)
private suspend fun handleImageCapture(
	cameraController: CameraController,
	imageSaver: SecureImageManager,
	context: Context,
	orientation: TiffOrientation,
	location: Location?,
	isFlashOn: Boolean,
) {
	val gpsCoordinates = location?.let {
		GpsCoordinates(
			latitude = it.latitude,
			longitude = it.longitude,
		)
	}

	cameraController.setFlashMode(if (isFlashOn) FlashMode.ON else FlashMode.OFF)

	when (val result = cameraController.takePicture()) {
		is ImageCaptureResult.Success -> {
			vibrateDevice(context)
			imageSaver.saveImage(
				byteArray = result.byteArray,
				orientation = orientation,
				latLng = gpsCoordinates
			).let { path ->
				println("Image saved at: $path")
			}
		}

		is ImageCaptureResult.Error -> {
			println("Image Capture Error: ${result.exception.message}")
		}
	}
}
