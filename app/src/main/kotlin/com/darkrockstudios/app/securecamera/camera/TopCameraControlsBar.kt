package com.darkrockstudios.app.securecamera.camera

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.darkrockstudios.app.securecamera.Flashlight
import com.darkrockstudios.app.securecamera.FlashlightOff
import com.darkrockstudios.app.securecamera.R

@Composable
fun TopCameraControlsBar(
	isFlashOn: Boolean,
	isVisible: Boolean,
	onFlashToggle: (Boolean) -> Unit,
	onLensToggle: () -> Unit,
	onClose: () -> Unit,
	paddingValues: PaddingValues? = null
) {
	AnimatedVisibility(
		visible = isVisible,
		enter = slideInHorizontally(
			initialOffsetX = { fullWidth -> fullWidth },
			animationSpec = tween(durationMillis = 300)
		) + fadeIn(animationSpec = tween(durationMillis = 300)),
		exit = slideOutHorizontally(
			targetOffsetX = { fullWidth -> fullWidth },
			animationSpec = tween(durationMillis = 300)
		) + fadeOut(animationSpec = tween(durationMillis = 300))
	) {
		Surface(
			modifier = Modifier.Companion
				.fillMaxWidth()
				.padding(
					start = 16.dp,
					end = 16.dp,
					top = paddingValues?.calculateTopPadding()?.plus(16.dp) ?: 16.dp,
					bottom = 16.dp
				),
			color = Color.Companion.Black.copy(alpha = 0.6f),
			shape = RoundedCornerShape(16.dp)
		) {
			Row(
				modifier = Modifier.Companion
					.fillMaxWidth()
					.padding(16.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.Companion.CenterVertically
			) {
				Column(
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					CameraControlSwitch(
						icon = if (isFlashOn) Flashlight else FlashlightOff,
						checked = isFlashOn,
						onCheckedChange = onFlashToggle
					)
				}

				Row(
					horizontalArrangement = Arrangement.spacedBy(16.dp),
					verticalAlignment = Alignment.Companion.CenterVertically
				) {
					FilledTonalButton(
						onClick = onLensToggle,
						modifier = Modifier.Companion
							.background(MaterialTheme.colorScheme.primary, CircleShape),
						colors = ButtonDefaults.filledTonalButtonColors(
							containerColor = MaterialTheme.colorScheme.primary
						)
					) {
						Icon(
							imageVector = Icons.Filled.Cameraswitch,
							contentDescription = stringResource(id = R.string.camera_toggle_content_description),
							tint = MaterialTheme.colorScheme.onPrimary,
						)
					}

					FilledTonalButton(
						onClick = onClose,
						modifier = Modifier.Companion
							.background(MaterialTheme.colorScheme.primary, CircleShape),
						colors = ButtonDefaults.filledTonalButtonColors(
							containerColor = MaterialTheme.colorScheme.primary
						)
					) {
						Icon(
							imageVector = Icons.Filled.Close,
							contentDescription = stringResource(id = R.string.camera_close_controls_content_description),
							tint = MaterialTheme.colorScheme.onPrimary,
						)
					}
				}
			}
		}
	}
}

@Composable
private fun CameraControlSwitch(
	icon: ImageVector,
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit
) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier.padding(horizontal = 8.dp)
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = Color.White,
			modifier = Modifier.size(24.dp)
		)
		Spacer(modifier = Modifier.width(8.dp))
		Text(
			text = stringResource(id = R.string.camera_flash_text),
			color = Color.White,
			style = MaterialTheme.typography.bodyMedium
		)
		Spacer(modifier = Modifier.width(8.dp))
		Switch(
			checked = checked,
			onCheckedChange = onCheckedChange,
			colors = SwitchDefaults.colors(
				checkedThumbColor = MaterialTheme.colorScheme.primary,
				checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
			)
		)
	}
}
