package edu.stevens.cs597.budgetgarden

import android.app.DatePickerDialog
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class CreatePlantGoalPage(
    private val root: View,
    private val viewModel: CreatePlantGoalViewModel,
    private val scope: CoroutineScope,
    private val lifecycle: Lifecycle
) {
    fun bind(onNavigate: (String) -> Unit) {
        val nameInput = root.findViewById<EditText>(R.id.goal_name_input)
        val amountInput = root.findViewById<EditText>(R.id.goal_amount_input)
        val descriptionInput =
            root.findViewById<EditText>(R.id.goal_description_input)
        val dateButton = root.findViewById<Button>(R.id.goal_date_button)
        val saveButton = root.findViewById<Button>(R.id.save_goal_button)
        val cancelButton = root.findViewById<Button>(R.id.cancel_goal_button)
        val previousPlantButton = root.findViewById<Button>(R.id.previous_plant_button)
        val nextPlantButton = root.findViewById<Button>(R.id.next_plant_button)
        val plantPreview = root.findViewById<ImageView>(R.id.plant_preview)
        val plantName = root.findViewById<TextView>(R.id.plant_name_text)

        nameInput.setText(viewModel.name)
        amountInput.setText(viewModel.amount)
        descriptionInput.setText(viewModel.description)
        nameInput.doAfterTextChanged { viewModel.name = it.toString() }
        amountInput.doAfterTextChanged { viewModel.amount = it.toString() }
        descriptionInput.doAfterTextChanged { viewModel.description = it.toString() }
        viewModel.date?.let { dateButton.text = it.toString() }

        var selectedPlantIndex = PlantCatalog.choices.indexOf(
            PlantCatalog.fromKey(viewModel.plantKey)
        )

        fun showSelectedPlant() {
            val plant = PlantCatalog.choices[selectedPlantIndex]
            viewModel.plantKey = plant.key
            plantPreview.setImageResource(plant.drawableRes)
            plantPreview.contentDescription = root.context.getString(plant.labelRes)
            plantName.setText(plant.labelRes)
        }

        previousPlantButton.setOnClickListener {
            selectedPlantIndex =
                (selectedPlantIndex + PlantCatalog.choices.size - 1) % PlantCatalog.choices.size
            showSelectedPlant()
        }
        nextPlantButton.setOnClickListener {
            selectedPlantIndex = (selectedPlantIndex + 1) % PlantCatalog.choices.size
            showSelectedPlant()
        }
        showSelectedPlant()

        fun setSaving(saving: Boolean) {
            listOf(
                nameInput, amountInput, descriptionInput,
                dateButton, saveButton, cancelButton,
                previousPlantButton, nextPlantButton
            ).forEach { it.isEnabled = !saving }

            saveButton.setText(
                if (saving) R.string.saving_goal else R.string.save_goal
            )
        }
        setSaving(viewModel.isSaving)

        scope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveState.collect { state ->
                    setSaving(state == GoalSaveState.SAVING)
                    when (state) {
                        GoalSaveState.SAVED -> {
                            Toast.makeText(
                                root.context, R.string.goal_created, Toast.LENGTH_SHORT
                            ).show()
                            onNavigate("garden")
                        }
                        GoalSaveState.FAILED -> {
                            Toast.makeText(
                                root.context, R.string.goal_save_failed, Toast.LENGTH_LONG
                            ).show()
                            viewModel.acknowledgeFailure()
                        }
                        else -> Unit
                    }
                }
            }
        }

        dateButton.setOnClickListener {
            val initialDate = viewModel.date ?: LocalDate.now()

            DatePickerDialog(
                root.context,
                { _, year, month, day ->
                    val date = LocalDate.of(year, month + 1, day)
                    viewModel.date = date
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
            if (viewModel.isSaving) return@setOnClickListener

            nameInput.error = null
            amountInput.error = null
            dateButton.error = null

            val name = nameInput.text.toString().trim()
            val cents = parseCents(amountInput.text.toString().trim())
            val date = viewModel.date

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
            val plantKey = PlantCatalog.choices[selectedPlantIndex].key
            viewModel.saveGoal(name, cents, date, description, plantKey)
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
