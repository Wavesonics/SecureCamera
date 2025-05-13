package com.darkrockstudios.app.securecamera.usecases

import com.darkrockstudios.app.securecamera.security.SecurityLevel
import com.darkrockstudios.app.securecamera.security.SecurityLevelDetector

class PinSizeUseCase(
	val securityLevelDetector: SecurityLevelDetector
) {
	fun getPinSizeRange(level: SecurityLevel = securityLevelDetector.detectSecurityLevel()): IntRange {
		return when (level) {
			SecurityLevel.TEE, SecurityLevel.STRONGBOX -> (4..16)
			SecurityLevel.SOFTWARE -> (6..16)
		}
	}
}