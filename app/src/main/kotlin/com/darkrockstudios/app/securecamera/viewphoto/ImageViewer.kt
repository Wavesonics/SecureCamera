package com.darkrockstudios.app.securecamera.viewphoto

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged

@Stable
class ImageViewerState internal constructor(
	scale: Float = 1f,
	offset: Offset = Offset.Zero,
	val minScale: Float = 1f,
	val maxScale: Float = 5f,
) {
	var scale by mutableStateOf(scale)
	var offset by mutableStateOf(offset)
	var isGestureInProgress by mutableStateOf(false)
	var containerSize by mutableStateOf(Size.Zero)
	var imageSize by mutableStateOf(Size.Zero)

	fun reset() {
		scale = 1f
		offset = Offset.Zero
	}

	fun calculateConstrainedOffset(currentOffset: Offset, scale: Float): Offset {
		if (scale <= 1f) {
			return Offset.Zero
		}

		val scaledImageWidth = imageSize.width * scale
		val scaledImageHeight = imageSize.height * scale

		val maxX = maxOf(0f, scaledImageWidth - containerSize.width)
		val maxY = maxOf(0f, scaledImageHeight - containerSize.height)

		// This allows panning the image so that its edge aligns with the container edge
		return Offset(
			x = currentOffset.x.coerceIn(-maxX / 2, maxX / 2),
			y = currentOffset.y.coerceIn(-maxY / 2, maxY / 2)
		)
	}
}

/**
 * Call this from your Composable to keep the state across recompositions.
 */
@Composable
fun rememberImageViewerState(
	initialScale: Float = 1f,
	initialOffset: Offset = Offset.Zero,
	minScale: Float = 1f,
	maxScale: Float = 5f,
): ImageViewerState = remember {
	ImageViewerState(
		scale = initialScale,
		offset = initialOffset,
		minScale = minScale,
		maxScale = maxScale,
	)
}

/**
 * A zoomable / pannable image-viewer composable.
 *
 * @param bitmap    The photo to display.
 * @param state        State returned from [rememberImageViewerState].
 * @param modifier    Extra modifiers (size, background, etc.).
 */
@Composable
fun ImageViewer(
	bitmap: ImageBitmap,
	state: ImageViewerState,
	modifier: Modifier = Modifier,
) {
	LaunchedEffect(bitmap) {
		state.imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
	}

	var isGestureInProgress by remember { mutableStateOf(false) }

	// Animate offset when gesture ends
	val animatedOffset by animateOffsetAsState(
		targetValue = if (isGestureInProgress) {
			println("Animating, gesture in progress")
			state.offset
		} else {
			println("Animating, gesture NOT in progress")
			state.calculateConstrainedOffset(state.offset, state.scale)
		}
	) {
		state.offset = it
	}

	state.isGestureInProgress = isGestureInProgress

	Image(
		bitmap = bitmap,
		contentDescription = null,
		contentScale = ContentScale.Fit,
		modifier = modifier
			.clipToBounds()
			.onSizeChanged { size ->
				state.containerSize = Size(size.width.toFloat(), size.height.toFloat())
			}
			// Track touch events to detect when gesture starts and ends
			.pointerInput(Unit) {
				awaitPointerEventScope {
					while (true) {
						val event = awaitPointerEvent()
						when (event.type) {
							PointerEventType.Press -> {
								println("Press")
								isGestureInProgress = true
							}

							PointerEventType.Release -> {
								println("Release")
								isGestureInProgress = false
							}
						}
					}
				}
			}
			// Handle transform gestures (pan and zoom)
			.pointerInput(state) {
				detectTransformGestures { _, pan, zoom, _ ->
					val newScale = (state.scale * zoom).coerceIn(state.minScale, state.maxScale)

					state.scale = newScale
					state.offset = state.offset + pan
				}
			}
			.pointerInput(Unit) {
				detectTapGestures(
					onDoubleTap = { state.reset() }
				)
			}
			.graphicsLayer {
				scaleX = state.scale
				scaleY = state.scale

				translationX = if (isGestureInProgress) state.offset.x else animatedOffset.x
				translationY = if (isGestureInProgress) state.offset.y else animatedOffset.y
			}
	)
}
