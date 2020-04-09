package com.ono.kakaopiggybank

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ono.roulette.RouletteSize
import kotlinx.android.synthetic.main.activity_main.*
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size

class MainActivity : AppCompatActivity() {
    private val shapeResources = arrayOf(
        R.drawable.apple,
        R.drawable.avocado,
        R.drawable.grapes,
        R.drawable.grinning_face,
        R.drawable.rowboat,
        R.drawable.smiling_face,
        R.drawable.surfing,
        R.drawable.sushi,
        R.drawable.watermelon
    )

    private val shapes by lazy {
        shapeResources.map { shapeBuilder(it) }.toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val swipeLayout = findViewById<SwipeLayout>(R.id.swipe_layout)

        swipeLayout.setOnDragSuccessCallback(object : SwipeLayout.OnDragSuccess {
            override fun onDragSucces() {
                KonfettiView.build()
                    .setDirection(240.0, 310.0)
                    .setSpeed(3f, 6f)
                    .addShapes(*shapes)
                    .setTimeToLive(10000L)
                    .addSizes(Size(30))
                    .setPosition(
                        KonfettiView.x + KonfettiView.width / 2,
                        KonfettiView.y + KonfettiView.height / 3
                    )
                    .burst(6)
            }
        })
    }

    private fun shapeBuilder(id: Int): Shape =
        Shape.DrawableShape(resources.getDrawable(id, null))

}
