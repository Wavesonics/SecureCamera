package com.darkrockstudios.app.securecamera.obfuscation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BorderClear
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.R
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObfuscatePhotoTopBar(
	navController: NavController,
	onObscureClick: () -> Unit,
	onClearClick: () -> Unit,
	onSaveClick: () -> Unit,
	readyToObscure: Boolean,
	canClear: Boolean,
	readyToSave: Boolean,
	isFindingFaces: Boolean,
) {
	TopAppBar(
		title = {
			Text(
				stringResource(id = R.string.obfuscate_photo_button),
				color = MaterialTheme.colorScheme.onPrimaryContainer,
			)
		},
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer,
			titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
		),
		navigationIcon = {
			IconButton(
				onClick = { navController.navigateUp() },
				modifier = Modifier.padding(8.dp)
			) {
				Icon(
					imageVector = Icons.Filled.ArrowBack,
					contentDescription = stringResource(id = R.string.settings_back_description),
					tint = MaterialTheme.colorScheme.onPrimaryContainer,
				)
			}
		},
		actions = {
			Timber.tag("abrown").d("canClear: $canClear isFindingFaces: $isFindingFaces")
			if (canClear) {
				IconButton(
					onClick = onClearClick,
					modifier = Modifier.padding(8.dp),
				) {
					Icon(
						imageVector = Icons.Filled.BorderClear,
						contentDescription = stringResource(id = R.string.obscure_action_button_clear),
					)
				}
			} else if (isFindingFaces) {
				CircularProgressIndicator(
					modifier = Modifier.size(24.dp),
					color = MaterialTheme.colorScheme.onPrimaryContainer,
					strokeWidth = 2.dp
				)
			} else {
				IconButton(
					onClick = {
						Timber.e("blur click")
						onObscureClick()
					},
					modifier = Modifier.padding(8.dp),
					enabled = readyToObscure,
				) {
					Icon(
						imageVector = Icons.Filled.BlurOn,
						contentDescription = stringResource(id = R.string.obscure_action_button),
					)
				}
			}

			IconButton(
				onClick = onSaveClick,
				modifier = Modifier.padding(8.dp),
				enabled = readyToSave,
			) {
				Icon(
					imageVector = Icons.Filled.Save,
					contentDescription = stringResource(id = R.string.obscure_action_button_save),
				)
			}
		}
	)
}
