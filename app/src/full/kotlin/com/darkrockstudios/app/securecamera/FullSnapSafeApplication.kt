package com.darkrockstudios.app.securecamera

import com.darkrockstudios.app.securecamera.obfuscation.FacialDetection
import com.darkrockstudios.app.securecamera.obfuscation.MlFacialDetection
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

class FullSnapSafeApplication : SnapSafeApplication() {
	override fun flavorModule(): Module = module {
		factoryOf(::MlFacialDetection) bind FacialDetection::class
	}
}