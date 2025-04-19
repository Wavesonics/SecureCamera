package com.darkrockstudios.app.securecamera

import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
	singleOf(::SecureImageManager)
	singleOf(::AppPreferencesManager)
	singleOf(::AuthorizationManager)
	singleOf(::LocationRepository)
}
