package com.darkrockstudios.app.securecamera

import android.app.Application
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import timber.log.Timber

class MainApplication : Application(), KoinComponent {
	private val imageManager by inject<SecureImageRepository>()

	override fun onCreate() {
		super.onCreate()

		//if (BuildConfig.DEBUG) {
		Timber.plant(Timber.DebugTree())
		//}

		startKoin {
			androidLogger()
			androidContext(this@MainApplication)
			modules(appModule)
		}
	}

	override fun onTrimMemory(level: Int) {
		super.onTrimMemory(level)
		imageManager.thumbnailCache.clear()
	}

	override fun onLowMemory() {
		super.onLowMemory()
		imageManager.thumbnailCache.clear()
	}
}
