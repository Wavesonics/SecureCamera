package com.darkrockstudios.app.securecamera.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.darkrockstudios.app.securecamera.LocationPermissionStatus
import com.darkrockstudios.app.securecamera.LocationRepository
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import kotlinx.coroutines.launch

/**
 * Settings screen content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
	navController: NavHostController,
	modifier: Modifier = Modifier,
	paddingValues: PaddingValues,
	preferencesManager: AppPreferencesManager,
	locationRepository: LocationRepository
) {
	val coroutineScope = rememberCoroutineScope()
	val context = LocalContext.current

	val sanitizeFileName by preferencesManager.sanitizeFileName.collectAsState(initial = true)
	val sanitizeMetadata by preferencesManager.sanitizeMetadata.collectAsState(initial = true)

	var locationPermissionStatus by rememberSaveable { mutableStateOf(locationRepository.getLocationPermissionStatus()) }

	var showLocationDialog by rememberSaveable { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		locationPermissionStatus = locationRepository.getLocationPermissionStatus()
	}

	if (showLocationDialog) {
		LocationDialog(locationPermissionStatus, context) {
			showLocationDialog = false
		}
	}

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		TopAppBar(
			title = {
				Text(
					text = stringResource(id = R.string.settings_title),
					color = MaterialTheme.colorScheme.onSurface
				)
			},
			colors = TopAppBarDefaults.topAppBarColors(
				containerColor = MaterialTheme.colorScheme.primaryContainer,
				titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
			),
			navigationIcon = {
				IconButton(onClick = { navController.popBackStack() }) {
					Icon(
						imageVector = Icons.AutoMirrored.Filled.ArrowBack,
						contentDescription = stringResource(id = R.string.settings_back_description),
						tint = MaterialTheme.colorScheme.onSurface
					)
				}
			}
		)

		// Settings content
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(
					start = 16.dp,
					end = 16.dp,
					bottom = 0.dp,
					top = 8.dp
				),
			verticalArrangement = Arrangement.Top,
			horizontalAlignment = Alignment.Start
		) {
			Text(
				text = stringResource(id = R.string.settings_title),
				style = MaterialTheme.typography.headlineMedium
			)

			Spacer(modifier = Modifier.height(16.dp))

			Text(
				text = stringResource(id = R.string.settings_description),
				style = MaterialTheme.typography.bodyLarge
			)

			Spacer(modifier = Modifier.height(24.dp))

			// Sharing section
			Text(
				text = stringResource(id = R.string.settings_sharing_section),
				style = MaterialTheme.typography.titleLarge
			)

			Spacer(modifier = Modifier.height(8.dp))

			// Sanitize file name checkbox
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = stringResource(id = R.string.settings_sanitize_filename),
						style = MaterialTheme.typography.bodyLarge
					)
					Text(
						text = stringResource(id = R.string.settings_sanitize_filename_description),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				Switch(
					checked = sanitizeFileName,
					onCheckedChange = { checked ->
						coroutineScope.launch {
							preferencesManager.setSanitizeFileName(checked)
						}
					}
				)
			}

			Spacer(modifier = Modifier.height(16.dp))

			// Sanitize metadata checkbox
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = stringResource(id = R.string.settings_sanitize_metadata),
						style = MaterialTheme.typography.bodyLarge
					)
					Text(
						text = stringResource(id = R.string.settings_sanitize_metadata_description),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				Switch(
					checked = sanitizeMetadata,
					onCheckedChange = { checked ->
						coroutineScope.launch {
							preferencesManager.setSanitizeMetadata(checked)
						}
					}
				)
			}

			Spacer(modifier = Modifier.height(24.dp))

			// Location section
			Text(
				text = stringResource(id = R.string.settings_location_section),
				style = MaterialTheme.typography.titleLarge
			)

			Spacer(modifier = Modifier.height(8.dp))

			// Location permission status
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.clickable { showLocationDialog = true },
				verticalAlignment = Alignment.CenterVertically
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = stringResource(id = R.string.settings_location_status),
						style = MaterialTheme.typography.bodyLarge
					)
					Text(
						text = stringResource(id = R.string.settings_location_description),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				Text(
					text = when (locationPermissionStatus) {
						LocationPermissionStatus.DENIED -> stringResource(id = R.string.settings_location_status_denied)
						LocationPermissionStatus.COARSE -> stringResource(id = R.string.settings_location_status_coarse)
						LocationPermissionStatus.FINE -> stringResource(id = R.string.settings_location_status_fine)
					},
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.primary
				)
			}
		}
	}
}

@Composable
private fun LocationDialog(
	locationPermissionStatus: LocationPermissionStatus,
	context: Context,
	onDismiss: () -> Unit
) {
	Dialog(onDismissRequest = onDismiss) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			shape = MaterialTheme.shapes.medium
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp)
			) {
				Text(
					text = when (locationPermissionStatus) {
						LocationPermissionStatus.DENIED -> stringResource(id = R.string.location_dialog_title_denied)
						LocationPermissionStatus.COARSE -> stringResource(id = R.string.location_dialog_title_coarse)
						LocationPermissionStatus.FINE -> stringResource(id = R.string.location_dialog_title_fine)
					},
					style = MaterialTheme.typography.headlineSmall
				)

				Spacer(modifier = Modifier.height(8.dp))

				Text(
					text = when (locationPermissionStatus) {
						LocationPermissionStatus.DENIED -> stringResource(id = R.string.location_dialog_message_denied)
						LocationPermissionStatus.COARSE -> stringResource(id = R.string.location_dialog_message_coarse)
						LocationPermissionStatus.FINE -> stringResource(id = R.string.location_dialog_message_fine)
					},
					style = MaterialTheme.typography.bodyMedium
				)

				Spacer(modifier = Modifier.height(16.dp))

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.End
				) {
					TextButton(onClick = onDismiss) {
						Text(stringResource(id = R.string.cancel_button))
					}

					Spacer(modifier = Modifier.width(8.dp))

					Button(
						onClick = {
							val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
								data = Uri.fromParts("package", context.packageName, null)
							}
							context.startActivity(intent)
							onDismiss()
						}
					) {
						Text(
							text = stringResource(id = R.string.location_dialog_change_settings)
						)
					}
				}
			}
		}
	}
}
