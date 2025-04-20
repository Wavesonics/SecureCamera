package com.darkrockstudios.app.securecamera.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import com.ashampoo.kim.Kim
import com.ashampoo.kim.model.GpsCoordinates
import com.ashampoo.kim.model.MetadataUpdate
import com.ashampoo.kim.model.MetadataUpdate.TakenDate
import com.ashampoo.kim.model.TiffOrientation
import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.preferences.HashedPin
import dev.whyoleg.cryptography.BinarySize
import dev.whyoleg.cryptography.BinarySize.Companion.bytes
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class KeyParams(
	val iterations: Int = 600_000,
	val outputSize: BinarySize = 32.bytes,
)

class SecureImageManager(
	private val appContext: Context,
	private val authorizationManager: AuthorizationManager,
) {

	private val provider = CryptographyProvider.Default
	private var keyFlow: ByteArray? = null
	private val keyMutex = Mutex()

	fun getGalleryDirectory(): File = File(appContext.filesDir, PHOTOS_DIR)

	fun evictKey() {
		keyFlow = null
	}

	/**
	 * Derives the encryption key from the user's PIN, then encrypted the plainText bytes and writes it to targetFile
	 */
	private suspend fun encryptToFile(plainPin: String, hashedPin: HashedPin, plain: ByteArray, targetFile: File) {
		val secrete = deriveKey(plainPin, hashedPin)

		val aesKey = provider
			.get(AES.GCM)
			.keyDecoder()
			.decodeFromByteArray(AES.Key.Format.RAW, secrete)
		val encryptedBytes = aesKey.cipher().encrypt(plain)
		targetFile.writeBytes(encryptedBytes)
	}

	/**
	 * Derives the encryption key from the user's PIN, then decrypts encryptedFile and returns the plainText bytes
	 */
	private suspend fun decryptFile(plainPin: String, hashedPin: HashedPin, encryptedFile: File): ByteArray {
		val encryptedBytes = encryptedFile.readBytes()
		val keyBytes = deriveKey(plainPin, hashedPin)

		val aesKey = provider.get(AES.GCM).keyDecoder()
			.decodeFromByteArray(AES.Key.Format.RAW, keyBytes)

		return aesKey.cipher().decrypt(encryptedBytes)
	}

	private suspend fun deriveKey(
		plainPin: String,
		hashedPin: HashedPin,
		keyParams: KeyParams = KeyParams(),
	): ByteArray {
		keyFlow?.let { return it }

		return keyMutex.withLock {
			keyFlow?.let { return@withLock it }

			val secretDerivation = provider.get(PBKDF2).secretDerivation(
				digest = SHA256,
				iterations = keyParams.iterations,
				outputSize = keyParams.outputSize,
				salt = hashedPin.salt.toByteArray()
			)

			// Double the input length
			val keyInput = plainPin + plainPin.reversed()
			val derivedKey = secretDerivation.deriveSecret(keyInput.toByteArray()).toByteArray()
			keyFlow = derivedKey
			derivedKey
		}
	}

	suspend fun saveImage(
		byteArray: ByteArray,
		quality: Int = 90,
		orientation: TiffOrientation,
		latLng: GpsCoordinates?
	): File {
		val dir = getGalleryDirectory()

		if (!dir.exists()) {
			dir.mkdirs()
		}

		val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SS", Locale.US)
		val finalImageName: String = "photo_" + dateFormat.format(Date()) + ".jpg"

		val photoFile = File(dir, finalImageName)
		val tempFile = File(dir, "$finalImageName.tmp")

		FileOutputStream(tempFile).use { outputStream ->
			val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
			bitmap.compress(CompressFormat.JPEG, quality, outputStream)
		}

		val dateUpdate: MetadataUpdate = TakenDate(System.currentTimeMillis())
		var updatedBytes = Kim.update(bytes = tempFile.readBytes(), dateUpdate)

		val orientationUpdate: MetadataUpdate = MetadataUpdate.Orientation(orientation)
		updatedBytes = Kim.update(bytes = updatedBytes, orientationUpdate)

		if (latLng != null) {
			val gpsUpdate: MetadataUpdate = MetadataUpdate.GpsCoordinates(latLng)
			updatedBytes = Kim.update(bytes = updatedBytes, gpsUpdate)
		}

		tempFile.writeBytes(updatedBytes)
		tempFile.renameTo(photoFile)

//		val thumbnailBitmap = ThumbnailUtils.createImageThumbnail(photoFile, Size(640, 480), null)
//		val thumbnailBytes = thumbnailBitmap.let { bitmap ->
//			ByteArrayOutputStream().use { outputStream ->
//				bitmap.compress(CompressFormat.JPEG, quality, outputStream)
//				outputStream.toByteArray()
//			}
//		}
//
//		photoFile.writeBytes(
//			Kim.updateThumbnail(
//				bytes = photoFile.readBytes(),
//				thumbnailBytes = thumbnailBytes
//			)
//		)

		val pin = authorizationManager.securityPin ?: throw IllegalStateException("No Security PIN")

		encryptToFile(
			plainPin = pin.plainPin,
			hashedPin = pin.hashedPin,
			plain = photoFile.readBytes(),
			targetFile = photoFile,
		)

		return photoFile
	}

	suspend fun readImage(photo: PhotoDef): Bitmap {
		val pin = authorizationManager.securityPin ?: throw IllegalStateException("No Security PIN")

		val plainBytes = decryptFile(
			plainPin = pin.plainPin,
			hashedPin = pin.hashedPin,
			encryptedFile = photo.photoFile,
		)
		return BitmapFactory.decodeByteArray(plainBytes, 0, plainBytes.size)
	}

	suspend fun decryptJpg(photo: PhotoDef): ByteArray {
		val pin = authorizationManager.securityPin ?: throw IllegalStateException("No Security PIN")

		val plainBytes = decryptFile(
			plainPin = pin.plainPin,
			hashedPin = pin.hashedPin,
			encryptedFile = photo.photoFile,
		)
		return plainBytes
	}

	private fun getThumbnailsDir(): File {
		val thumbnailsDir = File(appContext.cacheDir, THUMBNAILS_DIR)
		thumbnailsDir.mkdirs()
		return thumbnailsDir
	}

	private fun getThumbnail(photoDef: PhotoDef): File {
		val dir = getThumbnailsDir()
		return File(dir, photoDef.photoName)
	}

	suspend fun readThumbnail(photo: PhotoDef): Bitmap {
		val pin = authorizationManager.securityPin ?: throw IllegalStateException("No Security PIN")
		val thumbFile = getThumbnail(photo)

		return if (thumbFile.exists()) {
			val plainBytes = decryptFile(
				plainPin = pin.plainPin,
				hashedPin = pin.hashedPin,
				encryptedFile = photo.photoFile,
			)
			BitmapFactory.decodeByteArray(plainBytes, 0, plainBytes.size)
		} else {
			val plainBytes = decryptFile(
				plainPin = pin.plainPin,
				hashedPin = pin.hashedPin,
				encryptedFile = photo.photoFile,
			)

			val options = BitmapFactory.Options().apply {
				inSampleSize = 4
			}

			val thumbnailBitmap = BitmapFactory.decodeByteArray(plainBytes, 0, plainBytes.size, options)
			val thumbnailBytes = thumbnailBitmap.let { bitmap ->
				ByteArrayOutputStream().use { outputStream ->
					bitmap.compress(CompressFormat.JPEG, 75, outputStream)
					outputStream.toByteArray()
				}
			}
			encryptToFile(
				plainPin = pin.plainPin,
				hashedPin = pin.hashedPin,
				plain = thumbnailBytes,
				targetFile = thumbFile,
			)

			thumbnailBitmap
		}
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
			getThumbnail(photoDef).delete()
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
		const val PHOTOS_DIR = "photos"
		const val THUMBNAILS_DIR = ".thumbnails"
	}
}
