package edu.stevens.cs597.budgetgarden.examples

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.stevens.cs597.budgetgarden.data.AppDatabase
import edu.stevens.cs597.budgetgarden.data.BudgetRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseExampleActivity : AppCompatActivity() {

    // Connect to the database
    private val database by lazy {
        AppDatabase.get(applicationContext)
    }

    private val dao by lazy {
        database.budgetDao()
    }

    private val repository by lazy {
        BudgetRepository(database)
    }

    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple screen. No XML needed.
        val button = Button(this).apply {
            text = "Run Database Examples"
        }

        resultText = TextView(this)
        val resetButton = Button(this).apply {
            text = "Reset Database"
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(button)
            addView(resetButton)
            addView(resultText)
        }

        setContentView(ScrollView(this).apply {
            addView(layout)
        })

        button.setOnClickListener {
            button.isEnabled = false
            resultText.text = ""

            // Room database operations run in a coroutine
            lifecycleScope.launch {
                try {
                    runExamples()
                } catch (e: Exception) {
                    show("ERROR: ${e.message}")
                }
            }
        }
        resetButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Database?")
                .setMessage("All budgets and transactions will be deleted.")
                .setPositiveButton("Reset") { _, _ ->
                    resetDatabase()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun show(message: String) {
        resultText.append("$message\n")
    }
    private fun resetDatabase() {
        lifecycleScope.launch {
            try {
                // Delete all budgets and transactions
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }

                // Create a new Water Jug
                repository.ensureFirstWallet()

                resultText.text = "Database reset successfully!"

            } catch (e: Exception) {
                resultText.text = "Error: ${e.message}"
            }
        }
    }
    private suspend fun runExamples() {

        // ========================================
        // 1. GET OR CREATE THE WALLET
        // ========================================

        repository.ensureFirstWallet()

        val allBudgets = dao.getBudgets()

        val wallet = allBudgets.first {
            it.type == "WALLET" && !it.isArchived
        }

        val walletId = wallet.id

        show("1. Wallet: ${wallet.name}")
        show("Wallet ID: $walletId\n")


        // ========================================
        // 2. CREATE TWO BUDGETS
        // ========================================

        // Example 1: Food
        val foodName = "Food"
        val foodGoal = 500_00L // $500.00

        val foodId = repository.createBudget(
            name = foodName,
            type = "SPENDING",
            frequency = "MONTHLY",
            goalAmount = foodGoal,
            neededDate = null,
            description = "Monthly food budget",
            icon = "plant_default"
        )

        // Example 2: Entertainment
        val entertainmentName = "Entertainment"
        val entertainmentGoal = 200_00L // $200.00

        val entertainmentId = repository.createBudget(
            name = entertainmentName,
            type = "SPENDING",
            frequency = "MONTHLY",
            goalAmount = entertainmentGoal,
            neededDate = null,
            description = "Fun activities",
            icon = "plant_default"
        )

        show("2. Created Food ID: $foodId")
        show("Created Entertainment ID: $entertainmentId\n")


        // ========================================
        // 3. READ BUDGETS FROM DATABASE
        // ========================================

        // Example 1: Read one budget using ID
        val foodBudget = dao.getBudget(foodId)

        show("3. Food name: ${foodBudget?.name}")
        show("Food goal: ${foodBudget?.goalAmount} cents")

        // Example 2: Read all budgets
        val budgets = dao.getBudgets()

        for (budget in budgets) {
            show("Budget: ${budget.name}, ID: ${budget.id}")
        }

        show("")


        // ========================================
        // 4. ADD INCOME TO WALLET
        // ========================================

        val now = System.currentTimeMillis()

        val incomeAmount = 1000_00L // $1,000.00

        val incomeId = repository.addTransaction(
            fromId = null,         // Outside
            toId = walletId,       // Wallet
            amount = incomeAmount,
            postDate = now,
            memo = "Paycheck"
        )

        show("4. Added $1,000 to Wallet")
        show("Income transaction ID: $incomeId\n")


        // ========================================
        // 5. TRANSFER MONEY TO PLANTS
        // ========================================

        // Example 1: Wallet -> Food
        val foodFunding = 500_00L

        val foodTransferId = repository.addTransaction(
            fromId = walletId,
            toId = foodId,
            amount = foodFunding,
            postDate = now,
            memo = "Water Food plant"
        )

        // Example 2: Wallet -> Entertainment
        val entertainmentFunding = 200_00L

        val entertainmentTransferId = repository.addTransaction(
            fromId = walletId,
            toId = entertainmentId,
            amount = entertainmentFunding,
            postDate = now,
            memo = "Water Entertainment plant"
        )

        show("5. Transferred $500 to Food")
        show("Transferred $200 to Entertainment")
        show("Transfer IDs: $foodTransferId, $entertainmentTransferId\n")


        // ========================================
        // 6. RECORD AN EXPENSE
        // ========================================

        val expenseAmount = 25_00L // $25.00

        val expenseId = repository.addTransaction(
            fromId = foodId,
            toId = null,           // Outside
            amount = expenseAmount,
            postDate = now,
            memo = "Lunch"
        )

        show("6. Spent $25 from Food")
        show("Expense transaction ID: $expenseId\n")


        // ========================================
        // 7. QUERY CURRENT BALANCES
        // ========================================

        val currentTime = System.currentTimeMillis()

        val walletBalance = dao.balanceAt(walletId, currentTime)
        val foodBalance = dao.balanceAt(foodId, currentTime)
        val entertainmentBalance =
            dao.balanceAt(entertainmentId, currentTime)

        // Total money across all budgets
        val totalMoney = dao.totalMoneyAt(currentTime)

        show("7. Current balances (cents):")
        show("Wallet: $walletBalance")
        show("Food: $foodBalance")
        show("Entertainment: $entertainmentBalance")
        show("Total money: $totalMoney\n")


        // ========================================
        // 8. QUERY MONTHLY SPENDING
        // ========================================

        val monthStart = LocalDate.now().withDayOfMonth(1)

        val startDate = monthStart
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val endDate = monthStart.plusMonths(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val foodSpent = dao.spentBetween(
            foodId,
            startDate,
            endDate
        )

        show("8. Food spending this month: $foodSpent cents\n")


        // ========================================
        // 9. READ TRANSACTION HISTORY
        // ========================================

        // Example 1: Read one transaction
        val expense = dao.getTransaction(expenseId)

        show("9. Expense memo: ${expense?.memo}")

        // Example 2: Read recent transactions
        val transactions = dao.recentTransactions()

        for (transaction in transactions.take(10)) {
            show(
                "${transaction.fromName ?: "Outside"} -> " +
                        "${transaction.toName ?: "Outside"}: " +
                        "${transaction.amount} cents"
            )
        }

        show("")


        // ========================================
        // 10. RETURN FUNDS TO WALLET
        // ========================================

        val returnAmount = 30_00L

        val returnTransactionId = repository.addTransaction(
            fromId = entertainmentId,
            toId = walletId,
            amount = returnAmount,
            postDate = System.currentTimeMillis(),
            memo = "Return unused funds"
        )

        show("10. Returned $30 from Entertainment to Wallet\n")


        // ========================================
        // 11. DELETE A TRANSACTION
        // ========================================

        repository.deleteTransaction(returnTransactionId)

        val deletedTransaction =
            dao.getTransaction(returnTransactionId)

        show("11. Deleted transaction: $returnTransactionId")
        show("Query after delete: $deletedTransaction\n")


        // ========================================
        // 12. ARCHIVE A BUDGET
        // ========================================

        // Automatically returns remaining Entertainment
        // funds to Wallet, then archives the plant.
        repository.archiveBudget(
            budgetId = entertainmentId,
            walletId = walletId
        )

        val archivedBudget = dao.getBudget(entertainmentId)

        show("12. Entertainment archived")
        show("isArchived: ${archivedBudget?.isArchived}\n")


        // ========================================
        // 13. READ UPDATED DATA
        // ========================================

        val finalTime = System.currentTimeMillis()

        val finalWalletBalance =
            dao.balanceAt(walletId, finalTime)

        val finalFoodBalance =
            dao.balanceAt(foodId, finalTime)

        val finalEntertainmentBalance =
            dao.balanceAt(entertainmentId, finalTime)

        val finalTotal = dao.totalMoneyAt(finalTime)

        show("13. Final balances (cents):")
        show("Wallet: $finalWalletBalance")
        show("Food: $finalFoodBalance")
        show("Entertainment: $finalEntertainmentBalance")
        show("Total: $finalTotal")

        show("\nAll examples completed!")
    }
}
