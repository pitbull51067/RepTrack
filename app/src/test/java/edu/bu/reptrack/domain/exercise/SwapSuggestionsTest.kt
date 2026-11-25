package edu.bu.reptrack.domain.exercise

import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.profile.PlanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwapSuggestionsTest {

    private fun exercise(id: Int, name: String, muscles: List<String>, equipment: List<String>) =
        Exercise(id, name, "", "Chest", muscles, equipment, null)

    private val catalog = listOf(
        exercise(1, "Bench Press", listOf("Chest", "Triceps"), listOf("Barbell", "Bench")),
        exercise(2, "Dips", listOf("Chest", "Triceps"), listOf("none (bodyweight exercise)")),
        exercise(3, "Incline Bench Press", listOf("Chest"), listOf("Barbell", "Incline bench")),
        exercise(4, "Push-Ups", listOf("Chest", "Triceps"), listOf("none (bodyweight exercise)")),
        exercise(5, "Pull-Ups", listOf("Lats"), listOf("Pull-up bar")),
        exercise(6, "Dumbbell Fly", listOf("Chest"), listOf("Dumbbell")),
    )

    @Test
    fun everyBuiltInExercise_hasItsOwnPicks() {
        val builtIn = PlanType.entries.flatMap { StarterRoutines.forPlan(it) }.flatMap { it.exercises }.distinct()

        builtIn.forEach { name ->
            assertTrue("$name has no picks", SwapSuggestions.handPicked(name).size >= 3)
        }
        assertEquals(builtIn.sorted(), SwapSuggestions.builtInNames.sorted())
    }

    @Test
    fun noPick_isTheExerciseItself() {
        SwapSuggestions.builtInNames.forEach { name ->
            val picks = SwapSuggestions.handPicked(name).map { CatalogMatch.squash(it.name) }
            assertFalse("$name suggests itself", CatalogMatch.squash(name) in picks)
            assertEquals("$name has a pick twice", picks.distinct(), picks)
        }
    }

    @Test
    fun withoutTheCatalog_ourPicksAreStillThere() {
        val suggestions = SwapSuggestions.suggest("Bench press", emptyList(), TrainingPlace.GYM)

        assertEquals(
            listOf("Dumbbell bench press", "Machine chest press", "Dumbbell floor press", "Push-up"),
            suggestions.map { it.name },
        )
        assertEquals(listOf("Chest", "Triceps"), suggestions.first().muscles)
        assertEquals(listOf("Dumbbell", "Bench"), suggestions.first().equipment)
    }

    @Test
    fun atTheGym_ourPicksComeFirst_thenTheCatalogForTheSameMuscles() {
        val names = SwapSuggestions.suggest("Bench press", catalog, TrainingPlace.GYM).map { it.name }

        // "Push-Ups" in the catalog is our "Push-up", so it is not there twice.
        assertEquals(
            listOf("Dumbbell bench press", "Machine chest press", "Dumbbell floor press", "Push-up", "Dips", "Dumbbell Fly", "Incline Bench Press"),
            names,
        )
    }

    @Test
    fun atHome_onlyWhatNeedsNoGym() {
        val suggestions = SwapSuggestions.suggest("Bench press", catalog, TrainingPlace.HOME)

        assertEquals(listOf("Dumbbell floor press", "Push-up", "Dips", "Dumbbell Fly"), suggestions.map { it.name })
        // wger's "none (bodyweight exercise)" shows as bodyweight.
        assertEquals(emptyList<String>(), suggestions.first { it.name == "Dips" }.equipment)
    }

    @Test
    fun namesAlreadyInTheWorkout_areLeftOut() {
        val names = SwapSuggestions.suggest("Squat", emptyList(), TrainingPlace.GYM, exclude = listOf("Leg press", "goblet SQUAT")).map { it.name }

        assertEquals(listOf("Split squat", "Bodyweight squat"), names)
    }

    @Test
    fun aPick_suggestsTheOriginalFirst_soSwappingBackIsOneTap() {
        val names = SwapSuggestions.handPicked("Dumbbell bench press").map { it.name }

        assertEquals("Bench press", names.first())
        assertFalse("Dumbbell bench press" in names)
    }

    @Test
    fun anExerciseWeDoNotKnow_usesTheCatalogOnly() {
        val names = SwapSuggestions.suggest("Dumbbell Fly", catalog, TrainingPlace.GYM).map { it.name }

        assertEquals(listOf("Bench Press", "Dips", "Incline Bench Press", "Push-Ups"), names)
        assertTrue(SwapSuggestions.suggest("Face pull", catalog, TrainingPlace.GYM).isEmpty())
    }

    @Test
    fun theListStaysShort() {
        val many = (1..20).map { exercise(100 + it, "Chest move $it", listOf("Chest", "Triceps"), emptyList()) }

        assertEquals(SwapSuggestions.LIMIT, SwapSuggestions.suggest("Bench press", catalog + many, TrainingPlace.GYM).size)
    }

    @Test
    fun homeEquipment() {
        assertTrue(SwapSuggestions.fitsHome(emptyList()))
        assertTrue(SwapSuggestions.fitsHome(listOf("none (bodyweight exercise)")))
        assertTrue(SwapSuggestions.fitsHome(listOf("Dumbbell", "Gym mat")))
        assertTrue(SwapSuggestions.fitsHome(listOf("Resistance band")))
        assertTrue(SwapSuggestions.fitsHome(listOf("Chair")))
        assertFalse(SwapSuggestions.fitsHome(listOf("Barbell")))
        assertFalse(SwapSuggestions.fitsHome(listOf("Dumbbell", "Bench")))
        assertFalse(SwapSuggestions.fitsHome(listOf("Machine")))
        assertFalse(SwapSuggestions.fitsHome(listOf("Adjustable bench")))
        assertFalse(SwapSuggestions.fitsHome(listOf("Pull-up bar")))
    }
}
