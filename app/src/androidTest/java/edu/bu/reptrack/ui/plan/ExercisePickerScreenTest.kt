package edu.bu.reptrack.ui.plan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.domain.exercise.SwapSuggestion
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the catalog picker, drawn from a fixed state. */
@RunWith(AndroidJUnit4::class)
class ExercisePickerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var picked: String? = null
    private var typed: String? = null
    private var keep: Boolean? = null

    private val dips = Exercise(3, "Dips", "", "Chest", listOf("Chest", "Triceps"), emptyList(), null)
    private val incline = Exercise(2, "Incline Bench Press", "", "Chest", listOf("Chest"), listOf("Barbell"), null)
    private val dumbbellBench = SwapSuggestion("Dumbbell bench press", listOf("Chest", "Triceps"), listOf("Dumbbell", "Bench"))
    private val pushUp = SwapSuggestion("Push-up", listOf("Chest", "Triceps"), emptyList())

    private fun show(state: ExercisePickerUiState) {
        composeRule.setContent {
            RepTrackTheme {
                ExercisePickerScreen(
                    state = state,
                    onBack = {},
                    onQueryChange = { typed = it },
                    onPick = { picked = it },
                    onKeepChange = { keep = it },
                )
            }
        }
    }

    @Test
    fun swapping_showsTheSuggestionsFirst_withWhyTheyFit() {
        show(
            ExercisePickerUiState(
                isLoading = false,
                routineName = "Upper body",
                replacing = "Bench press",
                suggestions = listOf(dumbbellBench, pushUp),
                results = listOf(incline),
            ),
        )

        composeRule.onNodeWithTag("picker_title").assertTextEquals("Swap Bench press")
        composeRule.onNodeWithText("Suggested for the gym", ignoreCase = true).assertIsDisplayed()
        composeRule.onNodeWithText("Chest, Triceps · Dumbbell, Bench").assertIsDisplayed()
        composeRule.onNodeWithText("Chest, Triceps · Bodyweight").assertIsDisplayed()
        // Only a swap during a workout can be kept for next time.
        composeRule.onNodeWithTag("picker_keep").assertDoesNotExist()
        composeRule.onNodeWithTag("picker_suggestion_1").performClick()

        assertEquals("Push-up", picked)
    }

    @Test
    fun atHome_saysSo() {
        show(ExercisePickerUiState(isLoading = false, replacing = "Push-up", suggestions = listOf(pushUp.copy(name = "Knee push-up")), place = TrainingPlace.HOME))

        composeRule.onNodeWithText("Suggested for home", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun duringAWorkout_keepForNextTime_isOffUntilTicked() {
        show(ExercisePickerUiState(isLoading = false, routineName = "Upper body", replacing = "Bench press", suggestions = listOf(pushUp), canKeep = true))

        composeRule.onNodeWithTag("picker_keep").assertIsOff()
        composeRule.onNodeWithText("Only for this workout").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_keep").performClick()

        assertEquals(true, keep)
        assertNull(picked)
    }

    @Test
    fun duringAWorkout_keepTicked_saysWhichRoutineChanges() {
        show(ExercisePickerUiState(isLoading = false, routineName = "Upper body", replacing = "Bench press", canKeep = true, keep = true))

        composeRule.onNodeWithTag("picker_keep").assertIsOn()
        composeRule.onNodeWithText("Also changes Upper body").assertIsDisplayed()
    }

    @Test
    fun adding_offersTheTypedName_asTheUsersOwn() {
        show(ExercisePickerUiState(isLoading = false, routineName = "Upper body", query = "Face pull", customName = "Face pull"))

        composeRule.onNodeWithTag("picker_title").assertTextEquals("Add to Upper body")
        composeRule.onNodeWithTag("picker_custom").performClick()

        assertEquals("Face pull", picked)
    }

    @Test
    fun typing_reportsTheSearch() {
        show(ExercisePickerUiState(isLoading = false, routineName = "Upper body"))

        composeRule.onNodeWithTag("picker_search").performTextInput("row")

        assertEquals("row", typed)
    }

    @Test
    fun withoutTheCatalog_saysHowToGetIt() {
        show(ExercisePickerUiState(isLoading = false, routineName = "Upper body", catalogEmpty = true))
        composeRule.onNodeWithTag("picker_catalog_empty").assertIsDisplayed()
    }
}
