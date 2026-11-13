package edu.stevens.cs597.budgetgarden

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var currentPage = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<View>(R.id.take_out_button).setOnClickListener { showPage("withdraw") }
        findViewById<View>(R.id.home_button).setOnClickListener { showPage("home") }
        findViewById<View>(R.id.garden_button).setOnClickListener { showPage("garden") }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentPage != "home") showPage("home") else finish()
            }
        })
        showPage(savedInstanceState?.getString("page") ?: "home")
    }

    private fun showPage(page: String) {
        currentPage = page
        val layout = when (page) {
            "withdraw" -> R.layout.page_take_out
            "garden" -> R.layout.page_garden
            "details" -> R.layout.page_details
            "water" -> R.layout.page_water_delivery
            else -> R.layout.page_home
        }
        val container = findViewById<FrameLayout>(R.id.page_container)
        container.removeAllViews()
        LayoutInflater.from(this).inflate(layout, container, true)
        if (page == "home") HomePage(container).bind(::showPage)
        // Load fresh database values for the other pages here after integration.
        val selectedId = when (page) {
            "withdraw" -> R.id.take_out_button
            "garden" -> R.id.garden_button
            else -> R.id.home_button
        }
        listOf(R.id.take_out_button, R.id.home_button, R.id.garden_button).forEach { id ->
            findViewById<View>(id).apply {
                isSelected = id == selectedId
                val labelId = when (id) {
                    R.id.take_out_button -> R.id.take_out_label
                    R.id.home_button -> R.id.home_label
                    else -> R.id.garden_label
                }
                findViewById<TextView>(labelId).setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                alpha = 1f
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("page", currentPage)
        super.onSaveInstanceState(outState)
    }
}
