package com.darkrockstudios.app.securecamera.gallery

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.darkrockstudios.app.securecamera.R

@Composable
fun rememberPhotoPickerLauncher(onImagesSelected: (List<Uri>) -> Unit): () -> Unit {
	val context = LocalContext.current
	val pickImagesLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			val selectedUris = mutableListOf<Uri>()
			val data = result.data

			// Handle single image
			val singleData = data?.data
			if (singleData != null) {
				selectedUris.add(singleData)
			}
			// Handle multiple images
			else if (data?.clipData != null) {
				val clipData = data.clipData!!
				for (i in 0 until clipData.itemCount) {
					selectedUris.add(clipData.getItemAt(i).uri)
				}
			}

			if (selectedUris.isNotEmpty()) {
				onImagesSelected(selectedUris)
			}
		}
	}

	return {
		val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
			type = "image/jpeg"
			putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
		}
		pickImagesLauncher.launch(
			Intent.createChooser(intent, context.getString(R.string.import_chooser_intent_title))
		)
	}
}