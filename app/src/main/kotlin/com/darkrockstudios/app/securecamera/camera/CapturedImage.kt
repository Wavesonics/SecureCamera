package com.darkrockstudios.app.securecamera.camera

import android.graphics.Bitmap

data class CapturedImage(
	val sensorBitmap: Bitmap,
	val timestamp: Long,
	val rotationDegrees: Int,
)