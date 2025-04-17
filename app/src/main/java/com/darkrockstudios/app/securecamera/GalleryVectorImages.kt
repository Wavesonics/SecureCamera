package com.darkrockstudios.app.securecamera

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Gallery: ImageVector
    get() {
        if (_Gallery != null) {
            return _Gallery!!
        }
        _Gallery = ImageVector.Builder(
            name = "Gallery",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                // Simple gallery icon - a rectangle with a smaller rectangle inside (like a picture frame)
                moveTo(3f, 3f)
                lineTo(21f, 3f)
                lineTo(21f, 21f)
                lineTo(3f, 21f)
                close()
                
                // Inner picture frame
                moveTo(5f, 5f)
                lineTo(19f, 5f)
                lineTo(19f, 19f)
                lineTo(5f, 19f)
                close()
                
                // Small mountain/landscape inside
                moveTo(7f, 15f)
                lineTo(10f, 11f)
                lineTo(13f, 14f)
                lineTo(17f, 9f)
                lineTo(17f, 17f)
                lineTo(7f, 17f)
                close()
                
                // Small circle (sun)
                moveTo(16f, 8f)
                arcTo(1.5f, 1.5f, 0f, false, true, 13f, 8f)
                arcTo(1.5f, 1.5f, 0f, false, true, 16f, 8f)
                close()
            }
        }.build()
        return _Gallery!!
    }

private var _Gallery: ImageVector? = null