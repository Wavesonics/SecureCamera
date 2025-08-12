package com.darkrockstudios.app.securecamera.navigation

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private fun getJson(): Json {
	val module = SerializersModule {
		contextual(Uri::class, UriSerializer)
	}
	return Json { serializersModule = module }
}

private val navJson = getJson()

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
	const val IMPORT_PHOTOS_ROUTE = "import_photos/{photoUris}"

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

	@OptIn(ExperimentalEncodingApi::class)
	fun createImportPhotosRoute(photos: List<Uri>): String {
		val job = PhotoImportJob(photos)
		val json = navJson.encodeToString<PhotoImportJob>(job)
		val b64 = Base64.UrlSafe.encode(json.toByteArray())
		return "import_photos/$b64"
	}

	@OptIn(ExperimentalEncodingApi::class)
	fun encodeReturnRoute(route: String): String = Base64.UrlSafe.encode(route.toByteArray())

	@OptIn(ExperimentalEncodingApi::class)
	fun decodeReturnRoute(encodedRoute: String): String = String(Base64.UrlSafe.decode(encodedRoute))

	fun decodeRouteArg(route: String, prefix: String): String? =
		route.removePrefix(prefix).takeIf { it.isNotBlank() }

	@OptIn(ExperimentalEncodingApi::class)
	fun decodeImportJob(route: String): PhotoImportJob? {
		val b64 = route.substringAfter("import_photos/", missingDelimiterValue = "")
		if (b64.isBlank()) return null
		return try {
			val jsonStr = String(Base64.UrlSafe.decode(b64))
			navJson.decodeFromString<PhotoImportJob>(jsonStr)
		} catch (_: Throwable) {
			null
		}
	}
}

@Serializable
@Parcelize
data class PhotoImportJob(
	val photos: List<@Contextual Uri>
) : Parcelable

private object UriSerializer : KSerializer<Uri> {
	override fun serialize(encoder: Encoder, value: Uri) {
		encoder.encodeString(value.toString())
	}

	override fun deserialize(decoder: Decoder): Uri {
		return decoder.decodeString().toUri()
	}

	override val descriptor: SerialDescriptor
		get() = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)
}
