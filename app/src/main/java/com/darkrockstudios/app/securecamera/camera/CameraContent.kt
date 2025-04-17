package com.darkrockstudios.app.securecamera.camera

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.darkrockstudios.app.securecamera.*
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.*
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.ui.CameraPreview
import com.kashif.imagesaverplugin.ImageSaverPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@Composable
internal fun CameraContent(
	cameraController: MutableState<CameraController?>,
	imageSaverPlugin: ImageSaverPlugin,
	navController: NavHostController,
	modifier: Modifier,
	paddingValues: PaddingValues? = null,
) {
	Box(modifier = modifier.fillMaxSize()) {
		CameraPreview(
			modifier = Modifier.fillMaxSize(),
			cameraConfiguration = {
				setCameraLens(CameraLens.BACK)
				setFlashMode(FlashMode.OFF)
				setImageFormat(ImageFormat.JPEG)
				setDirectory(Directory.PICTURES)
				setTorchMode(TorchMode.OFF)
				addPlugin(imageSaverPlugin)
			},
			onCameraControllerReady = {
				print("==> Camera Controller Ready")
				cameraController.value = it

			}
		)

		cameraController.value?.let { controller ->
			EnhancedCameraScreen(
				cameraController = controller,
				imageSaverPlugin = imageSaverPlugin,
				navController = navController,
				paddingValues = paddingValues,
			)
		}
	}
}

@Composable
fun EnhancedCameraScreen(
	cameraController: CameraController,
	imageSaverPlugin: ImageSaverPlugin,
	navController: NavHostController,
	paddingValues: PaddingValues? = null,
) {
	val scope = rememberCoroutineScope()
	var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
	var isFlashOn by remember { mutableStateOf(false) }
	var isTorchOn by remember { mutableStateOf(false) }

	Box(modifier = Modifier.fillMaxSize()) {
		TopControlsBar(
			isFlashOn = isFlashOn,
			isTorchOn = isTorchOn,
			onFlashToggle = {
				isFlashOn = it
				cameraController.toggleFlashMode()
			},
			onTorchToggle = {
				isTorchOn = it
				cameraController.toggleTorchMode()
			},
			onLensToggle = { cameraController.toggleCameraLens() },
			paddingValues = paddingValues
		)

		BottomControls(
			modifier = Modifier.align(Alignment.BottomCenter),
			navController = navController,
			onCapture = {
				scope.launch {
					handleImageCapture(
						cameraController = cameraController,
						imageSaverPlugin = imageSaverPlugin,
						onImageCaptured = {
							imageBitmap = it
						}
					)
				}
			}
		)

		CapturedImagePreview(imageBitmap = imageBitmap) {
			imageBitmap = null
		}
	}
}

@Composable
private fun TopControlsBar(
	isFlashOn: Boolean,
	isTorchOn: Boolean,
	onFlashToggle: (Boolean) -> Unit,
	onTorchToggle: (Boolean) -> Unit,
	onLensToggle: () -> Unit,
	paddingValues: PaddingValues? = null
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
					text = "Flash",
					checked = isFlashOn,
					onCheckedChange = onFlashToggle
				)

				CameraControlSwitch(
					icon = if (isTorchOn) Flash_on else Flash_off,
					text = "Torch",
					checked = isTorchOn,
					onCheckedChange = onTorchToggle
				)
			}

			IconButton(
				onClick = onLensToggle,
				modifier = Modifier
					.background(MaterialTheme.colorScheme.primary, CircleShape)
					.padding(8.dp)
			) {
				Icon(
					imageVector = Icons.Filled.Refresh,
					contentDescription = "Toggle Camera",
					tint = Color.White
				)
			}
		}
	}
}

@Composable
private fun CameraControlSwitch(
	icon: ImageVector,
	text: String,
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
			text = text,
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
private fun BottomControls(modifier: Modifier = Modifier, onCapture: () -> Unit, navController: NavHostController) {
	Box(
		modifier = modifier
			.fillMaxWidth()
			.padding(bottom = 32.dp),
	) {
		FilledTonalButton(
			onClick = onCapture,
			modifier = Modifier
				.size(80.dp)
				.clip(CircleShape)
				.align(Alignment.BottomCenter),
			colors = ButtonDefaults.filledTonalButtonColors(
				containerColor = MaterialTheme.colorScheme.primary
			)
		) {
			Icon(
				imageVector = Icons.Filled.Done,
				contentDescription = "Capture",
				tint = Color.White,
				modifier = Modifier.size(32.dp)
			)
		}

		IconButton(
			onClick = { navController.navigate(AppDestinations.GALLERY_ROUTE) },
			modifier = Modifier
				.background(MaterialTheme.colorScheme.primary, CircleShape)
				.padding(8.dp)
				.align(Alignment.BottomEnd),
		) {
			Icon(
				imageVector = Icons.Filled.Refresh,
				contentDescription = "Gallery",
				tint = Color.White
			)
		}
	}
}

@Composable
private fun CapturedImagePreview(
	imageBitmap: ImageBitmap?,
	onDismiss: () -> Unit
) {
	imageBitmap?.let { bitmap ->
		Surface(
			modifier = Modifier.fillMaxSize(),
			color = Color.Black.copy(alpha = 0.9f)
		) {
			Box(modifier = Modifier.fillMaxSize()) {
				Image(
					bitmap = bitmap,
					contentDescription = "Captured Image",
					modifier = Modifier
						.fillMaxSize()
						.padding(16.dp),
					contentScale = ContentScale.Fit
				)

				IconButton(
					onClick = onDismiss,
					modifier = Modifier
						.align(Alignment.TopEnd)
						.padding(16.dp)
						.background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f), CircleShape)
				) {
					Icon(
						imageVector = Icons.Filled.Close,
						contentDescription = "Close Preview",
						tint = MaterialTheme.colorScheme.onSurface,
					)
				}
			}
		}

		LaunchedEffect(bitmap) {
			delay(3000)
			onDismiss()
		}
	}
}

@OptIn(ExperimentalUuidApi::class)
private suspend fun handleImageCapture(
	cameraController: CameraController,
	imageSaverPlugin: ImageSaverPlugin,
	onImageCaptured: (ImageBitmap) -> Unit
) {
	when (val result = cameraController.takePicture()) {
		is ImageCaptureResult.Success -> {
			val bitmap = result.byteArray.decodeToImageBitmap()
			onImageCaptured(bitmap)
			if (!imageSaverPlugin.config.isAutoSave) {
				val customName = "Manual_${Uuid.random().toHexString()}"
				imageSaverPlugin.saveImage(
					byteArray = result.byteArray,
					imageName = customName
				)?.let { path ->
					println("Image saved at: $path")
				}
			}
		}

		is ImageCaptureResult.Error -> {
			println("Image Capture Error: ${result.exception.message}")
		}
	}
}
