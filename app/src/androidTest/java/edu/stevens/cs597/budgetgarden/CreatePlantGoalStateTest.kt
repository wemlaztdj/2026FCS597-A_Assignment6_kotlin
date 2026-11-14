package edu.stevens.cs597.budgetgarden

import android.view.View
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CreatePlantGoalStateTest {
    @Test
    fun unfinishedGoalSurvivesRecreation() {
        val date = LocalDate.now().plusMonths(1)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fillDraft(scenario, date)
            scenario.recreate()

            scenario.onActivity { activity ->
                assertEquals(
                    "New laptop",
                    activity.findViewById<EditText>(R.id.goal_name_input).text.toString()
                )
                assertEquals(
                    "1234.56",
                    activity.findViewById<EditText>(R.id.goal_amount_input).text.toString()
                )
                assertEquals(
                    "For school",
                    activity.findViewById<EditText>(R.id.goal_description_input).text.toString()
                )
                assertEquals(
                    date.toString(),
                    activity.findViewById<TextView>(R.id.goal_date_button).text.toString()
                )
                val plant = activity.getString(R.string.orange_flower_description)
                assertEquals(
                    plant,
                    activity.findViewById<TextView>(R.id.plant_name_text).text.toString()
                )
                assertEquals(
                    plant,
                    activity.findViewById<ImageView>(R.id.plant_preview).contentDescription
                )
            }
        }
    }

    @Test
    fun cancelDiscardsTheRestoredDraft() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fillDraft(scenario, LocalDate.now().plusMonths(1))
            scenario.recreate()

            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.cancel_goal_button).performClick()
                activity.findViewById<View>(R.id.create_goal_button).performClick()

                listOf(
                    R.id.goal_name_input, R.id.goal_amount_input, R.id.goal_description_input
                ).forEach { id ->
                    assertEquals("", activity.findViewById<EditText>(id).text.toString())
                }
                assertEquals(
                    activity.getString(R.string.goal_date_prompt),
                    activity.findViewById<TextView>(R.id.goal_date_button).text.toString()
                )
                assertEquals(
                    activity.getString(R.string.yellow_flower_description),
                    activity.findViewById<TextView>(R.id.plant_name_text).text.toString()
                )
            }
        }
    }

    private fun fillDraft(scenario: ActivityScenario<MainActivity>, date: LocalDate) {
        scenario.onActivity { activity ->
            activity.findViewById<View>(R.id.create_goal_button).performClick()
            activity.findViewById<EditText>(R.id.goal_name_input).setText("New laptop")
            activity.findViewById<EditText>(R.id.goal_amount_input).setText("1234.56")
            activity.findViewById<EditText>(R.id.goal_description_input).setText("For school")
            repeat(2) { activity.findViewById<View>(R.id.next_plant_button).performClick() }
            activity.findViewById<View>(R.id.goal_date_button).performClick()
        }

        onView(isAssignableFrom(DatePicker::class.java)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> =
                isAssignableFrom(DatePicker::class.java)

            override fun getDescription() = "Choose the goal target date"

            override fun perform(uiController: UiController, view: View) {
                (view as DatePicker).updateDate(date.year, date.monthValue - 1, date.dayOfMonth)
                uiController.loopMainThreadUntilIdle()
            }
        })
        onView(withId(android.R.id.button1)).perform(click())
    }
}
