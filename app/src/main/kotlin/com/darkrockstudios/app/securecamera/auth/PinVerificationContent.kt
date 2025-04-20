package com.darkrockstudios.app.securecamera.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Screen for PIN verification
 */
@Composable
fun PinVerificationContent(
	navController: NavController,
	returnRoute: String,
	modifier: Modifier = Modifier
) {
	val authManager = koinInject<AuthorizationManager>()
	val coroutineScope = rememberCoroutineScope()

	var pin by remember { mutableStateOf("") }
	var errorMessage by remember { mutableStateOf<String?>(null) }
	var isVerifying by remember { mutableStateOf(false) }

	// Store string resources
	val pinVerificationTitle = stringResource(R.string.pin_verification_title)
	val pinVerificationLabel = stringResource(R.string.pin_verification_label)
	val pinEmptyError = stringResource(R.string.pin_verification_empty_error)
	val pinInvalidError = stringResource(R.string.pin_verification_invalid_error)
	val verifyButtonText = stringResource(R.string.pin_verification_button)

	val focusRequester = remember { FocusRequester() }
	val keyboardController = LocalSoftwareKeyboardController.current

	LaunchedEffect(Unit) {
		focusRequester.requestFocus()
	}

	fun verify() {
		if (pin.isBlank()) {
			errorMessage = pinEmptyError
			return
		}

		keyboardController?.hide()

		isVerifying = true
		coroutineScope.launch {
			val isValid = authManager.verifyPin(pin)
			isVerifying = false

			if (isValid) {
				navController.navigate(returnRoute) {
					popUpTo(AppDestinations.PIN_VERIFICATION_ROUTE) { inclusive = true }
					launchSingleTop = true
				}
			} else {
				errorMessage = pinInvalidError
				pin = ""
			}
		}
	}

	Box(modifier = modifier
		.fillMaxSize()
		.background(color = MaterialTheme.colorScheme.background)) {
		Column(
			modifier = Modifier
				.padding(16.dp)
				.widthIn(max = 512.dp)
				.align(Alignment.Center),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Icon(
				modifier = Modifier
					.size(96.dp)
					.padding(16.dp),
				imageVector = Icons.Filled.Camera,
				contentDescription = stringResource(id = R.string.pin_verification_icon),
				tint = Color.White
			)

			Text(
				text = pinVerificationTitle,
				style = MaterialTheme.typography.headlineMedium,
				modifier = Modifier.padding(bottom = 24.dp)
			)

			OutlinedTextField(
				value = pin,
				onValueChange = {
					if (it.length <= pinSize.max() && it.all { char -> char.isDigit() }) {
						pin = it
						errorMessage = null
					}
				},
				label = { Text(pinVerificationLabel) },
				visualTransformation = PasswordVisualTransformation(),
				singleLine = true,
				keyboardOptions = KeyboardOptions(
					keyboardType = KeyboardType.NumberPassword,
					imeAction = ImeAction.Done
				),
				keyboardActions = KeyboardActions(
					onDone = { verify() }
				),
				isError = errorMessage != null,
				enabled = !isVerifying,
				modifier = Modifier
					.fillMaxWidth()
					.focusRequester(focusRequester)
			)

			errorMessage?.let {
				Text(
					text = it,
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodySmall,
					modifier = Modifier.padding(top = 4.dp)
				)
			}

			Spacer(modifier = Modifier.height(24.dp))

			Button(
				onClick = { verify() },
				enabled = !isVerifying,
				modifier = Modifier.fillMaxWidth()
			) {
				Text(verifyButtonText)
			}

			if (isVerifying) {
				CircularProgressIndicator(
					modifier = Modifier.padding(top = 16.dp)
				)
			}
		}
	}
}
