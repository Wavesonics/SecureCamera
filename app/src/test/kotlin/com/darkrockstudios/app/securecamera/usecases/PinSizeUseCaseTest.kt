package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.security.SecurityLevel
import com.darkrockstudios.app.securecamera.security.SecurityLevelDetector
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PinSizeUseCaseTest {

	private lateinit var securityLevelDetector: SecurityLevelDetector
	private lateinit var pinSizeUseCase: PinSizeUseCase

	@Before
	fun setup() {
		securityLevelDetector = mockk()
		pinSizeUseCase = PinSizeUseCase(securityLevelDetector)
	}

	@Test
	fun `getPinSizeRange should return 4-16 for TEE security level`() {
		// Given
		val securityLevel = SecurityLevel.TEE

		// When
		val result = pinSizeUseCase.getPinSizeRange(securityLevel)

		// Then
		assertEquals(4..16, result)
	}

	@Test
	fun `getPinSizeRange should return 4-16 for STRONGBOX security level`() {
		// Given
		val securityLevel = SecurityLevel.STRONGBOX

		// When
		val result = pinSizeUseCase.getPinSizeRange(securityLevel)

		// Then
		assertEquals(4..16, result)
	}

	@Test
	fun `getPinSizeRange should return 6-16 for SOFTWARE security level`() {
		// Given
		val securityLevel = SecurityLevel.SOFTWARE

		// When
		val result = pinSizeUseCase.getPinSizeRange(securityLevel)

		// Then
		assertEquals(6..16, result)
	}

	@Test
	fun `getPinSizeRange should use detected security level when not provided`() {
		// Given
		every { securityLevelDetector.detectSecurityLevel() } returns SecurityLevel.TEE

		// When
		val result = pinSizeUseCase.getPinSizeRange()

		// Then
		assertEquals(4..16, result)
	}

	@Test
	fun `getPinSizeRange should use detected security level when not provided - SOFTWARE case`() {
		// Given
		every { securityLevelDetector.detectSecurityLevel() } returns SecurityLevel.SOFTWARE

		// When
		val result = pinSizeUseCase.getPinSizeRange()

		// Then
		assertEquals(6..16, result)
	}

	@Test
	fun `getPinSizeRange should use detected security level when not provided - STRONGBOX case`() {
		// Given
		every { securityLevelDetector.detectSecurityLevel() } returns SecurityLevel.STRONGBOX

		// When
		val result = pinSizeUseCase.getPinSizeRange()

		// Then
		assertEquals(4..16, result)
	}
}