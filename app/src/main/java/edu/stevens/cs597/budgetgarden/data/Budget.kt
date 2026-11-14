package edu.stevens.cs597.budgetgarden.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,              // WALLET, SPENDING, SAVINGS
    val frequency: String,         // NONE, MONTHLY, ONE_TIME
    @ColumnInfo(name = "goal_amount") val goalAmount: Long = 0,
    @ColumnInfo(name = "needed_date") val neededDate: Long? = null,
    val description: String = "",
    val icon: String = "plant_default",
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false
)
