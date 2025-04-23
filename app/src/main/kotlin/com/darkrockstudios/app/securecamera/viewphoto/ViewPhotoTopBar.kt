package com.darkrockstudios.app.securecamera.viewphoto

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewPhotoTopBar(
	navController: NavController,
	onDeleteClick: () -> Unit,
	onShareClick: () -> Unit,
	showDecoyButton: Boolean = false,
	isDecoy: Boolean = false,
	isDecoyLoading: Boolean = false,
	onDecoyClick: () -> Unit = {},
) {
	TopAppBar(
		title = {
			Text(
				stringResource(id = R.string.photo_title),
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
					imageVector = Icons.Filled.Close,
					contentDescription = stringResource(id = R.string.close_photo_content_description),
					tint = MaterialTheme.colorScheme.onPrimaryContainer,
				)
			}
		},
		actions = {
			if (showDecoyButton) {
				if (isDecoyLoading) {
					// Show loading spinner
					IconButton(
						onClick = { /* Disabled during loading */ },
						modifier = Modifier.padding(8.dp),
						enabled = false
					) {
						CircularProgressIndicator(
							modifier = Modifier.size(24.dp),
							color = MaterialTheme.colorScheme.onPrimaryContainer,
							strokeWidth = 2.dp
						)
					}
				} else {
					// Show decoy button
					IconButton(
						onClick = onDecoyClick,
						modifier = Modifier.padding(8.dp)
					) {
						Icon(
							imageVector = if (isDecoy) Icons.Filled.AddCircle else Icons.Filled.AddCircleOutline,
							contentDescription = stringResource(id = R.string.decoy_photo_content_description),
							tint = MaterialTheme.colorScheme.onPrimaryContainer,
						)
					}
				}
			}
			IconButton(
				onClick = onShareClick,
				modifier = Modifier.padding(8.dp)
			) {
				Icon(
					imageVector = Icons.Filled.Share,
					contentDescription = stringResource(id = R.string.share_photo_content_description),
					tint = MaterialTheme.colorScheme.onPrimaryContainer,
				)
			}
			IconButton(
				onClick = onDeleteClick,
				modifier = Modifier.padding(8.dp)
			) {
				Icon(
					imageVector = Icons.Filled.Delete,
					contentDescription = stringResource(id = R.string.delete_photo_content_description),
					tint = MaterialTheme.colorScheme.onPrimaryContainer,
				)
			}
		}
	)
}
