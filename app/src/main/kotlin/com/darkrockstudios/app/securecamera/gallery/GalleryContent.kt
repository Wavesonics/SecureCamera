package com.darkrockstudios.app.securecamera.gallery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
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
import com.darkrockstudios.app.securecamera.sharePhotosData
import kotlinx.coroutines.*
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryContent(
	modifier: Modifier = Modifier,
	navController: NavController,
	paddingValues: PaddingValues
) {
	val imageManager = koinInject<SecureImageManager>()
	val preferencesManager = koinInject<AppPreferencesManager>()
	var photos by remember { mutableStateOf<List<PhotoDef>>(emptyList()) }
	var isLoading by rememberSaveable { mutableStateOf(true) }
	val context = LocalContext.current
	val scope = rememberCoroutineScope()

	// Selection state
	var isSelectionMode by rememberSaveable { mutableStateOf(false) }
	var selectedPhotos by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
	var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

	val sanitizeFileName by
	preferencesManager.sanitizeFileName.collectAsState(preferencesManager.sanitizeFileNameDefault)
	val sanitizeMetadata by
	preferencesManager.sanitizeMetadata.collectAsState(preferencesManager.sanitizeMetadataDefault)

	// Function to toggle selection of a photo
	val togglePhotoSelection = { photoName: String ->
		selectedPhotos = if (selectedPhotos.contains(photoName)) {
			selectedPhotos - photoName
		} else {
			selectedPhotos + photoName
		}

		// If no photos are selected, exit selection mode
		if (selectedPhotos.isEmpty()) {
			isSelectionMode = false
		}
	}

	val startSelectionMode = { photoName: String ->
		isSelectionMode = true
		selectedPhotos = setOf(photoName)
		vibrateDevice(context)
	}

	val clearSelection = {
		isSelectionMode = false
		selectedPhotos = emptySet()
	}

	val handleDelete = {
		showDeleteConfirmation = true
	}

	val performDelete = {
		val photoDefs = selectedPhotos.mapNotNull { imageManager.getPhotoByName(it) }
		imageManager.deleteImages(photoDefs)
		clearSelection()
		photos = photos.filter { it !in photoDefs }
		showDeleteConfirmation = false
	}

	val handleShare = {
		val photoDefs = selectedPhotos.mapNotNull { imageManager.getPhotoByName(it) }
		if (photoDefs.isNotEmpty()) {
			scope.launch(Dispatchers.IO) {
				sharePhotosData(
					photos = photoDefs,
					sanitizeName = sanitizeFileName,
					sanitizeMetadata = sanitizeMetadata,
					imageManager = imageManager,
					context = context
				)
				withContext(Dispatchers.Main) {
					clearSelection()
				}
			}
		}
	}

	LaunchedEffect(Unit) {
		photos = imageManager.getPhotos().sortedByDescending { it.dateTaken() }
		isLoading = false
	}

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		GalleryTopNav(
			navController = navController,
			onDeleteClick = handleDelete,
			onShareClick = handleShare,
			isSelectionMode = isSelectionMode,
			selectedCount = selectedPhotos.size,
			onCancelSelection = clearSelection
		)

		if (showDeleteConfirmation) {
			ConfirmDeletePhotoDialog(
				selectedCount = selectedPhotos.size,
				onConfirm = performDelete,
				onDismiss = { showDeleteConfirmation = false }
			)
		}

		Box(
			modifier = Modifier
				.padding(
					start = paddingValues.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
					end = paddingValues.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
					bottom = 0.dp,
					top = 8.dp
				)
				.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			if (isLoading) {
				// Show a loading indicator or placeholder
				Text(text = stringResource(id = R.string.gallery_loading))
			} else if (photos.isEmpty()) {
				Text(text = stringResource(id = R.string.gallery_empty))
			} else {
				PhotoGrid(
					photos = photos,
					selectedPhotoNames = selectedPhotos,
					onPhotoLongClick = startSelectionMode,
					onPhotoClick = { photoName ->
						if (isSelectionMode) {
							togglePhotoSelection(photoName)
						} else {
							navController.navigate(AppDestinations.createViewPhotoRoute(photoName))
						}
					}
				)
			}
		}
	}
}

