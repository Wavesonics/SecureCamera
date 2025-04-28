package com.darkrockstudios.app.securecamera.navigation

import androidx.navigation.NavDestination
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Navigation destinations for the app
 */
object AppDestinations {
	const val INTRODUCTION_ROUTE = "introduction"
	const val CAMERA_ROUTE = "camera"
	const val GALLERY_ROUTE = "gallery"
	const val VIEW_PHOTO_ROUTE = "viewphoto/{photoName}"
	const val OBFUSCATE_PHOTO_ROUTE = "obfuscatephoto/{photoName}"
	const val SETTINGS_ROUTE = "settings"
	const val ABOUT_ROUTE = "about"
	const val PIN_VERIFICATION_ROUTE = "pin_verification/{returnRoute}"

	fun createViewPhotoRoute(photoName: String): String {
		return "viewphoto/$photoName"
	}

	fun createObfuscatePhotoRoute(photoName: String): String {
		return "obfuscatephoto/$photoName"
	}

	fun createPinVerificationRoute(returnRoute: String): String {
		val encoded = encodeReturnRoute(returnRoute)
		return "pin_verification/$encoded"
	}

	fun isPinVerificationRoute(destination: NavDestination?): Boolean {
		if (destination == null) return false
		return destination.route?.startsWith("pin_verification/") ?: false
	}

	@OptIn(ExperimentalEncodingApi::class)
	fun encodeReturnRoute(route: String): String = Base64.UrlSafe.encode(route.toByteArray())

	@OptIn(ExperimentalEncodingApi::class)
	fun decodeReturnRoute(encodedRoute: String): String = String(Base64.UrlSafe.decode(encodedRoute))
}
