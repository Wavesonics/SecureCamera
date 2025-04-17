package com.darkrockstudios.app.securecamera.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryContent(
	modifier: Modifier = Modifier,
	navController: NavController
) {
	Box(
		modifier = modifier
			.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Text(text = "Gallery Screen")
	}
}
