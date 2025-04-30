package com.darkrockstudios.app.securecamera.viewphoto

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.viewModelScope
import com.darkrockstudios.app.securecamera.BaseViewModel
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.camera.PhotoDef
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import com.darkrockstudios.app.securecamera.share.sharePhotoWithProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewPhotoViewModel(
	private val appContext: Context,
	private val imageManager: SecureImageManager,
	private val preferencesManager: AppPreferencesManager,
) : BaseViewModel<ViewPhotoUiState>() {

	var currentIndex: Int = 0
		private set

	override fun createState() = ViewPhotoUiState()

	fun initialize(initialPhoto: PhotoDef) {
		val photos = imageManager.getPhotos().asReversed()
		val initialIndex = photos.indexOfFirst { it == initialPhoto }

		viewModelScope.launch {
			val hasPoisonPill = preferencesManager.hasPoisonPillPin()
			val isDecoy = imageManager.isDecoyPhoto(initialPhoto)

			_uiState.update {
				it.copy(
					photos = photos,
					initialIndex = initialIndex,
					hasPoisonPill = hasPoisonPill,
					isDecoy = isDecoy
				)
			}
		}

		viewModelScope.launch {
			preferencesManager.sanitizeFileName.collect { sanitizeFileName ->
				_uiState.update { it.copy(sanitizeFileName = sanitizeFileName) }
			}
		}

		viewModelScope.launch {
			preferencesManager.sanitizeMetadata.collect { sanitizeMetadata ->
				_uiState.update { it.copy(sanitizeMetadata = sanitizeMetadata) }
			}
		}
	}

	fun loadPhotoImage(photo: PhotoDef) {
		viewModelScope.launch(Dispatchers.IO) {
			val bitmap = imageManager.readImage(photo).asImageBitmap()
			_uiState.update {
				val photoImages = it.photoImages.toMutableMap()
				photoImages[photo.photoName] = bitmap
				it.copy(photoImages = photoImages)
			}
		}
	}

	fun setCurrentPhotoIndex(index: Int) {
		currentIndex = index
	}

	fun getCurrentPhoto(): PhotoDef? {
		val photos = uiState.value.photos
		return if (photos.isNotEmpty() && currentIndex >= 0 && currentIndex < photos.size) {
			photos[currentIndex]
		} else {
			null
		}
	}

	fun toggleDecoyStatus() {
		val currentPhoto = getCurrentPhoto() ?: return

		_uiState.update { it.copy(isDecoyLoading = true) }

		viewModelScope.launch(Dispatchers.Default) {
			if (uiState.value.isDecoy) {
				imageManager.removeDecoyPhoto(currentPhoto)
				withContext(Dispatchers.Main) {
					_uiState.update {
						it.copy(
							isDecoy = false,
							isDecoyLoading = false,
						)
					}
					showMessage(appContext.getString(R.string.decoy_removed))
				}
			} else {
				val success = imageManager.addDecoyPhoto(currentPhoto)
				withContext(Dispatchers.Main) {
					_uiState.update {
						it.copy(
							isDecoy = success,
							isDecoyLoading = false,
						)
					}
					if (success) {
						showMessage(appContext.getString(R.string.decoy_added))
					} else {
						showMessage(
							appContext.getString(
								R.string.decoy_limit_reached,
								SecureImageManager.MAX_DECOY_PHOTOS
							)
						)
					}
				}
			}
		}
	}

	fun showDeleteConfirmation() {
		_uiState.update { it.copy(showDeleteConfirmation = true) }
	}

	fun hideDeleteConfirmation() {
		_uiState.update { it.copy(showDeleteConfirmation = false) }
	}

	fun deleteCurrentPhoto() {
		val currentPhoto = getCurrentPhoto() ?: return
		imageManager.deleteImage(currentPhoto)
		_uiState.update { it.copy(photoDeleted = true) }
	}

	fun showInfoDialog() {
		_uiState.update { it.copy(showInfoDialog = true) }
	}

	fun hideInfoDialog() {
		_uiState.update { it.copy(showInfoDialog = false) }
	}

	fun sharePhoto(context: Context) {
		val currentPhoto = getCurrentPhoto() ?: return

		viewModelScope.launch {
			sharePhotoWithProvider(
				photo = currentPhoto,
				context = context
			)
		}
	}
}

data class ViewPhotoUiState(
	val photos: List<PhotoDef> = emptyList(),
	val photoImages: Map<String, ImageBitmap> = emptyMap(),
	val initialIndex: Int = 0,
	val hasPoisonPill: Boolean = false,
	val isDecoy: Boolean = false,
	val isDecoyLoading: Boolean = false,
	val showDeleteConfirmation: Boolean = false,
	val showInfoDialog: Boolean = false,
	val photoDeleted: Boolean = false,
	val sanitizeFileName: Boolean = false,
	val sanitizeMetadata: Boolean = false
)