package com.ono.kakaoheart

import android.graphics.drawable.Drawable
import com.ono.kakaoheart.model.Size
import com.ono.kakaoheart.model.Vector

data class Heart(
    var location: Vector,
    val size: Size,
    var velocity: Vector,
    val drawable: Drawable
) {
    fun getSize(): Float = size.sizeInPx

    fun getHeightRatio(): Float {
        return if (drawable.intrinsicHeight == -1 && drawable.intrinsicWidth == -1) {
            1f
        } else if (drawable.intrinsicHeight == -1 || drawable.intrinsicWidth == -1) {
            0f
        } else {
            drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth
        }
    }
}