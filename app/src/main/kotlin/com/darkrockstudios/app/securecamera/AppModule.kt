package com.darkrockstudios.app.securecamera

import com.darkrockstudios.app.securecamera.camera.SecureImageManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
	singleOf(::SecureImageManager)
}