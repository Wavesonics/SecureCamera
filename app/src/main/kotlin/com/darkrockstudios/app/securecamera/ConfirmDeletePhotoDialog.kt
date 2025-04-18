package com.darkrockstudios.app.securecamera

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDeletePhotoDialog(
	selectedCount: Int,
	onConfirm: () -> Unit,
	onDismiss: () -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Delete Photo${if (selectedCount > 1) "s" else ""}") },
		text = { Text("Are you sure you want to delete ${if (selectedCount > 1) "these $selectedCount photos" else "this photo"}?") },
		confirmButton = {
			TextButton(
				onClick = {
					onConfirm()
				}
			) {
				Text("Delete")
			}
		},
		dismissButton = {
			TextButton(
				onClick = onDismiss
			) {
				Text("Cancel")
			}
		}
	)
}
