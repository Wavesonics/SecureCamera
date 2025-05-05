package com.darkrockstudios.app.securecamera.obfuscation

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.media.FaceDetector
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AndroidFacialDetection : FacialDetection {
	override suspend fun processForFaces(bitmap: Bitmap): List<FacialDetection.FoundFace> =
		withContext(Dispatchers.Default) {
			// Android's FaceDetector requires RGB_565 format
			val bitmapForDetection = if (bitmap.config != Bitmap.Config.RGB_565) {
				try {
					createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565).also { targetBitmap ->
						val canvas = android.graphics.Canvas(targetBitmap)
						canvas.drawBitmap(bitmap, 0f, 0f, null)
					}
				} catch (e: Exception) {
					Timber.e(e, "Failed to convert bitmap to RGB_565")
					return@withContext emptyList<FacialDetection.FoundFace>()
				}
			} else {
				bitmap
			}

			// Maximum number of faces to detect
			val maxFaces = 100

			try {
				val detector = FaceDetector(bitmapForDetection.width, bitmapForDetection.height, maxFaces)
				val faces = arrayOfNulls<FaceDetector.Face>(maxFaces)

				// Find faces in the bitmap
				val facesFound = detector.findFaces(bitmapForDetection, faces)
				Timber.d("Found $facesFound faces using Android FaceDetector")

				// Convert the detected faces to our FoundFace format
				val foundFaces = mutableListOf<FacialDetection.FoundFace>()

				for (i in 0 until facesFound) {
					faces[i]?.let { face ->
						val midPoint = PointF()
						face.getMidPoint(midPoint)

						// Calculate the bounding box based on the face's position and confidence
						val eyeDistance = face.eyesDistance()
						val confidence = face.confidence()

						// Use eye distance to estimate face size
						// The multipliers are approximations and may need adjustment
						val faceWidth = (eyeDistance * 2.5f).toInt()
						val faceHeight = (eyeDistance * 3.0f).toInt()

						val left = (midPoint.x - faceWidth / 2).toInt().coerceAtLeast(0)
						val top = (midPoint.y - faceHeight / 2).toInt().coerceAtLeast(0)
						val right = (midPoint.x + faceWidth / 2).toInt().coerceAtMost(bitmap.width)
						val bottom = (midPoint.y + faceHeight / 2).toInt().coerceAtMost(bitmap.height)

						val boundingBox = Rect(left, top, right, bottom)

						// Calculate approximate eye positions based on the midpoint and eye distance
						val leftEyeX = midPoint.x - eyeDistance / 2
						val rightEyeX = midPoint.x + eyeDistance / 2
						val eyeY = midPoint.y - eyeDistance / 8  // Slight adjustment for eye height

						val eyes = FacialDetection.FoundFace.Eyes(
							left = PointF(leftEyeX, eyeY),
							right = PointF(rightEyeX, eyeY)
						)

						foundFaces.add(
							FacialDetection.FoundFace(
								boundingBox = boundingBox,
								eyes = eyes
							)
						)
					}
				}

				// Clean up if we created a new bitmap
				if (bitmapForDetection != bitmap) {
					bitmapForDetection.recycle()
				}

				foundFaces
			} catch (e: Exception) {
				Timber.e(e, "Error detecting faces with Android FaceDetector")
				emptyList()
			}
		}
}
