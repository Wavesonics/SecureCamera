package com.darkrockstudios.app.securecamera.import

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@Composable
fun ImportPhotosContent(
	photosToImport: List<Uri>,
	navController: NavHostController
) {
	val viewModel: ImportPhotosViewModel = koinViewModel()
	val context = LocalContext.current
	val uiState by viewModel.uiState.collectAsState()

	var showCancelDialog by remember { mutableStateOf(false) }

	BackHandler(enabled = !uiState.complete) {
		showCancelDialog = true
	}

	NotificationPermissionRationale()

	if (showCancelDialog) {
		CancelImportDialog(navController) {
			showCancelDialog = false
		}
	}

	var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

	fun loadBitmapFromUri(uri: Uri): Bitmap {
		val source = ImageDecoder.createSource(context.contentResolver, uri)
		return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
			decoder.setTargetSampleSize(2)
		}
	}

	LaunchedEffect(Unit) {
		viewModel.beginImport(
			photos = photosToImport,
			progress = { curPhoto ->
				currentBitmap = try {
					loadBitmapFromUri(curPhoto)
				} catch (e: Exception) {
					Timber.w(e, "Failed to load imported photo: $curPhoto")
					null
				}
			}
		)
	}

	Column(
		modifier = Modifier
			.fillMaxSize(),
	) {
		ImportPhotosTopBar(
			navController = navController
		)

		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
		) {
			LinearProgressIndicator(
				progress = {
					val total = uiState.totalPhotos
					if (total > 0) {
						(total - uiState.remainingPhotos.toFloat()) / total
					} else {
						0f
					}
				},
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp)
			)

			if (uiState.complete.not()) {
				Text(
					stringResource(
						R.string.import_photos_progress_label,
						(uiState.totalPhotos - uiState.remainingPhotos),
						uiState.totalPhotos,
					),
					style = MaterialTheme.typography.headlineSmall
				)
				if (uiState.failedPhotos > 0) {
					Text(
						stringResource(
							R.string.import_photos_failed_label,
							uiState.failedPhotos
						),
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.error
					)
				}
				Spacer(modifier = Modifier.height(16.dp))
				Box(
					modifier = Modifier
						.size(300.dp)
						.padding(16.dp),
					contentAlignment = Alignment.Center
				) {
					currentBitmap?.let { bitmap ->
						Image(
							bitmap = bitmap.asImageBitmap(),
							contentDescription = stringResource(R.string.import_photos_current_photo_description),
							modifier = Modifier.fillMaxWidth()
						)
					}
				}
			} else {
				Text(
					stringResource(R.string.import_photos_done_label),
					style = MaterialTheme.typography.displaySmall
				)
				Text(
					stringResource(
						R.string.import_photos_done_summary,
						uiState.successfulPhotos,
						uiState.failedPhotos
					),
					style = MaterialTheme.typography.bodyLarge
				)

				Button(
					modifier = Modifier.padding(16.dp),
					onClick = {
						navController.navigate(AppDestinations.GALLERY_ROUTE) {
							popUpTo(0) { inclusive = true }
						}
					}
				) {
					Text(stringResource(id = R.string.import_photos_done_button))
				}
			}
		}
	}
}

@Composable
private fun CancelImportDialog(navController: NavHostController, dismiss: () -> Unit) {
	val viewModel: ImportPhotosViewModel = koinViewModel()

	AlertDialog(
		onDismissRequest = { dismiss() },
		title = { Text(stringResource(id = R.string.discard_changes_dialog_title)) },
		text = { Text(stringResource(id = R.string.discard_changes_dialog_message)) },
		confirmButton = {
			TextButton(
				onClick = {
					viewModel.cancelImport()
					dismiss()
					navController.navigate(AppDestinations.GALLERY_ROUTE) {
						popUpTo(0) { inclusive = true }
					}
				}
			) {
				Text(stringResource(id = R.string.discard_button))
			}
		},
		dismissButton = {
			TextButton(
				onClick = { dismiss() }
			) {
				Text(stringResource(id = R.string.cancel_button))
			}
		}
	)
}

@Composable
private fun NotificationPermissionRationale() {
	val context = LocalContext.current

	val showNotificationPermissionDialog = remember { mutableStateOf(false) }

	val notificationPermissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { _ ->
		// Noop
	}

	// Check if we need to request notification permission (API 33+)
	LaunchedEffect(Unit) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
			if (ContextCompat.checkSelfPermission(
					context,
					notificationPermission
				) != PackageManager.PERMISSION_GRANTED
			) {
				showNotificationPermissionDialog.value = true
			}
		}
	}

	// Notification permission dialog (API 33+)
	if (showNotificationPermissionDialog.value) {
		AlertDialog(
			onDismissRequest = { showNotificationPermissionDialog.value = false },
			title = { Text(stringResource(id = R.string.notification_permission_dialog_title)) },
			text = { Text(stringResource(id = R.string.notification_permission_dialog_message)) },
			confirmButton = {
				TextButton(
					onClick = {
						showNotificationPermissionDialog.value = false
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
							notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
						}
					}
				) {
					Text(stringResource(id = R.string.notification_permission_button))
				}
			},
			dismissButton = {
				TextButton(
					onClick = { showNotificationPermissionDialog.value = false }
				) {
					Text(stringResource(id = R.string.cancel_button))
				}
			}
		)
	}
}
