package edu.bu.reptrack.domain.workout

import edu.bu.reptrack.domain.profile.PlanType
import edu.bu.reptrack.domain.profile.TrainingPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterRoutinesTest {

    private val allRoutines = PlanType.entries.flatMap { StarterRoutines.forPlan(it) }

    @Test
    fun everyPlan_hasAtLeastOneRoutine() {
        for (type in PlanType.entries) {
            assertTrue("$type has no routines", StarterRoutines.forPlan(type).isNotEmpty())
        }
    }

    @Test
    fun everyRoutine_hasThreeOrMoreExercises() {
        for (routine in allRoutines) {
            assertTrue("${routine.id} is too short", routine.exercises.size >= 3)
        }
    }

    @Test
    fun routineIds_areUnique() {
        val ids = allRoutines.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun find_returnsTheRoutine_orNull() {
        assertEquals("Upper body", StarterRoutines.find("upper")?.name)
        assertNull(StarterRoutines.find("does_not_exist"))
    }

    @Test
    fun placeOf_gymPlansAndHomePlans() {
        assertEquals(TrainingPlace.GYM, StarterRoutines.placeOf("full_body_a"))
        assertEquals(TrainingPlace.GYM, StarterRoutines.placeOf("lower"))
        assertEquals(TrainingPlace.HOME, StarterRoutines.placeOf("bodyweight_b"))
        assertEquals(TrainingPlace.HOME, StarterRoutines.placeOf("home_pull"))
        assertNull(StarterRoutines.placeOf("custom_1"))
    }
}