@Composable
private fun PhotoGrid(
	photos: List<PhotoDef>,
	modifier: Modifier = Modifier,
	selectedPhotoNames: Set<String> = emptySet(),
	onPhotoLongClick: (String) -> Unit = {},
	onPhotoClick: (String) -> Unit = {}
) {
	val limitedDispatcher = remember {
		Dispatchers.IO.limitedParallelism(4) // Limit to 4 concurrent thumbnail loads
	}

	val imageManager = koinInject<SecureImageManager>()
	val scope = rememberCoroutineScope()
	LazyVerticalGrid(
		columns = GridCells.Adaptive(minSize = 128.dp),
		contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 0.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
		modifier = modifier.fillMaxSize()
	) {
		items(items = photos, key = { it.photoName }) { photo ->
			PhotoItem(
				photo = photo,
				imageManager = imageManager,
				scope = scope,
				limitedDispatcher = limitedDispatcher,
				isSelected = selectedPhotoNames.contains(photo.photoName),
				onLongClick = { onPhotoLongClick(photo.photoName) },
				onClick = { onPhotoClick(photo.photoName) }
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PhotoItem(
	photo: PhotoDef,
	imageManager: SecureImageManager,
	scope: CoroutineScope,
	limitedDispatcher: CoroutineDispatcher,
	isSelected: Boolean = false,
	onLongClick: () -> Unit = {},
	onClick: () -> Unit = {},
	modifier: Modifier = Modifier
) {
	var thumbnailBitmap by remember(photo.photoName) { mutableStateOf<ImageBitmap?>(null) }
	val isDecoy = remember(photo) { imageManager.isDecoyPhoto(photo) }

	val imageAlpha by animateFloatAsState(
		targetValue = if (thumbnailBitmap != null) 1f else 0f,
		animationSpec = tween(durationMillis = 500),
		label = "imageAlpha"
	)

	LaunchedEffect(photo.photoName) {
		if (thumbnailBitmap == null) {
			scope.launch(limitedDispatcher) {
				thumbnailBitmap = imageManager.readThumbnail(photo).asImageBitmap()
			}
		}
	}

	val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
	val borderWidth = if (isSelected) 3.dp else 0.dp

	Box(
		modifier = modifier
			.aspectRatio(1f)
			.fillMaxWidth()
	) {
		Card(
			modifier = Modifier
				.fillMaxSize()
				.border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(8.dp))
				.combinedClickable(
					onClick = onClick,
					onLongClick = onLongClick,
				),
		) {
			Box(modifier = Modifier.fillMaxSize()) {
				thumbnailBitmap?.let {
					Image(
						bitmap = it,
						contentDescription = stringResource(
							id = R.string.gallery_photo_content_description,
							photo.photoName
						),
						contentScale = ContentScale.Crop,
						modifier = Modifier.fillMaxSize().alpha(imageAlpha)
					)
				} ?: run {
					Box(modifier = Modifier.fillMaxSize()) {
						CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
					}
				}

				Box(
					modifier = Modifier
						.padding(8.dp)
						.fillMaxSize()
				) {
					if (isDecoy) {
						Icon(
							imageVector = Icons.Filled.Warning,
							contentDescription = stringResource(R.string.gallery_decoy_indicator),
							tint = Color.LightGray,
							modifier = Modifier
								.size(24.dp)
								.align(Alignment.BottomEnd)
						)
					}

					if (isSelected) {
						Icon(
							imageVector = Icons.Filled.CheckCircle,
							contentDescription = stringResource(R.string.gallery_decoy_indicator),
							tint = MaterialTheme.colorScheme.primary,
							modifier = Modifier
								.size(24.dp)
								.align(Alignment.TopEnd)
						)
					}
				}
			}
		}
	}
}
