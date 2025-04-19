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
	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { granted ->
		if (granted) onGranted()
	}

	LaunchedEffect(Unit) {
		val perm = Manifest.permission.ACCESS_FINE_LOCATION
		if (ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED)
			onGranted()
		else
			launcher.launch(perm)
	}
}