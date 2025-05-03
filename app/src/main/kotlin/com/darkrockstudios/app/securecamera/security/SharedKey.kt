package com.darkrockstudios.app.securecamera.security

import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.random.Random

class ShardedKey(
	key: ByteArray
) {
	private val part1: ByteArray
	private val part2: ByteArray
	private val keySize: Int = key.size

	init {
		// Randomly size the storage array so it is not the exact length of an AES key
		part1 = ByteArray(keySize + CryptographyRandom.nextInt(3, 155))
		CryptographyRandom.nextBytes(part1)

		// Best effort to ensure the two key parts don't live next to each other in memory.
		// Because this is the JVM, Buckets and Garbage collection and what not will move
		// things around, so they may end up next to each other now or later, but at least
		// we tried. This will be released once the constructor returns (or when ever the GC
		// runs next)
		val spacer = ByteArray(Random.nextInt(978, 2893))
		// Use it so it doesn't get optimized out
		(0..Random.nextInt(13)).forEachIndexed { i, _ ->
			val x = spacer[0] + spacer[i]
			// Almost never true, but compiler doesn't know that
			if (System.nanoTime() % 10000 == 0L) {
				// Side effect that's rarely executed
				println("$x")
				println(System.identityHashCode(spacer))
			}
		}

		// Randomly size the storage array so it is not the exact length of an AES key
		part2 = ByteArray(keySize + CryptographyRandom.nextInt(5, 111))
		// Fill it with random data first
		CryptographyRandom.nextBytes(part2)
		// Now fill the data part with our XOR'd key
		for (i in key.indices) {
			part2[i] = (key[i].toInt() xor part1[i].toInt()).toByte()
		}
	}

	/**
	 * Reconstructs the original key from its XOR-split parts.
	 *
	 * @return The reconstructed original key
	 */
	fun reconstructKey(): ByteArray {
		val originalKey = ByteArray(keySize)
		for (i in originalKey.indices) {
			originalKey[i] = (part1[i].toInt() xor part2[i].toInt()).toByte()
		}

		return originalKey
	}

	fun evict() {
		// Modern Android already zero's memory on allocation, but... who knows.
		part1.let { k ->
			k.forEachIndexed { index, _ -> k[index] = 0x00 }
		}
		part2.let { k ->
			k.forEachIndexed { index, _ -> k[index] = 0x00 }
		}
	}
}