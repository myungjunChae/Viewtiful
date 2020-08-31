package com.ono.kakaoheart.model

import android.content.res.Resources

data class Size(val sizeInDp: Int, val mass: Float = 5f) {
    internal val sizeInPx: Float
        get() = sizeInDp * Resources.getSystem().displayMetrics.density

    init {
        require(mass != 0F) { "mass=$mass must be != 0" }
    }
}
