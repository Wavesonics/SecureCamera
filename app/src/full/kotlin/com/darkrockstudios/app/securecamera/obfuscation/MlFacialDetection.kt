package com.darkrockstudios.app.securecamera.obfuscation

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class MlFacialDetection : FacialDetection {
	private val detector = FaceDetection.getClient(
		FaceDetectorOptions.Builder()
			.setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
			.setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
			.setMinFaceSize(0.02f)
			.build()
	)

	override suspend fun processForFaces(bitmap: Bitmap): List<FacialDetection.FoundFace> {
		val inputImage = InputImage.fromBitmap(bitmap, 0)

		return suspendCancellableCoroutine { continuation ->
			detector.process(inputImage)
				.addOnSuccessListener { foundFaces ->
					val newRegions = foundFaces.map { face ->
						val leftEye =
							face.allLandmarks.find { it.landmarkType == FaceLandmark.LEFT_EYE }
						val rightEye =
							face.allLandmarks.find { it.landmarkType == FaceLandmark.RIGHT_EYE }
						val eyes = if (leftEye != null && rightEye != null) {
							FacialDetection.FoundFace.Eyes(
								left = leftEye.position,
								right = rightEye.position,
							)
						} else {
							null
						}
						FacialDetection.FoundFace(
							boundingBox = face.boundingBox,
							eyes = eyes
						)
					}
					continuation.resume(newRegions)
				}.addOnFailureListener { e ->
					Timber.Forest.e(e, "Failed face detection in Image")
					continuation.resume(emptyList())
				}
		}
	}
}