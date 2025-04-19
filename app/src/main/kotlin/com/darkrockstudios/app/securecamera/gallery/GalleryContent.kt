package com.darkrockstudios.app.securecamera.gallery

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.darkrockstudios.app.securecamera.sharePhotosData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryContent(
	modifier: Modifier = Modifier,
	navController: NavController,
	paddingValues: PaddingValues
) {
	val imageManager = koinInject<SecureImageManager>()
	var photos by remember { mutableStateOf<List<PhotoDef>>(emptyList()) }
	var isLoading by remember { mutableStateOf(true) }
	val context = LocalContext.current
	val scope = rememberCoroutineScope()

	// Selection state
	var isSelectionMode by remember { mutableStateOf(false) }
	var selectedPhotos by remember { mutableStateOf<Set<String>>(emptySet()) }
	var showDeleteConfirmation by remember { mutableStateOf(false) }

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
				sharePhotosData(photoDefs, false, imageManager, context)
				withContext(Dispatchers.Main) {
					clearSelection()
				}
			}
		}
	}

	LaunchedEffect(Unit) {
		photos = imageManager.getPhotos()
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
	val imageManager = koinInject<SecureImageManager>()
	val scope = rememberCoroutineScope()
	LazyVerticalGrid(
		columns = GridCells.Adaptive(minSize = 128.dp),
		contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 0.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
		modifier = modifier.fillMaxSize()
	) {
		items(items = photos, key = { it.hashCode() }) { photo ->
			PhotoItem(
				photo = photo,
				imageManager = imageManager,
				scope = scope,
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
	isSelected: Boolean = false,
	onLongClick: () -> Unit = {},
	onClick: () -> Unit = {},
	modifier: Modifier = Modifier
) {
	var thumbnailBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

	LaunchedEffect(Unit) {
		scope.launch(Dispatchers.IO) {
			thumbnailBitmap = imageManager.readThumbnail(photo).asImageBitmap()
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
				.border(width = borderWidth, color = borderColor)
				.combinedClickable(
					onClick = onClick,
					onLongClick = onLongClick,
				),
		) {
			thumbnailBitmap?.let {
				Image(
					bitmap = it,
					contentDescription = stringResource(
						id = R.string.gallery_photo_content_description,
						photo.photoName
					),
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize()
				)
			} ?: run {
				Box(modifier = Modifier.fillMaxSize()) {
					CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
				}
			}
		}
	}
}
