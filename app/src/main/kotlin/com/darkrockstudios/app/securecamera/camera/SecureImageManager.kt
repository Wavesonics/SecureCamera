package com.darkrockstudios.app.securecamera.camera

import android.content.Context
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.util.Size
import com.ashampoo.kim.Kim
import com.ashampoo.kim.model.MetadataUpdate
import com.ashampoo.kim.model.MetadataUpdate.TakenDate
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class SecureImageManager(private val appContext: Context) {
	fun getGalleryDirectory(): File = File(appContext.filesDir, DIRECTORY)

	fun saveImage(byteArray: ByteArray, quality: Int = 90): File {
		val dir = getGalleryDirectory()

		if (!dir.exists()) {
			dir.mkdirs()
		}

		val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SS", Locale.US)
		val finalImageName: String = "photo_" + dateFormat.format(Date()) + ".jpg"

		val photoFile = File(dir, finalImageName)

		FileOutputStream(photoFile).use { outputStream ->
			val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
			bitmap.compress(CompressFormat.JPEG, quality, outputStream)
		}

		println("JPEG size: ${photoFile.length()}")

		val update: MetadataUpdate = TakenDate(System.currentTimeMillis())
		photoFile.writeBytes(Kim.update(bytes = photoFile.readBytes(), update))

		val thumbnailBitmap = ThumbnailUtils.createImageThumbnail(photoFile, Size(640, 480), null)
		val thumbnailBytes = thumbnailBitmap.let { bitmap ->
			ByteArrayOutputStream().use { outputStream ->
				bitmap.compress(CompressFormat.JPEG, quality, outputStream)
				outputStream.toByteArray()
			}
		}

		photoFile.writeBytes(
			Kim.updateThumbnail(
				bytes = photoFile.readBytes(),
				thumbnailBytes = thumbnailBytes
			)
		)

		return photoFile
	}

	fun getPhotos(): List<PhotoDef> {
		val dir = getGalleryDirectory()
		if (!dir.exists()) {
			return emptyList()
		}

		return dir.listFiles()
			?.filter { it.isFile }
			?.map { file ->
				val name = file.name
				val format = name.substringAfterLast('.', "jpg")
				PhotoDef(
					photoName = name,
					photoFormat = format,
					photoFile = file
				)
			} ?: emptyList()
	}

	fun deleteImage(photoDef: PhotoDef): Boolean {
		return if (photoDef.photoFile.exists()) {
			photoDef.photoFile.delete()
		} else {
			false
		}
	}

	fun deleteImages(photos: List<PhotoDef>): Boolean {
		return photos.map { deleteImage(it) }.all { it }
	}

	fun getPhotoByName(photoName: String): PhotoDef? {
		val dir = getGalleryDirectory()
		if (!dir.exists()) {
			return null
		}

		val photoFile = File(dir, photoName)
		if (!photoFile.exists() || !photoFile.isFile) {
			return null
		}

		val format = photoName.substringAfterLast('.', "jpg")
		return PhotoDef(
			photoName = photoName,
			photoFormat = format,
			photoFile = photoFile
		)
	}

	companion object {
		const val DIRECTORY = "photos"
	}
}
