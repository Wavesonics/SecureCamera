package com.darkrockstudios.app.securecamera.viewphoto

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.ConfirmDeletePhotoDialog
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.camera.PhotoDef
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import com.darkrockstudios.app.securecamera.sharePhotoData
import com.zhangke.imageviewer.ImageViewer
import com.zhangke.imageviewer.rememberImageViewerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ViewPhotoContent(
	photo: PhotoDef,
	navController: NavController,
	modifier: Modifier = Modifier,
	paddingValues: PaddingValues
) {
	val imageManager = koinInject<SecureImageManager>()
	var showDeleteConfirmation by remember { mutableStateOf(false) }
	val preferencesManager = koinInject<AppPreferencesManager>()
	val context = LocalContext.current
	val scope = rememberCoroutineScope()

	val sanitizeFileName by
	preferencesManager.sanitizeFileName.collectAsState(preferencesManager.sanitizeFileNameDefault)
	val sanitizeMetadata by
	preferencesManager.sanitizeFileName.collectAsState(preferencesManager.sanitizeMetadataDefault)

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
			onShareClick = {
				scope.launch {
					sharePhotoData(
						photo = photo,
						sanitizeName = sanitizeFileName,
						sanitizeMetadata = sanitizeMetadata,
						imageManager = imageManager,
						context = context
					)
				}
			},
		)

		if (showDeleteConfirmation) {
			ConfirmDeletePhotoDialog(
				selectedCount = 1, onConfirm = {
					showDeleteConfirmation = false
					imageManager.deleteImage(photo)
					navController.navigateUp()
				},
				onDismiss = {
					showDeleteConfirmation = false
				}
			)
		}

		var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
		LaunchedEffect(Unit) {
			scope.launch(Dispatchers.IO) {
				imageBitmap = imageManager.readImage(photo).asImageBitmap()
			}
		}

		Box(
			modifier = Modifier
				.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			if (photo.photoFile.exists()) {
				val state = rememberImageViewerState {
					navController.navigateUp()
				}
				ImageViewer(
					state = state,
					modifier = Modifier
						.fillMaxSize()
						.clipToBounds()
				) {
					imageBitmap?.let {
						Image(
							bitmap = it,
							contentDescription = stringResource(id = R.string.photo_content_description),
							contentScale = ContentScale.Fit,
						)
					} ?: run {
						Box(modifier = Modifier.fillMaxSize()) {
							Text(
								text = stringResource(R.string.photo_content_loading),
								modifier = Modifier.align(Alignment.Center)
							)
						}
					}
				}
			} else {
				Text(text = stringResource(id = R.string.photo_not_found))
			}
		}
	}
}
