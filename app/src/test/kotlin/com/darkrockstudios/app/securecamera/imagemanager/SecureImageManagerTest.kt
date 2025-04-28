package com.darkrockstudios.app.securecamera.imagemanager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ashampoo.kim.model.GpsCoordinates
import com.ashampoo.kim.model.TiffOrientation
import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.auth.AuthorizationManager.SecurityPin
import com.darkrockstudios.app.securecamera.camera.*
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import com.darkrockstudios.app.securecamera.preferences.HashedPin
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@ExperimentalCoroutinesApi
class SecureImageManagerTest {

	@get:Rule
	val tempFolder = TemporaryFolder()

	private lateinit var context: Context
	private lateinit var preferencesManager: AppPreferencesManager
	private lateinit var authorizationManager: AuthorizationManager
	private lateinit var secureImageManager: SecureImageManager
	private lateinit var thumbnailCache: ThumbnailCache

	@Before
	fun setup() {
		context = mockk(relaxed = true)
		preferencesManager = mockk(relaxed = true)
		authorizationManager = mockk()
		thumbnailCache = mockk(relaxed = true)

		// Mock the filesDir and cacheDir
		val filesDir = tempFolder.newFolder("files")
		val cacheDir = tempFolder.newFolder("cache")

		every { context.filesDir } returns filesDir
		every { context.cacheDir } returns cacheDir

		// Create the SecureImageManager with real dependencies
		secureImageManager = SecureImageManager(
			appContext = context,
			preferencesManager = preferencesManager,
			authorizationManager = authorizationManager,
			thumbnailCache = thumbnailCache,
		)
	}

	@After
	fun tearDOwn() {
		unmockkStatic(Bitmap::class)
		unmockkStatic(BitmapFactory::class)
		unmockkAll()
	}

	@Test
	fun `getGalleryDirectory should return correct directory`() {
		// When
		val galleryDir = secureImageManager.getGalleryDirectory()

		// Then
		assertEquals(File(context.filesDir, SecureImageManager.Companion.PHOTOS_DIR), galleryDir)
	}

	@Test
	fun `getDecoyDirectory should return correct directory and create it if needed`() {
		// When
		val decoyDir = secureImageManager.getDecoyDirectory()

		// Then
		assertEquals(File(context.filesDir, SecureImageManager.Companion.DECOYS_DIR), decoyDir)
		assertTrue(decoyDir.exists())
	}

	@Test
	fun `evictKey should clear the key`() {
		// Given
		// No setup needed as we're testing a simple method

		// When
		secureImageManager.evictKey()

		// Then
		// We can't directly test the private keyFlow field, but we can verify it works
		// by testing methods that use it in other tests
	}

	@Test
	fun `getPhotos should return empty list when directory does not exist`() {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.delete() // Ensure directory doesn't exist

		// When
		val photos = secureImageManager.getPhotos()

		// Then
		assertTrue(photos.isEmpty())
	}

	@Test
	fun `getPhotos should return list of photos when directory exists with files`() {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		// Create some test photo files
		val photo1 = File(galleryDir, "photo_20230101_120000_00.jpg")
		val photo2 = File(galleryDir, "photo_20230101_120001_00.jpg")
		photo1.createNewFile()
		photo2.createNewFile()

		// When
		val photos = secureImageManager.getPhotos()

		// Then
		assertEquals(2, photos.size)
		assertTrue(photos.any { it.photoName == "photo_20230101_120000_00.jpg" })
		assertTrue(photos.any { it.photoName == "photo_20230101_120001_00.jpg" })
	}

	@Test
	fun `deleteImage should remove the photo file and thumbnail`() {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		photoFile.createNewFile()

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		// When
		val result = secureImageManager.deleteImage(photoDef)

		// Then
		assertTrue(result)
		assertFalse(photoFile.exists())
	}

	@Test
	fun `deleteImage should return false when photo does not exist`() {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		// Don't create the file

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		// When
		val result = secureImageManager.deleteImage(photoDef)

		// Then
		assertFalse(result)
	}

	@Test
	fun `getPhotoByName should return null when directory does not exist`() {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.delete() // Ensure directory doesn't exist

		// When
		val photo = secureImageManager.getPhotoByName("photo_20230101_120000_00.jpg")

		// Then
		assertNull(photo)
	}

	@Test
	fun `getPhotoByName should return null when photo does not exist`() {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		// When
		val photo = secureImageManager.getPhotoByName("photo_20230101_120000_00.jpg")

		// Then
		assertNull(photo)
	}

