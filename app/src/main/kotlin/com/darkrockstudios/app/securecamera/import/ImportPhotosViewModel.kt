package com.darkrockstudios.app.securecamera.import

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.darkrockstudios.app.securecamera.BaseViewModel
import com.darkrockstudios.app.securecamera.import.ImportWorker.Companion.NOTIFICATION_ID
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA512
import dev.whyoleg.cryptography.operations.Hasher
import kotlinx.coroutines.launch
import timber.log.Timber

class ImportPhotosViewModel(
	private val appContext: Context,
	private val workManager: WorkManager,
) : BaseViewModel<ImportPhotosState>() {

	val hasher: Hasher = CryptographyProvider.Default.get(SHA512).hasher()
	private var currentImportWorkName: String? = null

	companion object {
		private const val IMPORT_WORK_NAME = "photo_import_work_"
	}

	override fun createState() = ImportPhotosState()

	fun beginImport(
		photos: List<Uri>,
		progress: (curPhoto: Uri) -> Unit,
	) {
		if (photos.isEmpty()) {
			_uiState.value = _uiState.value.copy(
				complete = true
			)
			return
		}

		_uiState.value = _uiState.value.copy(
			totalPhotos = photos.size,
			remainingPhotos = photos.size
		)

		val photoUrisString = photos.map { it.toString() }.toTypedArray()

		val importWorkRequest = OneTimeWorkRequestBuilder<ImportWorker>()
			.setInputData(
				workDataOf(
					ImportWorker.KEY_PHOTO_URIS to photoUrisString
				)
			)
			.build()

		val uniqueWorkId = hasher.hashBlocking(photos.joinToString { it.toString() }.toByteArray())
		val workerName = IMPORT_WORK_NAME + uniqueWorkId
		currentImportWorkName = workerName

		workManager.enqueueUniqueWork(
			workerName,
			ExistingWorkPolicy.KEEP,
			importWorkRequest
		)

		// Observe work progress
		viewModelScope.launch {
			workManager.getWorkInfoByIdLiveData(importWorkRequest.id).observeForever { workInfo ->
				when (workInfo?.state) {
					WorkInfo.State.RUNNING -> {
						val progressData = workInfo.progress
						val totalPhotos = progressData.getInt(ImportWorker.KEY_TOTAL_PHOTOS, 0)
						val remainingPhotos = progressData.getInt(ImportWorker.KEY_REMAINING_PHOTOS, 0)
						val successfulPhotos = progressData.getInt(ImportWorker.KEY_SUCCESSFUL_PHOTOS, 0)
						val failedPhotos = progressData.getInt(ImportWorker.KEY_FAILED_PHOTOS, 0)
						val currentPhotoUriString = progressData.getString(ImportWorker.KEY_CURRENT_PHOTO_URI)

						_uiState.value = _uiState.value.copy(
							totalPhotos = totalPhotos,
							remainingPhotos = remainingPhotos,
							successfulPhotos = successfulPhotos,
							failedPhotos = failedPhotos
						)

						if (currentPhotoUriString != null) {
							val currentPhotoUri = currentPhotoUriString.toUri()
							progress(currentPhotoUri)
						}
					}

					WorkInfo.State.SUCCEEDED -> {
						val outputData = workInfo.outputData
						val successfulPhotos = outputData.getInt(ImportWorker.KEY_SUCCESSFUL_PHOTOS, 0)
						val failedPhotos = outputData.getInt(ImportWorker.KEY_FAILED_PHOTOS, 0)
						val totalPhotos = outputData.getInt(ImportWorker.KEY_TOTAL_PHOTOS, 0)

						_uiState.value = _uiState.value.copy(
							successfulPhotos = successfulPhotos,
							failedPhotos = failedPhotos,
							totalPhotos = totalPhotos,
							remainingPhotos = 0,
							complete = true
						)

						Timber.d("Import completed: $successfulPhotos of $totalPhotos photos imported successfully, $failedPhotos failed")
					}

					WorkInfo.State.FAILED -> {
						Timber.e("Import work failed")
						_uiState.value = _uiState.value.copy(
							complete = true
						)
					}

					WorkInfo.State.CANCELLED -> {
						Timber.d("Import work cancelled")
						_uiState.value = _uiState.value.copy(
							complete = true
						)
					}

					else -> {
						// Other states (BLOCKED, ENQUEUED) - no action needed
					}
				}
			}
		}
	}

	fun cancelImport() {
		currentImportWorkName?.let { workName ->
			Timber.d("Cancelling import work: $workName")
			workManager.cancelUniqueWork(workName)

			val notificationManager =
				appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.cancel(NOTIFICATION_ID)
		}
	}
}

data class ImportPhotosState(
	val totalPhotos: Int = 0,
	val remainingPhotos: Int = 0,
	val successfulPhotos: Int = 0,
	val failedPhotos: Int = 0,
	val complete: Boolean = false,
)
