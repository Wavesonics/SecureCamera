package com.darkrockstudios.app.securecamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class LocationPermissionStatus {
	DENIED,
	COARSE,
	FINE
}

class LocationRepository(private val ctx: Context) {
	private val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
	private var lastKnownLocation: Location? = null

	fun getLocationPermissionStatus(): LocationPermissionStatus {
		val finePermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
		val coarsePermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)

		return when {
			finePermission == PackageManager.PERMISSION_GRANTED -> LocationPermissionStatus.FINE
			coarsePermission == PackageManager.PERMISSION_GRANTED -> LocationPermissionStatus.COARSE
			else -> LocationPermissionStatus.DENIED
		}
	}

	suspend fun currentLocation(): Location? {
		// Check if we have permission
		if (ActivityCompat.checkSelfPermission(
				ctx,
				Manifest.permission.ACCESS_FINE_LOCATION
			) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
				ctx,
				Manifest.permission.ACCESS_COARSE_LOCATION
			) != PackageManager.PERMISSION_GRANTED
		) {
			return null
		}

		// Try to get last known location first
		val providers = locationManager.getProviders(true)
		for (provider in providers) {
			val location = locationManager.getLastKnownLocation(provider)
			if (location != null) {
				lastKnownLocation = location
				break
			}
		}

		// If we have a recent location, return it
		lastKnownLocation?.let {
			val timeDiff = System.currentTimeMillis() - it.time
			if (timeDiff < 5 * 60 * 1000) { // Less than 5 minutes old
				return it
			}
		}

		// Otherwise, request a fresh location
		return suspendCancellableCoroutine { continuation ->
			val locationListener = object : LocationListener {
				override fun onLocationChanged(location: Location) {
					lastKnownLocation = location
					locationManager.removeUpdates(this)
					continuation.resume(location)
				}

				override fun onProviderDisabled(provider: String) {}
				override fun onProviderEnabled(provider: String) {}
			}

			// Try to request location updates
			try {
				// Try GPS first
				if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					locationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER,
						0L,
						0f,
						locationListener
					)
				} else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					// Fall back to network provider
					locationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER,
						0L,
						0f,
						locationListener
					)
				} else {
					// No provider available, return last known location or null
					continuation.resume(lastKnownLocation)
				}

				// Cancel the location updates if the coroutine is cancelled
				continuation.invokeOnCancellation {
					locationManager.removeUpdates(locationListener)
				}
			} catch (e: Exception) {
				continuation.resume(lastKnownLocation)
			}
		}
	}
}
