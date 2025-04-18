package com.darkrockstudios.app.securecamera.gallery

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
fun GalleryTopNav(
	navController: NavController?,
	onDeleteClick: () -> Unit = {},
	onShareClick: () -> Unit = {},
	isSelectionMode: Boolean = false,
	selectedCount: Int = 0,
	onCancelSelection: () -> Unit = {}
) {
	TopAppBar(
		title = {
			Text(
				if (isSelectionMode) "$selectedCount Selected" else "Gallery",
				color = MaterialTheme.colorScheme.onSurface,
			)
		},
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer,
			titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
		),
		navigationIcon = {
			IconButton(
				onClick = {
					if (isSelectionMode) {
						// Exit selection mode
						onCancelSelection()
					} else {
						navController?.navigateUp()
					}
				},
				modifier = Modifier.padding(8.dp)
			) {
				Icon(
					imageVector = Icons.Filled.Close,
					contentDescription = if (isSelectionMode) "Cancel Selection" else "Close Gallery",
					tint = MaterialTheme.colorScheme.onSurface,
				)
			}
		},
		actions = {
			if (isSelectionMode && selectedCount > 0) {
				// Show delete button
				IconButton(onClick = onDeleteClick) {
					Icon(
						imageVector = Icons.Filled.Delete,
						contentDescription = "Delete Selected",
						tint = MaterialTheme.colorScheme.onSurface
					)
				}

				// Show share button
				IconButton(onClick = onShareClick) {
					Icon(
						imageVector = Icons.Filled.Share,
						contentDescription = "Share Selected",
						tint = MaterialTheme.colorScheme.onSurface
					)
				}
			}
		}
	)
}
