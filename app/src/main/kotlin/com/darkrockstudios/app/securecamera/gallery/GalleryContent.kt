package com.darkrockstudios.app.securecamera.gallery

import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.camera.PhotoDef
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
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

	// Selection state
	var isSelectionMode by remember { mutableStateOf(false) }
	var selectedPhotos by remember { mutableStateOf<Set<String>>(emptySet()) }

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

	fun refreshPhotos() {
		photos = imageManager.getPhotos()
	}

	val startSelectionMode = { photoName: String ->
		isSelectionMode = true
		selectedPhotos = setOf(photoName)
		vibrateDevice(context)
	}

	val cancelSelection = {
		isSelectionMode = false
		selectedPhotos = emptySet()
	}

	val handleDelete = {
		val photoDefs = selectedPhotos.mapNotNull { imageManager.getPhotoByName(it) }
		imageManager.deleteImages(photoDefs)
		cancelSelection()
		photos -= photoDefs
	}

	val handleShare = {
		// TODO: Implement share functionality
		// For now, just cancel selection
		cancelSelection()
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
			onCancelSelection = cancelSelection
		)

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
				Text(text = "Loading photos...")
			} else if (photos.isEmpty()) {
				Text(text = "No photos yet")
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
	isSelected: Boolean = false,
	onLongClick: () -> Unit = {},
	onClick: () -> Unit = {},
	modifier: Modifier = Modifier
) {
	val thumbnailBitmap = remember {
		val options = BitmapFactory.Options().apply {
			inSampleSize = 4
		}
		BitmapFactory.decodeFile(photo.photoFile.absolutePath, options).asImageBitmap()
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
			Image(
				bitmap = thumbnailBitmap,
				contentDescription = "Photo ${photo.photoName}",
				contentScale = ContentScale.Crop,
				modifier = Modifier.fillMaxSize()
			)
		}
	}
}

