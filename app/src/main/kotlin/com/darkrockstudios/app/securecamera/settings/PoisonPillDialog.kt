package com.darkrockstudios.app.securecamera.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.darkrockstudios.app.securecamera.R

@Composable
fun PoisonPillDialog(
	onDismiss: () -> Unit,
	onContinue: () -> Unit
) {
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
                    .padding(16.dp)
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier.fillMaxWidth()
				) {
					Icon(
						imageVector = Icons.Filled.Info,
						contentDescription = null,
						modifier = Modifier.size(24.dp)
					)
					Spacer(modifier = Modifier.width(8.dp))
					Text(
						text = stringResource(id = R.string.poison_pill_dialog_title),
						style = MaterialTheme.typography.headlineSmall
					)
				}

				Spacer(modifier = Modifier.height(8.dp))

				Text(
					text = stringResource(id = R.string.poison_pill_dialog_message),
					style = MaterialTheme.typography.bodyMedium
				)

				Spacer(modifier = Modifier.height(16.dp))

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.End
				) {
					TextButton(onClick = onDismiss) {
						Text(stringResource(id = R.string.poison_pill_cancel_button))
					}

					Spacer(modifier = Modifier.width(8.dp))

					Button(
						onClick = onContinue,
					) {
						Text(
							text = stringResource(id = R.string.poison_pill_continue_button),
						)
					}
				}
			}
		}
	}
}