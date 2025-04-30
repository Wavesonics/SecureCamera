package com.darkrockstudios.app.securecamera

import com.darkrockstudios.app.securecamera.auth.AuthorizationManager
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.camera.ThumbnailCache
import com.darkrockstudios.app.securecamera.gallery.GalleryViewModel
import com.darkrockstudios.app.securecamera.obfuscation.ObfuscatePhotoViewModel
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesManager
import com.darkrockstudios.app.securecamera.settings.SettingsViewModel
import com.darkrockstudios.app.securecamera.usecases.PinStrengthCheckUseCase
import com.darkrockstudios.app.securecamera.usecases.SecurityResetUseCase
import com.darkrockstudios.app.securecamera.usecases.VerifyPinUseCase
import com.darkrockstudios.app.securecamera.viewphoto.ViewPhotoViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
	singleOf(::SecureImageRepository)
	single<AppPreferencesManager> { AppPreferencesManager(context = get()) }
	singleOf(::AuthorizationManager)
	singleOf(::LocationRepository)

	factoryOf(::ThumbnailCache)
	factoryOf(::SecurityResetUseCase)
	factoryOf(::PinStrengthCheckUseCase)
	factoryOf(::VerifyPinUseCase)

	viewModelOf(::ObfuscatePhotoViewModel)
	viewModelOf(::ViewPhotoViewModel)
	viewModelOf(::GalleryViewModel)
	viewModelOf(::SettingsViewModel)
}
