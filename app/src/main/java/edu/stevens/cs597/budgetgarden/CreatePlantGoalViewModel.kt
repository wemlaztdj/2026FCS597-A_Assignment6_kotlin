package edu.stevens.cs597.budgetgarden

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class GoalSaveState { IDLE, SAVING, SAVED, FAILED }

class CreatePlantGoalViewModel(
    application: Application,
    private val savedState: SavedStateHandle
) : AndroidViewModel(application) {
    private val gardenApp = application as GardenBudgetApp
    private val mutableSaveState = MutableStateFlow(GoalSaveState.IDLE)
    val saveState = mutableSaveState.asStateFlow()
    val isSaving: Boolean
        get() = saveState.value == GoalSaveState.SAVING

    var name: String
        get() = savedState["name"] ?: ""
        set(value) { savedState["name"] = value }

    var amount: String
        get() = savedState["amount"] ?: ""
        set(value) { savedState["amount"] = value }

    var description: String
        get() = savedState["description"] ?: ""
        set(value) { savedState["description"] = value }

    var date: LocalDate?
        get() = savedState.get<String>("date")?.let { LocalDate.parse(it) }
        set(value) { savedState["date"] = value?.toString() }

    var plantKey: String
        get() = savedState["plant"] ?: PlantCatalog.choices.first().key
        set(value) { savedState["plant"] = value }

    fun resetDraft() {
        if (isSaving) return
        name = ""
        amount = ""
        description = ""
        date = null
        plantKey = PlantCatalog.choices.first().key
        mutableSaveState.value = GoalSaveState.IDLE
    }

    fun acknowledgeFailure() {
        if (saveState.value == GoalSaveState.FAILED) {
            mutableSaveState.value = GoalSaveState.IDLE
        }
    }

    fun saveGoal(
        name: String,
        cents: Long,
        date: LocalDate,
        description: String,
        plantKey: String
    ) {
        if (isSaving || saveState.value == GoalSaveState.SAVED) return
        mutableSaveState.value = GoalSaveState.SAVING

        // The operation belongs to the retained model, so rotation cannot cancel it.
        viewModelScope.launch {
            try {
                gardenApp.awaitDatabaseReady()
                gardenApp.repository.createBudget(
                    name = name,
                    type = "SAVINGS",
                    frequency = "ONE_TIME",
                    goalAmount = cents,
                    neededDate = date.atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli(),
                    description = description,
                    icon = plantKey
                )
                mutableSaveState.value = GoalSaveState.SAVED
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CreatePlantGoal", "Could not save goal", e)
                mutableSaveState.value = GoalSaveState.FAILED
            }
        }
    }
}
