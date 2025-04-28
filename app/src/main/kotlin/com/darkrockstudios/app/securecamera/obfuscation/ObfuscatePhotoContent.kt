package com.darkrockstudios.app.securecamera.obfuscation

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_ALL
import com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun ObfuscatePhotoContent(
	photoName: String,
	navController: NavController
) {
	val context = LocalContext.current
	val imageManager = koinInject<SecureImageManager>()
	val scope = rememberCoroutineScope()
	var photoDef by remember { mutableStateOf<PhotoDef?>(null) }
	var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
	var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
	var isLoading by remember { mutableStateOf(true) }
	var isFacesObscured by remember { mutableStateOf(false) }
	val detector = remember {
		FaceDetection.getClient(
			FaceDetectorOptions.Builder()
				.setLandmarkMode(LANDMARK_MODE_ALL)
				.setPerformanceMode(PERFORMANCE_MODE_ACCURATE)
				.build()
		)
	}

	var faces by remember { mutableStateOf<List<Face>>(emptyList()) }

	LaunchedEffect(photoName) {
		scope.launch(Dispatchers.IO) {
			// Get the PhotoDef from the photoName
			val photo = imageManager.getPhotoByName(photoName)
			photoDef = photo

			// Load the image if PhotoDef was found
			if (photo != null) {
				val bitmap = imageManager.readImage(photo)
				originalBitmap = bitmap
				imageBitmap = bitmap.asImageBitmap()

				val inputImage = InputImage.fromBitmap(bitmap, 0)
				detector.process(inputImage)
					.addOnSuccessListener { foundFaces ->
						faces = foundFaces
						Timber.i("Found ${faces.size} faces")
					}
					.addOnFailureListener { e ->
						Timber.e(e, "Failed face detection in Image")
					}
			}

			isLoading = false
		}
	}

	fun obscureFaces() {
		originalBitmap?.let { bitmap ->
			if (faces.isNotEmpty() && !isFacesObscured) {
				val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

				faces.forEach { face ->
					maskFace(mutableBitmap, face, context, MaskMode.PIXELATE)
				}

				imageBitmap = mutableBitmap.asImageBitmap()
				isFacesObscured = true

				// Save the obscured image
				photoDef?.let { photo ->
					scope.launch(Dispatchers.IO) {
						try {
							imageManager.updateImage(
								bitmap = mutableBitmap,
								photoDef = photo,
							)

							Timber.i("Saved obscured image: ${photo.photoName}")
						} catch (e: Exception) {
							Timber.e(e, "Failed to save obscured image")
						}
					}
				}
			}
		}
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		ObfuscatePhotoTopBar(
			navController = navController,
			onObscureClick = { obscureFaces() }
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
}
