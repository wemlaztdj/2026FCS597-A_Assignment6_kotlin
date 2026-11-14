package edu.stevens.cs597.budgetgarden

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class GardenPage(
    private val root: View,
    private val gardenApp: GardenBudgetApp,
    private val scope: CoroutineScope
) {
    fun bind(onNavigate: (String) -> Unit) {
        val container = root.findViewById<LinearLayout>(R.id.garden_container)
        val status = root.findViewById<TextView>(R.id.garden_status_text)
        root.findViewById<View>(R.id.garden_create_goal_button).setOnClickListener {
            onNavigate("create_goal")
        }

        scope.launch {
            try {
                gardenApp.awaitDatabaseReady()
                val dao = gardenApp.dao
                val now = System.currentTimeMillis()
                val plants = dao.getBudgets().filter {
                    !it.isArchived && it.type != "WALLET"
                }
                val balances = plants.map { it to dao.balanceAt(it.id, now) }
                val currency = NumberFormat.getCurrencyInstance(Locale.US)
                val inflater = LayoutInflater.from(root.context)

                container.removeAllViews()
                balances.forEach { (budget, balance) ->
                    val row = inflater.inflate(R.layout.item_plant_goal, container, false)
                    val appearance = PlantCatalog.fromKey(budget.icon)
                    row.findViewById<ImageView>(R.id.goal_plant_image).apply {
                        setImageResource(appearance.drawableRes)
                        contentDescription = root.context.getString(appearance.labelRes)
                    }
                    row.findViewById<TextView>(R.id.garden_goal_name).text = budget.name
                    row.findViewById<TextView>(R.id.garden_goal_amounts).text =
                        root.context.getString(
                            R.string.garden_goal_amounts,
                            currency.format(BigDecimal.valueOf(balance, 2)),
                            currency.format(BigDecimal.valueOf(budget.goalAmount, 2))
                        )
                    container.addView(row)
                }

                status.setText(R.string.garden_empty)
                status.visibility = if (plants.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("GardenPage", "Could not load garden", e)
                container.removeAllViews()
                status.setText(R.string.garden_load_failed)
                status.visibility = View.VISIBLE
            }
        }
    }
}
