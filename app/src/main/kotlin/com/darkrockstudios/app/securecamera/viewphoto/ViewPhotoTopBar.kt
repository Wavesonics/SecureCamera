package com.darkrockstudios.app.securecamera.viewphoto

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewPhotoTopBar(
	navController: NavController,
	onDeleteClick: () -> Unit,
	onShareClick: () -> Unit,
) {
	TopAppBar(
		title = {
			Text(
				"Photo",
				color = MaterialTheme.colorScheme.onSurface,
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
					contentDescription = "Close Photo",
					tint = MaterialTheme.colorScheme.onSurface,
				)
			}
		},
		actions = {
			IconButton(
				onClick = onShareClick,
				modifier = Modifier.padding(8.dp)
			) {
				Icon(
					imageVector = Icons.Filled.Share,
					contentDescription = "Share Photos",
					tint = MaterialTheme.colorScheme.onSurface,
				)
			}
			IconButton(
				onClick = onDeleteClick,
				modifier = Modifier.padding(8.dp)
			) {
				Icon(
					imageVector = Icons.Filled.Delete,
					contentDescription = "Delete Photos",
					tint = MaterialTheme.colorScheme.onSurface,
				)
			}
		}
	)
}