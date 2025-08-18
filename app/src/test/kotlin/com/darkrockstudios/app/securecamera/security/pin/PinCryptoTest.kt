package com.darkrockstudios.app.securecamera.security.pin

import com.darkrockstudios.app.securecamera.preferences.HashedPin
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class PinCryptoTest {
	private val deviceId: ByteArray = "test-device-id-123".encodeToByteArray()
	private val crypto = PinCrypto()

	@Test
	fun `hashPin generates salt and hash`() = runTest {
		val pin = "1234"

		val hashedPin: HashedPin = crypto.hashPin(pin, deviceId)

		Assert.assertNotNull("Salt should not be null", hashedPin.salt)
		Assert.assertNotNull("Hash should not be null", hashedPin.hash)
		Assert.assertTrue("Salt should not be empty", hashedPin.salt.isNotEmpty())
		Assert.assertTrue("Hash should not be empty", hashedPin.hash.isNotEmpty())
	}

	@Test
	fun `hashPin generates different hashes for same PIN`() = runTest {
		val pin = "1234"

		val hashedPin1 = crypto.hashPin(pin, deviceId)
		val hashedPin2 = crypto.hashPin(pin, deviceId)

		Assert.assertNotEquals("Salts should be different", hashedPin1.salt, hashedPin2.salt)
		Assert.assertNotEquals("Hashes should be different", hashedPin1.hash, hashedPin2.hash)
	}

	@Test
	fun `verifyPin returns true for correct PIN`() = runTest {
		val pin = "1234"
		val hashedPin = crypto.hashPin(pin, deviceId)

		val result = crypto.verifyPin(pin, hashedPin, deviceId)

		Assert.assertTrue("Verification should succeed for correct PIN", result)
	}

	@Test
	fun `verifyPin returns false for incorrect PIN`() = runTest {
		val correctPin = "1234"
		val incorrectPin = "5678"
		val hashedPin = crypto.hashPin(correctPin, deviceId)

		val result = crypto.verifyPin(incorrectPin, hashedPin, deviceId)

		Assert.assertFalse("Verification should fail for incorrect PIN", result)
	}

	@Test
	fun `verifyPin handles empty PIN`() = runTest {
		val correctPin = "1234"
		val emptyPin = ""
		val hashedPin = crypto.hashPin(correctPin, deviceId)

		val result = crypto.verifyPin(emptyPin, hashedPin, deviceId)

		Assert.assertFalse("Verification should fail for empty PIN", result)
	}
}
