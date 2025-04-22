package com.darkrockstudios.app.securecamera.preferences

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AppPreferencesManagerTest {

	private lateinit var context: Context
	private lateinit var preferencesManager: AppPreferencesManager

	@Before
	fun setup() {
		context = mockk(relaxed = true)
		preferencesManager = AppPreferencesManager(context)
	}

	@Test
	fun `hashPin generates salt and hash`() = runTest {
		// Given
		val pin = "1234"

		// When
		val hashedPin = preferencesManager.hashPin(pin)

		// Then
		assertNotNull("Salt should not be null", hashedPin.salt)
		assertNotNull("Hash should not be null", hashedPin.hash)
		assertTrue("Salt should not be empty", hashedPin.salt.isNotEmpty())
		assertTrue("Hash should not be empty", hashedPin.hash.isNotEmpty())
	}

	@Test
	fun `hashPin generates different hashes for same PIN`() = runTest {
		// Given
		val pin = "1234"

		// When
		val hashedPin1 = preferencesManager.hashPin(pin)
		val hashedPin2 = preferencesManager.hashPin(pin)

		// Then
		assertNotEquals("Salts should be different", hashedPin1.salt, hashedPin2.salt)
		assertNotEquals("Hashes should be different", hashedPin1.hash, hashedPin2.hash)
	}

	@Test
	fun `verifyPin returns true for correct PIN`() = runTest {
		// Given
		val pin = "1234"
		val hashedPin = preferencesManager.hashPin(pin)

		// When
		val result = preferencesManager.verifyPin(pin, hashedPin)

		// Then
		assertTrue("Verification should succeed for correct PIN", result)
	}

	@Test
	fun `verifyPin returns false for incorrect PIN`() = runTest {
		// Given
		val correctPin = "1234"
		val incorrectPin = "5678"
		val hashedPin = preferencesManager.hashPin(correctPin)

		// When
		val result = preferencesManager.verifyPin(incorrectPin, hashedPin)

		// Then
		assertFalse("Verification should fail for incorrect PIN", result)
	}

	@Test
	fun `verifyPin handles empty PIN`() = runTest {
		// Given
		val correctPin = "1234"
		val emptyPin = ""
		val hashedPin = preferencesManager.hashPin(correctPin)

		// When
		val result = preferencesManager.verifyPin(emptyPin, hashedPin)

		// Then
		assertFalse("Verification should fail for empty PIN", result)
	}
}