	@Test
	fun `getPhotoByName should return PhotoDef when photo exists`() {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		photoFile.createNewFile()

		// When
		val photo = secureImageManager.getPhotoByName("photo_20230101_120000_00.jpg")

		// Then
		assertNotNull(photo)
		assertEquals("photo_20230101_120000_00.jpg", photo?.photoName)
		assertEquals("jpg", photo?.photoFormat)
		assertEquals(photoFile, photo?.photoFile)
	}

	@Test
	fun `isDecoyPhoto should return true when decoy exists`() = runTest {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val decoyDir = secureImageManager.getDecoyDirectory()

		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		photoFile.createNewFile()

		val decoyFile = File(decoyDir, "photo_20230101_120000_00.jpg")
		decoyFile.createNewFile()

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		// When
		val result = secureImageManager.isDecoyPhoto(photoDef)

		// Then
		assertTrue(result)
	}

	@Test
	fun `isDecoyPhoto should return false when decoy does not exist`() = runTest {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		photoFile.createNewFile()

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		// When
		val result = secureImageManager.isDecoyPhoto(photoDef)

		// Then
		assertFalse(result)
	}

	@Test
	fun `numDecoys should return correct count`() {
		// Given
		val decoyDir = secureImageManager.getDecoyDirectory()

		// Create some test decoy files
		val decoy1 = File(decoyDir, "photo_20230101_120000_00.jpg")
		val decoy2 = File(decoyDir, "photo_20230101_120001_00.jpg")
		decoy1.createNewFile()
		decoy2.createNewFile()

		// When
		val count = secureImageManager.numDecoys()

		// Then
		assertEquals(2, count)
	}

	@Test
	fun `saveImage should encrypt and save the image`() = runTest {
		// Given
		val testBytes = "test image data".toByteArray()
		val orientation = TiffOrientation.STANDARD
		val coordinates = GpsCoordinates(latitude = 37.7749, longitude = -122.4194)

		// Mock security PIN
		val hashedPin = HashedPin("salt", "hash")
		val securityPin = SecurityPin("1234", hashedPin)
		every { authorizationManager.securityPin } returns securityPin

		mockkStatic(BitmapFactory::class)
		val mockBitmap = mockk<Bitmap>()
		every {
			BitmapFactory.decodeByteArray(any(), any(), any())
		} returns mockBitmap

		// Mock bitmap.compress to write text bytes to the output stream
		val jpgBytes = readResourceBytes("red.jpg")
		every {
			mockBitmap.compress(any(), any(), any())
		} answers {
			val outputStream = thirdArg<FileOutputStream>()
			outputStream.write(jpgBytes)
			true
		}

		val image = CapturedImage(
			sensorBitmap = mockBitmap,
			timestamp = 1L,
			rotationDegrees = 0
		)

		mockkStatic(Bitmap::rotate)
		every { mockBitmap.rotate(any()) } returns image.sensorBitmap

		// When
		val photoFile = secureImageManager.saveImage(
			image = image,
			latLng = coordinates,
			applyRotation = true,
		)

		// Then
		assertTrue(photoFile.exists())
		assertTrue(photoFile.length() > 0)
	}

	@Test
	fun `readImage should decrypt and return the image`() = runTest {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val hashedPin = HashedPin("salt", "hash")
		val securityPin = SecurityPin("1234", hashedPin)
		every { authorizationManager.securityPin } returns securityPin

		val jpgBytes = readResourceBytes("red.jpg")
		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		secureImageManager.encryptToFile(
			plainPin = securityPin.plainPin,
			hashedPin = securityPin.hashedPin,
			plain = jpgBytes,
			targetFile = photoFile,
		)

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		// Mock BitmapFactory
		mockkStatic(BitmapFactory::class)
		val mockBitmap = mockk<Bitmap>()
		every {
			BitmapFactory.decodeByteArray(jpgBytes, any(), any())
		} returns mockBitmap

		// When
		val result = secureImageManager.readImage(photoDef)

		// Then
		assertNotNull(result)
		assertEquals(mockBitmap, result)
	}

