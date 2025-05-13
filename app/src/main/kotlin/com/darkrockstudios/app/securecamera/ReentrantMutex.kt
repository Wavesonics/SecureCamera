package com.darkrockstudios.app.securecamera

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex

class ReentrantMutex {
	private val mutex = Mutex()
	private var owner: Any? = null
	private var recursionCount = 0

	suspend fun lock() {
		val current = currentCoroutineContext()[Job]
		if (owner == current) {
			recursionCount++
			return
		}
		mutex.lock()
		owner = current
		recursionCount = 1
	}

	suspend fun unlock() {
		if (owner != currentCoroutineContext()[Job]) {
			throw IllegalStateException("Not the owner of the lock")
		}
		recursionCount--
		if (recursionCount == 0) {
			owner = null
			mutex.unlock()
		}
	}

	suspend fun <T> withLock(action: suspend () -> T): T {
		lock()
		try {
			return action()
		} finally {
			unlock()
		}
	}
}