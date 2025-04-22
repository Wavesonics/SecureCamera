package com.darkrockstudios.app.securecamera.usecases

class PinStrengthCheckUseCase {
	fun isPinStrongEnough(pin: String): Boolean {
		// Check if PIN is at least 4 digits long and contains only digits
		if (pin.length < 4 || !pin.all { it.isDigit() }) {
			return false
		}

		// Check if all digits are the same (e.g., "1111")
		if (pin.all { it == pin[0] }) {
			return false
		}

		// Check if PIN is a sequence (ascending or descending)
		val isAscendingSequence = (0 until pin.length - 1).all {
			pin[it + 1].digitToInt() - pin[it].digitToInt() == 1
		}

		val isDescendingSequence = (0 until pin.length - 1).all {
			pin[it + 1].digitToInt() - pin[it].digitToInt() == -1
		}

		if (isAscendingSequence || isDescendingSequence) {
			return false
		}

		return true
	}
}
