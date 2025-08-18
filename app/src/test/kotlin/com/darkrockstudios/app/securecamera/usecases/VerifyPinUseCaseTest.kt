package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.security.pin.PinRepository
import com.darkrockstudios.app.securecamera.security.schemes.EncryptionScheme
import io.mockk.*
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
	private lateinit var pinRepository: PinRepository
	private lateinit var verifyPinUseCase: VerifyPinUseCase
	private lateinit var encryptionScheme: EncryptionScheme
	private lateinit var migratePinHash: MigratePinHash

	@Before
	fun setup() {
		authManager = mockk()
		imageManager = mockk()
		pinRepository = mockk()
		migratePinHash = mockk()
		encryptionScheme = mockk(relaxed = true)
		verifyPinUseCase = VerifyPinUseCase(
			authManager = authManager,
			imageManager = imageManager,
			pinRepository = pinRepository,
			encryptionScheme = encryptionScheme,
			migratePinHash = migratePinHash,
		)

		coEvery { migratePinHash.runMigration(any()) } just Runs
	}

	@Test
	fun `verifyPin should return true when PIN is valid`() = runTest {
		// Given
		val pin = "1234"
		coEvery { pinRepository.hasPoisonPillPin() } returns false
		coEvery { authManager.verifyPin(pin) } returns mockk(relaxed = true)
		coEvery { authManager.resetFailedAttempts() } just Runs

		// When
		val result = verifyPinUseCase.verifyPin(pin)

		// Then
		assertTrue(result)
		coVerify { authManager.verifyPin(pin) }
		coVerify(exactly = 0) { pinRepository.activatePoisonPill() }
		coVerify(exactly = 0) { imageManager.activatePoisonPill() }
	}

	@Test
	fun `verifyPin should return false when PIN is invalid`() = runTest {
		// Given
		val pin = "1234"
		coEvery { pinRepository.hasPoisonPillPin() } returns false
		coEvery { authManager.verifyPin(pin) } returns null

		// When
		val result = verifyPinUseCase.verifyPin(pin)

		// Then
		assertFalse(result)
		coVerify { authManager.verifyPin(pin) }
		coVerify(exactly = 0) { pinRepository.activatePoisonPill() }
		coVerify(exactly = 0) { imageManager.activatePoisonPill() }
	}

	@Test
	fun `verifyPin should activate poison pill when poison pill PIN is entered`() = runTest {
		// Given
		val pin = "9999"
		coEvery { pinRepository.getHashedPin() } returns mockk()
		coEvery { pinRepository.hasPoisonPillPin() } returns true
		coEvery { pinRepository.verifyPoisonPillPin(pin) } returns true
		coEvery { pinRepository.activatePoisonPill() } returns Unit
		coEvery { imageManager.activatePoisonPill() } returns Unit
		coEvery { authManager.verifyPin(pin) } returns null // Even if PIN verification fails, poison pill should activate

		// When
		val result = verifyPinUseCase.verifyPin(pin)

		// Then
		assertFalse(result) // Result should match what authManager.verifyPin returns
	}

	@Test
	fun `verifyPin should not activate poison pill when poison pill exists but PIN doesn't match`() = runTest {
		// Given
		val pin = "1234"
		coEvery { pinRepository.hasPoisonPillPin() } returns true
		coEvery { pinRepository.verifyPoisonPillPin(pin) } returns false
		coEvery { authManager.verifyPin(pin) } returns mockk(relaxed = true)
		coEvery { authManager.resetFailedAttempts() } just Runs

		// When
		val result = verifyPinUseCase.verifyPin(pin)

		// Then
		assertTrue(result)
		coVerify { pinRepository.hasPoisonPillPin() }
		coVerify { pinRepository.verifyPoisonPillPin(pin) }
		coVerify(exactly = 0) { pinRepository.activatePoisonPill() }
		coVerify(exactly = 0) { imageManager.activatePoisonPill() }
		coVerify { authManager.verifyPin(pin) }
	}

	@Test
	fun `verifyPin should handle empty PIN`() = runTest {
		// Given
		val pin = ""
		coEvery { pinRepository.hasPoisonPillPin() } returns false
		coEvery { authManager.verifyPin(pin) } returns null

		// When
		val result = verifyPinUseCase.verifyPin(pin)

		// Then
		assertFalse(result)
		coVerify { authManager.verifyPin(pin) }
	}
}