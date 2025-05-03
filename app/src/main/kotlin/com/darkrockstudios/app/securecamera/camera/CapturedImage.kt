package com.darkrockstudios.app.securecamera.camera

import android.graphics.Bitmap
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class CapturedImage(
	val sensorBitmap: Bitmap,
	val timestamp: Instant,
	val rotationDegrees: Int,
)