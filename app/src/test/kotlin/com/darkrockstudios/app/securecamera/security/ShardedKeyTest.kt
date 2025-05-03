package com.darkrockstudios.app.securecamera.security

import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotEquals

class ShardedKeyTest {

	@Test
	fun `constructor should split key into two parts`() {
		// Given
		val originalKey = ByteArray(32) { it.toByte() }

		// When
		val shardedKey = ShardedKey(originalKey)

		// Then
		// We can't directly test private fields, but we can verify the key is reconstructed correctly
		val reconstructed = shardedKey.reconstructKey()
		assertContentEquals(originalKey, reconstructed, "Reconstructed key should match original")
	}

	@Test
	fun `reconstructKey should return original key`() {
		// Given
		val originalKey = ByteArray(16) { (it * 2).toByte() }
		val shardedKey = ShardedKey(originalKey)

		// When
		val reconstructed = shardedKey.reconstructKey()

		// Then
		assertContentEquals(originalKey, reconstructed, "Reconstructed key should match original")
	}

	@Test
	fun `evict should clear key parts`() {
		// Given
		val originalKey = ByteArray(24) { it.toByte() }
		val shardedKey = ShardedKey(originalKey)

		// When
		val beforeEviction = shardedKey.reconstructKey()
		shardedKey.evict()
		val afterEviction = shardedKey.reconstructKey()

		// Then
		assertContentEquals(originalKey, beforeEviction, "Key should be correct before eviction")

		// After eviction, the reconstructed key should be all zeros
		val allZeros = ByteArray(originalKey.size)
		assertContentEquals(allZeros, afterEviction, "After eviction, reconstructed key should be all zeros")
	}

	@Test
	fun `should work with empty key`() {
		// Given
		val emptyKey = ByteArray(0)

		// When
		val shardedKey = ShardedKey(emptyKey)
		val reconstructed = shardedKey.reconstructKey()

		// Then
		assertContentEquals(emptyKey, reconstructed, "Should handle empty keys correctly")
	}

	@Test
	fun `should work with large keys`() {
		// Given
		val largeKey = ByteArray(1024) { (it % 256).toByte() }

		// When
		val shardedKey = ShardedKey(largeKey)
		val reconstructed = shardedKey.reconstructKey()

		// Then
		assertContentEquals(largeKey, reconstructed, "Should handle large keys correctly")
	}

	@Test
	fun `multiple reconstructions should return same key`() {
		// Given
		val originalKey = ByteArray(32) { it.toByte() }
		val shardedKey = ShardedKey(originalKey)

		// When
		val firstReconstruction = shardedKey.reconstructKey()
		val secondReconstruction = shardedKey.reconstructKey()

		// Then
		assertContentEquals(
			firstReconstruction,
			secondReconstruction,
			"Multiple reconstructions should return the same key"
		)
	}

	@Test
	fun `different keys should produce different sharded representations`() {
		// Given
		val key1 = ByteArray(32) { it.toByte() }
		val key2 = ByteArray(32) { (it + 1).toByte() }

		// When
		val shardedKey1 = ShardedKey(key1)
		val shardedKey2 = ShardedKey(key2)

		// Then
		// We can't directly compare private fields, but we can verify the reconstructed keys are different
		val reconstructed1 = shardedKey1.reconstructKey()
		val reconstructed2 = shardedKey2.reconstructKey()

		assertContentEquals(key1, reconstructed1, "First key should be reconstructed correctly")
		assertContentEquals(key2, reconstructed2, "Second key should be reconstructed correctly")
		assertNotEquals(
			reconstructed1.toList(),
			reconstructed2.toList(),
			"Different keys should produce different reconstructed values"
		)
	}
}
