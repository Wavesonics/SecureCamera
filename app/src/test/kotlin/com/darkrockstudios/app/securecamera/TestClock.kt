package com.darkrockstudios.app.securecamera

import kotlin.time.Clock
import kotlin.time.Instant

class TestClock(var fixedInstant: Instant) : Clock {
	override fun now(): Instant = fixedInstant
}