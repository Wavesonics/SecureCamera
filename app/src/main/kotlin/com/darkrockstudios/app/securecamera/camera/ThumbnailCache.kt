package com.darkrockstudios.app.securecamera.camera

import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ThumbnailCache {
	private val maxMemory = Runtime.getRuntime().maxMemory() / 1024
	private val cacheSize = (maxMemory / 8).toInt()

	private val thumbnailCache = object : LruCache<String, Bitmap>(cacheSize) {
		override fun sizeOf(key: String, bitmap: Bitmap): Int {
			// The cache size will be measured in kilobytes
			return bitmap.byteCount / 1024
		}
	}
	private val cacheMutex = Mutex()

	suspend fun getThumbnail(photo: PhotoDef): Bitmap? {
		cacheMutex.withLock {
			thumbnailCache.get(photo.photoName)?.let {
				return it
			}
		}
		return null
	}

	fun evictThumbnail(photo: PhotoDef) {
		cacheMutex.tryLock()
		try {
			thumbnailCache.remove(photo.photoName)
		} finally {
			cacheMutex.unlock()
		}
	}

	fun clear() {
		cacheMutex.tryLock()
		try {
			thumbnailCache.evictAll()
		} finally {
			cacheMutex.unlock()
		}
	}

	suspend fun putThumbnail(photo: PhotoDef, thumbnailBitmap: Bitmap) {
		cacheMutex.withLock {
			thumbnailCache.put(photo.photoName, thumbnailBitmap)
		}
	}
}