package com.darkrockstudios.app.securecamera

import com.darkrockstudios.app.securecamera.obfuscation.AndroidFacialDetection
import com.darkrockstudios.app.securecamera.obfuscation.FacialDetection
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

class OssSnapSafeApplication : SnapSafeApplication() {
	override fun flavorModule(): Module = module {
		factoryOf(::AndroidFacialDetection) bind FacialDetection::class
	}
}
