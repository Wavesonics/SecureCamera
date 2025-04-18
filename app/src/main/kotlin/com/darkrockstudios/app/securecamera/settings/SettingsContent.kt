package com.darkrockstudios.app.securecamera.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.darkrockstudios.app.securecamera.R

/**
 * Settings screen content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
	navController: NavHostController,
	modifier: Modifier = Modifier,
	paddingValues: PaddingValues
) {
	Column(
		modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
	) {
		TopAppBar(
			title = {
				Text(
					text = stringResource(id = R.string.settings_title),
					color = MaterialTheme.colorScheme.onSurface
				)
			},
			colors = TopAppBarDefaults.topAppBarColors(
				containerColor = MaterialTheme.colorScheme.primaryContainer,
				titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
			),
			navigationIcon = {
				IconButton(onClick = { navController.popBackStack() }) {
					Icon(
						imageVector = Icons.Filled.ArrowBack,
						contentDescription = "Back",
						tint = MaterialTheme.colorScheme.onSurface
					)
				}
			}
		)

		// Settings content
		Column(
			modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 0.dp,
                    top = 8.dp
                ),
			verticalArrangement = Arrangement.Top,
			horizontalAlignment = Alignment.Start
		) {
			Text(
				text = stringResource(id = R.string.settings_title),
				style = MaterialTheme.typography.headlineMedium
			)

			Spacer(modifier = Modifier.height(16.dp))

			Text(
				text = stringResource(id = R.string.settings_description),
				style = MaterialTheme.typography.bodyLarge
			)
		}
	}
}
