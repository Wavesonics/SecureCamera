package com.darkrockstudios.app.securecamera.camera

import timber.log.Timber
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

data class PhotoDef(
	val photoName: String,
	val photoFormat: String,
	val photoFile: File,
) {
	fun dateTaken(): Date {
		try {
			val dateString = photoName.removePrefix("photo_").removeSuffix(".jpg")
			val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SS", Locale.US)
			return dateFormat.parse(dateString) ?: Date()
		} catch (e: ParseException) {
			Timber.w(e, "Failed to parse photo name to date")
			return Date()
		}
	}
}
