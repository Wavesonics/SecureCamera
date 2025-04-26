package com.darkrockstudios.app.securecamera.camera

import com.ashampoo.kim.model.GpsCoordinates
import com.ashampoo.kim.model.TiffOrientation
import java.util.*

data class PhotoMetaData(
	val name: String,
	val dateTaken: Date,
	val orientation: TiffOrientation?,
	val location: GpsCoordinates?,
)