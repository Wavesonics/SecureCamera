package com.darkrockstudios.app.securecamera.camera

import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Create and remember a [CameraState] inside composition. */
@Composable
fun rememberCameraState(initialLensFacing: Int = CameraSelector.LENS_FACING_BACK): CameraState {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	val provider = remember { ProcessCameraProvider.getInstance(context).get() }
	val previewView = remember {
		PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
	}

	val state = remember {
		CameraState(
			previewView = previewView,
			lifecycleOwner = lifecycleOwner,
			providerFuture = provider,
			initialLensFacing = initialLensFacing
		)
	}

	// (Re)bind camera whenever lens changes.
	DisposableEffect(state.lensFacing) {
		state.bindCamera()
		onDispose { provider.unbindAll() }
	}

	DisposableEffect(Unit) {
		onDispose { state.shutdown() }
	}

	return state
}

/** Composable that renders the camera preview and UI using the provided [CameraState]. */
@Composable
fun CameraPreview(
	state: CameraState,
	modifier: Modifier = Modifier.fillMaxSize()
) {
	val scope = rememberCoroutineScope()
	// Pinch‑to‑zoom transformable
	val zoomState = rememberTransformableState { zoomChange, _, _ ->
		state.camera?.let { cam ->
			val current = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
			val newZoom = (current * zoomChange).coerceIn(state.minZoom, state.maxZoom)
			state.setZoomRatio(newZoom)
		}
	}

	// focus indicator fade animation
	val indicatorAlpha by animateFloatAsState(
		targetValue = if (state.focusOffset != null) 1f else 0f,
		animationSpec = tween(durationMillis = 300)
	)

	Box(
		modifier = modifier
			.transformable(zoomState)
			.pointerInput(Unit) {
				detectTapGestures { offset ->
					state.focusAt(offset)

					scope.launch {
						delay(800)
						state.clearFocusOffset()
					}
				}
			}
	) {
		// Camera preview
		AndroidView(
			factory = { state.previewView },
			modifier = Modifier.fillMaxSize()
		)

		// Draw focus ring
		state.focusOffset?.let { pos ->
			Canvas(
				modifier = Modifier
					.fillMaxSize()
					.pointerInput(Unit) {} // intercept taps
			) {
				drawCircle(
					color = Color.White.copy(alpha = indicatorAlpha),
					radius = 40f,
					center = pos
				)
			}
		}
	}
}
