package com.darkrockstudios.app.securecamera.obfuscation

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkrockstudios.app.securecamera.camera.PhotoDef
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class ObfuscatePhotoViewModel(
	private val imageManager: SecureImageManager
) : ViewModel() {

	private val _uiState = MutableStateFlow(ObfuscatePhotoUiState())
	val uiState: StateFlow<ObfuscatePhotoUiState> = _uiState.asStateFlow()

	private val detector = FaceDetection.getClient(
		FaceDetectorOptions.Builder()
			.setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
			.setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
			.setMinFaceSize(0.01f)
			.build()
	)

	fun loadPhoto(photoName: String) {
		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true) }

			imageManager.getPhotoByName(photoName)?.let { photo ->
				val bitmap = imageManager.readImage(photo)
				_uiState.update {
					it.copy(
						photoDef = photo,
						originalBitmap = bitmap,
						imageBitmap = bitmap.asImageBitmap(),
						isLoading = false
					)
				}
				findFaces()
			} ?: run {
				_uiState.update { it.copy(isLoading = false) }
			}
		}
	}

	fun findFaces() {
		_uiState.update { it.copy(isFindingFaces = true) }
		viewModelScope.launch(Dispatchers.IO) {
			uiState.value.originalBitmap?.let { bitmap ->
				val inputImage = InputImage.fromBitmap(bitmap, 0)
				detector.process(inputImage)
					.addOnSuccessListener { foundFaces ->
						_uiState.update {
							it.copy(
								faces = foundFaces,
								isFindingFaces = false
							)
						}
						Timber.i("Found ${foundFaces.size} faces")
					}
					.addOnFailureListener { e ->
						Timber.e(e, "Failed face detection in Image")
						_uiState.update { it.copy(isFindingFaces = false) }
					}
			} ?: run {
				Timber.e("findFaces: originalBitmap was null")
				_uiState.update { it.copy(isFindingFaces = false) }
			}
		}
	}

	fun obscureFaces(context: Context, onComplete: () -> Unit) {
		Timber.e("obscureFaces!")
		uiState.value.originalBitmap?.let { bitmap ->
			if (uiState.value.faces.isNotEmpty()) {
				val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

				uiState.value.faces.forEach { face ->
					maskFace(mutableBitmap, face, context, MaskMode.PIXELATE)
				}

				onComplete()

				_uiState.update {
					it.copy(
						imageBitmap = mutableBitmap.asImageBitmap(),
						obscuredBitmap = mutableBitmap,
						faces = emptyList()
					)
				}
			}
		} ?: run {
			Timber.e("obscureFaces: originalBitmap was null")
		}
	}

	fun clear(onComplete: () -> Unit) {
		_uiState.update {
			it.copy(
				obscuredBitmap = null,
				imageBitmap = it.originalBitmap?.asImageBitmap()
			)
		}
		findFaces()
		onComplete()
	}

	fun showSaveDialog() {
		_uiState.update { it.copy(showSaveDialog = true) }
	}

	fun dismissSaveDialog() {
		_uiState.update { it.copy(showSaveDialog = false) }
	}

	fun overwriteOriginal(onError: () -> Unit, onSuccess: () -> Unit) {
		val bitmap = uiState.value.obscuredBitmap ?: return
		uiState.value.photoDef?.let { photo ->
			viewModelScope.launch {
				try {
					imageManager.updateImage(
						bitmap = bitmap,
						photoDef = photo,
					)

					Timber.i("Overwritten original image: ${photo.photoName}")
					onSuccess()
				} catch (e: Exception) {
					Timber.e(e, "Failed to overwrite original image")
					onError()
				}
			}
		} ?: run {
			Timber.e("overwriteOriginal: photoDef was null")
		}
	}

	fun saveAsCopy(onComplete: () -> Unit, onError: () -> Unit, onNavigate: (String) -> Unit) {
		val bitmap = uiState.value.obscuredBitmap ?: return
		uiState.value.photoDef?.let { photo ->
			viewModelScope.launch {
				try {
					val newPhotoDef = imageManager.saveImageCopy(
						bitmap = bitmap,
						photoDef = photo,
					)

					Timber.i("Saved copy of image: ${newPhotoDef.photoName}")
					onComplete()
					onNavigate(AppDestinations.createViewPhotoRoute(newPhotoDef.photoName))
				} catch (e: Exception) {
					Timber.e(e, "Failed to save copy of image")
					onError()
				}
			}
		} ?: run {
			Timber.e("saveAsCopy: photoDef was null")
		}
	}
}

data class ObfuscatePhotoUiState(
	val photoDef: PhotoDef? = null,
	val imageBitmap: ImageBitmap? = null,
	val originalBitmap: Bitmap? = null,
	val obscuredBitmap: Bitmap? = null,
	val isLoading: Boolean = true,
	val isFindingFaces: Boolean = false,
	val showSaveDialog: Boolean = false,
	val faces: List<Face> = emptyList(),
)
