package com.darkrockstudios.app.securecamera.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

	// Collect preferences as state
	val sanitizeFileName by preferencesManager.sanitizeFileName.collectAsState(initial = true)
	val sanitizeMetadata by preferencesManager.sanitizeMetadata.collectAsState(initial = true)

	// Get current location permission status
	val locationPermissionStatus = locationRepository.getLocationPermissionStatus()

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
						contentDescription = "Back",
						tint = MaterialTheme.colorScheme.onSurface
					)
				}
			}
		)

		// Settings content
		Column(
			modifier = Modifier
				.fillMaxSize()
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
				modifier = Modifier.fillMaxWidth(),
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
