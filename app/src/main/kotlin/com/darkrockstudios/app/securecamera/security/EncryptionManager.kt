package com.darkrockstudios.app.securecamera.security

import android.content.Context
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.preferences.HashedPin
import java.io.File

class EncryptionManager(
	private val appContext: Context,
	private val deviceInfo: DeviceInfo,
	private val appPreferencesDataSource: AppPreferencesDataSource,
	private val securityLevelDetector: SecurityLevelDetector,
) : EncryptionScheme {

	private lateinit var currentScheme: EncryptionScheme

	private fun getEncryptionScheme(): EncryptionScheme {
		val securityLevel = securityLevelDetector.detectSecurityLevel()

		return if (::currentScheme.isInitialized) {
			currentScheme
		} else {
			val newScheme = when (securityLevel) {
				SecurityLevel.TEE, SecurityLevel.STRONGBOX ->
					HardwareBackedEncryptionScheme(
						appContext = appContext,
						deviceInfo = deviceInfo,
						appPreferencesDataSource = appPreferencesDataSource,
					)

				SecurityLevel.SOFTWARE ->
					SoftwareEncryptionScheme(
						deviceInfo = deviceInfo,
					)
			}
			currentScheme = newScheme
			newScheme
		}
	}

	override suspend fun encryptToFile(
		plainPin: String,
		hashedPin: HashedPin,
		plain: ByteArray,
		targetFile: File
	) = getEncryptionScheme().encryptToFile(
		plainPin = plainPin,
		hashedPin = hashedPin,
		plain = plain,
		targetFile = targetFile
	)

	override suspend fun encryptToFile(plain: ByteArray, keyBytes: ByteArray, targetFile: File) =
		getEncryptionScheme().encryptToFile(
			plain = plain,
			keyBytes = keyBytes,
			targetFile = targetFile
		)

	override suspend fun decryptFile(
		plainPin: String,
		hashedPin: HashedPin,
		encryptedFile: File
	): ByteArray = getEncryptionScheme().decryptFile(
		plainPin = plainPin,
		hashedPin = hashedPin,
		encryptedFile = encryptedFile
	)

	override suspend fun deriveKey(
		plainPin: String,
		hashedPin: HashedPin
	): ByteArray = getEncryptionScheme().deriveKey(plainPin = plainPin, hashedPin = hashedPin)

	override suspend fun deriveKeyRaw(
		plainPin: String,
		hashedPin: HashedPin
	): ByteArray = getEncryptionScheme().deriveKeyRaw(plainPin = plainPin, hashedPin = hashedPin)

	override fun evictKey() = getEncryptionScheme().evictKey()

	override suspend fun createKey() = getEncryptionScheme().createKey()
}