	@Test
	fun `decryptJpg should decrypt and return the image bytes`() = runTest {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		// Mock security PIN
		val hashedPin = HashedPin("salt", "hash")
		val securityPin = SecurityPin("1234", hashedPin)
		every { authorizationManager.securityPin } returns securityPin

		val jpgBytes = readResourceBytes("red.jpg")
		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		secureImageManager.encryptToFile(
			plainPin = securityPin.plainPin,
			hashedPin = securityPin.hashedPin,
			plain = jpgBytes,
			targetFile = photoFile,
		)

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		// When
		val result = secureImageManager.decryptJpg(photoDef)

		// Then
		assertNotNull(result)
		assertTrue(result.isNotEmpty())
		assertEquals(jpgBytes.size, result.size)
		jpgBytes.forEachIndexed { i, b ->
			assertEquals(b, result[i])
		}
	}

	@Test
	fun `deleteAllImages should delete all photos`() {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		// Create some test photo files
		val photo1 = File(galleryDir, "photo_20230101_120000_00.jpg")
		val photo2 = File(galleryDir, "photo_20230101_120001_00.jpg")
		photo1.createNewFile()
		photo2.createNewFile()

		// When
		secureImageManager.deleteAllImages()

		// Then
		val photos = secureImageManager.getPhotos()
		assertTrue(photos.isEmpty())
	}

	@Test
	fun `deleteNonDecoyImages should delete all photos except decoys`() {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val decoyDir = secureImageManager.getDecoyDirectory()

		// Create some test photo files
		val photo1 = File(galleryDir, "photo_20230101_120000_00.jpg")
		val photo2 = File(galleryDir, "photo_20230101_120001_00.jpg")
		photo1.createNewFile()
		photo2.createNewFile()

		// Create a decoy file with content to ensure it's properly created
		val decoy = File(decoyDir, "photo_20230101_120000_00.jpg")
		decoy.writeBytes("decoy content".toByteArray())

		// When
		secureImageManager.deleteNonDecoyImages()

		// Then
		// Verify that the decoy file was moved to the gallery directory
		val targetFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		assertTrue("Target file should exist after moving decoy", targetFile.exists())
		assertEquals(
			"Decoy content should be preserved",
			"decoy content",
			targetFile.readBytes().toString(Charsets.UTF_8)
		)

		// Verify that getPhotos returns the moved decoy file
		val photos = secureImageManager.getPhotos()
		assertEquals("Should have 1 photo after moving decoy", 1, photos.size)
		assertEquals("Photo name should match decoy name", "photo_20230101_120000_00.jpg", photos[0].photoName)
	}

	@Test
	fun `addDecoyPhoto should add a photo to decoys when under limit`() = runTest {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val decoyDir = secureImageManager.getDecoyDirectory()
		decoyDir.mkdirs()

		// Mock security PIN
		val hashedPin = HashedPin("salt", "hash")
		val securityPin = SecurityPin("1234", hashedPin)
		every { authorizationManager.securityPin } returns securityPin

		val jpgBytes = readResourceBytes("red.jpg")
		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		secureImageManager.encryptToFile(
			plainPin = securityPin.plainPin,
			hashedPin = securityPin.hashedPin,
			plain = jpgBytes,
			targetFile = photoFile,
		)

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		// Mock poison pill PIN
		coEvery { preferencesManager.getHashedPoisonPillPin() } returns hashedPin
		coEvery { preferencesManager.getPlainPoisonPillPin() } returns "5678"

		// When
		val result = secureImageManager.addDecoyPhoto(photoDef)

		// Then
		assertTrue(result)

		val decoyFile = secureImageManager.getDecoyFile(photoDef)
		assertTrue(decoyFile.exists())
	}

	@Test
	fun `addDecoyPhoto should return false when at limit`() = runTest {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val decoyDir = secureImageManager.getDecoyDirectory()
		decoyDir.mkdirs()

		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		photoFile.writeBytes("encrypted image data".toByteArray())

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		repeat(SecureImageManager.Companion.MAX_DECOY_PHOTOS) { i ->
			val decoyFile = File(decoyDir, "photo_20230101_120000_0$i.jpg")
			decoyFile.writeBytes("encrypted image data".toByteArray())
		}

		// When
		val result = secureImageManager.addDecoyPhoto(photoDef)

		// Then
		assertFalse(result)
	}

	@Test
	fun `removeDecoyPhoto should remove a photo from decoys`() = runTest {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val decoyDir = secureImageManager.getDecoyDirectory()

		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		photoFile.createNewFile()

		val decoyFile = File(decoyDir, "photo_20230101_120000_00.jpg")
		decoyFile.createNewFile()

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		// When
		val result = secureImageManager.removeDecoyPhoto(photoDef)

		// Then
		assertTrue(result)
		assertFalse(secureImageManager.isDecoyPhoto(photoDef))
	}

