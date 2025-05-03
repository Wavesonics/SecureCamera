package com.darkrockstudios.app.securecamera.import

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ashampoo.kim.Kim
import com.ashampoo.kim.common.convertToPhotoMetadata
import com.ashampoo.kim.model.GpsCoordinates
import com.ashampoo.kim.model.TiffOrientation
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.camera.CapturedImage
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.camera.toDegrees
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class ImportWorker(
	appContext: Context,
	params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

	private val secureImageRepository: SecureImageRepository by inject()

	companion object {
		const val KEY_PHOTO_URIS = "KEY_PHOTO_URIS"
		const val KEY_TOTAL_PHOTOS = "KEY_TOTAL_PHOTOS"
		const val KEY_REMAINING_PHOTOS = "KEY_REMAINING_PHOTOS"
		const val KEY_SUCCESSFUL_PHOTOS = "KEY_SUCCESSFUL_PHOTOS"
		const val KEY_FAILED_PHOTOS = "KEY_FAILED_PHOTOS"
		const val KEY_CURRENT_PHOTO_URI = "KEY_CURRENT_PHOTO_URI"

		private const val NOTIFICATION_CHANNEL_ID = "import_photos_channel"
		private const val NOTIFICATION_ID = 1
	}

	@OptIn(ExperimentalTime::class)
	override suspend fun doWork(): Result {
		val photoUrisString = inputData.getStringArray(KEY_PHOTO_URIS) ?: return Result.failure()
		val photoUris = photoUrisString.map { it.toUri() }

		if (photoUris.isEmpty()) {
			return Result.success()
		}

		// Set up as foreground service with notification
		setForeground(createForegroundInfo(0, photoUris.size))

		var successfulPhotos = 0
		var failedPhotos = 0

		photoUris.forEachIndexed { index, photoUri ->
			// Update progress data
			val progressData = workDataOf(
				KEY_TOTAL_PHOTOS to photoUris.size,
				KEY_REMAINING_PHOTOS to (photoUris.size - index - 1),
				KEY_SUCCESSFUL_PHOTOS to successfulPhotos,
				KEY_FAILED_PHOTOS to failedPhotos,
				KEY_CURRENT_PHOTO_URI to photoUri.toString()
			)
			setProgress(progressData)

			setForeground(createForegroundInfo(index + 1, photoUris.size))

			val jpgBytes = readPhotoBytes(photoUri)
			if (jpgBytes != null) {
				var orientation: TiffOrientation = TiffOrientation.STANDARD
				var coords: GpsCoordinates? = null
				var timestamp: Instant = Instant.fromEpochSeconds(0)

				Kim.readMetadata(jpgBytes)?.convertToPhotoMetadata()?.let { imageMetadata ->
					orientation = imageMetadata.orientation ?: TiffOrientation.STANDARD
					coords = imageMetadata.gpsCoordinates
					timestamp =
						Instant.fromEpochSeconds(imageMetadata.takenDate?.milliseconds?.inWholeSeconds ?: 0L)
				}

				try {
					val bitmap = BitmapFactory.decodeByteArray(jpgBytes, 0, jpgBytes.size)
					val photoToSave = CapturedImage(
						sensorBitmap = bitmap,
						timestamp = timestamp,
						rotationDegrees = orientation.toDegrees(),
					)

					secureImageRepository.saveImage(
						image = photoToSave,
						latLng = coords,
						applyRotation = true,
					)

					successfulPhotos++

				} catch (_: Exception) {
					Timber.e("Failed to decode image: $photoUri")
					failedPhotos++
				}
			} else {
				Timber.e("Unable to read image for import: $photoUri")
				failedPhotos++
			}
		}

		// Return success with the final count of successful and failed imports
		return Result.success(
			workDataOf(
				KEY_SUCCESSFUL_PHOTOS to successfulPhotos,
				KEY_FAILED_PHOTOS to failedPhotos,
				KEY_TOTAL_PHOTOS to photoUris.size
			)
		)
	}

	private suspend fun readPhotoBytes(uri: Uri): ByteArray? {
		return withContext(Dispatchers.IO) {
			applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
				inputStream.readBytes()
			}
		}
	}

	private fun createForegroundInfo(progress: Int, total: Int): ForegroundInfo {
		createNotificationChannel()

		val title = applicationContext.getString(R.string.import_worker_notification_title)
		val contentText = applicationContext.getString(R.string.import_worker_notification_content, progress, total)

		val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.setContentTitle(title)
			.setContentText(contentText)
			.setSmallIcon(android.R.drawable.ic_menu_gallery)
			.setOngoing(true)
			.setProgress(total, progress, false)
			.build()

		return ForegroundInfo(
			NOTIFICATION_ID,
			notification,
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
		)
	}

	private fun createNotificationChannel() {
		val name = applicationContext.getString(R.string.import_worker_channel_name)
		val description = applicationContext.getString(R.string.import_worker_channel_description)
		val importance = NotificationManager.IMPORTANCE_LOW
		val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
			this.description = description
		}

		val notificationManager =
			applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.createNotificationChannel(channel)
	}
}
