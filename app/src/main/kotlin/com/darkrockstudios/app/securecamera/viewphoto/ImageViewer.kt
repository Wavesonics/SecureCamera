package com.darkrockstudios.app.securecamera.viewphoto

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale

@Stable
class ImageViewerState internal constructor(
    scale: Float = 1f,
    offset: Offset = Offset.Zero,
    val minScale: Float = 1f,
    val maxScale: Float = 5f,
) {
    var scale by mutableStateOf(scale)
    var offset by mutableStateOf(offset)

    fun reset() {
        scale = 1f
        offset = Offset.Zero
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
 * @param bitmap	The photo to display.
 * @param state		State returned from [rememberImageViewerState].
 * @param modifier	Extra modifiers (size, background, etc.).
 */
@Composable
fun ImageViewer(
    bitmap: ImageBitmap,
    state: ImageViewerState,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clipToBounds()
            .pointerInput(state) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // ----- Zoom -----
                    val newScale = (state.scale * zoom).coerceIn(state.minScale, state.maxScale)

                    // ----- Pan (don’t forget current scale) -----
                    val newOffset = state.offset + pan

                    state.scale = newScale
                    state.offset = newOffset
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
                translationX = state.offset.x
                translationY = state.offset.y
            }
    )
}