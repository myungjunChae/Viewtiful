package com.ono.kakaopiggybank

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val swipeLayout = findViewById<SwipeLayout>(R.id.swipe_layout)

        swipeLayout.setOnDragSuccessCallback(object : SwipeLayout.OnDragSuccess {
            override fun onDragSucces() {
                val t1 = Shape.DrawableShape(resources.getDrawable(R.drawable.apple,null))
                val t2 = Shape.DrawableShape(resources.getDrawable(R.drawable.surfing,null))

                KonfettiView.build()
                    .setDirection(180.0, 360.0)
                    .setSpeed(3f, 5f)
                    .addShapes(t1,t2)
                    .setTimeToLive(10000L)
                    .addSizes(Size(25), Size(25, 6f))
                    .setPosition(KonfettiView.x + KonfettiView.width / 2, KonfettiView.y + KonfettiView.height / 3)
                    .burst(6)
            }
        })
    }
}
