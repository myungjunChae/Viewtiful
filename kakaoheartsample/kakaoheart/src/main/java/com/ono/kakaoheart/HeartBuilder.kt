package com.ono.kakaoheart

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.ono.kakaoheart.model.Size
import com.ono.kakaoheart.model.Vector

class HeartBuilder(private val heartView: HeartView) {
    companion object {
        const val MIN_LOCATION_RATIO = 0.95
        const val MAX_LOCATION_RATIO = 1.05
    }

    internal lateinit var location: Vector
    internal lateinit var size: Size
    internal lateinit var drawable: Drawable

    init {
        setDrawable()
    }

    fun setLocation(x: Float, y: Float): HeartBuilder {
        val min = (x * MIN_LOCATION_RATIO).toInt()
        val max = (x * MAX_LOCATION_RATIO).toInt()
        val mutatedX = (min..max).random().toFloat()
        location = Vector(mutatedX, y)
        return this
    }

    fun setSize(min: Int, max: Int): HeartBuilder {
        size = Size((min..max).random())
        return this
    }

    fun start() {
        heartView.start(this)
    }

    private fun setDrawable(): HeartBuilder {
        drawable = ContextCompat.getDrawable(heartView.context, R.drawable.icon)!!.mutate()
        return this
    }
}
