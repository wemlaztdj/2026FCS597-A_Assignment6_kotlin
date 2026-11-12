package edu.stevens.cs597.budgetgarden.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Budget::class,
            parentColumns = ["id"],
            childColumns = ["from_budget_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Budget::class,
            parentColumns = ["id"],
            childColumns = ["to_budget_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("from_budget_id"),
        Index("to_budget_id"),
        Index("post_date")
    ]
)
data class BudgetTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "from_budget_id") val fromBudgetId: Long? = null,
    @ColumnInfo(name = "to_budget_id") val toBudgetId: Long? = null,
    val amount: Long,              // Positive cents; direction is from -> to
    @ColumnInfo(name = "post_date") val postDate: Long,
    val memo: String = ""
) {
    init {
        require(fromBudgetId != null || toBudgetId != null) {
            "Source and destination cannot both be null"
        }

        require(fromBudgetId == null ||
                toBudgetId == null ||
                fromBudgetId != toBudgetId) {
            "Source and destination cannot be the same"
        }
    }
}
