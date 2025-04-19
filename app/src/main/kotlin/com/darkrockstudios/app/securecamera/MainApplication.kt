package com.darkrockstudios.app.securecamera

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class MainApplication : Application() {
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
}
