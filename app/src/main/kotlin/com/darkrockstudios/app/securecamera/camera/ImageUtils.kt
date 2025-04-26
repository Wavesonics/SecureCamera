package com.darkrockstudios.app.securecamera.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

internal fun Bitmap.rotate(degrees: Int): Bitmap {
	val m = Matrix();
	m.postRotate(degrees.toFloat())
	return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}

internal fun Bitmap.toJpegByteArray(quality: Int = 90): ByteArray {
	val out = ByteArrayOutputStream()
	compress(Bitmap.CompressFormat.JPEG, quality, out)
	return out.toByteArray()
}

internal fun imageProxyToBytes(proxy: ImageProxy): ByteArray {
	val buffer: ByteBuffer = proxy.planes[0].buffer
	return ByteArray(buffer.remaining()).also { buffer.get(it) }
}

internal fun rotateAndEncode(proxy: ImageProxy, quality: Int = 90): ByteArray {
	val bmp = BitmapFactory.decodeByteArray(imageProxyToBytes(proxy), 0, proxy.planes[0].buffer.remaining())
	val rotation = proxy.imageInfo.rotationDegrees
	val rotated = if (rotation == 0) bmp else bmp.rotate(rotation)
	return rotated.toJpegByteArray().also { if (rotated != bmp) bmp.recycle() }
}