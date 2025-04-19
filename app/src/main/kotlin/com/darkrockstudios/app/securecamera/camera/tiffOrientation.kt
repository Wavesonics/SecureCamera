package com.darkrockstudios.app.securecamera.camera

import android.content.Context
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.ashampoo.kim.model.TiffOrientation

/**
 * Pure logic: convert a rotation (0/90/180/270 deg) and an optional mirror flag
 * into the correct TIFF orientation value.
 */
fun calculateTiffOrientation(
	rotationDegrees: Int,
	isMirrored: Boolean = false
): TiffOrientation = when (rotationDegrees) {
	0 -> if (isMirrored) TiffOrientation.MIRROR_HORIZONTAL
	else TiffOrientation.STANDARD

	90 -> if (isMirrored) TiffOrientation.MIRROR_HORIZONTAL_AND_ROTATE_LEFT
	else TiffOrientation.ROTATE_RIGHT

	180 -> if (isMirrored) TiffOrientation.MIRROR_VERTICAL
	else TiffOrientation.UPSIDE_DOWN

	270 -> if (isMirrored) TiffOrientation.MIRROR_HORIZONTAL_AND_ROTATE_RIGHT
	else TiffOrientation.ROTATE_LEFT

	else -> TiffOrientation.STANDARD            // Fallback
}

/**
 * Compose hook that keeps the orientation up to date automatically.
 *
 * @param isMirrored    true if the final image will be mirror‑flipped (e.g. selfie preview).
 */
@Composable
fun rememberCurrentTiffOrientation(isMirrored: Boolean = false): TiffOrientation {
	val context = LocalContext.current
	val configuration = LocalConfiguration.current            // Triggers recomposition on rotation
	val rotationDegrees = remember(configuration) {            // Re‑evaluate on each rotation
		currentDisplayRotationDegrees(context)
	}
	return calculateTiffOrientation(rotationDegrees, isMirrored)
}

/** Helper to get the display’s *absolute* rotation in degrees (0, 90, 180, 270). */
private fun currentDisplayRotationDegrees(context: Context): Int {
	val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
		context.display?.rotation ?: Surface.ROTATION_0
	} else {
		@Suppress("DEPRECATION")
		(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
			.defaultDisplay.rotation
	}

	return when (rotation) {
		Surface.ROTATION_90 -> 90
		Surface.ROTATION_180 -> 180
		Surface.ROTATION_270 -> 270
		else -> 0            // ROTATION_0
	}
}
