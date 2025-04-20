package com.darkrockstudios.app.securecamera.introduction

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a slide in the introduction
 */
data class IntroductionSlide(
	val icon: ImageVector,
	val title: String,
	val description: String,
)