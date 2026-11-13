package edu.stevens.cs597.budgetgarden

import android.view.View
import android.widget.TextView

/** Created again whenever Home opens, so database values can be refreshed here. */
class HomePage(private val root: View) {
    fun bind(onNavigate: (String) -> Unit) {
        root.findViewById<View>(R.id.details_button).setOnClickListener { onNavigate("details") }
        root.findViewById<View>(R.id.water_delivery_button).setOnClickListener { onNavigate("water") }
        // Database integration: collect/read the existing database's balances here,
        // then call renderBalances on the main thread. XML values are demo data.
    }

    fun renderBalances(overallFormatted: String, leftoverFormatted: String) {
        root.findViewById<TextView>(R.id.overall_funds_text).text = overallFormatted
        root.findViewById<TextView>(R.id.leftover_funds_text).text = leftoverFormatted
    }
}
