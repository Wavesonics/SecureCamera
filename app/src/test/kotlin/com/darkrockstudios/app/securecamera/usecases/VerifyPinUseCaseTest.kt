package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.security.EncryptionScheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class VerifyPinUseCaseTest {

	private lateinit var authManager: AuthorizationRepository
	private lateinit var imageManager: SecureImageRepository
	private lateinit var preferencesManager: AppPreferencesDataSource
	private lateinit var verifyPinUseCase: VerifyPinUseCase
	private lateinit var encryptionScheme: EncryptionScheme

	@Before
	fun setup() {
		authManager = mockk()
		imageManager = mockk()
		preferencesManager = mockk()
		encryptionScheme = mockk(relaxed = true)
		verifyPinUseCase = VerifyPinUseCase(
			authManager = authManager,
			imageManager = imageManager,
			preferencesManager = preferencesManager,
			encryptionScheme = encryptionScheme,
		)
	}

	@Test
	fun `verifyPin should return true when PIN is valid`() = runTest {
		// Given
		val pin = "1234"
		coEvery { preferencesManager.hasPoisonPillPin() } returns false
		coEvery { authManager.verifyPin(pin) } returns mockk(relaxed = true)

		// When
		val result = verifyPinUseCase.verifyPin(pin)

		// Then
		assertTrue(result)
		coVerify { authManager.verifyPin(pin) }
		coVerify(exactly = 0) { authManager.activatePoisonPill() }
		coVerify(exactly = 0) { imageManager.activatePoisonPill() }
	}

	@Test
	fun `verifyPin should return false when PIN is invalid`() = runTest {
		// Given
		val pin = "1234"
		coEvery { preferencesManager.hasPoisonPillPin() } returns false
		coEvery { authManager.verifyPin(pin) } returns null

		// When
		val result = verifyPinUseCase.verifyPin(pin)

		// Then
		assertFalse(result)
		coVerify { authManager.verifyPin(pin) }
		coVerify(exactly = 0) { authManager.activatePoisonPill() }
		coVerify(exactly = 0) { imageManager.activatePoisonPill() }
	}

	@Test
	fun `verifyPin should activate poison pill when poison pill PIN is entered`() = runTest {
		// Given
		val pin = "9999"
		coEvery { preferencesManager.hasPoisonPillPin() } returns true
		coEvery { preferencesManager.verifyPoisonPillPin(pin) } returns true
		coEvery { authManager.activatePoisonPill() } returns Unit
		coEvery { imageManager.activatePoisonPill() } returns Unit
		coEvery { authManager.verifyPin(pin) } returns null // Even if PIN verification fails, poison pill should activate

		// When
		val result = verifyPinUseCase.verifyPin(pin)

		// Then
		assertFalse(result) // Result should match what authManager.verifyPin returns
		coVerify { preferencesManager.hasPoisonPillPin() }
		coVerify { preferencesManager.verifyPoisonPillPin(pin) }
		coVerify { authManager.activatePoisonPill() }
		coVerify { imageManager.activatePoisonPill() }
		coVerify { authManager.verifyPin(pin) }
	}

	@Test
	fun `verifyPin should not activate poison pill when poison pill exists but PIN doesn't match`() = runTest {
		// Given
		val pin = "1234"
		coEvery { preferencesManager.hasPoisonPillPin() } returns true
		coEvery { preferencesManager.verifyPoisonPillPin(pin) } returns false
		coEvery { authManager.verifyPin(pin) } returns mockk(relaxed = true)

		// When
		val result = verifyPinUseCase.verifyPin(pin)

		// Then
		assertTrue(result)
		coVerify { preferencesManager.hasPoisonPillPin() }
		coVerify { preferencesManager.verifyPoisonPillPin(pin) }
		coVerify(exactly = 0) { authManager.activatePoisonPill() }
		coVerify(exactly = 0) { imageManager.activatePoisonPill() }
		coVerify { authManager.verifyPin(pin) }
	}

	@Test
	fun `verifyPin should handle empty PIN`() = runTest {
		// Given
		val pin = ""
		coEvery { preferencesManager.hasPoisonPillPin() } returns false
		coEvery { authManager.verifyPin(pin) } returns null

		// When
		val result = verifyPinUseCase.verifyPin(pin)

		// Then
		assertFalse(result)
		coVerify { authManager.verifyPin(pin) }
	}
}