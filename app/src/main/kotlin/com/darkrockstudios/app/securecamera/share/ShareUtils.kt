package com.darkrockstudios.app.securecamera.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.darkrockstudios.app.securecamera.camera.PhotoDef

private const val DECRYPTING_PROVIDER_AUTHORITY = ".decryptingprovider"

/**
 * Creates a URI for a photo using the DecryptingImageProvider
 */
private fun getDecryptingFileProviderUri(
    photo: PhotoDef,
    context: Context
): Uri {
    val authority = context.packageName + DECRYPTING_PROVIDER_AUTHORITY
    return Uri.Builder()
        .scheme("content")
        .authority(authority)
        .path("photos/${photo.photoName}")
        .build()
}

/**
 * Share a photo using DecryptingImageProvider (no temp files)
 */
fun sharePhotoWithProvider(
    photo: PhotoDef,
    context: Context
): Boolean {
    val uri = getDecryptingFileProviderUri(photo, context)

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/jpeg"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share Photo"))
    return true
}

/**
 * Share multiple photos using DecryptingImageProvider (no temp files)
 */
fun sharePhotosWithProvider(
    photos: List<PhotoDef>,
    context: Context
): Boolean {
    if (photos.isEmpty()) {
        return false
    }

    val uris = photos.map { photo ->
        getDecryptingFileProviderUri(photo, context)
    }

    val shareIntent = if (uris.size == 1) {
        // Single photo share
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uris.first())
            type = "image/jpeg"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        // Multiple photo share
        Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            type = "image/jpeg"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share Photos"))
    return true
}
