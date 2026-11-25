package edu.bu.reptrack.ui.plan

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.domain.exercise.SwapSuggestions
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.testing.FakeExerciseRepository
import edu.bu.reptrack.testing.FakePlanRepository
import edu.bu.reptrack.testing.FakeProfileRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import edu.bu.reptrack.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisePickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun exercise(id: Int, name: String, vararg muscles: String) =
        Exercise(id, name, "", "Chest", muscles.toList(), listOf("Barbell"), null)

    private val plan = FakePlanRepository()
    private val profiles = FakeProfileRepository()
    private val catalog = FakeExerciseRepository(
        saved = listOf(
            exercise(1, "Bench Press", "Chest", "Triceps"),
            exercise(2, "Incline Bench Press", "Chest"),
            exercise(3, "Dips", "Chest", "Triceps"),
            exercise(4, "Pull-Ups", "Lats"),
        ),
    )

    // Upper: Bench press, Barbell row, Overhead press, Lat pulldown, Biceps curl.
    private fun TestScope.open(replaceIndex: Int = -1, routineId: String = "upper"): ExercisePickerViewModel =
        start(mapOf(Routes.ARG_ROUTINE_ID to routineId, Routes.ARG_REPLACE_INDEX to replaceIndex))

    /** Opened from a workout, to swap [exercise] there. [original] is the routine's own exercise in its place. */
    private fun TestScope.openFromWorkout(exercise: String, original: String? = null, routineId: String = "upper"): ExercisePickerViewModel =
        start(mapOf(Routes.ARG_ROUTINE_ID to routineId, Routes.ARG_EXERCISE_NAME to exercise, Routes.ARG_ORIGINAL to original))

    private fun TestScope.start(args: Map<String, Any?>): ExercisePickerViewModel {
        val viewModel = ExercisePickerViewModel(plan, catalog, profiles, SavedStateHandle(args))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun adding_listsTheCatalog_withoutWhatIsAlreadyInTheRoutine() = runTest {
        val state = open().uiState.value

        assertNull(state.replacing)
        assertEquals(listOf("Dips", "Incline Bench Press", "Pull-Ups"), state.results.map { it.name }.sorted())
    }

    @Test
    fun pick_addsAtTheEnd_andCloses() = runTest {
        val viewModel = open()

        viewModel.pick("Pull-Ups")

        assertEquals("Pull-Ups", plan.edits.value.getValue("upper").last())
        assertEquals(6, plan.edits.value.getValue("upper").size)
        assertTrue(viewModel.uiState.value.done)
    }

    @Test
    fun swapping_suggestsOurPicksThenTheSameMuscles_andKeepsThePlace() = runTest {
        val viewModel = open(replaceIndex = 0)

        val state = viewModel.uiState.value
        assertEquals("Bench press", state.replacing)
        assertEquals(TrainingPlace.GYM, state.place)
        assertEquals(
            listOf("Dumbbell bench press", "Machine chest press", "Dumbbell floor press", "Push-up", "Dips", "Incline Bench Press"),
            state.suggestions.map { it.name },
        )
        // What is suggested is not listed again below.
        assertEquals(listOf("Pull-Ups"), state.results.map { it.name })
        assertFalse(state.canKeep)

        viewModel.pick("Dips")

        val saved = plan.edits.value.getValue("upper")
        assertEquals("Dips", saved[0])
        assertEquals(listOf("Barbell row", "Overhead press", "Lat pulldown", "Biceps curl"), saved.drop(1))
    }

    @Test
    fun aHomeRoutine_onlySuggestsWhatNeedsNoGym() = runTest {
        // Bodyweight circuit A: Push-up, Bodyweight squat, Glute bridge, Plank.
        val state = open(replaceIndex = 0, routineId = "bodyweight_a").uiState.value

        assertEquals(TrainingPlace.HOME, state.place)
        assertEquals("Push-up", state.replacing)
        assertTrue(state.suggestions.isNotEmpty())
        assertTrue(state.suggestions.all { SwapSuggestions.fitsHome(it.equipment) })
        // The catalog's barbell exercises are left out.
        assertFalse(state.suggestions.any { it.name == "Incline Bench Press" })
    }

    @Test
    fun ownRoutine_usesWhereTheUserTrains() = runTest {
        plan.ownRoutines.value = listOf("custom_1" to "Push day")
        plan.setRoutineExercises("custom_1", listOf("Bench press", "Dips"))
        profiles.saveProfile(UserProfile(178f, 80f, 75f, FitnessGoal.STAY_FIT, TrainingPlace.HOME))

        val state = open(replaceIndex = 0, routineId = "custom_1").uiState.value

        assertEquals(TrainingPlace.HOME, state.place)
        assertEquals(listOf("Dumbbell floor press", "Push-up"), state.suggestions.map { it.name })
    }

    @Test
    fun searching_hidesTheSuggestions() = runTest {
        val viewModel = open(replaceIndex = 0)

        viewModel.onQueryChange("dip")

        assertTrue(viewModel.uiState.value.suggestions.isEmpty())
        assertEquals(listOf("Dips"), viewModel.uiState.value.results.map { it.name })
    }

    @Test
    fun duringAWorkout_theSwapIsForThatWorkoutOnly() = runTest {
        val viewModel = openFromWorkout("Bench press")

        val state = viewModel.uiState.value
        assertEquals("Swapping", "Bench press", state.replacing)
        assertTrue(state.canKeep)
        assertFalse(state.keep)

        viewModel.pick("Push-up")

        assertEquals("Bench press" to "Push-up", viewModel.uiState.value.swapped)
        assertTrue(viewModel.uiState.value.done)
        // The routine is as it was.
        assertNull(plan.edits.value["upper"])
    }

    @Test
    fun duringAWorkout_keepForNextTime_changesTheRoutineToo() = runTest {
        val viewModel = openFromWorkout("Barbell row")

        viewModel.onKeepChange(true)
        assertTrue(viewModel.uiState.value.keep)
        viewModel.pick("Dumbbell row")

        assertEquals(
            listOf("Bench press", "Dumbbell row", "Overhead press", "Lat pulldown", "Biceps curl"),
            plan.edits.value["upper"],
        )
        assertEquals("Barbell row" to "Dumbbell row", viewModel.uiState.value.swapped)
        assertTrue(viewModel.uiState.value.done)
    }

    @Test
    fun duringAWorkout_keepThatFails_staysOpen() = runTest {
        val viewModel = openFromWorkout("Barbell row")
        plan.failWrites = true

        viewModel.onKeepChange(true)
        viewModel.pick("Dumbbell row")

        assertTrue(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.done)
        assertNull(viewModel.uiState.value.swapped)
    }

    @Test
    fun duringAWorkout_aSwapFromEarlier_offersTheRoutinesOwnFirst_andKeepsInItsPlace() = runTest {
        // Bench press was swapped for Dumbbell bench press earlier in this workout.
        val viewModel = openFromWorkout("Dumbbell bench press", original = "Bench press")

        val state = viewModel.uiState.value
        assertEquals("Dumbbell bench press", state.replacing)
        assertEquals("Bench press", state.suggestions.first().name)
        assertTrue(state.canKeep)

        viewModel.onKeepChange(true)
        viewModel.pick("Push-up")

        assertEquals("Dumbbell bench press" to "Push-up", viewModel.uiState.value.swapped)
        assertEquals("Push-up", plan.edits.value.getValue("upper").first())
    }

    @Test
    fun duringAWorkout_anExerciseNotInTheRoutine_cannotBeKept() = runTest {
        val viewModel = openFromWorkout("Face pull")

        assertFalse(viewModel.uiState.value.canKeep)
        viewModel.onKeepChange(true)
        viewModel.pick("Dips")

        assertEquals("Face pull" to "Dips", viewModel.uiState.value.swapped)
        assertNull(plan.edits.value["upper"])
    }

    @Test
    fun search_filters_andOffersTheTypedName() = runTest {
        val viewModel = open()

        viewModel.onQueryChange("incl")
        assertEquals(listOf("Incline Bench Press"), viewModel.uiState.value.results.map { it.name })
        assertEquals("incl", viewModel.uiState.value.customName)

        viewModel.onQueryChange("pull-ups")
        assertNull(viewModel.uiState.value.customName)
    }

    @Test
    fun ownExercise_canBeAdded_evenWithoutTheCatalog() = runTest {
        val viewModel = ExercisePickerViewModel(
            plan,
            FakeExerciseRepository(),
            profiles,
            SavedStateHandle(mapOf(Routes.ARG_ROUTINE_ID to "upper", Routes.ARG_REPLACE_INDEX to -1)),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.catalogEmpty)

        viewModel.onQueryChange("Face pull")
        viewModel.pick(viewModel.uiState.value.customName!!)

        assertEquals("Face pull", plan.edits.value.getValue("upper").last())
    }

    @Test
    fun saveFailure_staysOpen_andSaysSo() = runTest {
        val viewModel = open()
        plan.failWrites = true

        viewModel.pick("Dips")

        assertTrue(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.done)
    }

    @Test
    fun addingToAnEmptyOwnRoutine() = runTest {
        plan.ownRoutines.value = listOf("custom_1" to "Push day")
        val savedState = SavedStateHandle(mapOf(Routes.ARG_ROUTINE_ID to "custom_1", Routes.ARG_REPLACE_INDEX to -1))
        val viewModel = ExercisePickerViewModel(plan, catalog, profiles, savedState)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals("Push day", viewModel.uiState.value.routineName)
        assertEquals(4, viewModel.uiState.value.results.size)

        viewModel.pick("Dips")

        assertEquals(listOf("Dips"), plan.edits.value["custom_1"])
        assertTrue(viewModel.uiState.value.done)
    }
}
