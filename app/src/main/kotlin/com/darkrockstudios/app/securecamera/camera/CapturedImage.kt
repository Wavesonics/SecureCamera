package com.darkrockstudios.app.securecamera.camera

import android.graphics.Bitmap
import android.graphics.Matrix

data class CapturedImage(
	val sensorBitmap: Bitmap,
	val timestamp: Long,
	val rotationMatrix: Matrix,
	val rotationDegrees: Int,
)