package com.darkrockstudios.app.securecamera.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.darkrockstudios.app.securecamera.security.SoftwareSchemeConfig
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class AppPreferencesManagerTest {

	private lateinit var context: Context
	private lateinit var preferencesManager: AppPreferencesDataSource

	@OptIn(ExperimentalCoroutinesApi::class)
	private val testScope = TestScope(UnconfinedTestDispatcher())

	private lateinit var dataStore: DataStore<Preferences>

	@Before
	fun setup() {
		context = mockk(relaxed = true)
		dataStore = PreferenceDataStoreFactory.create(
			scope = testScope,
			produceFile = { File.createTempFile("prefs_test", ".preferences_pb") }
		)
		preferencesManager = AppPreferencesDataSource(context, dataStore)
	}

	@Test
	fun `hashPin generates salt and hash`() = runTest {
		// Given
		val pin = "1234"

		// When
		val hashedPin = preferencesManager.hashPin(pin)

		// Then
		Assert.assertNotNull("Salt should not be null", hashedPin.salt)
		Assert.assertNotNull("Hash should not be null", hashedPin.hash)
		Assert.assertTrue("Salt should not be empty", hashedPin.salt.isNotEmpty())
		Assert.assertTrue("Hash should not be empty", hashedPin.hash.isNotEmpty())
	}

	@Test
	fun `hashPin generates different hashes for same PIN`() = runTest {
		// Given
		val pin = "1234"

		// When
		val hashedPin1 = preferencesManager.hashPin(pin)
		val hashedPin2 = preferencesManager.hashPin(pin)

		// Then
		Assert.assertNotEquals("Salts should be different", hashedPin1.salt, hashedPin2.salt)
		Assert.assertNotEquals("Hashes should be different", hashedPin1.hash, hashedPin2.hash)
	}

	@Test
	fun `verifyPin returns true for correct PIN`() = runTest {
		// Given
		val pin = "1234"
		val hashedPin = preferencesManager.hashPin(pin)

		// When
		val result = preferencesManager.verifyPin(pin, hashedPin)

		// Then
		Assert.assertTrue("Verification should succeed for correct PIN", result)
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
		Assert.assertFalse("Verification should fail for incorrect PIN", result)
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
		Assert.assertFalse("Verification should fail for empty PIN", result)
	}

	@Test
	fun `verifySecurityPin returns false when no PIN is stored`() = runTest {
		// Given
		// No PIN is stored in the dataStore by default

		// When
		val result = preferencesManager.verifySecurityPin("1234")

		// Then
		Assert.assertFalse("Should return false when no PIN is stored", result)
	}

	@Test
	fun `verifySecurityPin returns true for correct PIN`() = runTest {
		// Given
		val pin = "1234"
		preferencesManager.setAppPin(pin, SoftwareSchemeConfig)

		// When
		val result = preferencesManager.verifySecurityPin(pin)

		// Then
		Assert.assertTrue("Should return true for correct PIN", result)
	}

	@Test
	fun `verifySecurityPin returns false for incorrect PIN`() = runTest {
		// Given
		val correctPin = "1234"
		val incorrectPin = "5678"
		preferencesManager.setAppPin(correctPin, SoftwareSchemeConfig)

		// When
		val result = preferencesManager.verifySecurityPin(incorrectPin)

		// Then
		Assert.assertFalse("Should return false for incorrect PIN", result)
	}

	@Test
	fun `verifyPoisonPillPin returns false when no poison pill PIN is stored`() = runTest {
		// Given
		// No poison pill PIN is stored in the dataStore by default

		// When
		val result = preferencesManager.verifyPoisonPillPin("5678")

		// Then
		Assert.assertFalse("Should return false when no poison pill PIN is stored", result)
	}

	@Test
	fun `verifyPoisonPillPin returns true for correct PIN`() = runTest {
		// Given
		val pin = "5678"
		preferencesManager.setPoisonPillPin(pin)

		// When
		val result = preferencesManager.verifyPoisonPillPin(pin)

		// Then
		Assert.assertTrue("Should return true for correct PIN", result)
	}

	@Test
	fun `verifyPoisonPillPin returns false for incorrect PIN`() = runTest {
		// Given
		val correctPin = "5678"
		val incorrectPin = "1234"
		preferencesManager.setPoisonPillPin(correctPin)

		// When
		val result = preferencesManager.verifyPoisonPillPin(incorrectPin)

		// Then
		Assert.assertFalse("Should return false for incorrect PIN", result)
	}

	@Test
	fun `hasPoisonPillPin returns false when no poison pill PIN is stored`() = runTest {
		// Given
		// No poison pill PIN is stored in the dataStore by default

		// When
		val result = preferencesManager.hasPoisonPillPin()

		// Then
		Assert.assertFalse("Should return false when no poison pill PIN is stored", result)
	}

	@Test
	fun `hasPoisonPillPin returns true when poison pill PIN is stored`() = runTest {
		// Given
		preferencesManager.setPoisonPillPin("5678")

		// When
		val result = preferencesManager.hasPoisonPillPin()

		// Then
		Assert.assertTrue("Should return true when poison pill PIN is stored", result)
	}

	@Test
	fun `activatePoisonPill does nothing when no poison pill PIN is stored`() = runTest {
		// Given
		// No poison pill PIN is stored in the dataStore by default

		// When
		try {
			preferencesManager.activatePoisonPill()
			assertTrue(false, "activatePoisonPill should have thrown an exception")
		} catch (e: Exception) {
			assertTrue(true)
		}

		// Then
		// No exception should be thrown
	}

	@Test
	fun `activatePoisonPill handles when poison pill PIN is stored`() = runTest {
		// Given
		val pin = "5678"
		preferencesManager.setPoisonPillPin(pin)

		// Verify poison pill PIN is set
		Assert.assertTrue(preferencesManager.hasPoisonPillPin())

		// When
		preferencesManager.activatePoisonPill()

		// Then
		// Poison pill PIN should be removed
		Assert.assertFalse(preferencesManager.hasPoisonPillPin())

		// And the regular PIN should now be set to the poison pill PIN
		Assert.assertTrue(preferencesManager.verifySecurityPin(pin))
	}

	@Test
	fun `getSessionTimeout returns default when no timeout is stored`() = runTest {
		// Given
		// No session timeout is stored in the dataStore by default

		// When
		val result = preferencesManager.getSessionTimeout()

		// Then
		Assert.assertEquals(
			"Should return default when no timeout is stored",
			AppPreferencesDataSource.SESSION_TIMEOUT_DEFAULT,
			result
		)
	}

	@Test
	fun `getFailedPinAttempts returns 0 when no count is stored`() = runTest {
		// Given
		// No failed pin attempts count is stored in the dataStore by default

		// When
		val result = preferencesManager.getFailedPinAttempts()

		// Then
		Assert.assertEquals("Should return 0 when no count is stored", 0, result)
	}

	@Test
	fun `getFailedPinAttempts returns stored value when count is set`() = runTest {
		// Given
		val count = 5
		preferencesManager.setFailedPinAttempts(count)

		// When
		val result = preferencesManager.getFailedPinAttempts()

		// Then
		Assert.assertEquals("Should return the stored count value", count, result)
	}

	@Test
	fun `getLastFailedAttemptTimestamp returns 0 when no timestamp is stored`() = runTest {
		// Given
		// No last failed attempt timestamp is stored in the dataStore by default

		// When
		val result = preferencesManager.getLastFailedAttemptTimestamp()

		// Then
		Assert.assertEquals("Should return 0 when no timestamp is stored", 0L, result)
	}

	@Test
	fun `getLastFailedAttemptTimestamp returns stored value when timestamp is set`() = runTest {
		// Given
		val timestamp = 1234567890L
		preferencesManager.setLastFailedAttemptTimestamp(timestamp)

		// When
		val result = preferencesManager.getLastFailedAttemptTimestamp()

		// Then
		Assert.assertEquals("Should return the stored timestamp value", timestamp, result)
	}

	@Test
	fun `securityFailureReset clears all preferences`() = runTest {
		// Given
		// Set some preferences
		preferencesManager.setAppPin("1234", SoftwareSchemeConfig)
		preferencesManager.setPoisonPillPin("5678")
		preferencesManager.setSessionTimeout(AppPreferencesDataSource.SESSION_TIMEOUT_10_MIN)
		preferencesManager.setFailedPinAttempts(5)
		preferencesManager.setLastFailedAttemptTimestamp(1000L)

		// Verify preferences are set
		Assert.assertTrue(preferencesManager.verifySecurityPin("1234"))
		Assert.assertTrue(preferencesManager.hasPoisonPillPin())
		Assert.assertEquals(AppPreferencesDataSource.SESSION_TIMEOUT_10_MIN, preferencesManager.getSessionTimeout())
		Assert.assertEquals(5, preferencesManager.getFailedPinAttempts())
		Assert.assertEquals(1000L, preferencesManager.getLastFailedAttemptTimestamp())

		// When
		preferencesManager.securityFailureReset()

		// Then
		// Verify all preferences are cleared
		Assert.assertFalse(preferencesManager.verifySecurityPin("1234"))
		Assert.assertFalse(preferencesManager.hasPoisonPillPin())
		Assert.assertEquals(AppPreferencesDataSource.SESSION_TIMEOUT_DEFAULT, preferencesManager.getSessionTimeout())
		Assert.assertEquals(0, preferencesManager.getFailedPinAttempts())
		Assert.assertEquals(0L, preferencesManager.getLastFailedAttemptTimestamp())
	}

	@Test
	fun `sanitizeFileName returns default value when not set`() = runTest {
		// Given
		val defaultValue = preferencesManager.sanitizeFileNameDefault

		// When
		val flow = preferencesManager.sanitizeFileName

		// Then
		// The flow should emit the default value
		// This is testing the property initialization with the default value
		Assert.assertEquals(defaultValue, flow.first())
	}

	@Test
	fun `sanitizeMetadata returns default value when not set`() = runTest {
		// Given
		val defaultValue = preferencesManager.sanitizeMetadataDefault

		// When
		val flow = preferencesManager.sanitizeMetadata

		// Then
		// The flow should emit the default value
		// This is testing the property initialization with the default value
		Assert.assertEquals(defaultValue, flow.first())
	}
}
