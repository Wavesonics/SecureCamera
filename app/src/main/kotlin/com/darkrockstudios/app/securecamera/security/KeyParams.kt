package com.darkrockstudios.app.securecamera.security

import dev.whyoleg.cryptography.BinarySize
import dev.whyoleg.cryptography.BinarySize.Companion.bytes

data class KeyParams(
	val iterations: Int = 600_000,
	val outputSize: BinarySize = 32.bytes,
)