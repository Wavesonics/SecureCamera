package com.darkrockstudios.app.securecamera.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.navigation.AppDestinations

@Composable
fun BottomCameraControls(
	modifier: Modifier = Modifier.Companion,
	onCapture: (() -> Unit)?,
	isLoading: Boolean,
	navController: NavHostController,
) {
	Box(
		modifier = modifier
			.fillMaxWidth()
			.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
	) {
		ElevatedButton(
			onClick = { navController.navigate(AppDestinations.SETTINGS_ROUTE) },
			enabled = isLoading.not(),
			modifier = Modifier.Companion.align(Alignment.Companion.BottomStart),
		) {
			Icon(
				imageVector = Icons.Filled.Settings,
				contentDescription = stringResource(R.string.camera_settings_button),
				modifier = Modifier.Companion.size(32.dp),
			)
		}

		if (onCapture != null) {
			FilledTonalButton(
				onClick = onCapture,
				modifier = Modifier.Companion
					.size(80.dp)
					.clip(CircleShape)
					.align(Alignment.Companion.BottomCenter),
				colors = ButtonDefaults.filledTonalButtonColors(
					containerColor = MaterialTheme.colorScheme.primary,
				),
			) {
				Icon(
					imageVector = Icons.Filled.Camera,
					contentDescription = stringResource(id = R.string.camera_capture_content_description),
					tint = MaterialTheme.colorScheme.onPrimary,
					modifier = Modifier.Companion.size(32.dp),
				)
			}
		}

		ElevatedButton(
			onClick = { navController.navigate(AppDestinations.GALLERY_ROUTE) },
			enabled = isLoading.not(),
			modifier = Modifier.Companion.align(Alignment.Companion.BottomEnd),
		) {
			Icon(
				imageVector = Icons.Filled.PhotoLibrary,
				contentDescription = stringResource(id = R.string.camera_gallery_content_description),
				modifier = Modifier.Companion.size(32.dp),
			)
		}
	}
}