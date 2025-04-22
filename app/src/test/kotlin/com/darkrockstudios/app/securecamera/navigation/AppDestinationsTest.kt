package com.darkrockstudios.app.securecamera.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import com.darkrockstudios.app.securecamera.navigation.AppDestinations.decodeReturnRoute
import com.darkrockstudios.app.securecamera.navigation.AppDestinations.encodeReturnRoute
import com.darkrockstudios.app.securecamera.navigation.AppDestinations.isPinVerificationRoute
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppDestinationsTest {

	@Test
	fun `encodeReturnRoute should encode simple string`() {
		// Given
		val route = "camera"

		// When
		val encoded = encodeReturnRoute(route)

		// Then
		assertEquals("Y2FtZXJh", encoded)
	}

	@Test
	fun `encodeReturnRoute should encode empty string`() {
		// Given
		val route = ""

		// When
		val encoded = encodeReturnRoute(route)

		// Then
		assertEquals("", encoded)
	}

	@Test
	fun `encodeReturnRoute should encode string with special characters`() {
		// Given
		val route = "view_photo/image?with=special&chars"

		// When
		val encoded = encodeReturnRoute(route)

		// Then
		assertEquals("dmlld19waG90by9pbWFnZT93aXRoPXNwZWNpYWwmY2hhcnM=", encoded)
	}

	@Test
	fun `decodeReturnRoute should decode encoded simple string`() {
		// Given
		val encoded = "Y2FtZXJh"

		// When
		val decoded = decodeReturnRoute(encoded)

		// Then
		assertEquals("camera", decoded)
	}

	@Test
	fun `decodeReturnRoute should decode encoded empty string`() {
		// Given
		val encoded = ""

		// When
		val decoded = decodeReturnRoute(encoded)

		// Then
		assertEquals("", decoded)
	}

	@Test
	fun `decodeReturnRoute should decode encoded string with special characters`() {
		// Given
		val encoded = "dmlld19waG90by9pbWFnZT93aXRoPXNwZWNpYWwmY2hhcnM="

		// When
		val decoded = decodeReturnRoute(encoded)

		// Then
		assertEquals("view_photo/image?with=special&chars", decoded)
	}

	@Test
	fun `encode and decode should be reversible`() {
		// Given
		val originalRoutes = listOf(
			"camera",
			"",
			"view_photo/image?with=special&chars",
			"settings/advanced#section",
			"gallery/folder/subfolder"
		)

		// When & Then
		originalRoutes.forEach { route ->
			val encoded = encodeReturnRoute(route)
			val decoded = decodeReturnRoute(encoded)
			assertEquals(route, decoded, "Failed for route: $route")
		}
	}

	@Test
	fun `isPinVerificationRoute should return true for pin verification route`() {
		// Given
		val navDestination = mockk<NavDestination>()
		every { navDestination.route } returns "pin_verification/Y2FtZXJh"

		val navBackStackEntry = mockk<NavBackStackEntry>()
		every { navBackStackEntry.destination } returns navDestination

		// When
		val result = isPinVerificationRoute(navBackStackEntry)

		// Then
		assertTrue(result)
	}

	@Test
	fun `isPinVerificationRoute should return false for non-pin verification route`() {
		// Given
		val navDestination = mockk<NavDestination>()
		every { navDestination.route } returns "camera"

		val navBackStackEntry = mockk<NavBackStackEntry>()
		every { navBackStackEntry.destination } returns navDestination

		// When
		val result = isPinVerificationRoute(navBackStackEntry)

		// Then
		assertFalse(result)
	}

	@Test
	fun `isPinVerificationRoute should return false for null route`() {
		// Given
		val navDestination = mockk<NavDestination>()
		every { navDestination.route } returns null

		val navBackStackEntry = mockk<NavBackStackEntry>()
		every { navBackStackEntry.destination } returns navDestination

		// When
		val result = isPinVerificationRoute(navBackStackEntry)

		// Then
		assertFalse(result)
	}
}
