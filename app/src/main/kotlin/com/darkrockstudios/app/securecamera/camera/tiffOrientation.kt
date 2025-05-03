package com.darkrockstudios.app.securecamera.camera

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

fun TiffOrientation.toDegrees(): Int = when (this) {
	TiffOrientation.STANDARD,
	TiffOrientation.MIRROR_HORIZONTAL -> 0

	TiffOrientation.ROTATE_RIGHT,
	TiffOrientation.MIRROR_HORIZONTAL_AND_ROTATE_LEFT -> 90

	TiffOrientation.UPSIDE_DOWN,
	TiffOrientation.MIRROR_VERTICAL -> 180

	TiffOrientation.ROTATE_LEFT,
	TiffOrientation.MIRROR_HORIZONTAL_AND_ROTATE_RIGHT -> 270
}