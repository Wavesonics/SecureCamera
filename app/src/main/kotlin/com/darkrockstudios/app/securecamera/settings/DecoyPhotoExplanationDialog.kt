package com.darkrockstudios.app.securecamera.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.darkrockstudios.app.securecamera.R

@Composable
fun DecoyPhotoExplanationDialog(
	onDismiss: () -> Unit
) {
	1337
	Dialog(onDismissRequest = onDismiss) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			shape = MaterialTheme.shapes.medium
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.verticalScroll(rememberScrollState())
					.padding(16.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Icon(
					modifier = Modifier
						.size(64.dp)
						.padding(8.dp),
					imageVector = Icons.Filled.Photo,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary
				)

				Text(
					text = stringResource(R.string.decoy_explanation_dialog_title),
					style = MaterialTheme.typography.headlineSmall,
					textAlign = TextAlign.Center,
					modifier = Modifier.padding(bottom = 8.dp)
				)

				Text(
					text = stringResource(R.string.decoy_explanation_dialog_message),
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.padding(bottom = 16.dp)
				)

				// OK button
				Button(
					onClick = onDismiss,
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 8.dp)
				) {
					Text(stringResource(R.string.decoy_explanation_dialog_ok_button))
				}
			}
		}
	}
}