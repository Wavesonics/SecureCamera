package com.darkrockstudios.app.securecamera.gallery

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
	val secureImageManager = koinInject<SecureImageManager>()
	var photos by remember { mutableStateOf<List<PhotoDef>>(emptyList()) }
	var isLoading by remember { mutableStateOf(true) }

	LaunchedEffect(Unit) {
		photos = secureImageManager.getPhotos()
		isLoading = false
	}

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		GalleryTopNav(
			navController = navController,
			onDeleteClick = {
			},
			onShareClick = {
			}
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
				PhotoGrid(photos = photos, navController = navController)
			}
		}
	}
}

@Composable
private fun PhotoGrid(
	photos: List<PhotoDef>,
	navController: NavController,
	modifier: Modifier = Modifier
) {
	LazyVerticalGrid(
		columns = GridCells.Adaptive(minSize = 128.dp),
		contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 0.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
		modifier = modifier.fillMaxSize()
	) {
		items(photos) { photo ->
			PhotoItem(photo = photo, navController = navController)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoItem(
	photo: PhotoDef,
	navController: NavController,
	modifier: Modifier = Modifier
) {
	val thumbnailBitmap = remember {
		val options = BitmapFactory.Options().apply {
			inSampleSize = 4
		}
		BitmapFactory.decodeFile(photo.photoFile.absolutePath, options).asImageBitmap()
	}

	Card(
		modifier = modifier
			.aspectRatio(1f)
			.fillMaxWidth(),
		onClick = {
			navController.navigate(AppDestinations.createViewPhotoRoute(photo.photoName))
		}
	) {
		Image(
			bitmap = thumbnailBitmap,
			contentDescription = "Photo ${photo.photoName}",
			contentScale = ContentScale.Crop,
			modifier = Modifier.fillMaxSize()
		)
	}
}
