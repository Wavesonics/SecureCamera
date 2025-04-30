package com.darkrockstudios.app.securecamera.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.compose.composable

private const val Duration = 240
private val StandardEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val fSpec = tween<Float>(
	durationMillis = Duration,
	easing = StandardEasing
)

private val iSpec = tween<IntOffset>(
	durationMillis = Duration,
	easing = StandardEasing
)

fun defaultEnter(): EnterTransition =
	fadeIn(animationSpec = fSpec) +
			scaleIn(
				// start a little smaller – matches Settings/Files app
				initialScale = 0.75f,
				animationSpec = fSpec
			) +
			slideInVertically(
				// slide up ≈10 % of the height
				initialOffsetY = { it / 10 },
				animationSpec = iSpec
			)

fun defaultExit(): ExitTransition =
	fadeOut(animationSpec = fSpec) +
			scaleOut(
				targetScale = 0.9f,          // recedes slightly
				animationSpec = fSpec
			) +
			slideOutVertically(
				targetOffsetY = { -it / 10 }, // moves up a bit
				animationSpec = iSpec
			)

fun defaultPopEnter(): EnterTransition =
	fadeIn(animationSpec = fSpec) +
			scaleIn(
				initialScale = 0.9f,          // small lift-off, then settles
				animationSpec = fSpec
			) +
			slideInVertically(
				initialOffsetY = { -it / 10 }, // comes down slightly
				animationSpec = iSpec
			)

fun defaultPopExit(): ExitTransition =
	fadeOut(animationSpec = fSpec) +
			scaleOut(
				targetScale = 0.75f,
				animationSpec = fSpec
			) +
			slideOutVertically(
				targetOffsetY = { it / 10 },  // slides downward
				animationSpec = iSpec
			)

fun androidx.navigation.NavGraphBuilder.defaultAnimatedComposable(
	route: String,
	arguments: List<NamedNavArgument> = emptyList(),
	deepLinks: List<NavDeepLink> = emptyList(),
	content: @Composable() (AnimatedContentScope.(NavBackStackEntry) -> Unit)
) {
	composable(
		route = route,
		arguments = arguments,
		deepLinks = deepLinks,
		enterTransition = { defaultEnter() },
		exitTransition = { defaultExit() },
		popEnterTransition = { defaultPopEnter() },
		popExitTransition = { defaultPopExit() },
		content = content,
	)
}