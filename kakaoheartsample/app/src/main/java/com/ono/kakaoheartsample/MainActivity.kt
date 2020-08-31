package com.ono.kakaoheartsample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    heart_view
                        .build()
                        .setLocation(button.x, button.y - 100)
                        .setSize(16, 32)
                        .start()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val animation = AnimationUtils.loadAnimation(this, R.anim.button_click)
                    view.startAnimation(animation)
                    false
                }
                else -> false
            }
        }
    }
}