package com.darkrockstudios.app.securecamera.obfuscation

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.koin.androidx.compose.koinViewModel

@Composable
fun ObfuscatePhotoContent(
	photoName: String,
	navController: NavController,
	snackbarHostState: SnackbarHostState,
	outerScope: CoroutineScope
) {
	val context = LocalContext.current
	val viewModel: ObfuscatePhotoViewModel = koinViewModel()
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()

	LaunchedEffect(photoName) {
		if (uiState.imageBitmap == null) {
			viewModel.loadPhoto(photoName)
		}
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		ObfuscatePhotoTopBar(
			navController = navController,
			onObscureClick = {
				viewModel.obscureFaces(context) {
					outerScope.launch {
						snackbarHostState.showSnackbar(
							context.getString(
								R.string.obscure_toast_faces_obscured,
								uiState.faces.size
							)
						)
					}
				}
			},
			readyToObscure = (uiState.faces.isNotEmpty()),
			onClearClick = {
				viewModel.clear {
					outerScope.launch {
						snackbarHostState.showSnackbar(context.getString(R.string.obscure_toast_faces_cleared))
					}
				}
			},
			canClear = (uiState.obscuredBitmap != null),
			onSaveClick = { viewModel.showSaveDialog() },
			readyToSave = (uiState.obscuredBitmap != null),
			isFindingFaces = uiState.isFindingFaces,
		)

		Box(
			modifier = Modifier
				.fillMaxSize()
				.clipToBounds(),
			contentAlignment = Alignment.Center
		) {
			if (uiState.isLoading) {
				CircularProgressIndicator()
			} else if (uiState.photoDef == null) {
				Text(
					text = stringResource(id = R.string.photo_not_found),
					color = MaterialTheme.colorScheme.onBackground
				)
			} else {
				uiState.imageBitmap?.let {
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
							if (uiState.faces.isNotEmpty() && imageWidth > 0 && imageHeight > 0) {
								val scaleX = imageWidth / it.width
								val scaleY = imageHeight / it.height

								uiState.faces.forEach { face ->
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

	if (uiState.showSaveDialog) {
		ConfirmSaveDialog(
			onDismiss = { viewModel.dismissSaveDialog() },
			saveAsCopy = {
				viewModel.saveAsCopy(
					onComplete = {
						outerScope.launch {
							snackbarHostState.showSnackbar(context.getString(R.string.obscure_toast_copy_success))
						}
					},
					onError = {
						outerScope.launch {
							snackbarHostState.showSnackbar(context.getString(R.string.obscure_toast_save_error))
						}
					},
					onNavigate = { route ->
						navController.navigate(route)
					}
				)
			},
			overwriteOriginal = {
				viewModel.overwriteOriginal(
					onError = {
						outerScope.launch {
							snackbarHostState.showSnackbar(context.getString(R.string.obscure_toast_save_error))
						}
					},
					onSuccess = {
						outerScope.launch {
							snackbarHostState.showSnackbar(context.getString(R.string.obscure_toast_overwrite_success))
						}
						navController.popBackStack()
					}
				)
			},
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
