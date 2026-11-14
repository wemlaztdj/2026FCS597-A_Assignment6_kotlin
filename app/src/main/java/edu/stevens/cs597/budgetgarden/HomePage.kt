package edu.stevens.cs597.budgetgarden

import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Created again whenever Home opens, so database values can be refreshed here. */
class HomePage(
    private val root: View,
    private val gardenApp: GardenBudgetApp,
    private val scope: CoroutineScope
) {
    fun bind(onNavigate: (String) -> Unit) {
        root.findViewById<View>(R.id.details_button).setOnClickListener { onNavigate("details") }
        root.findViewById<View>(R.id.water_delivery_button).setOnClickListener { onNavigate("water") }
        root.findViewById<View>(R.id.create_goal_button)
            .setOnClickListener {
                onNavigate("create_goal")
            }
        val loading = root.context.getString(R.string.balance_loading)
        renderBalances(loading, loading)

        scope.launch {
            try {
                gardenApp.awaitDatabaseReady()
                val dao = gardenApp.dao
                val now = System.currentTimeMillis()
                val totalMoney = dao.totalMoneyAt(now)
                val wallets = dao.getBudgets().filter {
                    it.type == "WALLET" && !it.isArchived
                }
                var walletBalance = 0L
                for (wallet in wallets) {
                    walletBalance += dao.balanceAt(wallet.id, now)
                }

                renderBalances(money(totalMoney), money(walletBalance))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HomePage", "Could not load balances", e)
                val unavailable = root.context.getString(R.string.balance_unavailable)
                renderBalances(unavailable, unavailable)
                Toast.makeText(
                    root.context,
                    R.string.home_balances_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun renderBalances(overallFormatted: String, leftoverFormatted: String) {
        root.findViewById<TextView>(R.id.overall_funds_text).text = overallFormatted
        root.findViewById<TextView>(R.id.leftover_funds_text).text = leftoverFormatted
    }

    private fun money(cents: Long): String =
        NumberFormat.getCurrencyInstance(Locale.US)
            .format(BigDecimal.valueOf(cents, 2))
}
