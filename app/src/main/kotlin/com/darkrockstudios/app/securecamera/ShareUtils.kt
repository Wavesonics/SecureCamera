package com.darkrockstudios.app.securecamera

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.darkrockstudios.app.securecamera.camera.PhotoDef
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import java.io.File
import java.io.FileOutputStream
import java.util.*

private val SHAR_DIR = "share"

/**
 * Clears all photos in the share directory
 */
fun clearShareDirectory(context: Context): Boolean {
	val shareDir = File(context.cacheDir, SHAR_DIR)
	if (!shareDir.exists()) {
		return true
	}

	val files = shareDir.listFiles() ?: return true
	return if (files.isEmpty()) true else files.all { it.delete() }
}

/**
 * Creates a temporary file from a ByteArray for sharing purposes
 */
private fun createTempFileFromByteArray(
	imageData: ByteArray,
	photoDef: PhotoDef,
	sanitizeName: Boolean,
	context: Context
): File {
	val shareDir = File(context.cacheDir, SHAR_DIR)
	shareDir.mkdirs()

	val fileName = if (sanitizeName) "image_${UUID.randomUUID()}.jpg" else photoDef.photoName
	val tempFile = File(shareDir, fileName)
	FileOutputStream(tempFile).use { outputStream ->
		outputStream.write(imageData)
	}
	return tempFile
}

/**
 * Share a photo using its decrypted ByteArray data
 */
suspend fun sharePhotoData(
	photo: PhotoDef,
	sanitizeName: Boolean,
	imageManager: SecureImageManager,
	context: Context
): Boolean {
	val imageData = imageManager.decryptJpg(photo)
	val tempFile = createTempFileFromByteArray(imageData, photo, sanitizeName, context)

	val uri = FileProvider.getUriForFile(
		context,
		context.packageName + ".fileprovider",
		tempFile
	)

	val shareIntent = Intent().apply {
		action = Intent.ACTION_SEND
		putExtra(Intent.EXTRA_STREAM, uri)
		type = "image/jpeg"
		addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
	}

	context.startActivity(Intent.createChooser(shareIntent, "Share Photo"))
	return true
}

/**
 * Share multiple photos using their decrypted ByteArray data
 */
suspend fun sharePhotosData(
	photos: List<PhotoDef>,
	sanitizeName: Boolean,
	imageManager: SecureImageManager,
	context: Context
): Boolean {
	if (photos.isEmpty()) {
		return false
	}

	val imagesData = photos.map { photo ->
		Pair(photo, imageManager.decryptJpg(photo))
	}

	val tempFiles = imagesData.map { (photo, imageData) ->
		createTempFileFromByteArray(imageData, photo, sanitizeName, context)
	}

	val uris = tempFiles.map { file ->
		FileProvider.getUriForFile(
			context,
			context.packageName + ".fileprovider",
			file
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