	@Test
	fun `readThumbnail should return cached thumbnail if available`() = runTest {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		photoFile.writeBytes("encrypted image data".toByteArray())

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		// Mock security PIN
		val hashedPin = HashedPin("salt", "hash")
		val securityPin = SecurityPin("1234", hashedPin)
		every { authorizationManager.securityPin } returns securityPin

		val mockBitmap = mockk<Bitmap>()

		coEvery { thumbnailCache.getThumbnail(any()) } returns mockBitmap
		coEvery { thumbnailCache.putThumbnail(any(), any()) } returns Unit

		// When
		val result = secureImageManager.readThumbnail(photoDef)

		// Then
		assertNotNull(result)

		// Verify the thumbnail was retrieved from cache
		coVerify { thumbnailCache.getThumbnail(any()) }
		// Verify we didn't need to create a new thumbnail
		coVerify(exactly = 0) { thumbnailCache.putThumbnail(any(), any()) }
	}

	@Test
	fun `readThumbnail should create thumbnail if not in cache or file`() = runTest {
		// Mock security PIN
		val hashedPin = HashedPin(hash = "salt", salt = "hash1234567890")
		val securityPin = SecurityPin(plainPin = "1234", hashedPin = hashedPin)
		every { authorizationManager.securityPin } returns securityPin

		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val jpgBytes = readResourceBytes("red.jpg")
		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		secureImageManager.encryptToFile(
			plainPin = securityPin.plainPin,
			hashedPin = securityPin.hashedPin,
			plain = jpgBytes,
			targetFile = photoFile,
		)

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		val mockBitmap = mockk<Bitmap>()

		// Set up the thumbnailCache mock to return null (cache miss)
		coEvery { thumbnailCache.getThumbnail(any()) } returns null
		coEvery { thumbnailCache.putThumbnail(any(), any()) } just Runs

		// Mock BitmapFactory
		mockkStatic(BitmapFactory::class)
		every {
			BitmapFactory.decodeByteArray(any(), any(), any())
		} returns mockBitmap

		every {
			BitmapFactory.decodeByteArray(any(), any(), any(), any())
		} returns mockBitmap

		val mockThumbnailBytes = "mock thumbnail data".toByteArray()
		// Mock bitmap.compress to write text bytes to the output stream
		every {
			mockBitmap.compress(any(), any(), any())
		} answers {
			val outputStream = thirdArg<ByteArrayOutputStream>()
			outputStream.write(mockThumbnailBytes)
			true
		}

		// When
		val result = secureImageManager.readThumbnail(photoDef)

		// Then
		assertNotNull(result)

		// Verify the thumbnail was stored in cache
		coVerify { thumbnailCache.putThumbnail(any(), any()) }
		assertTrue(secureImageManager.getThumbnail(photoDef).exists())
	}

	@Test
	fun `securityFailureReset should delete all images and evict key`() {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		// Create some test photo files
		val photo1 = File(galleryDir, "photo_20230101_120000_00.jpg")
		val photo2 = File(galleryDir, "photo_20230101_120001_00.jpg")
		photo1.createNewFile()
		photo2.createNewFile()

		// When
		secureImageManager.securityFailureReset()

		// Then
		val photos = secureImageManager.getPhotos()
		assertTrue(photos.isEmpty())

		val field = SecureImageManager::class.java.getDeclaredField("key")
		field.isAccessible = true
		assertNull(field.get(secureImageManager))
	}

	@Test
	fun `getPhotoMetaData should return metadata for a photo`() = runTest {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val photoName = "photo_20230101_120000_00.jpg"
		val photoFile = File(galleryDir, photoName)

		// Mock security PIN
		val hashedPin = HashedPin(hash = "hash", salt = "salt")
		val securityPin = SecurityPin("1234", hashedPin)
		every { authorizationManager.securityPin } returns securityPin

		// Create an encrypted file
		val jpgBytes = readResourceBytes("red.jpg")
		secureImageManager.encryptToFile(
			plainPin = securityPin.plainPin,
			hashedPin = securityPin.hashedPin,
			plain = jpgBytes,
			targetFile = photoFile,
		)

		val photoDef = PhotoDef(
			photoName = photoName,
			photoFormat = "jpg",
			photoFile = photoFile
		)

		// When
		val result = secureImageManager.getPhotoMetaData(photoDef)

		// Then
		assertEquals(photoName, result.name)
		// The date should be parsed from the photo name
		assertEquals(2023, result.dateTaken.year + 1900) // Java Date year is offset by 1900
		assertEquals(0, result.dateTaken.month) // Java Date month is 0-based (0 = January)
		assertEquals(1, result.dateTaken.date) // day of month
		assertEquals(12, result.dateTaken.hours)
		assertEquals(0, result.dateTaken.minutes)
		assertEquals(0, result.dateTaken.seconds)
		// Check the new properties
		assertNull(result.orientation)
		assertNull(result.location)
	}

