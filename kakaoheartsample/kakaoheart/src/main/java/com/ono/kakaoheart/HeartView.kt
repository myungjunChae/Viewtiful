package com.ono.kakaoheart

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class HeartView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val heartRendererList = mutableListOf<HeartRenderer>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (i in heartRendererList.size - 1 downTo 0) {
            heartRendererList[i].also { heartRenderer ->
                heartRenderer.render(canvas)

                if (heartRenderer.isFinish()) {
                    heartRendererList.removeAt(i)
                }
            }
        }

        if (heartRendererList.size > 0) {
            invalidate()
        }
    }

    fun build(): HeartBuilder = HeartBuilder(this)

    fun start(heartBuilder : HeartBuilder) {
        val heartRenderer = HeartRenderer(heartBuilder)
        heartRendererList.add(heartRenderer)
        invalidate()
    }
}