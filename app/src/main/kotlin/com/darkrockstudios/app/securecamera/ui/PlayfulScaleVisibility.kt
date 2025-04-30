package com.darkrockstudios.app.securecamera.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PlayfulScaleVisibility(
	isVisible: Boolean,
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	AnimatedVisibility(
		modifier = modifier,
		visible = isVisible,
		enter = scaleIn(
			initialScale = 0.7f,
			animationSpec = spring(
				stiffness = Spring.StiffnessLow,
				dampingRatio = Spring.DampingRatioMediumBouncy
			)
		) + fadeIn(),
		exit = scaleOut(
			targetScale = 0.7f,
			animationSpec = tween(
				durationMillis = 250,
				easing = FastOutSlowInEasing
			)
		) + fadeOut()
	) {
		content()
	}
}