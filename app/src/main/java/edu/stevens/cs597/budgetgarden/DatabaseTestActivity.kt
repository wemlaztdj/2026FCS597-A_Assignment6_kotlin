package edu.stevens.cs597.budgetgarden

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.stevens.cs597.budgetgarden.data.AppDatabase
import edu.stevens.cs597.budgetgarden.data.BudgetRepository
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class DatabaseTestActivity : AppCompatActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepository

    private lateinit var typeSpinner: Spinner
    private lateinit var frequencySpinner: Spinner
    private lateinit var fromSpinner: Spinner
    private lateinit var toSpinner: Spinner
    private lateinit var archiveBudgetSpinner: Spinner
    private lateinit var archiveWalletSpinner: Spinner
    private lateinit var summaryText: TextView
    private lateinit var budgetsText: TextView
    private lateinit var transactionsText: TextView

    private val types = listOf("SPENDING", "SAVINGS", "WALLET")
    private val frequencies = listOf("NONE", "MONTHLY", "ONE_TIME")
    private var firstLoad = true
    private var initialReady = false

    private data class BudgetOption(val id: Long?, val label: String) {
        override fun toString(): String = label
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_test)
        findViewById<Button>(R.id.openCreateGoalButton)
            .setOnClickListener {
                startActivity(
                    Intent(this, CreatePlantGoalActivity::class.java)
                )
            }

        database = AppDatabase.get(applicationContext)
        repository = BudgetRepository(database)

        typeSpinner = findViewById(R.id.typeSpinner)
        frequencySpinner = findViewById(R.id.frequencySpinner)
        fromSpinner = findViewById(R.id.fromSpinner)
        toSpinner = findViewById(R.id.toSpinner)
        archiveBudgetSpinner = findViewById(R.id.archiveBudgetSpinner)
        archiveWalletSpinner = findViewById(R.id.archiveWalletSpinner)
        summaryText = findViewById(R.id.summaryText)
        budgetsText = findViewById(R.id.budgetsText)
        transactionsText = findViewById(R.id.transactionsText)

        bindSpinner(typeSpinner, types)
        typeSpinner.setSelection(0) // SPENDING
        bindSpinner(frequencySpinner, frequencies)
        frequencySpinner.setSelection(1) // MONTHLY
        findViewById<EditText>(R.id.postDateInput).setText(LocalDate.now().toString())

        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            runAction()
        }

        findViewById<Button>(R.id.createBudgetButton).setOnClickListener {
            runAction {
                val type = typeSpinner.selectedItem.toString()
                val frequency = if (type == "WALLET") "NONE" else frequencySpinner.selectedItem.toString()
                val goal = if (type == "WALLET") 0L else moneyInput(R.id.goalInput, emptyMeansZero = true)
                val deadline = if (type == "WALLET") null else optionalDate(R.id.neededDateInput)
                repository.createBudget(
                    name = input(R.id.nameInput),
                    type = type,
                    frequency = frequency,
                    goalAmount = goal,
                    neededDate = deadline,
                    description = input(R.id.descriptionInput),
                    icon = input(R.id.iconInput)
                )
                findViewById<EditText>(R.id.nameInput).setText("")
                findViewById<EditText>(R.id.goalInput).setText("")
                findViewById<EditText>(R.id.neededDateInput).setText("")
                findViewById<EditText>(R.id.descriptionInput).setText("")
                findViewById<EditText>(R.id.iconInput).setText("")
                "Budget created"
            }
        }

        findViewById<Button>(R.id.addTransactionButton).setOnClickListener {
            runAction {
                val fromId = (fromSpinner.selectedItem as? BudgetOption)?.id
                val toId = (toSpinner.selectedItem as? BudgetOption)?.id
                val cents = moneyInput(R.id.amountInput)
                val postDate = requiredDate(R.id.postDateInput)
                repository.addTransaction(
                    fromId = fromId,
                    toId = toId,
                    amount = cents,
                    postDate = postDate,
                    memo = input(R.id.memoInput)
                )
                findViewById<EditText>(R.id.amountInput).setText("")
                findViewById<EditText>(R.id.memoInput).setText("")
                "Transaction saved"
            }
        }

        findViewById<Button>(R.id.archiveButton).setOnClickListener {
            runAction {
                val budgetId = (archiveBudgetSpinner.selectedItem as? BudgetOption)?.id
                    ?: error("Create a plant before archiving")
                val walletId = (archiveWalletSpinner.selectedItem as? BudgetOption)?.id
                    ?: error("An active wallet is required")
                repository.archiveBudget(budgetId, walletId)
                "Budget archived; balance returned to wallet"
            }
        }

        findViewById<Button>(R.id.deleteButton).setOnClickListener {
            runAction {
                val id = input(R.id.deleteIdInput).toLongOrNull()
                    ?: error("Enter a valid transaction ID")
                repository.deleteTransaction(id)
                findViewById<EditText>(R.id.deleteIdInput).setText("")
                "Transaction deleted"
            }
        }

        lifecycleScope.launch {
            try {
                repository.ensureFirstWallet()
                initialReady = true
                refreshView()
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // A fresh open (including the next day) recalculates posted transactions.
        if (initialReady) {
            lifecycleScope.launch { refreshView() }
        }
    }

    private fun runAction(action: suspend () -> String = { "Refreshed" }) {
        lifecycleScope.launch {
            try {
                val message = action()
                refreshView()
                Toast.makeText(this@DatabaseTestActivity, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    private suspend fun refreshView() {
        val dao = database.budgetDao()
        val now = System.currentTimeMillis()
        val budgets = dao.getBudgets()
        val balances = budgets.associate { it.id to dao.balanceAt(it.id, now) }
        val total = dao.totalMoneyAt(now)
        val walletBalance = budgets.filter { it.type == "WALLET" }.sumOf { balances[it.id] ?: 0L }
        val plantsBalance = budgets.filter { it.type != "WALLET" }.sumOf { balances[it.id] ?: 0L }
        val fundsNeeded = maxOf(0L, -walletBalance)

        summaryText.text = buildString {
            appendLine("Total money: ${cash(total)}")
            appendLine("Free money (wallets): ${cash(walletBalance)}")
            appendLine("Funds needed: ${cash(fundsNeeded)}")
            append("Allocated to plants: ${cash(plantsBalance)}")
        }

        val monthStart = LocalDate.now().withDayOfMonth(1)
        val fromMonth = startOfDay(monthStart)
        val nextMonth = startOfDay(monthStart.plusMonths(1))
        budgetsText.text = if (budgets.isEmpty()) "No budgets" else buildString {
            for (budget in budgets) {
                val status = if (budget.isArchived) " [ARCHIVED]" else ""
                appendLine("#${budget.id} ${budget.name} (${budget.type})$status")
                appendLine("   Balance: ${cash(balances[budget.id] ?: 0L)}")
                if (budget.type != "WALLET") {
                    appendLine("   Goal: ${cash(budget.goalAmount)} | ${budget.frequency}")
                    if (budget.frequency == "MONTHLY") {
                        appendLine("   Spent this month: ${cash(dao.spentBetween(budget.id, fromMonth, nextMonth))}")
                    }
                }
                appendLine()
            }
        }

        val rows = dao.recentTransactions()
        transactionsText.text = if (rows.isEmpty()) "No transactions yet" else buildString {
            for (row in rows) {
                val future = if (row.postDate > now) " [UPCOMING]" else ""
                val source = row.fromName ?: "Outside"
                val dest = row.toName ?: "Outside"
                appendLine("#${row.id} ${formatDate(row.postDate)}$future")
                appendLine("   $source → $dest | ${cash(row.amount)}")
                if (row.memo.isNotBlank()) appendLine("   ${row.memo}")
                appendLine()
            }
        }

        // Rebuild selections after a budget is inserted/archived.
        val active = budgets.filter { !it.isArchived }
        val previousFrom = (fromSpinner.selectedItem as? BudgetOption)?.id
        val previousTo = (toSpinner.selectedItem as? BudgetOption)?.id
        val previousArchive = (archiveBudgetSpinner.selectedItem as? BudgetOption)?.id
        val previousWallet = (archiveWalletSpinner.selectedItem as? BudgetOption)?.id
        val outside = BudgetOption(null, "Outside (income / expense)")
        val allOptions = listOf(outside) + active.map { BudgetOption(it.id, "#${it.id} ${it.name}") }
        val plants = active.filter { it.type != "WALLET" }
            .map { BudgetOption(it.id, "#${it.id} ${it.name}") }
        val wallets = active.filter { it.type == "WALLET" }
            .map { BudgetOption(it.id, "#${it.id} ${it.name}") }

        bindSpinner(fromSpinner, allOptions)
        bindSpinner(toSpinner, allOptions)
        bindSpinner(archiveBudgetSpinner, plants)
        bindSpinner(archiveWalletSpinner, wallets)

        selectById(fromSpinner, previousFrom)
        selectById(toSpinner, previousTo)
        selectById(archiveBudgetSpinner, previousArchive)
        selectById(archiveWalletSpinner, previousWallet)

        if (firstLoad) {
            selectById(toSpinner, wallets.firstOrNull()?.id)
            firstLoad = false
        }
    }

    private fun <T> bindSpinner(spinner: Spinner, choices: List<T>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, choices)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun selectById(spinner: Spinner, id: Long?) {
        val adapter = spinner.adapter ?: return
        for (i in 0 until adapter.count) {
            if ((adapter.getItem(i) as? BudgetOption)?.id == id) {
                spinner.setSelection(i)
                return
            }
        }
    }

    private fun input(id: Int): String =
        findViewById<EditText>(id).text.toString().trim()

    private fun moneyInput(id: Int, emptyMeansZero: Boolean = false): Long {
        val raw = input(id)
        if (raw.isEmpty() && emptyMeansZero) return 0L
        require(raw.isNotBlank()) { "Enter an amount" }
        // No floating-point arithmetic; reject >2 decimal places.
        return try {
            BigDecimal(raw).movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY).longValueExact()
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException("Enter a valid amount with at most 2 decimal places")
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Enter a valid dollar amount, e.g. 25.50")
        }
    }

    private fun optionalDate(id: Int): Long? =
        input(id).takeIf { it.isNotEmpty() }?.let { parseDate(it) }

    private fun requiredDate(id: Int): Long {
        val date = input(id)
        require(date.isNotBlank()) { "Enter a post date in YYYY-MM-DD format" }
        return parseDate(date)
    }

    private fun parseDate(raw: String): Long = try {
        startOfDay(LocalDate.parse(raw))
    } catch (e: Exception) {
        throw IllegalArgumentException("Use YYYY-MM-DD, e.g. 2026-11-20")
    }

    private fun startOfDay(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun formatDate(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    private fun cash(cents: Long): String =
        NumberFormat.getCurrencyInstance(Locale.US)
            .format(BigDecimal.valueOf(cents, 2))

    private fun showError(e: Exception) {
        Toast.makeText(this, e.message ?: "Operation failed", Toast.LENGTH_LONG).show()
    }
}
