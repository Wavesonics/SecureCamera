package com.darkrockstudios.app.securecamera.camera

import android.graphics.Bitmap
import kotlin.time.Instant

data class CapturedImage(
	val sensorBitmap: Bitmap,
	val timestamp: Instant,
	val rotationDegrees: Int,
)
