package com.darkrockstudios.app.securecamera

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertTrue

class ShareUtilsTest {

	@get:Rule
	val tempFolder = TemporaryFolder()

	private lateinit var mockContext: Context
	private lateinit var shareDir: File
	private lateinit var file1: File
	private lateinit var file2: File

	@Before
	fun setup() {
		mockContext = mockk(relaxed = true)

		// Create real temporary files for testing
		val cacheDir = tempFolder.newFolder("cache")
		shareDir = File(cacheDir, "share")
		shareDir.mkdir()

		file1 = File(shareDir, "file1.txt")
		file2 = File(shareDir, "file2.txt")

		// Mock the context to return our real temporary directory
		every { mockContext.cacheDir } returns cacheDir
	}

	@Test
	fun `clearShareDirectory returns true when directory does not exist`() {
		// Given
		shareDir.delete() // Actually delete the directory

		// When
		val result = clearShareDirectory(mockContext)

		// Then
		assertTrue(result)
	}

	@Test
	fun `clearShareDirectory returns true when all files are deleted`() {
		// Given
		file1.createNewFile()
		file2.createNewFile()

		// When
		val result = clearShareDirectory(mockContext)

		// Then
		assertTrue(result)
		assertTrue(!file1.exists())
		assertTrue(!file2.exists())
	}

	@Test
	fun `clearShareDirectory returns true when directory is empty`() {
		// Given
		// Directory exists but has no files

		// When
		val result = clearShareDirectory(mockContext)

		// Then
		assertTrue(result)
	}
}
