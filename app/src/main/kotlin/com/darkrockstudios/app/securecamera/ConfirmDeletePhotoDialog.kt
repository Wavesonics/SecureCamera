package com.darkrockstudios.app.securecamera

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun ConfirmDeletePhotoDialog(
	selectedCount: Int,
	onConfirm: () -> Unit,
	onDismiss: () -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = {
			Text(
				text = stringResource(
					id = if (selectedCount > 1) R.string.delete_photo_title_plural else R.string.delete_photo_title_singular
				)
			)
		},
		text = {
			Text(
				text = if (selectedCount > 1) {
					stringResource(id = R.string.delete_photo_message_plural, selectedCount)
				} else {
					stringResource(id = R.string.delete_photo_message_singular)
				}
			)
		},
		confirmButton = {
			TextButton(
				onClick = {
					onConfirm()
				}
			) {
				Text(stringResource(id = R.string.delete_button))
			}
		},
		dismissButton = {
			TextButton(
				onClick = onDismiss
			) {
				Text(stringResource(id = R.string.cancel_button))
			}
		}
	)
}
