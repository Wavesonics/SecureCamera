package com.darkrockstudios.app.securecamera.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.darkrockstudios.app.securecamera.LocationPermissionStatus
import com.darkrockstudios.app.securecamera.LocationRepository
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager.Companion.SESSION_TIMEOUT_10_MIN
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager.Companion.SESSION_TIMEOUT_1_MIN
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager.Companion.SESSION_TIMEOUT_5_MIN
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager.Companion.SESSION_TIMEOUT_DEFAULT
import com.darkrockstudios.app.securecamera.usecases.SecurityResetUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
	locationRepository: LocationRepository,
	snackbarHostState: SnackbarHostState,
) {
	val coroutineScope = rememberCoroutineScope()
	val context = LocalContext.current
	val securityResetUseCase = koinInject<SecurityResetUseCase>()
	val authorizationManager = koinInject<AuthorizationManager>()
	val imageManager = koinInject<SecureImageManager>()

	val sanitizeFileName by preferencesManager.sanitizeFileName.collectAsState(initial = true)
	val sanitizeMetadata by preferencesManager.sanitizeMetadata.collectAsState(initial = true)
	val sessionTimeout by preferencesManager.sessionTimeout.collectAsState(initial = SESSION_TIMEOUT_DEFAULT)

	val locationPermissionStatus by locationRepository.locationPermissionStatus.collectAsState()

	var showLocationDialog by rememberSaveable { mutableStateOf(false) }
	var showSecurityResetDialog by rememberSaveable { mutableStateOf(false) }
	var showPoisonPillDialog by rememberSaveable { mutableStateOf(false) }
	var showPoisonPillPinCreationDialog by rememberSaveable { mutableStateOf(false) }
	var showDecoyPhotoExplanationDialog by rememberSaveable { mutableStateOf(false) }
	var showRemovePoisonPillDialog by rememberSaveable { mutableStateOf(false) }
	var hasPoisonPillPin by rememberSaveable { mutableStateOf(false) }

	// Check if a Poison Pill PIN is set
	LaunchedEffect(Unit) {
		hasPoisonPillPin = preferencesManager.hasPoisonPillPin()
	}

	LaunchedEffect(Unit) {
		locationRepository.refreshPermissionStatus()
	}

	if (showLocationDialog) {
		LocationDialog(locationPermissionStatus, context) {
			showLocationDialog = false
		}
	}

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		TopAppBar(
			title = {
				Text(
					text = stringResource(id = R.string.settings_title),
					color = MaterialTheme.colorScheme.onPrimaryContainer
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
						tint = MaterialTheme.colorScheme.onPrimaryContainer
					)
				}
			},
			actions = {
				IconButton(
					onClick = { navController.navigate(AppDestinations.ABOUT_ROUTE) },
					modifier = Modifier.padding(8.dp)
				) {
					Icon(
						imageVector = Icons.Filled.Info,
						contentDescription = stringResource(id = R.string.settings_about_button),
						tint = MaterialTheme.colorScheme.onPrimaryContainer,
					)
				}
			}
		)

		// Settings content
		Column(
			modifier = Modifier
				.widthIn(max = 512.dp)
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

			Spacer(modifier = Modifier.height(24.dp))

			// Security section
			Text(
				text = stringResource(id = R.string.settings_security_section),
				style = MaterialTheme.typography.titleLarge
			)

			Spacer(modifier = Modifier.height(8.dp))

			// Session timeout dropdown
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = stringResource(id = R.string.settings_session_timeout),
						style = MaterialTheme.typography.bodyLarge
					)
					Text(
						text = stringResource(id = R.string.settings_session_timeout_description),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				// Dropdown menu for session timeout
				var expanded by remember { mutableStateOf(false) }
				Box {
					Text(
						text = when (sessionTimeout) {
							SESSION_TIMEOUT_1_MIN -> stringResource(id = R.string.settings_session_timeout_1_min)
							SESSION_TIMEOUT_5_MIN -> stringResource(id = R.string.settings_session_timeout_5_min)
							SESSION_TIMEOUT_10_MIN -> stringResource(id = R.string.settings_session_timeout_10_min)
							else -> stringResource(id = R.string.settings_session_timeout_5_min)
						},
						modifier = Modifier
							.clickable { expanded = true }
							.padding(8.dp),
						color = MaterialTheme.colorScheme.primary
					)
					DropdownMenu(
						expanded = expanded,
						onDismissRequest = { expanded = false }
					) {
						DropdownMenuItem(
							text = { Text(stringResource(id = R.string.settings_session_timeout_1_min)) },
							onClick = {
								coroutineScope.launch {
									preferencesManager.setSessionTimeout(SESSION_TIMEOUT_1_MIN)
								}
								expanded = false
							}
						)
						DropdownMenuItem(
							text = { Text(stringResource(id = R.string.settings_session_timeout_5_min)) },
							onClick = {
								coroutineScope.launch {
									preferencesManager.setSessionTimeout(SESSION_TIMEOUT_5_MIN)
								}
								expanded = false
							}
						)
						DropdownMenuItem(
							text = { Text(stringResource(id = R.string.settings_session_timeout_10_min)) },
							onClick = {
								coroutineScope.launch {
									preferencesManager.setSessionTimeout(SESSION_TIMEOUT_10_MIN)
								}
								expanded = false
							}
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(16.dp))

			// Poison Pill option
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.clickable {
						if (hasPoisonPillPin) {
							showRemovePoisonPillDialog = true
						} else {
							showPoisonPillDialog = true
						}
					},
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = stringResource(
							id = if (hasPoisonPillPin) {
								R.string.settings_remove_poison_pill
							} else {
								R.string.settings_poison_pill
							}
						),
						style = MaterialTheme.typography.bodyLarge
					)
					Text(
						text = stringResource(
							id = if (hasPoisonPillPin) {
								R.string.settings_remove_poison_pill_description
							} else {
								R.string.settings_poison_pill_description
							}
						),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}

			Spacer(modifier = Modifier.height(16.dp))

			// Security Reset option
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.clickable { showSecurityResetDialog = true },
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = stringResource(id = R.string.settings_security_reset),
						style = MaterialTheme.typography.bodyLarge
					)
					Text(
						text = stringResource(id = R.string.settings_security_reset_description),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				Icon(
					imageVector = Icons.Filled.Warning,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.error,
					modifier = Modifier.size(32.dp)
				)
			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}

	// Show Security Reset Dialog if needed
	if (showSecurityResetDialog) {
		val msg = stringResource(R.string.security_reset_complete_toast)
		SecurityResetDialog(
			onDismiss = { showSecurityResetDialog = false },
			onConfirm = {
				coroutineScope.launch {
					securityResetUseCase.reset()
					snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long)
					navController.navigate(AppDestinations.INTRODUCTION_ROUTE) {
						popUpTo(0) { inclusive = true }
					}
				}
			}
		)
	}

	// Show Poison Pill Dialog if needed
	if (showPoisonPillDialog) {
		PoisonPillDialog(
			onDismiss = { showPoisonPillDialog = false },
			onContinue = {
				showPoisonPillDialog = false
				showPoisonPillPinCreationDialog = true
			}
		)
	}

	// Show Poison Pill PIN Creation Dialog if needed
	if (showPoisonPillPinCreationDialog) {
		val currentPin = authorizationManager.securityPin?.plainPin ?: ""
		PoisonPillPinCreationDialog(
			currentPin = currentPin,
			onDismiss = { showPoisonPillPinCreationDialog = false },
			onPinCreated = { pin ->
				coroutineScope.launch {
					preferencesManager.setPoisonPillPin(pin)
					showPoisonPillPinCreationDialog = false
					hasPoisonPillPin = true
					showDecoyPhotoExplanationDialog = true
				}
			}
		)
	}

	// Show Decoy Photo Explanation Dialog if needed
	if (showDecoyPhotoExplanationDialog) {
		val setupCompleteMsg = stringResource(R.string.poison_pill_setup_complete)
		DecoyPhotoExplanationDialog(
			onDismiss = {
				showDecoyPhotoExplanationDialog = false
				coroutineScope.launch {
					snackbarHostState.showSnackbar(
						setupCompleteMsg,
						duration = SnackbarDuration.Long
					)
				}
			}
		)
	}

	// Show Remove Poison Pill Dialog if needed
	if (showRemovePoisonPillDialog) {
		val removeCompleteMsg = stringResource(R.string.remove_poison_pill_complete)
		RemovePoisonPillDialog(
			onDismiss = { showRemovePoisonPillDialog = false },
			onConfirm = {
				coroutineScope.launch {
					preferencesManager.removePoisonPillPin()
					imageManager.removeAllDecoyPhotos()
					showRemovePoisonPillDialog = false
					hasPoisonPillPin = false
					snackbarHostState.showSnackbar(
						removeCompleteMsg,
						duration = SnackbarDuration.Long
					)
				}
			}
		)
	}
}
