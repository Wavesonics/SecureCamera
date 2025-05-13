package com.darkrockstudios.app.securecamera.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.darkrockstudios.app.securecamera.R

@Composable
fun NotificationPermissionRationale(
	@StringRes title: Int,
	@StringRes text: Int,
) {
	val context = LocalContext.current

	val showNotificationPermissionDialog = remember { mutableStateOf(false) }

	val notificationPermissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { _ ->
		// Noop
	}

	// Check if we need to request notification permission (API 33+)
	LaunchedEffect(Unit) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
			if (ContextCompat.checkSelfPermission(
					context,
					notificationPermission
				) != PackageManager.PERMISSION_GRANTED
			) {
				showNotificationPermissionDialog.value = true
			}
		}
	}

	// Notification permission dialog (API 33+)
	if (showNotificationPermissionDialog.value) {
		AlertDialog(
			onDismissRequest = { showNotificationPermissionDialog.value = false },
			title = { Text(stringResource(id = title)) },
			text = { Text(stringResource(id = text)) },
			confirmButton = {
				TextButton(
					onClick = {
						showNotificationPermissionDialog.value = false
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
							notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
						}
					}
				) {
					Text(stringResource(id = R.string.notification_permission_button))
				}
			},
			dismissButton = {
				TextButton(
					onClick = { showNotificationPermissionDialog.value = false }
				) {
					Text(stringResource(id = R.string.cancel_button))
				}
			}
		)
	}
}
