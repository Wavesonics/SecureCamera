package com.darkrockstudios.app.securecamera.camera

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NoCameraPermission(navController: NavHostController, permissionsState: MultiplePermissionsState) {
	val context = LocalContext.current

	Box(
		modifier = Modifier
			.fillMaxSize()
	) {
		Card(modifier = Modifier.align(Alignment.Center)) {
			Column(
				modifier = Modifier.padding(16.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Center,
			) {
				Text(
					stringResource(R.string.camera_permissions_required),
					modifier = Modifier.padding(16.dp)
				)

				if (permissionsState.revokedPermissions.isNotEmpty()) {
					Button(onClick = {
						val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
							data = Uri.fromParts("package", context.packageName, null)
						}
						context.startActivity(intent)
					}) {
						Text(text = stringResource(R.string.camera_open_settings))
					}
				}
			}
		}

		NoPermissionBottomBar(modifier = Modifier.align(Alignment.BottomCenter), navController)
	}
}

@Composable
fun NoPermissionBottomBar(modifier: Modifier, navController: NavHostController) {
	Box(
		modifier = modifier
			.fillMaxWidth()
			.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
	) {
		IconButton(
			onClick = { navController.navigate(AppDestinations.SETTINGS_ROUTE) },
			modifier = Modifier
				.background(MaterialTheme.colorScheme.primary, CircleShape)
				.padding(8.dp)
				.align(Alignment.BottomStart),
		) {
			Icon(
				imageVector = Icons.Filled.Settings,
				contentDescription = stringResource(R.string.camera_settings_button),
				tint = Color.White
			)
		}

		IconButton(
			onClick = { navController.navigate(AppDestinations.GALLERY_ROUTE) },
			modifier = Modifier
				.background(MaterialTheme.colorScheme.primary, CircleShape)
				.padding(8.dp)
				.align(Alignment.BottomEnd),
		) {
			Icon(
				imageVector = Icons.Filled.PhotoLibrary,
				contentDescription = stringResource(id = R.string.camera_gallery_content_description),
				tint = Color.White
			)
		}
	}
}