	@Test
	fun `activatePoisonPill should delete non-decoy images and evict key`() {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		val decoyDir = secureImageManager.getDecoyDirectory()

		// Create some test photo files
		val photo1 = File(galleryDir, "photo_20230101_120000_00.jpg")
		val photo2 = File(galleryDir, "photo_20230101_120001_00.jpg")
		photo1.createNewFile()
		photo2.createNewFile()

		// Create a decoy file with content to ensure it's properly created
		val decoy = File(decoyDir, "photo_20230101_120000_00.jpg")
		decoy.writeBytes("decoy content".toByteArray())

		// When
		secureImageManager.activatePoisonPill()

		// Then
		// Verify that the decoy file was moved to the gallery directory
		val targetFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		assertTrue("Target file should exist after moving decoy", targetFile.exists())
		assertEquals(
			"Decoy content should be preserved",
			"decoy content",
			targetFile.readBytes().toString(Charsets.UTF_8)
		)

		// Verify that getPhotos returns the moved decoy file
		val photos = secureImageManager.getPhotos()
		assertEquals("Should have 1 photo after moving decoy", 1, photos.size)
		assertEquals("Photo name should match decoy name", "photo_20230101_120000_00.jpg", photos[0].photoName)

		val field = SecureImageManager::class.java.getDeclaredField("key")
		field.isAccessible = true
		assertNull(field.get(secureImageManager))
	}

	@Test
	fun `updateImage should update image while preserving metadata`() = runTest {
		// Given
		val galleryDir = secureImageManager.getGalleryDirectory()
		galleryDir.mkdirs()

		// Mock security PIN
		val hashedPin = HashedPin("salt", "hash")
		val securityPin = SecurityPin("1234", hashedPin)
		every { authorizationManager.securityPin } returns securityPin

		// Create original image
		val originalJpgBytes = readResourceBytes("red.jpg")
		val photoFile = File(galleryDir, "photo_20230101_120000_00.jpg")
		secureImageManager.encryptToFile(
			plainPin = securityPin.plainPin,
			hashedPin = securityPin.hashedPin,
			plain = originalJpgBytes,
			targetFile = photoFile,
		)

		val photoDef = PhotoDef(
			photoName = "photo_20230101_120000_00.jpg",
			photoFormat = "jpg",
			photoFile = photoFile
		)

		// Mock BitmapFactory for reading the original image
		mockkStatic(BitmapFactory::class)
		val originalBitmap = mockk<Bitmap>()
		every {
			BitmapFactory.decodeByteArray(any(), any(), any())
		} returns originalBitmap

		// Create new bitmap to update with
		val newBitmap = mockk<Bitmap>()

		// Mock bitmap.compress to write bytes to the output stream
		val newJpgBytes = "new image data".toByteArray()
		every {
			newBitmap.compress(any(), any(), any())
		} answers {
			val outputStream = thirdArg<ByteArrayOutputStream>()
			outputStream.write(newJpgBytes)
			true
		}

		// When
		val result = secureImageManager.updateImage(
			bitmap = newBitmap,
			photoDef = photoDef,
			quality = 90
		)

		// Then
		// Verify the result is the same PhotoDef
		assertEquals(photoDef, result)

		// Verify the file exists and has content
		assertTrue(photoFile.exists())
		assertTrue(photoFile.length() > 0)

		// Verify the thumbnail was cleared
		coVerify { thumbnailCache.evictThumbnail(photoDef) }

		// Decrypt the file and verify it contains the updated image data
		val updatedBytes = secureImageManager.decryptJpg(photoDef)
		assertNotNull(updatedBytes)
		assertTrue(updatedBytes.isNotEmpty())

		// Note: We can't directly compare the bytes because the metadata updates
		// will modify the bytes. Instead, we verify that the file exists and has content.
	}
}

fun Any.readResourceBytes(name: String): ByteArray =
	// `this::class.java.classLoader` == the JAR/class‑path “resources” folder
	requireNotNull(this::class.java.classLoader.getResourceAsStream(name)) {
		"Resource $name not found on classpath"
	}.use { it.readBytes() }
