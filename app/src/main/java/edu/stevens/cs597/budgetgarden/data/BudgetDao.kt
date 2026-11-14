package edu.stevens.cs597.budgetgarden.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// Result row for the test activity's transaction history.
data class TransactionRow(
    val id: Long,
    val fromName: String?,
    val toName: String?,
    val amount: Long,
    val postDate: Long,
    val memo: String
)

@Dao
interface BudgetDao {
    @Insert
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Query("SELECT * FROM budgets ORDER BY id")
    suspend fun getBudgets(): List<Budget>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getBudget(id: Long): Budget?

    @Query("SELECT COUNT(*) FROM budgets WHERE type = 'WALLET' AND is_archived = 0")
    suspend fun activeWalletCount(): Int

    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN to_budget_id = :budgetId THEN amount
                WHEN from_budget_id = :budgetId THEN -amount
                ELSE 0
            END
        ), 0)
        FROM transactions
        WHERE post_date <= :asOf
          AND (from_budget_id = :budgetId OR to_budget_id = :budgetId)
    """)
    suspend fun balanceAt(budgetId: Long, asOf: Long): Long

    // Only external money movements affect the total; internal transfers net to zero.
    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN from_budget_id IS NULL THEN amount
                WHEN to_budget_id IS NULL THEN -amount
                ELSE 0
            END
        ), 0)
        FROM transactions
        WHERE post_date <= :asOf
    """)
    suspend fun totalMoneyAt(asOf: Long): Long

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE from_budget_id = :budgetId
          AND to_budget_id IS NULL
          AND post_date >= :fromInclusive
          AND post_date < :toExclusive
    """)
    suspend fun spentBetween(
        budgetId: Long,
        fromInclusive: Long,
        toExclusive: Long
    ): Long

    @Insert
    suspend fun insertTransaction(transaction: BudgetTransaction): Long

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransaction(id: Long): BudgetTransaction?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM transactions
            WHERE post_date > :now
              AND (from_budget_id = :budgetId OR to_budget_id = :budgetId)
        )
    """)
    suspend fun hasFutureTransactions(budgetId: Long, now: Long): Boolean

    @Query("SELECT DISTINCT post_date FROM transactions ORDER BY post_date")
    suspend fun allPostDates(): List<Long>

    @Query("""
        SELECT t.id AS id,
               fb.name AS fromName,
               tb.name AS toName,
               t.amount AS amount,
               t.post_date AS postDate,
               t.memo AS memo
        FROM transactions t
        LEFT JOIN budgets fb ON fb.id = t.from_budget_id
        LEFT JOIN budgets tb ON tb.id = t.to_budget_id
        ORDER BY t.post_date DESC, t.id DESC
        LIMIT 100
    """)
    suspend fun recentTransactions(): List<TransactionRow>
}
