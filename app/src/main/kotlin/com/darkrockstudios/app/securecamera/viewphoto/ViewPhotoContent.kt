package com.darkrockstudios.app.securecamera.viewphoto

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.ConfirmDeletePhotoDialog
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.camera.PhotoDef
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import com.darkrockstudios.app.securecamera.sharePhotoData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomableWithScroll
import org.koin.compose.koinInject

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalZoomableApi::class)
@Composable
fun ViewPhotoContent(
	initialPhoto: PhotoDef,
	navController: NavController,
	modifier: Modifier = Modifier,
	snackbarHostState: SnackbarHostState,
	paddingValues: PaddingValues
) {
	val imageManager = koinInject<SecureImageManager>()
	var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
	var showInfoDialog by rememberSaveable { mutableStateOf(false) }
	val preferencesManager = koinInject<AppPreferencesManager>()
	val context = LocalContext.current
	val scope = rememberCoroutineScope()

	val photos = remember { imageManager.getPhotos().asReversed() }
	val listState = rememberLazyListState(initialFirstVisibleItemIndex = photos.indexOfFirst { it == initialPhoto })

	val sanitizeFileName by
	preferencesManager.sanitizeFileName.collectAsState(preferencesManager.sanitizeFileNameDefault)
	val sanitizeMetadata by
	preferencesManager.sanitizeFileName.collectAsState(preferencesManager.sanitizeMetadataDefault)

	var hasPoisonPill by remember { mutableStateOf(false) }
	var isDecoy by remember { mutableStateOf(false) }
	var isDecoyLoading by remember { mutableStateOf(false) }

	val decoyAddedMessage = stringResource(id = R.string.decoy_added)
	val decoyRemovedMessage = stringResource(id = R.string.decoy_removed)

	val decoyLimitReachedMessage = stringResource(
		id = R.string.decoy_limit_reached,
		SecureImageManager.MAX_DECOY_PHOTOS
	)

	fun curPhoto(): PhotoDef {
		return photos[listState.firstVisibleItemIndex]
	}

	LaunchedEffect(Unit) {
		hasPoisonPill = preferencesManager.hasPoisonPillPin()
		isDecoy = imageManager.isDecoyPhoto(initialPhoto)
	}

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		ViewPhotoTopBar(
			navController = navController,
			onDeleteClick = {
				showDeleteConfirmation = true
			},
			onInfoClick = {
				showInfoDialog = true
			},
			onObfuscateClick = {
				val photoName = curPhoto().photoName
				navController.navigate(AppDestinations.createObfuscatePhotoRoute(photoName))
			},
			onShareClick = {
				scope.launch {
					sharePhotoData(
						photo = initialPhoto,
						sanitizeName = sanitizeFileName,
						sanitizeMetadata = sanitizeMetadata,
						imageManager = imageManager,
						context = context
					)
				}
			},
			showDecoyButton = hasPoisonPill,
			isDecoy = isDecoy,
			isDecoyLoading = isDecoyLoading,
			onDecoyClick = {
				isDecoyLoading = true
				scope.launch(Dispatchers.Default) {
					if (isDecoy) {
						imageManager.removeDecoyPhoto(initialPhoto)
						withContext(Dispatchers.Main) {
							isDecoy = false
							isDecoyLoading = false
							snackbarHostState.showSnackbar(decoyRemovedMessage)
						}
					} else {
						val success = imageManager.addDecoyPhoto(initialPhoto)
						withContext(Dispatchers.Main) {
							isDecoyLoading = false
							isDecoy = success
							if (!success) {
								snackbarHostState.showSnackbar(decoyLimitReachedMessage)
							} else {
								snackbarHostState.showSnackbar(decoyAddedMessage)
							}
						}
					}
				}
			}
		)

		if (showDeleteConfirmation) {
			ConfirmDeletePhotoDialog(
				selectedCount = 1, onConfirm = {
					showDeleteConfirmation = false
					imageManager.deleteImage(initialPhoto)
					navController.navigateUp()
				},
				onDismiss = {
					showDeleteConfirmation = false
				}
			)
		}

		LazyRow(
			state = listState,
			modifier = Modifier.fillMaxSize(),
			flingBehavior = rememberSnapFlingBehavior(lazyListState = listState, snapPosition = SnapPosition.Start),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(32.dp),
		) {
			items(count = photos.size, key = { photos[it].photoName }) {
				ViewPhoto(
					Modifier.fillParentMaxSize(),
					photos[it],
					imageManager,
					scope
				)
			}
		}

		if (showInfoDialog) {
			PhotoInfoDialog(curPhoto()) {
				showInfoDialog = false
			}
		}
	}
}

@Composable
@OptIn(ExperimentalZoomableApi::class)
private fun ViewPhoto(
	modifier: Modifier,
	photo: PhotoDef,
	imageManager: SecureImageManager,
	scope: CoroutineScope
) {
	var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
	val imageAlpha by animateFloatAsState(
		targetValue = if (imageBitmap != null) 1f else 0f,
		animationSpec = tween(durationMillis = 500),
		label = "imageAlpha"
	)
	LaunchedEffect(photo) {
		scope.launch(Dispatchers.IO) {
			imageBitmap = imageManager.readImage(photo).asImageBitmap()
		}
	}

	Box(
		modifier = modifier.clipToBounds(),
		contentAlignment = Alignment.Center
	) {
		if (photo.photoFile.exists()) {
			imageBitmap?.let {
				Image(
					contentScale = ContentScale.Fit,
					modifier = Modifier
						.fillMaxSize()
						.alpha(imageAlpha)
						.zoomableWithScroll(rememberZoomState()),
					bitmap = it,
					contentDescription = stringResource(id = R.string.photo_content_description),
				)
			} ?: run {
				Row(
					modifier = Modifier.align(Alignment.Center),
					verticalAlignment = Alignment.CenterVertically
				) {
					CircularProgressIndicator(
						modifier = Modifier.size(16.dp),
						color = MaterialTheme.colorScheme.onPrimaryContainer,
						strokeWidth = 2.dp
					)
					Spacer(modifier = Modifier.size(16.dp))
					Text(
						text = stringResource(R.string.photo_content_loading),
					)
				}
			}
		} else {
			Text(
				modifier = Modifier.align(alignment = Alignment.Center),
				text = stringResource(id = R.string.photo_not_found),
			)
		}
	}
}
