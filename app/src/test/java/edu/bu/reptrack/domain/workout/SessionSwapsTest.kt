package edu.bu.reptrack.domain.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSwapsTest {

    private val routine = listOf("Squat", "Bench press", "Barbell row", "Plank")

    @Test
    fun aSwap_takesTheOldExercisesPlace() {
        val shown = SessionSwaps.apply(routine, mapOf("Bench press" to "Push-up"))

        assertEquals(listOf("Squat", "Push-up", "Barbell row", "Plank"), shown)
    }

    @Test
    fun anExerciseWithSets_staysJustBeforeTheNewOne() {
        val shown = SessionSwaps.apply(routine, mapOf("Bench press" to "Push-up"), logged = setOf("Bench press"))

        assertEquals(listOf("Squat", "Bench press", "Push-up", "Barbell row", "Plank"), shown)
    }

    @Test
    fun swappingASwap_keepsOneEntryForTheRoutinesExercise() {
        val first = SessionSwaps.record(emptyMap(), "Bench press", "Push-up", routine)
        val second = SessionSwaps.record(first, "Push-up", "Dips", SessionSwaps.apply(routine, first))

        assertEquals(mapOf("Bench press" to "Dips"), second)
    }

    @Test
    fun swappingBack_removesTheSwap() {
        val swaps = mapOf("Bench press" to "Push-up")

        assertEquals(emptyMap<String, String>(), SessionSwaps.record(swaps, "Push-up", "bench press", SessionSwaps.apply(routine, swaps)))
    }

    @Test
    fun anExerciseAlreadyOnTheScreen_orABlankName_changesNothing() {
        assertEquals(emptyMap<String, String>(), SessionSwaps.record(emptyMap(), "Bench press", "plank", routine))
        assertEquals(emptyMap<String, String>(), SessionSwaps.record(emptyMap(), "Bench press", "  ", routine))
    }

    @Test
    fun originalOf_findsTheRoutinesOwnExercise() {
        val swaps = mapOf("Bench press" to "Push-up")

        assertEquals("Bench press", SessionSwaps.originalOf(swaps, "Push-up"))
        assertEquals("Squat", SessionSwaps.originalOf(swaps, "Squat"))
    }

    @Test
    fun encodeAndDecode_roundTrip() {
        val swaps = mapOf("Bench press" to "Push-up", "Squat" to "Goblet squat")

        assertEquals(listOf("Bench press", "Push-up", "Squat", "Goblet squat"), SessionSwaps.encode(swaps))
        assertEquals(swaps, SessionSwaps.decode(SessionSwaps.encode(swaps)))
        assertEquals(emptyMap<String, String>(), SessionSwaps.decode(null))
        assertEquals(mapOf("a" to "b"), SessionSwaps.decode(listOf("a", "b", "orphan")))
    }
}
