package com.darkrockstudios.app.securecamera.camera

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoDefTest {

	@Test
	fun `dateTaken parses valid photoName correctly`() {
		// Create a known date
		val calendar = Calendar.getInstance()
		calendar.set(2023, Calendar.JANUARY, 15, 10, 30, 45)
		calendar.set(Calendar.MILLISECOND, 500)
		val expectedDate = calendar.time

		// Format the date as it would be in a photoName
		val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SS", Locale.US)
		val dateString = dateFormat.format(expectedDate)
		val photoName = "photo_$dateString.jpg"

		// Create a PhotoDef with this photoName
		val photoDef = PhotoDef(
			photoName = photoName,
			photoFormat = "jpg",
			photoFile = File("dummy/path")
		)

		// Get the parsed date
		val parsedDate = photoDef.dateTaken()

		// Compare the dates (using string representation to avoid millisecond precision issues)
		assertEquals(dateFormat.format(expectedDate), dateFormat.format(parsedDate))
	}

	@Test
	fun `dateTaken handles invalid photoName gracefully`() {
		// Create a PhotoDef with an invalid photoName
		val photoDef = PhotoDef(
			photoName = "invalid_photo_name.jpg",
			photoFormat = "jpg",
			photoFile = File("dummy/path")
		)

		// Get the current time before calling dateTaken
		val beforeTime = Date()

		// Call dateTaken which should return current date for invalid format
		val parsedDate = photoDef.dateTaken()

		// Get the current time after calling dateTaken
		val afterTime = Date()

		// Verify the returned date is between beforeTime and afterTime
		// This is a loose check since we can't predict the exact time it will use
		assert(parsedDate.time >= beforeTime.time - 1000 && parsedDate.time <= afterTime.time + 1000)
	}
}