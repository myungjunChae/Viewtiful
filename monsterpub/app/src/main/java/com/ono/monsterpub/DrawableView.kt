package com.ono.monsterpub

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs

private const val STROKE_WIDTH = 30f
private const val MAINTAIN_TIME = 100L

data class TimedPath(
    val path: Path,
    val maintainTime: Long
) {
    private val startTime = System.currentTimeMillis()
    private var interpolatedTime: Long = 0L

    fun calcInterpolatedTime(): Boolean {
        interpolatedTime = abs(System.currentTimeMillis() - startTime)
        if (interpolatedTime >= maintainTime) {
            path.reset()
            return true
        }
        return false
    }
}

class DrawableView(context: Context) : View(context) {
    enum class State {
        DOWN,
        MOVE,
        UP
    }

    //buffer
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    //draw env
    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)
    private val colors = mutableListOf<Int>().apply {
        add(ResourcesCompat.getColor(resources, R.color.color1, null))
        add(ResourcesCompat.getColor(resources, R.color.color2, null))
        add(ResourcesCompat.getColor(resources, R.color.color3, null))
        add(ResourcesCompat.getColor(resources, R.color.color4, null))
        add(ResourcesCompat.getColor(resources, R.color.color5, null))
    }

    private val paint by lazy {
        Paint().apply {
            //color = drawColor
            isAntiAlias = true // 안티엘리어싱
            isDither = true // 디더링
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.BEVEL // 선과 곡선이 겹칠 때
            strokeCap = Paint.Cap.ROUND // 끝모양
            strokeWidth = STROKE_WIDTH
            shader = LinearGradient(
                0F,
                0F,
                width.toFloat(),
                height.toFloat(),
                colors.toIntArray(),
                null,
                Shader.TileMode.REPEAT
            )
        }
    }
    private val paths = mutableListOf<TimedPath>()

    //pos
    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f

    private var currentX = 0f
    private var currentY = 0f

    private var state: State = State.UP

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (::extraBitmap.isInitialized)
            extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchDown()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }
        return true
    }

    //start dragg
    private fun touchDown() {
        state = State.DOWN

        currentX = motionTouchEventX
        currentY = motionTouchEventY

        handler.post(object : Runnable {
            override fun run() {
                try {
                    val resetPath = mutableListOf<TimedPath>()
                    extraCanvas.drawColor(backgroundColor)

                    for (i in 0 until paths.size) {
                        paths[i].run {
                            if (calcInterpolatedTime()) {
                                resetPath.add(this)
                            }
                        }
                        extraCanvas.drawPath(paths[i].path, paint)
                    }

                    for (path in resetPath) {
                        paths.remove(path)
                    }

                    invalidate()
                } catch (e: Exception) {

                } finally {
                    if (state == State.UP && paths.isEmpty()) {
                        handler.removeCallbacks(this)
                    } else {
                        handler.postDelayed(this, 16)
                    }
                }
            }
        })
    }

    //move finger
    private fun touchMove() {
        state = State.MOVE

        // 이전의 값과 현재 위치의 차이
        val dx = abs(motionTouchEventX - currentX)
        val dy = abs(motionTouchEventY - currentY)

        val tPath = Path().apply { moveTo(currentX, currentY) }
        paths.add(TimedPath(tPath, MAINTAIN_TIME))

        for (timedPath in paths) {
            timedPath.path.lineTo(
                currentX,
                currentY
            )
        }

        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    //end touch
    private fun touchUp() {
        state = State.UP
    }
}