package com.darkrockstudios.app.securecamera.viewphoto

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.darkrockstudios.app.securecamera.R
import com.darkrockstudios.app.securecamera.camera.PhotoDef
import com.darkrockstudios.app.securecamera.camera.PhotoMetaData
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import org.koin.compose.koinInject

@Composable
fun PhotoInfoDialog(
	photo: PhotoDef,
	dismiss: () -> Unit
) {
	val imageManager = koinInject<SecureImageManager>()

	var metadata by remember { mutableStateOf<PhotoMetaData?>(null) }

	LaunchedEffect(photo) {
		metadata = imageManager.getPhotoMetaData(photo)
	}

	AlertDialog(
		onDismissRequest = dismiss,
		title = { Text(stringResource(id = R.string.info_dialog_title)) },
		text = {
			Column {
				Text(
					text = stringResource(id = R.string.photo_name_label),
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurface
				)
				Spacer(modifier = Modifier.Companion.height(4.dp))
				Text(
					text = photo.photoName,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)

				Spacer(modifier = Modifier.Companion.height(16.dp))

				Text(
					text = stringResource(id = R.string.photo_date_label),
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurface
				)
				Spacer(modifier = Modifier.Companion.height(4.dp))
				Text(
					text = metadata?.dateTaken?.toString() ?: stringResource(R.string.photo_no_data),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)

				Spacer(modifier = Modifier.Companion.height(16.dp))

				Text(
					text = stringResource(id = R.string.photo_location_label),
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurface
				)
				Spacer(modifier = Modifier.Companion.height(4.dp))
				Text(
					text = metadata?.location?.latLongString ?: stringResource(R.string.photo_no_data),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)

				Spacer(modifier = Modifier.Companion.height(16.dp))

				Text(
					text = stringResource(id = R.string.photo_orientation_label),
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurface
				)
				Spacer(modifier = Modifier.Companion.height(4.dp))
				Text(
					text = metadata?.orientation?.toString() ?: stringResource(R.string.photo_no_data),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		},
		confirmButton = {
			TextButton(onClick = dismiss) {
				Text(stringResource(id = R.string.ok_button))
			}
		}
	)
}