package com.darkrockstudios.app.securecamera.gallery

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
	onShareClick: () -> Unit = {}
) {
	TopAppBar(
		title = {
			Text(
				"Gallery",
				color = MaterialTheme.colorScheme.onSurface,
			)
		},
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer,
			titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
		),
		navigationIcon = {
			IconButton(
				onClick = { navController?.navigateUp() },
				modifier = Modifier.padding(8.dp)
			) {
				Icon(
					imageVector = Icons.Filled.Close,
					contentDescription = "Close Gallery",
					tint = MaterialTheme.colorScheme.onSurface,
				)
			}
		}
	)
}
