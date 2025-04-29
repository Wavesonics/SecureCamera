package com.darkrockstudios.app.securecamera.obfuscation

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.camera.PhotoDef
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_ALL
import com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun ObfuscatePhotoContent(
	photoName: String,
	navController: NavController,
	snackbarHostState: SnackbarHostState,
	outerScope: CoroutineScope
) {
	val context = LocalContext.current
	val imageManager = koinInject<SecureImageManager>()
	val scope = rememberCoroutineScope()
	var photoDef by remember { mutableStateOf<PhotoDef?>(null) }
	var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
	var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
	var obscuredBitmap by remember { mutableStateOf<Bitmap?>(null) }
	var isLoading by remember { mutableStateOf(true) }
	var isFindingFaces by remember { mutableStateOf(true) }
	var showSaveDialog by remember { mutableStateOf(false) }
	var faces by remember { mutableStateOf<List<Face>>(emptyList()) }
	val detector = remember {
		FaceDetection.getClient(
			FaceDetectorOptions.Builder()
				.setLandmarkMode(LANDMARK_MODE_ALL)
				.setPerformanceMode(PERFORMANCE_MODE_ACCURATE)
				.setMinFaceSize(0.01f)
				.build()
		)
	}

	val faceErrorMessage = stringResource(R.string.obscure_toast_face_error)
	val saveErrorMessage = stringResource(R.string.obscure_toast_save_error)
	val overwriteCompleteMessage = stringResource(R.string.obscure_toast_overwrite_success)
	val copyCompleteMessage = stringResource(R.string.obscure_toast_copy_success)


	fun findFaces() {
		isFindingFaces = true
		scope.launch(Dispatchers.IO) {
			originalBitmap?.let { bitmap ->
				val inputImage = InputImage.fromBitmap(bitmap, 0)
				detector.process(inputImage)
					.addOnSuccessListener { foundFaces ->
						faces = foundFaces
						Timber.i("Found ${faces.size} faces")
						outerScope.launch {
							snackbarHostState.showSnackbar(
								context.getString(
									R.string.obscure_toast_faces_found,
									foundFaces.size
								)
							)
						}
						isFindingFaces = false
					}
					.addOnFailureListener { e ->
						Timber.e(e, "Failed face detection in Image")
						outerScope.launch {
							snackbarHostState.showSnackbar(faceErrorMessage)
						}
						isFindingFaces = false
					}
			} ?: run {
				Timber.e("findFaces: originalBitmap was null")
			}
		}
	}

	fun obscureFaces() {
		Timber.e("obscureFaces!")
		originalBitmap?.let { bitmap ->
			if (faces.isNotEmpty()) {
				val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

				faces.forEach { face ->
					maskFace(mutableBitmap, face, context, MaskMode.PIXELATE)
				}

				outerScope.launch {
					snackbarHostState.showSnackbar(context.getString(R.string.obscure_toast_faces_obscured, faces.size))
				}

				imageBitmap = mutableBitmap.asImageBitmap()
				obscuredBitmap = mutableBitmap
				faces = emptyList()
			}
		} ?: run {
			Timber.e("obscureFaces: originalBitmap was null")
		}
	}

	fun clear() {
		obscuredBitmap = null
		imageBitmap = originalBitmap?.asImageBitmap()
		findFaces()

		outerScope.launch {
			snackbarHostState.showSnackbar(context.getString(R.string.obscure_toast_faces_cleared))
		}
	}

	fun save() {
		showSaveDialog = true
	}

	fun overwriteOriginal() {
		val bitmap = obscuredBitmap ?: return
		photoDef?.let { photo ->
			scope.launch {
				try {
					imageManager.updateImage(
						bitmap = bitmap,
						photoDef = photo,
					)

					Timber.i("Overwritten original image: ${photo.photoName}")
					outerScope.launch {
						snackbarHostState.showSnackbar(overwriteCompleteMessage)
					}
					navController.popBackStack()
				} catch (e: Exception) {
					Timber.e(e, "Failed to overwrite original image")
					snackbarHostState.showSnackbar(saveErrorMessage)
				}
			}
		} ?: run {
			Timber.e("overwriteOriginal: photoDef was null")
		}
	}

	fun saveAsCopy() {
		val bitmap = obscuredBitmap ?: return
		photoDef?.let { photo ->
			scope.launch {
				try {
					val newPhotoDef = imageManager.saveImageCopy(
						bitmap = bitmap,
						photoDef = photo,
					)

					Timber.i("Saved copy of image: ${newPhotoDef.photoName}")
					outerScope.launch {
						snackbarHostState.showSnackbar(copyCompleteMessage)
					}
					navController.navigate(AppDestinations.createViewPhotoRoute(newPhotoDef.photoName))
				} catch (e: Exception) {
					Timber.e(e, "Failed to save copy of image")
					snackbarHostState.showSnackbar(saveErrorMessage)
				}
			}
		} ?: run {
			Timber.e("saveAsCopy: photoDef was null")
		}
	}

	LaunchedEffect(photoName) {
		withContext(Dispatchers.Main) {
			isLoading = true
		}

		scope.launch {
			imageManager.getPhotoByName(photoName)?.let { photo ->
				photoDef = photo

				val bitmap = imageManager.readImage(photo)
				originalBitmap = bitmap
				imageBitmap = bitmap.asImageBitmap()
			}

			isLoading = false

			findFaces()
		}
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		ObfuscatePhotoTopBar(
			navController = navController,
			onObscureClick = { obscureFaces() },
			readyToObscure = (faces.isNotEmpty()),
			onClearClick = { clear() },
			canClear = (obscuredBitmap != null),
			onSaveClick = { save() },
			readyToSave = (obscuredBitmap != null),
			isFindingFaces = isFindingFaces,
		)

		Box(
			modifier = Modifier
				.fillMaxSize()
				.clipToBounds(),
			contentAlignment = Alignment.Center
		) {
			if (isLoading) {
				CircularProgressIndicator()
			} else if (photoDef == null) {
				Text(
					text = stringResource(id = R.string.photo_not_found),
					color = MaterialTheme.colorScheme.onBackground
				)
			} else {
				imageBitmap?.let {
					Box(modifier = Modifier.zoomable(rememberZoomState())) {
						var imageWidth by remember { mutableStateOf(0f) }
						var imageHeight by remember { mutableStateOf(0f) }
						var boxWidth by remember { mutableStateOf(0f) }
						var boxHeight by remember { mutableStateOf(0f) }
						var imageTopLeft by remember { mutableStateOf(Offset(0f, 0f)) }

						val density = LocalDensity.current

						Image(
							bitmap = it,
							contentDescription = stringResource(id = R.string.photo_content_description),
							modifier = Modifier
								.fillMaxSize()
								.padding(16.dp)
								.onGloballyPositioned { coordinates ->
									// Get the size of the Box containing the image
									with(density) {
										boxWidth = coordinates.size.width.toFloat()
										boxHeight = coordinates.size.height.toFloat()
									}

									// Calculate the actual displayed image size (maintaining aspect ratio)
									val originalWidth = it.width.toFloat()
									val originalHeight = it.height.toFloat()
									val aspectRatio = originalWidth / originalHeight

									if (boxWidth / boxHeight > aspectRatio) {
										// Box is wider than image aspect ratio
										imageHeight = boxHeight
										imageWidth = imageHeight * aspectRatio
										imageTopLeft = Offset((boxWidth - imageWidth) / 2f, 0f)
									} else {
										// Box is taller than image aspect ratio
										imageWidth = boxWidth
										imageHeight = imageWidth / aspectRatio
										imageTopLeft = Offset(0f, (boxHeight - imageHeight) / 2f)
									}
								},
							contentScale = ContentScale.Fit
						)

						// Draw rectangles around detected faces
						Canvas(
							modifier = Modifier
								.padding(16.dp)
								.fillMaxSize()
						) {
							if (faces.isNotEmpty() && imageWidth > 0 && imageHeight > 0) {
								val scaleX = imageWidth / it.width
								val scaleY = imageHeight / it.height

								faces.forEach { face ->
									val boundingBox = face.boundingBox

									// Scale the bounding box to match the displayed image size
									val scaledLeft = boundingBox.left * scaleX + imageTopLeft.x
									val scaledTop = boundingBox.top * scaleY + imageTopLeft.y
									val scaledRight = boundingBox.right * scaleX + imageTopLeft.x
									val scaledBottom = boundingBox.bottom * scaleY + imageTopLeft.y

									// Draw rectangle around the face
									drawRect(
										color = Color.Red,
										topLeft = Offset(scaledLeft, scaledTop),
										size = Size(scaledRight - scaledLeft, scaledBottom - scaledTop),
										style = Stroke(width = 2.dp.toPx())
									)
								}
							}
						}
					}
				}
			}
		}
	}

	if (showSaveDialog) {
		ConfirmSaveDialog(
			onDismiss = { showSaveDialog = false },
			saveAsCopy = { saveAsCopy() },
			overwriteOriginal = { overwriteOriginal() },
		)
	}
}

@Composable
private fun ConfirmSaveDialog(
	onDismiss: () -> Unit,
	saveAsCopy: () -> Unit,
	overwriteOriginal: () -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			TextButton(
				onClick = {
					overwriteOriginal()
					onDismiss()
				}
			) {
				Text(stringResource(id = R.string.overwrite_button))
			}
		},
		dismissButton = {
			TextButton(
				onClick = onDismiss
			) {
				Text(stringResource(id = R.string.cancel_button))
			}
		},
		title = { Text(stringResource(id = R.string.save_photo_dialog_title)) },
		text = {
			Column {
				Text(stringResource(id = R.string.save_photo_dialog_message))

				// Add a third button as part of the dialog content
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.End
				) {
					TextButton(
						onClick = {
							saveAsCopy()
							onDismiss()
						}
					) {
						Text(stringResource(id = R.string.save_copy_button))
					}
				}
			}
		},
		containerColor = MaterialTheme.colorScheme.surface,
		titleContentColor = MaterialTheme.colorScheme.onSurface,
		textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
	)
}
