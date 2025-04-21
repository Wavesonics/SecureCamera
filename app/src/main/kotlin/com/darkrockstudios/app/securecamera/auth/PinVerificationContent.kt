package com.darkrockstudios.app.securecamera.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.gallery.vibrateDevice
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.darkrockstudios.app.securecamera.usecases.SecurityResetUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * Screen for PIN verification
 */
@Composable
fun PinVerificationContent(
	navController: NavController,
	snackbarHostState: SnackbarHostState,
	returnRoute: String,
	modifier: Modifier = Modifier
) {
	val authManager = koinInject<AuthorizationManager>()
	val imageManager = koinInject<SecureImageManager>()
	val securityResetUseCase = koinInject<SecurityResetUseCase>()

	val coroutineScope = rememberCoroutineScope()
	val context = LocalContext.current

	var pin by rememberSaveable { mutableStateOf("") }
	var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
	var isVerifying by rememberSaveable { mutableStateOf(false) }

	// Backoff mechanism state
	var failedAttempts by rememberSaveable { mutableStateOf(0) }
	var isBackoffActive by rememberSaveable { mutableStateOf(false) }
	var remainingBackoffSeconds by rememberSaveable { mutableStateOf(0) }

	// Store string resources
	val pinVerificationTitle = stringResource(R.string.pin_verification_title)
	val pinVerificationLabel = stringResource(R.string.pin_verification_label)
	val pinEmptyError = stringResource(R.string.pin_verification_empty_error)
	val pinInvalidError = stringResource(R.string.pin_verification_invalid_error)
	val verifyButtonText = stringResource(R.string.pin_verification_button)

	// Additional string resources
	val allDataDeletedText = stringResource(R.string.pin_verification_all_data_deleted)
	val remainingAttemptsText = stringResource(
		R.string.pin_verification_remaining_attempts,
		AuthorizationManager.MAX_FAILED_ATTEMPTS - failedAttempts,
		AuthorizationManager.MAX_FAILED_ATTEMPTS
	)
	val verifyWithCountdownText =
		stringResource(R.string.pin_verification_verify_with_countdown, remainingBackoffSeconds)
	val verifyOrWipeText = stringResource(R.string.pin_verification_verify_or_wipe)
	val wipeWarningText =
		stringResource(R.string.pin_verification_wipe_warning, AuthorizationManager.MAX_FAILED_ATTEMPTS)

	val focusRequester = remember { FocusRequester() }

	LaunchedEffect(Unit) {
		imageManager.evictKey()
		focusRequester.requestFocus()

		// Load the persisted failed attempts count
		failedAttempts = authManager.getFailedAttempts()

		// Check if there's an active backoff period
		val remainingBackoff = authManager.calculateRemainingBackoffSeconds()
		if (remainingBackoff > 0) {
			remainingBackoffSeconds = remainingBackoff
			isBackoffActive = true
			errorMessage = pinInvalidError
		}
	}

	// Countdown timer effect
	LaunchedEffect(isBackoffActive) {
		if (isBackoffActive && remainingBackoffSeconds > 0) {
			while (remainingBackoffSeconds > 0) {
				if (errorMessage?.startsWith(pinInvalidError) == true) {
					errorMessage = pinInvalidError
				}

				delay(1000)
				remainingBackoffSeconds--
			}

			if (errorMessage?.startsWith(pinInvalidError) == true) {
				errorMessage = pinInvalidError
			}

			isBackoffActive = false
		}
	}

	fun verify() {
		if (pin.isBlank()) {
			errorMessage = pinEmptyError
			return
		}

		if (isBackoffActive) {
			return
		}

		isVerifying = true
		coroutineScope.launch(Dispatchers.Default) {
			val isValid = authManager.verifyPin(pin)
			isVerifying = false

			if (isValid) {
				failedAttempts = 0
				authManager.resetFailedAttempts()

				withContext(Dispatchers.Main) {
					navController.navigate(returnRoute) {
						popUpTo(AppDestinations.PIN_VERIFICATION_ROUTE) { inclusive = true }
						launchSingleTop = true
					}
				}
			} else {
				failedAttempts = authManager.incrementFailedAttempts()

				remainingBackoffSeconds = authManager.calculateRemainingBackoffSeconds()
				isBackoffActive = remainingBackoffSeconds > 0

				if (failedAttempts < AuthorizationManager.MAX_FAILED_ATTEMPTS) {
					withContext(Dispatchers.Main) {
						vibrateDevice(context)

						errorMessage = pinInvalidError
						pin = ""
					}
				} else {
					// Nuke it all
					securityResetUseCase.reset()
					withContext(Dispatchers.Main) {
						snackbarHostState.showSnackbar(message = allDataDeletedText, duration = SnackbarDuration.Long)
						navController.navigate(AppDestinations.INTRODUCTION_ROUTE)
					}
				}
			}
		}
	}

	Box(
		modifier = modifier
			.fillMaxSize()
			.background(color = MaterialTheme.colorScheme.background)
	) {
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
				tint = MaterialTheme.colorScheme.onBackground
			)

			Text(
				text = pinVerificationTitle,
				style = MaterialTheme.typography.headlineMedium,
				modifier = Modifier.padding(bottom = 24.dp)
			)

			if (failedAttempts >= 3) {
				Text(
					text = remainingAttemptsText,
					style = MaterialTheme.typography.headlineSmall,
					color = MaterialTheme.colorScheme.error,
					modifier = Modifier.padding(bottom = 12.dp)
				)
			}

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
				enabled = !isVerifying && !isBackoffActive,
				modifier = Modifier
					.fillMaxWidth()
					.focusRequester(focusRequester)
			)

			errorMessage?.let {
				Text(
					text = it,
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodySmall,
					modifier = Modifier.padding(top = 4.dp),
					textAlign = TextAlign.Center,
				)
			}

			Spacer(modifier = Modifier.height(24.dp))

			val isLastAttempt = (failedAttempts >= (AuthorizationManager.MAX_FAILED_ATTEMPTS - 1))
			val butColors = if (isLastAttempt) {
				ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
			} else {
				ButtonDefaults.buttonColors()
			}

			Button(
				onClick = { verify() },
				enabled = !isVerifying && !isBackoffActive && (pin.length >= 4),
				modifier = Modifier.fillMaxWidth(),
				colors = butColors
			) {
				if (isBackoffActive) {
					Text(verifyWithCountdownText)
				} else {
					if (isLastAttempt) {
						Text(
							verifyOrWipeText,
							color = MaterialTheme.colorScheme.onErrorContainer
						)
					} else {
						Text(verifyButtonText)
					}
				}
			}

			if (failedAttempts >= 3) {
				Text(
					text = wipeWarningText,
					style = MaterialTheme.typography.labelLarge,
					color = MaterialTheme.colorScheme.error,
					textAlign = TextAlign.Center,
					modifier = Modifier.padding(top = 8.dp)
				)
			}

			if (isVerifying) {
				CircularProgressIndicator(
					modifier = Modifier.padding(top = 16.dp)
				)
			}
		}
	}
}
