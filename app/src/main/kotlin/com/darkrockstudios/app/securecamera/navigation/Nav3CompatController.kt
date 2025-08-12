package com.darkrockstudios.app.securecamera.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Minimal compatibility controller to keep most of the app code unchanged while using Navigation 3
 **/
interface NavController {
	fun navigate(key: NavKey, builder: (NavOptions.() -> Unit)? = null)
	fun navigate(key: NavKey)

	fun navigateUp(): Boolean
	fun popBackStack(): Boolean = navigateUp()
}

class NavOptions {
	var launchSingleTop: Boolean = false
}

class Nav3CompatController(
	private val backStack: NavBackStack
) : NavController {
	override fun navigate(key: NavKey, builder: (NavOptions.() -> Unit)?) {
		val opts = NavOptions().apply { builder?.invoke(this) }
		if (opts.launchSingleTop) {
			val current = backStack.lastOrNull()
			if (current != null && current == key) return
		}
		backStack.add(key)
	}

	override fun navigate(key: NavKey) {
		navigate(key, null)
	}

	override fun navigateUp(): Boolean {
		return backStack.removeLastOrNull() != null
	}

	/** Clears the entire back stack. */
	fun clearBackStack() {
		while (backStack.removeLastOrNull() != null) {
			// keep removing
		}
	}
}


fun NavController.navigateClearingBackStack(key: NavKey, launchSingleTop: Boolean = false) {
	when (this) {
		is Nav3CompatController -> {
			this.clearBackStack()
			this.navigate(key) { this.launchSingleTop = launchSingleTop }
		}
		else -> {
			this.navigate(key) { this.launchSingleTop = launchSingleTop }
		}
	}
}


fun NavController.navigateFromBase(baseKey: NavKey, targetKey: NavKey, launchSingleTop: Boolean = false) {
	when (this) {
		is Nav3CompatController -> {
			this.clearBackStack()
			this.navigate(baseKey)
			this.navigate(targetKey) { this.launchSingleTop = launchSingleTop }
		}
		else -> {
			this.navigate(targetKey) { this.launchSingleTop = launchSingleTop }
		}
	}
}