package edu.stevens.cs597.budgetgarden

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CreatePlantGoalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_plant_goal)

        findViewById<Button>(R.id.cancelGoalButton)
            .setOnClickListener {
                finish()
            }
    }
}