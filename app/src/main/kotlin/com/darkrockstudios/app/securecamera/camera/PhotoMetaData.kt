package com.darkrockstudios.app.securecamera.camera

import android.util.Size
import com.ashampoo.kim.model.GpsCoordinates
import com.ashampoo.kim.model.TiffOrientation
import java.util.*

data class PhotoMetaData(
	val name: String,
	val resolution: Size,
	val dateTaken: Date,
	val orientation: TiffOrientation?,
	val location: GpsCoordinates?,
) {
	fun resolutionString() = "${resolution.width}x${resolution.height}"
}