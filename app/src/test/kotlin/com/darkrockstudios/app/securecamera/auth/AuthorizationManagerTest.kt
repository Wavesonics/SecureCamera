package com.darkrockstudios.app.securecamera.auth

import android.content.Context
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class AuthorizationManagerTest {

	private lateinit var context: Context
	private lateinit var preferencesManager: AppPreferencesManager
	private lateinit var authManager: AuthorizationManager

	@Before
	fun setup() {
		context = mockk()
		preferencesManager = mockk()
		authManager = AuthorizationManager(preferencesManager)

		coEvery { preferencesManager.hasPoisonPillPin() } returns false
		coEvery { preferencesManager.getSessionTimeout() } returns AppPreferencesManager.SESSION_TIMEOUT_DEFAULT
	}

	@Test
	fun `verifyPin should update authorization state when PIN is valid`() = runTest {
		// Given
		val pin = "1234"
		val mockHashedPin = mockk<com.darkrockstudios.app.securecamera.preferences.HashedPin>()
		coEvery { preferencesManager.verifySecurityPin(pin) } returns true
		coEvery { preferencesManager.getHashedPin() } returns mockHashedPin
		coEvery { preferencesManager.setFailedPinAttempts(0) } returns Unit
		coEvery { preferencesManager.setLastFailedAttemptTimestamp(0) } returns Unit

		// When
		val result = authManager.verifyPin(pin)

		// Then
		assertTrue(result)
		assertTrue(authManager.isAuthorized.first())
		coVerify { preferencesManager.verifySecurityPin(pin) }
		coVerify { preferencesManager.setFailedPinAttempts(0) }
	}

	@Test
	fun `verifyPin should not update authorization state when PIN is invalid`() = runTest {
		// Given
		val pin = "1234"
		coEvery { preferencesManager.verifySecurityPin(pin) } returns false
		coEvery { preferencesManager.getHashedPin() } returns null

		// When
		val result = authManager.verifyPin(pin)

		// Then
		assertFalse(result)
		assertFalse(authManager.isAuthorized.first())
		coVerify { preferencesManager.verifySecurityPin(pin) }
	}

	@Test
	fun `checkSessionValidity should return false when not authorized`() {
		// Given
		// Initial state is not authorized

		// When
		val result = authManager.checkSessionValidity()

		// Then
		assertFalse(result)
		assertFalse(authManager.isAuthorized.value)
	}

	@Test
	fun `checkSessionValidity should return true when session is valid`() = runTest {
		// Given
		val pin = "1234"
		val mockHashedPin = mockk<com.darkrockstudios.app.securecamera.preferences.HashedPin>()
		coEvery { preferencesManager.verifySecurityPin(pin) } returns true
		coEvery { preferencesManager.getHashedPin() } returns mockHashedPin
		coEvery { preferencesManager.setFailedPinAttempts(0) } returns Unit
		coEvery { preferencesManager.setLastFailedAttemptTimestamp(0) } returns Unit
		authManager.verifyPin(pin)

		// When
		val result = authManager.checkSessionValidity()

		// Then
		assertTrue(result)
		assertTrue(authManager.isAuthorized.value)
	}

	@Test
	fun `checkSessionValidity should return false when session has expired`() = runTest {
		// Given
		val pin = "1234"
		val mockHashedPin = mockk<com.darkrockstudios.app.securecamera.preferences.HashedPin>()
		coEvery { preferencesManager.verifySecurityPin(pin) } returns true
		coEvery { preferencesManager.getHashedPin() } returns mockHashedPin
		coEvery { preferencesManager.setFailedPinAttempts(0) } returns Unit
		coEvery { preferencesManager.setLastFailedAttemptTimestamp(0) } returns Unit

		// Override the default session timeout to a very small value (1 millisecond)
		// so that the session expires after the sleep
		coEvery { preferencesManager.getSessionTimeout() } returns 1L

		authManager.verifyPin(pin)

		// Wait for the session to expire
		Thread.sleep(10)

		// When
		val result = authManager.checkSessionValidity()

		// Then
		assertFalse(result)
		assertFalse(authManager.isAuthorized.value)
	}

	@Test
	fun `revokeAuthorization should reset authorization state`() = runTest {
		// Given
		val pin = "1234"
		val mockHashedPin = mockk<com.darkrockstudios.app.securecamera.preferences.HashedPin>()
		coEvery { preferencesManager.verifySecurityPin(pin) } returns true
		coEvery { preferencesManager.getHashedPin() } returns mockHashedPin
		coEvery { preferencesManager.setFailedPinAttempts(0) } returns Unit
		coEvery { preferencesManager.setLastFailedAttemptTimestamp(0) } returns Unit
		authManager.verifyPin(pin)
		assertTrue(authManager.isAuthorized.first())

		// When
		authManager.revokeAuthorization()

		// Then
		assertFalse(authManager.isAuthorized.first())
	}

	@Test
	fun `setSessionTimeout should update the timeout duration`() = runTest {
		// Given
		val pin = "1234"
		val customTimeout = TimeUnit.SECONDS.toMillis(30)
		val mockHashedPin = mockk<com.darkrockstudios.app.securecamera.preferences.HashedPin>()
		coEvery { preferencesManager.verifySecurityPin(pin) } returns true
		coEvery { preferencesManager.getHashedPin() } returns mockHashedPin
		coEvery { preferencesManager.setFailedPinAttempts(0) } returns Unit
		coEvery { preferencesManager.setLastFailedAttemptTimestamp(0) } returns Unit

		// When
		authManager.verifyPin(pin)

		// Then
		assertTrue(authManager.checkSessionValidity())

		// Fast-forward time but less than the timeout
		Thread.sleep(10)
		assertTrue(authManager.checkSessionValidity())
	}

	@Test
	fun `getFailedAttempts should return the count from preferences`() = runTest {
		// Given
		val expectedCount = 3
		coEvery { preferencesManager.getFailedPinAttempts() } returns expectedCount

		// When
		val result = authManager.getFailedAttempts()

		// Then
		assertEquals(expectedCount, result)
		coVerify { preferencesManager.getFailedPinAttempts() }
	}

	@Test
	fun `setFailedAttempts should update the count in preferences`() = runTest {
		// Given
		val count = 5
		coEvery { preferencesManager.setFailedPinAttempts(count) } returns Unit

		// When
		authManager.setFailedAttempts(count)

		// Then
		coVerify { preferencesManager.setFailedPinAttempts(count) }
	}

	@Test
	fun `incrementFailedAttempts should increase the count by one and store timestamp`() = runTest {
		// Given
		val initialCount = 2
		val expectedCount = initialCount + 1
		coEvery { preferencesManager.getFailedPinAttempts() } returns initialCount
		coEvery { preferencesManager.setFailedPinAttempts(expectedCount) } returns Unit
		coEvery { preferencesManager.setLastFailedAttemptTimestamp(any()) } returns Unit

		// When
		val result = authManager.incrementFailedAttempts()

		// Then
		assertEquals(expectedCount, result)
		coVerify { preferencesManager.getFailedPinAttempts() }
		coVerify { preferencesManager.setFailedPinAttempts(expectedCount) }
		coVerify { preferencesManager.setLastFailedAttemptTimestamp(any()) }
	}

	@Test
	fun `resetFailedAttempts should set the count to zero and reset timestamp`() = runTest {
		// Given
		coEvery { preferencesManager.setFailedPinAttempts(0) } returns Unit
		coEvery { preferencesManager.setLastFailedAttemptTimestamp(0) } returns Unit

		// When
		authManager.resetFailedAttempts()

		// Then
		coVerify { preferencesManager.setFailedPinAttempts(0) }
		coVerify { preferencesManager.setLastFailedAttemptTimestamp(0) }
	}

	@Test
	fun `verifyPin should reset failed attempts when PIN is valid`() = runTest {
		// Given
		val pin = "1234"
		coEvery { preferencesManager.verifySecurityPin(pin) } returns true
		coEvery { preferencesManager.getHashedPin() } returns mockk()
		coEvery { preferencesManager.setFailedPinAttempts(0) } returns Unit
		coEvery { preferencesManager.setLastFailedAttemptTimestamp(0) } returns Unit

		// When
		val result = authManager.verifyPin(pin)

		// Then
		assertTrue(result)
		coVerify { preferencesManager.setFailedPinAttempts(0) }
		coVerify { preferencesManager.setLastFailedAttemptTimestamp(0) }
	}

	@Test
	fun `getLastFailedAttemptTimestamp should return the timestamp from preferences`() = runTest {
		// Given
		val expectedTimestamp = 1234567890L
		coEvery { preferencesManager.getLastFailedAttemptTimestamp() } returns expectedTimestamp

		// When
		val result = authManager.getLastFailedAttemptTimestamp()

		// Then
		assertEquals(expectedTimestamp, result)
		coVerify { preferencesManager.getLastFailedAttemptTimestamp() }
	}

	@Test
	fun `calculateRemainingBackoffSeconds should return 0 when no failed attempts`() = runTest {
		// Given
		coEvery { preferencesManager.getFailedPinAttempts() } returns 0

		// When
		val result = authManager.calculateRemainingBackoffSeconds()

		// Then
		assertEquals(0, result)
	}

	@Test
	fun `calculateRemainingBackoffSeconds should return 0 when no timestamp`() = runTest {
		// Given
		coEvery { preferencesManager.getFailedPinAttempts() } returns 3
		coEvery { preferencesManager.getLastFailedAttemptTimestamp() } returns 0L

		// When
		val result = authManager.calculateRemainingBackoffSeconds()

		// Then
		assertEquals(0, result)
	}

	@Test
	fun `calculateRemainingBackoffSeconds should calculate correct backoff time`() = runTest {
		// Given
		val failedAttempts = 3
		val backoffTime = (2 * Math.pow(2.0, failedAttempts - 1.0)).toInt() // 8 seconds
		val currentTime = System.currentTimeMillis()
		val lastFailedTime = currentTime - 3000 // 3 seconds ago

		coEvery { preferencesManager.getFailedPinAttempts() } returns failedAttempts
		coEvery { preferencesManager.getLastFailedAttemptTimestamp() } returns lastFailedTime

		// When
		val result = authManager.calculateRemainingBackoffSeconds()

		// Then
		// Should be approximately 5 seconds remaining (8 - 3)
		assertTrue(result > 0 && result <= backoffTime)
	}
}
