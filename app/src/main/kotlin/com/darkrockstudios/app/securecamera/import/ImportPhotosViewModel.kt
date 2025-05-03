package com.darkrockstudios.app.securecamera.import

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ashampoo.kim.Kim
import com.ashampoo.kim.common.convertToPhotoMetadata
import com.ashampoo.kim.model.GpsCoordinates
import com.ashampoo.kim.model.TiffOrientation
import com.darkrockstudios.app.securecamera.BaseViewModel
import com.darkrockstudios.app.securecamera.camera.CapturedImage
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.camera.toDegrees
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ImportPhotosViewModel(
	private val appContext: Context,
	private val secureImageRepository: SecureImageRepository,
) : BaseViewModel<ImportPhotosState>() {

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

		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				photos.forEachIndexed { index, photoUri ->
					progress(photoUri)

					val remaining = photos.size - index - 1

					_uiState.value = _uiState.value.copy(
						remainingPhotos = remaining
					)
					val jpgBytes = readPhotoBytes(photoUri)
					if (jpgBytes != null) {
						var orientation: TiffOrientation = TiffOrientation.STANDARD
						var coords: GpsCoordinates? = null
						var timestamp: Long = 0L

						Kim.readMetadata(jpgBytes)?.convertToPhotoMetadata()?.let { imageMetadata ->
							orientation = imageMetadata.orientation ?: TiffOrientation.STANDARD
							coords = imageMetadata.gpsCoordinates
							timestamp = imageMetadata.takenDate ?: 0L
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

							_uiState.updateAndGet { it.copy(successfulPhotos = _uiState.value.successfulPhotos + 1) }

						} catch (_: Exception) {
							Timber.e("Failed to decode image: $photoUri")
						}
					} else {
						Timber.e("Unable to read image for import: $photoUri")
					}
				}

				_uiState.value = _uiState.value.copy(
					complete = true
				)
			}
		}
	}

	private suspend fun readPhotoBytes(uri: Uri): ByteArray? {
		return withContext(Dispatchers.IO) {
			appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
				inputStream.readBytes()
			}
		}
	}
}

data class ImportPhotosState(
	val totalPhotos: Int = 0,
	val remainingPhotos: Int = 0,
	val successfulPhotos: Int = 0,
	val complete: Boolean = false,
)
