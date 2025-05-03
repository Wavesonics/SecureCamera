package com.darkrockstudios.app.securecamera.security

import com.darkrockstudios.app.securecamera.preferences.HashedPin
import java.io.File

/**
 * Encryption schemes used to encrypt and decrypt files.
 * We have two concrete Schemes: Software and Hardware
 */
interface EncryptionScheme {
	/**
	 * Encrypts plaintext data and writes it to a file.
	 * This will derive the key on-demand and cache it for later re-use.
	 */
	suspend fun encryptToFile(plainPin: String, hashedPin: HashedPin, plain: ByteArray, targetFile: File)

	/**
	 * Encrypts plaintext data using a pre-derived key and writes it to a file
	 */
	suspend fun encryptToFile(plain: ByteArray, keyBytes: ByteArray, targetFile: File)

	/**
	 * Decrypts an encrypted file and returns the plaintext data
	 * This will derive the key on-demand and cache it for later re-use.
	 */
	suspend fun decryptFile(plainPin: String, hashedPin: HashedPin, encryptedFile: File): ByteArray

	/**
	 * Derives an encryption key from a PIN and its hash and caches it
	 */
	suspend fun deriveKey(plainPin: String, hashedPin: HashedPin): ByteArray

	/**
	 * Derive a key, avoid mem caching
	 */
	suspend fun deriveKeyRaw(plainPin: String, hashedPin: HashedPin): ByteArray

	/**
	 * Clears any cached encryption keys
	 */
	fun evictKey()

	/**
	 * First time key creation
	 */
	suspend fun createKey()
}