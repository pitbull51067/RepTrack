package edu.bu.reptrack.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun howTo_encodesTheName() {
        assertEquals("how_to/Single-leg%20glute%20bridge", Routes.howTo("Single-leg glute bridge"))
    }

    @Test
    fun swapPick_carriesTheNameAndTheRoutinesOwnExercise() {
        assertEquals(
            "swap_pick/upper/Dumbbell%20bench%20press?original=Bench%20press",
            Routes.swapPick("upper", "Dumbbell bench press", "Bench press"),
        )
        assertEquals("swap_pick/full_body_a/Squat?original=Squat", Routes.swapPick("full_body_a", "Squat"))
    }
}
