package com.darkrockstudios.app.securecamera.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/** Minimal compatibility controller to keep most of the app code unchanged while using Navigation 3 */
interface NavController {
	fun navigate(route: String, builder: (NavOptions.() -> Unit)? = null)
	fun navigate(route: String)
	fun navigateUp(): Boolean
	fun popBackStack(): Boolean = navigateUp()
}

class NavOptions {
	var launchSingleTop: Boolean = false
}

class Nav3CompatController(
	private val backStack: NavBackStack
) : NavController {
	override fun navigate(route: String, builder: (NavOptions.() -> Unit)?) {
		val opts = NavOptions().apply { builder?.invoke(this) }
		val key = AppRouteMapper.toKey(route)
		if (key != null) {
			if (opts.launchSingleTop) {
				val current = backStack.lastOrNull()
				if (current != null && AppRouteMapper.toRoute(current) == route) return
			}
			backStack.add(key)
		}
	}

	override fun navigate(route: String) {
		navigate(route, null)
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

/**
 * Nav3-compatible helper to emulate Nav2's navigate { popUpTo(0) { inclusive = true } }
 */
fun NavController.navigateClearingBackStack(route: String, launchSingleTop: Boolean = false) {
	when (this) {
		is Nav3CompatController -> {
			this.clearBackStack()
			this.navigate(route) { this.launchSingleTop = launchSingleTop }
		}

		else -> {
			// Fallback: just navigate if we can't clear
			this.navigate(route) { this.launchSingleTop = launchSingleTop }
		}
	}
}

/**
 * Emulate Nav2's navigate(target) { popUpTo(base) } — ensure back stack is [base, target].
 */
fun NavController.navigateFromBase(baseRoute: String, targetRoute: String, launchSingleTop: Boolean = false) {
	when (this) {
		is Nav3CompatController -> {
			this.clearBackStack()
			this.navigate(baseRoute)
			this.navigate(targetRoute) { this.launchSingleTop = launchSingleTop }
		}

		else -> {
			// Fallback: best-effort — just navigate to target
			this.navigate(targetRoute) { this.launchSingleTop = launchSingleTop }
		}
	}
}

@Composable
fun rememberCompatNavController(startKey: NavKey): Pair<NavBackStack, Nav3CompatController> {
	val backStack = rememberNavBackStack(startKey)
	val controller = remember(backStack) { Nav3CompatController(backStack) }
	return backStack to controller
}