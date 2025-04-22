package com.darkrockstudios.app.securecamera.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class AuthorizationManagerTest {

	private lateinit var context: Context
	private lateinit var preferencesManager: AppPreferencesManager
	private lateinit var authManager: AuthorizationManager
	private lateinit var dataStore: DataStore<Preferences>

	@OptIn(ExperimentalCoroutinesApi::class)
	private val testScope = TestScope(UnconfinedTestDispatcher())

	@Before
	fun setup() {
		context = mockk(relaxed = true)
		dataStore = PreferenceDataStoreFactory.create(
			scope = testScope,
			produceFile = { File.createTempFile("prefs_test", ".preferences_pb") }
		)
		preferencesManager = spyk(AppPreferencesManager(context, dataStore))
		authManager = AuthorizationManager(preferencesManager)
	}

	@Test
	fun `verifyPin should update authorization state when PIN is valid`() = runTest {
		// Given
		val pin = "1234"
		preferencesManager.setAppPin(pin)

		// When
		val result = authManager.verifyPin(pin)

		// Then
		assertTrue(result)
		assertTrue(authManager.isAuthorized.first())
	}

	@Test
	fun `verifyPin should not update authorization state when PIN is invalid`() = runTest {
		// Given
		val correctPin = "1234"
		val incorrectPin = "5678"
		preferencesManager.setAppPin(correctPin)

		// When
		val result = authManager.verifyPin(incorrectPin)

		// Then
		assertFalse(result)
		assertFalse(authManager.isAuthorized.first())
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
		preferencesManager.setAppPin(pin)
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
		preferencesManager.setAppPin(pin)

		// Set a very small session timeout (1 millisecond)
		preferencesManager.setSessionTimeout(1L)

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
		preferencesManager.setAppPin(pin)
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
		preferencesManager.setAppPin(pin)
		preferencesManager.setSessionTimeout(customTimeout)

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
		preferencesManager.setFailedPinAttempts(expectedCount)

		// When
		val result = authManager.getFailedAttempts()

		// Then
		assertEquals(expectedCount, result)
	}

	@Test
	fun `setFailedAttempts should update the count in preferences`() = runTest {
		// Given
		val count = 5

		// When
		authManager.setFailedAttempts(count)

		// Then
		assertEquals(count, preferencesManager.getFailedPinAttempts())
	}

	@Test
	fun `incrementFailedAttempts should increase the count by one and store timestamp`() = runTest {
		// Given
		val initialCount = 2
		val expectedCount = initialCount + 1
		preferencesManager.setFailedPinAttempts(initialCount)
		preferencesManager.setLastFailedAttemptTimestamp(0) // Reset timestamp

		// When
		val result = authManager.incrementFailedAttempts()

		// Then
		assertEquals(expectedCount, result)
		assertEquals(expectedCount, preferencesManager.getFailedPinAttempts())
		assertTrue(preferencesManager.getLastFailedAttemptTimestamp() > 0) // Timestamp should be updated
	}

	@Test
	fun `resetFailedAttempts should set the count to zero and reset timestamp`() = runTest {
		// Given
		preferencesManager.setFailedPinAttempts(5)
		preferencesManager.setLastFailedAttemptTimestamp(1000L)

		// When
		authManager.resetFailedAttempts()

		// Then
		assertEquals(0, preferencesManager.getFailedPinAttempts())
		assertEquals(0L, preferencesManager.getLastFailedAttemptTimestamp())
	}

	@Test
	fun `verifyPin should reset failed attempts when PIN is valid`() = runTest {
		// Given
		val pin = "1234"
		preferencesManager.setAppPin(pin)
		preferencesManager.setFailedPinAttempts(3)
		preferencesManager.setLastFailedAttemptTimestamp(1000L)

		// When
		val result = authManager.verifyPin(pin)

		// Then
		assertTrue(result)
		assertEquals(0, preferencesManager.getFailedPinAttempts())
		assertEquals(0L, preferencesManager.getLastFailedAttemptTimestamp())
	}

	@Test
	fun `getLastFailedAttemptTimestamp should return the timestamp from preferences`() = runTest {
		// Given
		val expectedTimestamp = 1234567890L
		preferencesManager.setLastFailedAttemptTimestamp(expectedTimestamp)

		// When
		val result = authManager.getLastFailedAttemptTimestamp()

		// Then
		assertEquals(expectedTimestamp, result)
	}

	@Test
	fun `calculateRemainingBackoffSeconds should return 0 when no failed attempts`() = runTest {
		// Given
		preferencesManager.setFailedPinAttempts(0)

		// When
		val result = authManager.calculateRemainingBackoffSeconds()

		// Then
		assertEquals(0, result)
	}

	@Test
	fun `calculateRemainingBackoffSeconds should return 0 when no timestamp`() = runTest {
		// Given
		preferencesManager.setFailedPinAttempts(3)
		preferencesManager.setLastFailedAttemptTimestamp(0L)

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

		preferencesManager.setFailedPinAttempts(failedAttempts)
		preferencesManager.setLastFailedAttemptTimestamp(lastFailedTime)

		// When
		val result = authManager.calculateRemainingBackoffSeconds()

		// Then
		// Should be approximately 5 seconds remaining (8 - 3)
		assertTrue(result > 0 && result <= backoffTime)
	}

	@Test
	fun `securityFailureReset should delegate to preferencesManager`() = runTest {
		// Given
		val pin = "1234"
		preferencesManager.setAppPin(pin)
		preferencesManager.setFailedPinAttempts(5)
		preferencesManager.setLastFailedAttemptTimestamp(1000L)

		// Verify data is set
		assertTrue(preferencesManager.verifySecurityPin(pin))
		assertEquals(5, preferencesManager.getFailedPinAttempts())
		assertEquals(1000L, preferencesManager.getLastFailedAttemptTimestamp())

		// When
		authManager.securityFailureReset()

		// Then
		// Verify data is cleared
		assertFalse(preferencesManager.verifySecurityPin(pin))
		assertEquals(0, preferencesManager.getFailedPinAttempts())
		assertEquals(0L, preferencesManager.getLastFailedAttemptTimestamp())
	}

	@Test
	fun `activatePoisonPill should delegate to preferencesManager`() = runTest {
		// Given
		val regularPin = "1234"
		val poisonPillPin = "5678"
		preferencesManager.setAppPin(regularPin)
		preferencesManager.setPoisonPillPin(poisonPillPin)

		// Verify initial state
		assertTrue(preferencesManager.verifySecurityPin(regularPin))
		assertTrue(preferencesManager.verifyPoisonPillPin(poisonPillPin))
		assertTrue(preferencesManager.hasPoisonPillPin())

		// When
		authManager.activatePoisonPill()

		// Then
		// Verify poison pill was activated
		assertTrue(preferencesManager.verifySecurityPin(poisonPillPin))
		assertFalse(preferencesManager.hasPoisonPillPin())
	}

	@Test
	fun `verifyPin should not update authorization state when PIN is valid but hashedPin is null`() = runTest {
		// Given
		val pin = "1234"
		coEvery { preferencesManager.verifySecurityPin(pin) } returns true
		coEvery { preferencesManager.getHashedPin() } returns null

		// When
		val result = authManager.verifyPin(pin)

		// Then
		assertTrue(result)
		assertFalse(authManager.isAuthorized.first())
		coVerify { preferencesManager.verifySecurityPin(pin) }
	}

	@Test
	fun `calculateRemainingBackoffSeconds should return 0 when elapsed time exceeds backoff time`() = runTest {
		// Given
		val failedAttempts = 2
		val currentTime = System.currentTimeMillis()
		val lastFailedTime = currentTime - 5000 // 5 seconds ago (exceeds backoff time)

		coEvery { preferencesManager.getFailedPinAttempts() } returns failedAttempts
		coEvery { preferencesManager.getLastFailedAttemptTimestamp() } returns lastFailedTime

		// When
		val result = authManager.calculateRemainingBackoffSeconds()

		// Then
		assertEquals(0, result)
	}
}
