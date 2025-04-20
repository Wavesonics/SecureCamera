package com.darkrockstudios.app.securecamera.camera

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun CameraPermissionRationaleDialog(
	onContinue: () -> Unit,
	onDismiss: () -> Unit,
	title: String = "Camera access required",
	text: String =
		"We use the camera to capture photos. Images stay on your device unless you choose to share them.\n\n" +
				"We use location to save location of each photo when it's taken. This metadata is automatically stripped out by default when sharing the photos.\n\n" +
				"Denying Location permissions is okay, Location metadata will just not be attached to your photos.",

	icon: ImageVector = Icons.Default.CameraAlt
) {
	AlertDialog(
		icon = { Icon(icon, contentDescription = null) },
		title = { Text(title) },
		text = { Text(text) },
		onDismissRequest = onDismiss,
		confirmButton = {
			TextButton(onClick = onContinue) { Text("Request Permissions") }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Not now") }
		}
	)
}