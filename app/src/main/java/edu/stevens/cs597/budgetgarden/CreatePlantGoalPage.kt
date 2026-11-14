package edu.stevens.cs597.budgetgarden

import android.app.DatePickerDialog
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class CreatePlantGoalPage(
    private val root: View,
    private val gardenApp: GardenBudgetApp,
    private val scope: CoroutineScope
) {
    fun bind(onNavigate: (String) -> Unit) {
        val nameInput = root.findViewById<EditText>(R.id.goal_name_input)
        val amountInput = root.findViewById<EditText>(R.id.goal_amount_input)
        val descriptionInput =
            root.findViewById<EditText>(R.id.goal_description_input)
        val dateButton = root.findViewById<Button>(R.id.goal_date_button)
        val saveButton = root.findViewById<Button>(R.id.save_goal_button)
        val cancelButton = root.findViewById<Button>(R.id.cancel_goal_button)

        var selectedDate: LocalDate? = null
        var isSaving = false

        fun setSaving(saving: Boolean) {
            isSaving = saving
            listOf(
                nameInput, amountInput, descriptionInput,
                dateButton, saveButton, cancelButton
            ).forEach { it.isEnabled = !saving }

            saveButton.setText(
                if (saving) R.string.saving_goal else R.string.save_goal
            )
        }

        dateButton.setOnClickListener {
            val initialDate = selectedDate ?: LocalDate.now()

            DatePickerDialog(
                root.context,
                { _, year, month, day ->
                    val date = LocalDate.of(year, month + 1, day)
                    selectedDate = date
                    dateButton.text = date.toString()
                    dateButton.error = null
                },
                initialDate.year,
                initialDate.monthValue - 1,
                initialDate.dayOfMonth
            ).apply {
                datePicker.minDate = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.show()
        }

        cancelButton.setOnClickListener {
            onNavigate("home")
        }

        saveButton.setOnClickListener {
            if (isSaving) return@setOnClickListener

            nameInput.error = null
            amountInput.error = null
            dateButton.error = null

            val name = nameInput.text.toString().trim()
            val cents = parseCents(amountInput.text.toString().trim())
            val date = selectedDate

            if (name.isBlank()) {
                nameInput.error =
                    root.context.getString(R.string.goal_name_required)
                nameInput.requestFocus()
                return@setOnClickListener
            }

            if (cents == null) {
                amountInput.error =
                    root.context.getString(R.string.goal_amount_invalid)
                amountInput.requestFocus()
                return@setOnClickListener
            }

            if (date == null) {
                dateButton.error =
                    root.context.getString(R.string.goal_date_required)
                return@setOnClickListener
            }

            if (date.isBefore(LocalDate.now())) {
                dateButton.error =
                    root.context.getString(R.string.goal_date_past)
                return@setOnClickListener
            }

            val description = descriptionInput.text.toString().trim()
            val deadline = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            setSaving(true)

            scope.launch {
                try {
                    gardenApp.awaitDatabaseReady()
                    gardenApp.repository.createBudget(
                        name = name,
                        type = "SAVINGS",
                        frequency = "ONE_TIME",
                        goalAmount = cents,
                        neededDate = deadline,
                        description = description,
                        icon = "plant_default"
                    )

                    Toast.makeText(
                        root.context,
                        R.string.goal_created,
                        Toast.LENGTH_SHORT
                    ).show()

                    onNavigate("home")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("CreatePlantGoal", "Could not save goal", e)

                    Toast.makeText(
                        root.context,
                        R.string.goal_save_failed,
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    setSaving(false)
                }
            }
        }
    }

    private fun parseCents(raw: String): Long? {
        if (!raw.matches(Regex("""[0-9]+(\.[0-9]{1,2})?"""))) {
            return null
        }

        return try {
            BigDecimal(raw).movePointRight(2).longValueExact()
                .takeIf { it > 0L }
        } catch (_: ArithmeticException) {
            null
        }
    }
}
