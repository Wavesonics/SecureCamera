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