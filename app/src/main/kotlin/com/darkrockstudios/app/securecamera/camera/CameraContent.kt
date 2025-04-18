package com.darkrockstudios.app.securecamera.camera

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.darkrockstudios.app.securecamera.Flashlight
import com.darkrockstudios.app.securecamera.FlashlightOff
import com.darkrockstudios.app.securecamera.decodeToImageBitmap
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.*
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.ui.CameraPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.uuid.ExperimentalUuidApi


@Composable
internal fun CameraContent(
	cameraController: MutableState<CameraController?>,
	navController: NavHostController,
	modifier: Modifier,
	paddingValues: PaddingValues,
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
			},
			onCameraControllerReady = {
				print("==> Camera Controller Ready")
				cameraController.value = it
			}
		)

		cameraController.value?.let { controller ->
			EnhancedCameraScreen(
				cameraController = controller,
				navController = navController,
				paddingValues = paddingValues,
			)
		}

		CameraBottomBar(navController)
	}
}

@Composable
fun EnhancedCameraScreen(
	cameraController: CameraController,
	navController: NavHostController,
	paddingValues: PaddingValues? = null,
) {
	val scope = rememberCoroutineScope()
	var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
	var isFlashOn by remember { mutableStateOf(false) }
	var isTorchOn by remember { mutableStateOf(false) }
	var isTopControlsVisible by remember { mutableStateOf(false) }
	val imageSaver = koinInject<SecureImageManager>()

	Box(modifier = Modifier.fillMaxSize()) {
		if (!isTopControlsVisible) {
			IconButton(
				onClick = { isTopControlsVisible = true },
				modifier = Modifier
					.align(Alignment.TopEnd)
					.padding(
						top = paddingValues?.calculateTopPadding()?.plus(16.dp) ?: 16.dp,
						end = 16.dp
					)
					.background(MaterialTheme.colorScheme.primary, CircleShape)
					.padding(8.dp)
			) {
				Icon(
					imageVector = Icons.Filled.MoreVert,
					contentDescription = "More Options",
					tint = Color.White
				)
			}
		}

		TopControlsBar(
			isFlashOn = isFlashOn,
			isTorchOn = isTorchOn,
			isVisible = isTopControlsVisible,
			onFlashToggle = {
				isFlashOn = it
				cameraController.toggleFlashMode()
			},
			onLensToggle = { cameraController.toggleCameraLens() },
			onClose = { isTopControlsVisible = false },
			paddingValues = paddingValues
		)

		BottomControls(
			modifier = Modifier.align(Alignment.BottomCenter),
			navController = navController,
			onCapture = {
				scope.launch {
					handleImageCapture(
						cameraController = cameraController,
						imageSaver = imageSaver,
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

					IconButton(
						onClick = onClose,
						modifier = Modifier
							.background(MaterialTheme.colorScheme.primary, CircleShape)
							.padding(8.dp)
					) {
						Icon(
							imageVector = Icons.Filled.Close,
							contentDescription = "Close Controls",
							tint = Color.White
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
			text = "Flash",
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
				imageVector = Icons.Filled.Home,
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
			delay(2000)
			onDismiss()
		}
	}
}

@OptIn(ExperimentalUuidApi::class)
private suspend fun handleImageCapture(
	cameraController: CameraController,
	imageSaver: SecureImageManager,
	onImageCaptured: (ImageBitmap) -> Unit
) {
	when (val result = cameraController.takePicture()) {
		is ImageCaptureResult.Success -> {
			val bitmap = result.byteArray.decodeToImageBitmap()
			onImageCaptured(bitmap)
			imageSaver.saveImage(
				byteArray = result.byteArray,
			).let { path ->
				println("Image saved at: $path")
			}
		}

		is ImageCaptureResult.Error -> {
			println("Image Capture Error: ${result.exception.message}")
		}
	}
}
