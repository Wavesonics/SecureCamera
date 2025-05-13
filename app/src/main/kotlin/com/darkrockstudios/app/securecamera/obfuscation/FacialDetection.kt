package com.darkrockstudios.app.securecamera.obfuscation

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect

interface FacialDetection {
	suspend fun processForFaces(bitmap: Bitmap): List<FoundFace>

	data class FoundFace(
		val boundingBox: Rect,
		val eyes: Eyes?
	) {
		data class Eyes(
			val left: PointF,
			val right: PointF,
		)
	}
}