package com.darkrockstudios.app.securecamera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun RequestLocationPermission(onGranted: () -> Unit) {
	val ctx = LocalContext.current

	val fineLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { granted ->
		if (granted) {
			onGranted()
		} else {
			// If FINE location was denied, check if COARSE location is granted
			val coarsePerm = Manifest.permission.ACCESS_COARSE_LOCATION
			if (ContextCompat.checkSelfPermission(ctx, coarsePerm) == PackageManager.PERMISSION_GRANTED) {
				onGranted()
			}
			// We don't request COARSE if FINE was denied - as per requirements
		}
	}

	LaunchedEffect(Unit) {
		val finePerm = Manifest.permission.ACCESS_FINE_LOCATION
		val coarsePerm = Manifest.permission.ACCESS_COARSE_LOCATION

		// First check if FINE location is granted
		if (ContextCompat.checkSelfPermission(ctx, finePerm) == PackageManager.PERMISSION_GRANTED) {
			onGranted()
		}
		// Then check if COARSE location is granted
		else if (ContextCompat.checkSelfPermission(ctx, coarsePerm) == PackageManager.PERMISSION_GRANTED) {
			onGranted()
		}
		// If neither is granted, request FINE location
		else {
			fineLauncher.launch(finePerm)
		}
	}
}
