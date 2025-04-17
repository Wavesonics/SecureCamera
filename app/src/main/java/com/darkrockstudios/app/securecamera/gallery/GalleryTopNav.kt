package com.darkrockstudios.app.securecamera.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.Cross
import com.darkrockstudios.app.securecamera.navigation.AppDestinations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTopNav(
	navController: NavController?
) {
	TopAppBar(
		title = { Text("Gallery") },
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer,
			titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
		),
		navigationIcon = {
			IconButton(
				onClick = { navController?.navigate(AppDestinations.CAMERA_ROUTE) },
				modifier = Modifier
					.padding(8.dp)
					.background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f), CircleShape)
			) {
				Icon(
					imageVector = Cross,
					contentDescription = "Close Gallery",
					tint = MaterialTheme.colorScheme.onSurface,
					modifier = Modifier.rotate(45f)
				)
			}
		}
	)
}