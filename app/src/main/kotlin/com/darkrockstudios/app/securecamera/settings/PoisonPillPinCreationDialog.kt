package com.darkrockstudios.app.securecamera.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.auth.pinSize
import com.darkrockstudios.app.securecamera.usecases.PinStrengthCheckUseCase
import org.koin.compose.koinInject

@Composable
fun PoisonPillPinCreationDialog(
	currentPin: String,
	onDismiss: () -> Unit,
	onPinCreated: (String) -> Unit
) {
	var pin by rememberSaveable { mutableStateOf("") }
	var confirmPin by rememberSaveable { mutableStateOf("") }
	var showError by rememberSaveable { mutableStateOf<String?>(null) }
	val pinStrengthCheck = koinInject<PinStrengthCheckUseCase>()
	val context = LocalContext.current

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
					imageVector = Icons.Filled.Pin,
					contentDescription = null,
					tint = Color.Green
				)

				Text(
					text = stringResource(R.string.poison_pill_creation_title),
					style = MaterialTheme.typography.headlineSmall,
					textAlign = TextAlign.Center,
					modifier = Modifier.padding(bottom = 8.dp)
				)

				Text(
					text = stringResource(R.string.poison_pill_creation_description),
					style = MaterialTheme.typography.bodyMedium,
					textAlign = TextAlign.Center,
					modifier = Modifier.padding(bottom = 16.dp)
				)

				// PIN input
				OutlinedTextField(
					value = pin,
					onValueChange = {
						if (it.length <= pinSize.max() && it.all { char -> char.isDigit() }) {
							pin = it
							showError = null
						}
					},
					label = { Text(stringResource(R.string.pin_creation_hint)) },
					visualTransformation = PasswordVisualTransformation(),
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.NumberPassword,
						imeAction = ImeAction.Next
					),
					singleLine = true,
					modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
				)

				// Confirm PIN input
				OutlinedTextField(
					value = confirmPin,
					onValueChange = {
						if (it.length <= pinSize.max() && it.all { char -> char.isDigit() }) {
							confirmPin = it
							showError = null
						}
					},
					label = { Text(stringResource(R.string.pin_creation_confirm_hint)) },
					visualTransformation = PasswordVisualTransformation(),
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.NumberPassword,
						imeAction = ImeAction.Done
					),
					singleLine = true,
					modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
				)

				// Error message
				if (showError != null) {
					Text(
						text = showError ?: "",
						color = MaterialTheme.colorScheme.error,
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier.padding(bottom = 16.dp)
					)
				}

				// Buttons
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.End
				) {
					TextButton(onClick = onDismiss) {
						Text(stringResource(id = R.string.cancel_button))
					}

					Spacer(modifier = Modifier.width(8.dp))

					Button(
						onClick = {
							val strongPin = pinStrengthCheck.isPinStrongEnough(pin)
							if (pin != confirmPin || (pin.length in pinSize).not()) {
								showError = context.getString(R.string.pin_creation_error)
							} else if (pin == currentPin) {
								showError = context.getString(R.string.poison_pill_creation_error)
							} else if (strongPin.not()) {
								showError = context.getString(R.string.pin_creation_error_weak_pin)
							} else {
								onPinCreated(pin)
							}
						},
						enabled = pin.length in pinSize && confirmPin.length in pinSize,
					) {
						Text(stringResource(R.string.pin_creation_button))
					}
				}
			}
		}
	}
}