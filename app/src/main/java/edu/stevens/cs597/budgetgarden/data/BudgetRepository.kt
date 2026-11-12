package edu.stevens.cs597.budgetgarden.data

import androidx.room.withTransaction

/** Business rules for the two-table demo. All writes are atomic. */
class BudgetRepository(private val db: AppDatabase) {
    private val dao = db.budgetDao()

    suspend fun ensureFirstWallet() = db.withTransaction {
        if (dao.activeWalletCount() == 0) {
            dao.insertBudget(
                Budget(name = "Water Jug", type = "WALLET", frequency = "NONE", icon = "water_jug")
            )
        }
    }

    suspend fun createBudget(
        name: String,
        type: String,
        frequency: String,
        goalAmount: Long,
        neededDate: Long?,
        description: String,
        icon: String
    ) = db.withTransaction {
        require(name.isNotBlank()) { "Enter a budget name" }
        require(type in setOf("WALLET", "SPENDING", "SAVINGS")) { "Invalid budget type" }
        require(frequency in setOf("NONE", "MONTHLY", "ONE_TIME")) { "Invalid frequency" }
        require(goalAmount >= 0) { "Goal cannot be negative" }
        if (type == "WALLET") {
            require(frequency == "NONE" && goalAmount == 0L && neededDate == null) {
                "Wallets must have no frequency, goal, or deadline"
            }
        } else {
            require(frequency in setOf("MONTHLY", "ONE_TIME")) {
                "Plants need MONTHLY or ONE_TIME frequency"
            }
        }
        dao.insertBudget(
            Budget(
                name = name.trim(), type = type, frequency = frequency,
                goalAmount = goalAmount, neededDate = neededDate,
                description = description.trim(), icon = icon.trim().ifBlank { "plant_default" }
            )
        )
    }

    suspend fun addTransaction(
        fromId: Long?,
        toId: Long?,
        amount: Long,
        postDate: Long,
        memo: String
    ) = db.withTransaction {
        require(amount > 0L) { "Amount must be greater than $0" }
        require(fromId != null || toId != null) { "Select at least one budget" }
        require(fromId == null || toId == null || fromId != toId) {
            "Source and destination cannot be the same"
        }
        val from = fromId?.let { dao.getBudget(it) ?: error("Source budget not found") }
        val to = toId?.let { dao.getBudget(it) ?: error("Destination budget not found") }
        require(from?.isArchived != true && to?.isArchived != true) {
            "Archived budgets cannot have new transactions"
        }
        require(fromId != null || to?.type == "WALLET") {
            "External income must enter a wallet"
        }
        dao.insertTransaction(
            BudgetTransaction(
                fromBudgetId = fromId, toBudgetId = toId,
                amount = amount, postDate = postDate, memo = memo.trim()
            )
        )
        verifyBalances()
    }

    suspend fun deleteTransaction(id: Long) = db.withTransaction {
        val transaction = dao.getTransaction(id) ?: error("Transaction #$id not found")
        val affectedIds = listOfNotNull(transaction.fromBudgetId, transaction.toBudgetId)
        require(affectedIds.none { dao.getBudget(it)?.isArchived == true }) {
            "Cannot delete a transaction belonging to an archived budget"
        }
        dao.deleteTransaction(id)
        verifyBalances()
    }

    /** Move all remaining money back to wallet; keep the history, then archive. */
    suspend fun archiveBudget(budgetId: Long, walletId: Long) = db.withTransaction {
        val budget = dao.getBudget(budgetId) ?: error("Budget not found")
        val wallet = dao.getBudget(walletId) ?: error("Wallet not found")
        require(!budget.isArchived && budget.type != "WALLET") {
            "Only active plants can be archived"
        }
        require(wallet.type == "WALLET" && !wallet.isArchived) { "Choose an active wallet" }
        val now = System.currentTimeMillis()
        require(!dao.hasFutureTransactions(budgetId, now)) {
            "Delete or reschedule this budget's future transactions first"
        }
        val remaining = dao.balanceAt(budgetId, now)
        require(remaining >= 0) { "Resolve the negative balance before archiving" }
        if (remaining > 0) {
            dao.insertTransaction(
                BudgetTransaction(
                    fromBudgetId = budgetId,
                    toBudgetId = walletId,
                    amount = remaining,
                    postDate = now,
                    memo = "Archive: return balance from ${budget.name}"
                )
            )
        }
        dao.updateBudget(budget.copy(isArchived = true))
        verifyBalances()
    }

    /**
     * Check the running total and each plant after every posting date, including
     * future scheduled dates. Wallets may be negative; plants and total cannot.
     * Small-demo implementation: efficient enough for dozens/hundreds of rows.
     */
    private suspend fun verifyBalances() {
        val budgets = dao.getBudgets()
        val timestamps = dao.allPostDates()
        for (date in timestamps) {
            require(dao.totalMoneyAt(date) >= 0L) {
                "This change would make total money negative on a posting date"
            }
            for (budget in budgets) {
                if (budget.type != "WALLET") {
                    require(dao.balanceAt(budget.id, date) >= 0L) {
                        "${budget.name} would be negative on a posting date"
                    }
                }
            }
        }
    }
}
