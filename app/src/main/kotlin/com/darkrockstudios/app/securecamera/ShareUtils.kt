package com.darkrockstudios.app.securecamera

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.darkrockstudios.app.securecamera.camera.PhotoDef

fun sharePhoto(photo: PhotoDef, context: Context): Boolean {
	return if (photo.photoFile.exists()) {
		val uri = FileProvider.getUriForFile(
			context,
			context.packageName + ".fileprovider",
			photo.photoFile
		)

		val shareIntent = Intent().apply {
			action = Intent.ACTION_SEND
			putExtra(Intent.EXTRA_STREAM, uri)
			type = "image/jpeg"
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}

		context.startActivity(Intent.createChooser(shareIntent, "Share Photo"))
		true
	} else {
		false
	}
}

fun sharePhotos(photos: List<PhotoDef>, context: Context): Boolean {
	val existingPhotos = photos.filter { it.photoFile.exists() }

	if (existingPhotos.isEmpty()) {
		return false
	}

	val uris = existingPhotos.map { photo ->
		FileProvider.getUriForFile(
			context,
			context.packageName + ".fileprovider",
			photo.photoFile
		)
	}

	val shareIntent = if (uris.size == 1) {
		// Single photo share
		Intent().apply {
			action = Intent.ACTION_SEND
			putExtra(Intent.EXTRA_STREAM, uris.first())
			type = "image/jpeg"
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}
	} else {
		// Multiple photo share
		Intent().apply {
			action = Intent.ACTION_SEND_MULTIPLE
			putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
			type = "image/jpeg"
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}
	}

	context.startActivity(Intent.createChooser(shareIntent, "Share Photos"))
	return true
}
