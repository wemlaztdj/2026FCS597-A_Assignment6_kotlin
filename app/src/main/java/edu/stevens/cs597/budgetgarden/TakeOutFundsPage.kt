package edu.stevens.cs597.budgetgarden

import android.view.View
import android.widget.TextView

/** Keep the withdrawal form and its database calls here, rather than MainActivity. */
class TakeOutFundsPage(private val root: View) {
    fun renderAvailableFunds(formattedBalance: String) {
        root.findViewById<TextView>(R.id.available_funds_text).text =
            root.context.getString(R.string.available_funds_format, formattedBalance)
    }
}
