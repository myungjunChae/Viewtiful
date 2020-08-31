package com.ono.kakaoheart

import android.graphics.Canvas
import com.ono.kakaoheart.model.Vector
import kotlin.random.Random

class HeartRenderer(private val builder: HeartBuilder) {
    companion object {
        const val FADEOUT_TIME = 3000L
        const val FADEOUT_ALPHA = 5

        const val MAX_VELOCITY_X = 20F

        const val MIN_VELOCITY_Y = 5
        const val MAX_VELOCITY_Y = 7

        const val MIN_DISTANCE = 50
        const val MAX_DISTANCE = 150
    }

    private lateinit var heart: Heart

    private val timer = Timer()

    private var count: Int = 0
    private var alpha: Int = 255

    private var direction: Direction = getRandomDirection()
    private var distance: Float = -1f

    init {
        initHeart()
        getDeltaTime()
    }

    fun isFinish() = alpha <= 0f

    fun render(canvas: Canvas) {
        update(getDeltaTime())
        display(canvas)
    }

    private fun initHeart() {
        builder.run {
            heart = Heart(
                location = location,
                size = size,
                velocity = Vector(0f, 0f),
                drawable = drawable
            )
        }
    }

    private fun update(deltaTime: Float) {
        count += 1
        updateLocation()
        updateAlpha(deltaTime)
    }

    private fun updateLocation() {
        heart.run {
            if (distance <= 0f) {
                changeDirection()
                distance = getRandomDistance()
            } else {
                velocity.x = calcVelocityX(count)
                distance -= velocity.x

                if (direction == Direction.LEFT) {
                    velocity.x *= -1
                }
            }

            velocity.y = getRandomVelocityY()

            location.add(velocity)
        }
    }

    private fun updateAlpha(deltaTime: Float) {
        heart.run {
            if (FADEOUT_TIME <= deltaTime) {
                val interval = FADEOUT_ALPHA * (deltaTime / 1000)
                alpha -= interval.toInt()
            }

            if (alpha <= 0) {
                alpha = 0
            }
        }
    }

    private fun display(canvas: Canvas) {
        heart.run {
            // if the particle is outside the bottom of the view the lifetime is over.
            if (location.y < 0) {
                return
            }

            // Do not draw the particle if it's outside the canvas view
            if (location.x > canvas.width || location.x + getSize() < 0 || location.y + getSize() < 0) {
                return
            }

            val saveCount = canvas.save()
            canvas.translate(location.x, location.y)

            draw(canvas)
            canvas.restoreToCount(saveCount)
        }
    }

    private fun draw(canvas: Canvas) {
        heart.run {
            val height = (getSize() * getHeightRatio()).toInt()
            val top = ((getSize() - height) / 2f).toInt()
            drawable.alpha = alpha
            drawable.setBounds(0, top, getSize().toInt(), top + height)
            drawable.draw(canvas)
        }
    }

    private fun calcVelocityX(count: Int): Float {
        return (MAX_VELOCITY_X / count) + 0.1f
    }

    private fun changeDirection() {
        direction = if (direction == Direction.LEFT)
            Direction.RIGHT
        else
            Direction.LEFT
    }

    private fun getRandomDirection(): Direction {
        return if (Random.nextBoolean()) Direction.LEFT
        else Direction.RIGHT
    }

    private fun getRandomVelocityY(): Float {
        return (MIN_VELOCITY_Y..MAX_VELOCITY_Y).random().toFloat() * -1
    }

    private fun getRandomDistance(): Float {
        return (MIN_DISTANCE..MAX_DISTANCE).random().toFloat()
    }

    private fun getDeltaTime() = timer.getDeltaTime()

    enum class Direction {
        LEFT,
        RIGHT
    }
}

class Timer {
    private var startTime: Long = -1L

    fun getDeltaTime(): Float {
        if (startTime == -1L) startTime = System.currentTimeMillis()

        val currentTime = System.currentTimeMillis()
        val dt: Long = (currentTime - startTime)
        return dt.toFloat()
    }
}