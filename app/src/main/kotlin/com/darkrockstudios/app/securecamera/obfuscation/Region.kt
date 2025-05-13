package com.darkrockstudios.app.securecamera.obfuscation

import android.graphics.Rect

sealed class Region(
	val rect: Rect,
	val obfuscate: Boolean = true,
)

class FaceRegion(
	val face: FacialDetection.FoundFace,
	obfuscate: Boolean = true,
) : Region(face.boundingBox, obfuscate)

class ManualRegion(
	rect: Rect,
	obfuscate: Boolean = true,
) : Region(rect, obfuscate)
