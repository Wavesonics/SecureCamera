package com.darkrockstudios.app.securecamera

import android.app.Application
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.usecases.SecurityResetUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import timber.log.Timber

class SnapSafeApplication : Application(), KoinComponent {
	private val imageManager by inject<SecureImageRepository>()
	private val preferences by inject<AppPreferencesDataSource>()
	private val securityReset by inject<SecurityResetUseCase>()

	override fun onCreate() {
		super.onCreate()

		if (BuildConfig.DEBUG) {
			Timber.plant(Timber.DebugTree())
		} else {
			Timber.plant(ReleaseLogTree())
		}

		startKoin {
			androidLogger()
			androidContext(this@SnapSafeApplication)
			modules(appModule)
		}

		handleMigrationFromBeta()
	}

	override fun onTrimMemory(level: Int) {
		super.onTrimMemory(level)
		imageManager.thumbnailCache.clear()
	}

	override fun onLowMemory() {
		super.onLowMemory()
		imageManager.thumbnailCache.clear()
	}

	// DELETE ME after a few versions
	private fun handleMigrationFromBeta() {
		runBlocking {
			val intoComplete = (preferences.hasCompletedIntro.first() == true)
			val isProdReady = (preferences.isProdReady.first() == true)
			val wasInBeta = intoComplete && !isProdReady
			if (wasInBeta) {
				securityReset.reset()
				preferences.markProdReady()
			} else if (!isProdReady) {
				preferences.markProdReady()
			}
		}
	}
}
