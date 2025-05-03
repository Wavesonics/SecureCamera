package com.darkrockstudios.app.securecamera.security

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
sealed class SchemeConfig

@Serializable
data object SoftwareSchemeConfig : SchemeConfig()

@Serializable
data class HardwareSchemeConfig(
	val requireBiometricAttestation: Boolean,
	val authTimeout: Duration
) : SchemeConfig()