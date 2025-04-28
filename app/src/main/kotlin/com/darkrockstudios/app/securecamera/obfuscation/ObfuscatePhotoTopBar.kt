package com.darkrockstudios.app.securecamera.obfuscation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObfuscatePhotoTopBar(
	navController: NavController,
	onObscureClick: () -> Unit,
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
			Button(
				onClick = onObscureClick,
				modifier = Modifier.padding(8.dp)
			) {
				Text(stringResource(id = R.string.obscure_action_button))
			}
		}
	)
}
