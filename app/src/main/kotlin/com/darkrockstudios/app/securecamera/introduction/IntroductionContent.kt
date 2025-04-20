package com.darkrockstudios.app.securecamera.introduction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.auth.pinSize
import com.darkrockstudios.app.securecamera.navigation.AppDestinations
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Main content for the Introduction screen
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroductionContent(
	navController: NavHostController,
	modifier: Modifier = Modifier
) {
	val preferencesManager = koinInject<AppPreferencesManager>()
	val authorizationManager = koinInject<AuthorizationManager>()
	val coroutineScope = rememberCoroutineScope()

	val slides = listOf(
		IntroductionSlide(
			icon = Icons.Filled.Camera,
			title = stringResource(R.string.intro_slide0_title),
			description = stringResource(R.string.intro_slide0_description)
		),
		IntroductionSlide(
			icon = Icons.Filled.PrivacyTip,
			title = stringResource(R.string.intro_slide1_title),
			description = stringResource(R.string.intro_slide1_description)
		),
		IntroductionSlide(
			icon = Icons.Filled.Lock,
			title = stringResource(R.string.intro_slide2_title),
			description = stringResource(R.string.intro_slide2_description)
		),
		IntroductionSlide(
			icon = Icons.Filled.Send,
			title = stringResource(R.string.intro_slide3_title),
			description = stringResource(R.string.intro_slide3_description),
		),
		IntroductionSlide(
			icon = Icons.Filled.LocationOff,
			title = stringResource(R.string.intro_slide4_title),
			description = stringResource(R.string.intro_slide4_description),
		),
		IntroductionSlide(
			icon = Icons.Filled.MyLocation,
			title = stringResource(R.string.intro_slide5_title),
			description = stringResource(R.string.intro_slide5_description),
		),
	)

	val pagerState = rememberPagerState(pageCount = { slides.size + 1 })

	Column(
		modifier = modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		HorizontalPager(
			state = pagerState,
			modifier = Modifier
				.weight(1f)
				.fillMaxWidth()
		) { page ->
			if (page < slides.size) {
				IntroductionSlideContent(
					slide = slides[page],
					modifier = Modifier
						.fillMaxSize()
						.padding(16.dp)
				)
			} else {
				PinCreationContent(
					onPinCreated = { pin ->
						coroutineScope.launch {
							preferencesManager.setAppPin(pin)
							authorizationManager.verifyPin(pin)
							preferencesManager.setIntroCompleted(true)
							// Navigate to camera and clear the back stack
							navController.navigate(AppDestinations.CAMERA_ROUTE) {
								popUpTo(0)
							}
						}
					},
					modifier = Modifier
						.fillMaxSize()
						.padding(16.dp)
				)
			}
		}

		// Bottom navigation buttons
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			if (pagerState.currentPage < slides.size) {
				// Skip button (only on intro slides)
				TextButton(
					onClick = {
						coroutineScope.launch {
							pagerState.animateScrollToPage(slides.size) // Go to PIN creation
						}
					}
				) {
					Text(stringResource(R.string.intro_skip))
				}

				// Next button
				Button(
					onClick = {
						coroutineScope.launch {
							pagerState.animateScrollToPage(pagerState.currentPage + 1)
						}
					}
				) {
					Text(stringResource(R.string.intro_next))
				}
			}
		}
	}
}

/**
 * Content for a single introduction slide
 */
@Composable
fun IntroductionSlideContent(
	slide: IntroductionSlide,
	modifier: Modifier = Modifier
) {
	Box(modifier = modifier) {
		Column(
			modifier = Modifier
				.widthIn(max = 512.dp)
				.align(Alignment.Center),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Icon(
				modifier = Modifier
					.size(96.dp)
					.padding(16.dp),
				imageVector = slide.icon,
				contentDescription = stringResource(id = R.string.intro_slide_icon),
				tint = MaterialTheme.colorScheme.onBackground
			)

			Text(
				text = slide.title,
				style = MaterialTheme.typography.headlineMedium,
				textAlign = TextAlign.Center,
				modifier = Modifier.padding(bottom = 16.dp)
			)

			Text(
				text = slide.description,
				style = MaterialTheme.typography.bodyLarge,
				textAlign = TextAlign.Center
			)
		}
	}
}

/**
 * Content for PIN creation
 */
@Composable
fun PinCreationContent(
	onPinCreated: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	var pin by rememberSaveable { mutableStateOf("") }
	var confirmPin by rememberSaveable { mutableStateOf("") }
	var showError by rememberSaveable { mutableStateOf(false) }
	Box(modifier = modifier) {
		Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState())
				.widthIn(max = 512.dp)
				.align(Alignment.Center),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Icon(
				modifier = Modifier
					.size(96.dp)
					.padding(16.dp),
				imageVector = Icons.Filled.Pin,
				contentDescription = stringResource(id = R.string.pin_verification_icon),
				tint = MaterialTheme.colorScheme.onBackground
			)

			Text(
				text = stringResource(R.string.pin_creation_title),
				style = MaterialTheme.typography.headlineMedium,
				textAlign = TextAlign.Center,
				modifier = Modifier.padding(bottom = 8.dp)
			)

			Text(
				text = stringResource(R.string.pin_creation_description),
				style = MaterialTheme.typography.bodyLarge,
				textAlign = TextAlign.Center,
				modifier = Modifier.padding(bottom = 24.dp)
			)

			Text(
				text = stringResource(R.string.pin_creation_warning),
				style = MaterialTheme.typography.bodyMedium,
				fontStyle = FontStyle.Italic,
				color = Color.Red,
				textAlign = TextAlign.Center,
				modifier = Modifier.padding(bottom = 24.dp)
			)

			// PIN input
			OutlinedTextField(
				value = pin,
				onValueChange = {
					if (it.length <= pinSize.max() && it.all { char -> char.isDigit() }) {
						pin = it
						showError = false
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
					.padding(bottom = 16.dp)
			)

			// Confirm PIN input
			OutlinedTextField(
				value = confirmPin,
				onValueChange = {
					if (it.length <= pinSize.max() && it.all { char -> char.isDigit() }) {
						confirmPin = it
						showError = false
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
					.padding(bottom = 24.dp)
			)

			// Error message
			if (showError) {
				Text(
					text = stringResource(R.string.pin_creation_error),
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.padding(bottom = 16.dp)
				)
			}

			// Create PIN button
			Button(
				onClick = {
					if (pin == confirmPin && pin.length in pinSize) {
						onPinCreated(pin)
					} else {
						showError = true
					}
				},
				enabled = pin.length in pinSize && confirmPin.length in pinSize,
				modifier = Modifier.fillMaxWidth()
			) {
				Text(stringResource(R.string.pin_creation_button))
			}
		}
	}
}
