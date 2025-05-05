package com.darkrockstudios.app.securecamera

import androidx.work.WorkManager
import com.darkrockstudios.app.securecamera.auth.AuthorizationRepository
import com.darkrockstudios.app.securecamera.auth.PinVerificationViewModel
import com.darkrockstudios.app.securecamera.camera.SecureImageRepository
import com.darkrockstudios.app.securecamera.camera.ThumbnailCache
import com.darkrockstudios.app.securecamera.gallery.GalleryViewModel
import com.darkrockstudios.app.securecamera.import.ImportPhotosViewModel
import com.darkrockstudios.app.securecamera.introduction.IntroductionViewModel
import com.darkrockstudios.app.securecamera.obfuscation.FacialDetection
import com.darkrockstudios.app.securecamera.obfuscation.MlFacialDetection
import com.darkrockstudios.app.securecamera.obfuscation.ObfuscatePhotoViewModel
import com.darkrockstudios.app.securecamera.preferences.AppPreferencesDataSource
import com.darkrockstudios.app.securecamera.security.DeviceInfo
import com.darkrockstudios.app.securecamera.security.SecurityLevel
import com.darkrockstudios.app.securecamera.security.SecurityLevelDetector
import com.darkrockstudios.app.securecamera.security.schemes.EncryptionScheme
import com.darkrockstudios.app.securecamera.security.schemes.HardwareBackedEncryptionScheme
import com.darkrockstudios.app.securecamera.security.schemes.SoftwareEncryptionScheme
import com.darkrockstudios.app.securecamera.settings.SettingsViewModel
import com.darkrockstudios.app.securecamera.usecases.*
import com.darkrockstudios.app.securecamera.viewphoto.ViewPhotoViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
	singleOf(::SecureImageRepository)
	single<AppPreferencesDataSource> { AppPreferencesDataSource(context = get()) }
	singleOf(::AuthorizationRepository)
	singleOf(::LocationRepository)
	single<EncryptionScheme> {
		val detector = get<SecurityLevelDetector>()
		when (detector.detectSecurityLevel()) {
			SecurityLevel.SOFTWARE ->
				SoftwareEncryptionScheme(get())
			SecurityLevel.TEE, SecurityLevel.STRONGBOX -> {
				HardwareBackedEncryptionScheme(get(), get(), get())
			}
		}
	} bind EncryptionScheme::class
	singleOf(::SecurityLevelDetector)

	single { WorkManager.getInstance(get()) }

	factoryOf(::DeviceInfo)

	factoryOf(::ThumbnailCache)
	factoryOf(::SecurityResetUseCase)
	factoryOf(::PinStrengthCheckUseCase)
	factoryOf(::VerifyPinUseCase)
	factoryOf(::CreatePinUseCase)
	factoryOf(::PinSizeUseCase)
	factoryOf(::MlFacialDetection) bind FacialDetection::class

	viewModelOf(::ObfuscatePhotoViewModel)
	viewModelOf(::ViewPhotoViewModel)
	viewModelOf(::GalleryViewModel)
	viewModelOf(::SettingsViewModel)
	viewModelOf(::IntroductionViewModel)
	viewModelOf(::PinVerificationViewModel)
	viewModelOf(::ImportPhotosViewModel)
}
