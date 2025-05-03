package com.darkrockstudios.app.securecamera.obfuscation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.darkrockstudios.app.securecamera.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObfuscatePhotoTopBar(
	onAddRegionClick: () -> Unit,
	isFindingFaces: Boolean,
	isCreatingRegion: Boolean = false,
	onBackPressed: () -> Unit,
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
				onClick = { onBackPressed() },
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
			if (!isCreatingRegion) {
				if (isFindingFaces) {
					CircularProgressIndicator(
						modifier = Modifier.size(24.dp),
						color = MaterialTheme.colorScheme.onPrimaryContainer,
						strokeWidth = 2.dp
					)
				} else {
					// Add Region button
					IconButton(
						onClick = onAddRegionClick,
						modifier = Modifier.padding(8.dp),
					) {
						Icon(
							imageVector = Icons.Filled.AddBox,
							contentDescription = stringResource(id = R.string.obscure_action_button_add_region),
						)
					}
				}
			}
		}
	)
}
