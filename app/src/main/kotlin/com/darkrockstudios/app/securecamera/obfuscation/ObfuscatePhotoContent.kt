package com.darkrockstudios.app.securecamera.obfuscation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.ui.HandleUiEvents
import kotlinx.coroutines.CoroutineScope
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
				viewModel.obscureRegions()
			},
			readyToObscure = (uiState.regions.isNotEmpty()),
			onClearClick = {
				viewModel.clearFaces()
			},
			canClear = (uiState.obscuredBitmap != null),
			onSaveClick = { viewModel.showSaveDialog() },
			onAddRegionClick = {
				viewModel.startRegionCreation()
			},
			readyToSave = (uiState.obscuredBitmap != null),
			isFindingFaces = uiState.isFindingFaces,
			isCreatingRegion = uiState.isCreatingRegion,
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
						var dragStart by remember { mutableStateOf(Offset.Zero) }
						var dragEnd by remember { mutableStateOf(Offset.Zero) }

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
								.pointerInput(uiState.regions, uiState.isCreatingRegion) {
									if (uiState.isCreatingRegion) {
										// Handle drag for region creation
										detectDragGestures(
											onDragStart = { offset ->
												dragStart = offset
												dragEnd = offset
											},
											onDrag = { change, _ ->
												dragEnd = change.position

												// Convert screen coordinates to image coordinates
												if (imageWidth > 0 && imageHeight > 0) {
													val scaleX = it.width / imageWidth
													val scaleY = it.height / imageHeight

													val startX = ((dragStart.x - imageTopLeft.x) * scaleX).toInt()
													val startY = ((dragStart.y - imageTopLeft.y) * scaleY).toInt()
													val endX = ((dragEnd.x - imageTopLeft.x) * scaleX).toInt()
													val endY = ((dragEnd.y - imageTopLeft.y) * scaleY).toInt()

													// Update the region in the ViewModel
													viewModel.updateRegion(startX, startY, endX, endY)
												}
											}
										)
									} else {
										// Handle tap for toggling region obfuscation
										detectTapGestures { tapOffset ->
											if (uiState.regions.isNotEmpty() && imageWidth > 0 && imageHeight > 0) {
												val scaleX = imageWidth / it.width
												val scaleY = imageHeight / it.height

												// Check if tap is inside any region
												uiState.regions.forEachIndexed { index, region ->
													val boundingBox = region.rect

													// Scale the bounding box to match the displayed image size
													val scaledLeft = boundingBox.left * scaleX + imageTopLeft.x
													val scaledTop = boundingBox.top * scaleY + imageTopLeft.y
													val scaledRight = boundingBox.right * scaleX + imageTopLeft.x
													val scaledBottom = boundingBox.bottom * scaleY + imageTopLeft.y

													// Create a rectangle from the scaled coordinates
													val scaledRect = Rect(
														left = scaledLeft,
														top = scaledTop,
														right = scaledRight,
														bottom = scaledBottom
													)

													if (scaledRect.contains(tapOffset)) {
														viewModel.toggleRegionObfuscation(index)
														return@detectTapGestures
													}
												}
											}
										}
									}
								}
						) {
							if (imageWidth > 0 && imageHeight > 0) {
								val scaleX = imageWidth / it.width
								val scaleY = imageHeight / it.height

								// Draw existing regions
								if (uiState.regions.isNotEmpty()) {
									uiState.regions.forEach { region ->
										val boundingBox = region.rect

										// Scale the bounding box to match the displayed image size
										val scaledLeft = boundingBox.left * scaleX + imageTopLeft.x
										val scaledTop = boundingBox.top * scaleY + imageTopLeft.y
										val scaledRight = boundingBox.right * scaleX + imageTopLeft.x
										val scaledBottom = boundingBox.bottom * scaleY + imageTopLeft.y

										val color = if (region is FaceRegion) {
											if (region.obfuscate) Color.Red else Color.Green
										} else {
											Color.Blue
										}
										drawRect(
											color = color,
											topLeft = Offset(scaledLeft, scaledTop),
											size = Size(scaledRight - scaledLeft, scaledBottom - scaledTop),
											style = Stroke(width = 2.dp.toPx())
										)
									}
								}

								// Draw the region being created
								if (uiState.isCreatingRegion && uiState.currentRegion != null) {
									val rect = uiState.currentRegion!!

									val scaledLeft = rect.left * scaleX + imageTopLeft.x
									val scaledTop = rect.top * scaleY + imageTopLeft.y
									val scaledRight = rect.right * scaleX + imageTopLeft.x
									val scaledBottom = rect.bottom * scaleY + imageTopLeft.y

									drawRect(
										color = Color.Blue,
										topLeft = Offset(scaledLeft, scaledTop),
										size = Size(scaledRight - scaledLeft, scaledBottom - scaledTop),
										style = Stroke(width = 2.dp.toPx())
									)
								}
							}
						}
					}
				}

				// Add FABs for region creation
				if (uiState.isCreatingRegion) {
					Column(
						modifier = Modifier
							.align(Alignment.BottomEnd)
							.padding(16.dp),
						verticalArrangement = Arrangement.spacedBy(8.dp)
					) {
						// Finish region FAB
						FloatingActionButton(
							onClick = { viewModel.finishRegionCreation() },
							containerColor = MaterialTheme.colorScheme.primary,
							contentColor = MaterialTheme.colorScheme.onPrimary
						) {
							Icon(
								imageVector = Icons.Filled.Check,
								contentDescription = stringResource(id = R.string.obscure_action_button_finish_region)
							)
						}

						// Cancel region FAB
						FloatingActionButton(
							onClick = { viewModel.cancelRegionCreation() },
							containerColor = MaterialTheme.colorScheme.error,
							contentColor = MaterialTheme.colorScheme.onError
						) {
							Icon(
								imageVector = Icons.Filled.Close,
								contentDescription = stringResource(id = R.string.obscure_action_button_cancel_region)
							)
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
					onNavigate = { route ->
						navController.navigate(route)
					}
				)
			},
			overwriteOriginal = {
				viewModel.overwriteOriginal(
					onSuccess = {
						navController.popBackStack()
					}
				)
			},
		)
	}

	HandleUiEvents(viewModel.events, snackbarHostState, navController)
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
