package com.darkrockstudios.app.securecamera.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Define all destinations as typed Nav3 keys
@Serializable
object Introduction : NavKey
@Serializable
object Camera : NavKey
@Serializable
object Gallery : NavKey
@Serializable
data class ViewPhoto(val photoName: String) : NavKey
@Serializable
data class ObfuscatePhoto(val photoName: String) : NavKey
@Serializable
object Settings : NavKey
@Serializable
object About : NavKey

// We keep returnRoute as String for compatibility with existing route builders
@Serializable
data class PinVerification(val returnRoute: String) : NavKey
@Serializable
data class ImportPhotos(val job: PhotoImportJob) : NavKey

// Helpers to translate between legacy string routes and typed keys
object AppRouteMapper {
	fun toKey(route: String): NavKey? = when {
		route == AppDestinations.INTRODUCTION_ROUTE -> Introduction
		route == AppDestinations.CAMERA_ROUTE -> Camera
		route == AppDestinations.GALLERY_ROUTE -> Gallery
		route.startsWith("viewphoto/") -> AppDestinations
			.decodeRouteArg(route, "viewphoto/")?.let { ViewPhoto(it) }

		route.startsWith("obfuscatephoto/") -> AppDestinations
			.decodeRouteArg(route, "obfuscatephoto/")?.let { ObfuscatePhoto(it) }

		route.startsWith("pin_verification/") -> AppDestinations
			.decodeReturnRoute(route.substringAfter("pin_verification/"))
			.let { PinVerification(it) }

		route.startsWith("import_photos/") -> AppDestinations
			.decodeImportJob(route)?.let { ImportPhotos(it) }

		else -> null
	}

	fun toRoute(key: NavKey): String = when (key) {
		Introduction -> AppDestinations.INTRODUCTION_ROUTE
		Camera -> AppDestinations.CAMERA_ROUTE
		Gallery -> AppDestinations.GALLERY_ROUTE
		is ViewPhoto -> AppDestinations.createViewPhotoRoute(key.photoName)
		is ObfuscatePhoto -> AppDestinations.createObfuscatePhotoRoute(key.photoName)
		Settings -> AppDestinations.SETTINGS_ROUTE
		About -> AppDestinations.ABOUT_ROUTE
		is PinVerification -> AppDestinations.createPinVerificationRoute(key.returnRoute)
		is ImportPhotos -> AppDestinations.createImportPhotosRoute(key.job.photos)
		else -> error("Unknown NavKey type: ${key::class}")
	}
}