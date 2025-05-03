package com.darkrockstudios.app.securecamera.camera

import android.annotation.SuppressLint
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Holds the mutable state and low‑level camera plumbing so that the UI composable is lightweight.
 */
@Stable
class CameraState internal constructor(
	val previewView: PreviewView,
	private val lifecycleOwner: LifecycleOwner,
	private val providerFuture: ProcessCameraProvider,
	initialLensFacing: Int = CameraSelector.LENS_FACING_BACK,
	initialFlashMode: Int = ImageCapture.FLASH_MODE_OFF,
) {
	private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

	var lensFacing by mutableStateOf(initialLensFacing)
		private set

	var camera: Camera? by mutableStateOf(null)
		private set

	var minZoom by mutableStateOf(1f)
		private set
	var maxZoom by mutableStateOf(1f)
		private set

	var focusOffset by mutableStateOf<Offset?>(null)
		private set

	fun clearFocusOffset() {
		focusOffset = null
	}

	private var imageCapture: ImageCapture? = null

	private var _flashMode by mutableStateOf(initialFlashMode)
	var flashMode: Int
		get() = _flashMode
		set(value) {
			if (value !in arrayOf(
					ImageCapture.FLASH_MODE_OFF,
					ImageCapture.FLASH_MODE_ON,
					ImageCapture.FLASH_MODE_AUTO
				)
			) return

			_flashMode = value
			imageCapture?.flashMode = value
		}

	/** Toggle between front and back lenses. */
	fun toggleLens() {
		switchLens(
			if (lensFacing == CameraSelector.LENS_FACING_BACK)
				CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
		)
	}

	/**
	 * Switch explicitly to the given lens facing (use [CameraSelector.LENS_FACING_BACK] or
	 * [CameraSelector.LENS_FACING_FRONT]). If already on that lens, this is a no‑op.
	 */
	fun switchLens(facing: Int) {
		if (facing == lensFacing) return
		lensFacing = facing
		bindCamera()
	}

	/** Call after external zoom gesture math. */
	fun setZoomRatio(ratio: Float) {
		camera?.cameraControl?.setZoomRatio(ratio)
	}

	fun getZoomState() = camera?.cameraInfo?.zoomState

	/** Focus + meter at the given px location from Compose coordinates. */
	fun focusAt(offset: Offset) {
		camera?.let { cam ->
			val factory = previewView.meteringPointFactory
			val point = factory.createPoint(offset.x, offset.y)
			val action = FocusMeteringAction.Builder(
				point,
				FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
			).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
			cam.cameraControl.startFocusAndMetering(action)

			focusOffset = offset
		}
	}

	/**
	 * Suspend version of capturePhoto that returns a Result containing the JPEG bytes on success
	 * or an exception on failure.
	 */
	@OptIn(ExperimentalTime::class)
	@SuppressLint("MissingPermission")
	suspend fun capturePhoto(): Result<CapturedImage> = suspendCoroutine { continuation ->
		val capture = imageCapture ?: run {
			continuation.resume(Result.failure(IllegalStateException("ImageCapture not ready")))
			return@suspendCoroutine
		}

		capture.flashMode = flashMode

		capture.takePicture(
			cameraExecutor,
			object : ImageCapture.OnImageCapturedCallback() {
				override fun onCaptureSuccess(image: ImageProxy) {
					try {
						val captured = CapturedImage(
							sensorBitmap = image.toBitmap(),
							timestamp = Clock.System.now(),
							rotationDegrees = image.imageInfo.rotationDegrees,
						)
						continuation.resume(Result.success(captured))
					} catch (t: Throwable) {
						continuation.resume(Result.failure(t))
					} finally {
						image.close()
					}
				}

				override fun onError(exception: ImageCaptureException) {
					continuation.resume(Result.failure(exception))
				}
			}
		)
	}

	internal fun bindCamera() {
		val provider = providerFuture

		val preview = Preview.Builder().build().also {
			it.surfaceProvider = previewView.surfaceProvider
		}

		imageCapture = ImageCapture.Builder()
			.setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
			.build()

		val selector = CameraSelector.Builder()
			.requireLensFacing(lensFacing)
			.build()

		provider.unbindAll()
		camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)

		// Observe zoom bounds
		camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) {
			minZoom = it.minZoomRatio
			maxZoom = it.maxZoomRatio
		}
	}

	// Call when your app/session finishes with the camera
	fun shutdown() {
		cameraExecutor.shutdown()
	}
}