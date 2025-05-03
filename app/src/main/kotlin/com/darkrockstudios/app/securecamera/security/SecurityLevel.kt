package com.darkrockstudios.app.securecamera.security

import android.annotation.SuppressLint
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties.*
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

enum class SecurityLevel { STRONGBOX, TEE, SOFTWARE }

class SecurityLevelDetector {
	private lateinit var securityLevel: SecurityLevel

	private fun createKeySpec(alias: String, strongbox: Boolean): KeyGenParameterSpec {
		val specBuilder = KeyGenParameterSpec.Builder(
			alias,
			PURPOSE_ENCRYPT or PURPOSE_DECRYPT
		)
			.setBlockModes(BLOCK_MODE_GCM)
			.setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
		if (strongbox) {
			specBuilder.setIsStrongBoxBacked(true)
		}
		return specBuilder.build()
	}

	@SuppressLint("NewApi")
	fun detectSecurityLevel(): SecurityLevel {
		if (::securityLevel.isInitialized) return securityLevel

		val alias = "snapSafe_probe"

		val ks = try {
			KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
		} catch (_: StrongBoxUnavailableException) {
			securityLevel = SecurityLevel.SOFTWARE
			// Looks like no key manager is available
			return securityLevel
		}

		// Try to create it in StrongBox first
		try {
			val spec = createKeySpec(alias, true)
			KeyGenerator.getInstance(KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
				init(spec)
				generateKey()
			}
		} catch (_: StrongBoxUnavailableException) {
			// Must only have TEE
			val spec = createKeySpec(alias, false)
			KeyGenerator.getInstance(KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
				init(spec)
				generateKey()
			}
		}

		val key = ks.getKey(alias, null) as SecretKey

		val info: KeyInfo = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
			.getKeySpec(key, KeyInfo::class.java) as KeyInfo
		val level = info.securityLevel

		ks.deleteEntry(alias)

		securityLevel = when (level) {
			SECURITY_LEVEL_UNKNOWN, SECURITY_LEVEL_SOFTWARE -> SecurityLevel.SOFTWARE
			SECURITY_LEVEL_UNKNOWN_SECURE, SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> SecurityLevel.TEE
			SECURITY_LEVEL_STRONGBOX -> SecurityLevel.STRONGBOX
			else -> error("Unknown security level")
		}

		return securityLevel
	}
}