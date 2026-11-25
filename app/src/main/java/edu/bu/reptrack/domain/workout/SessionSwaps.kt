package edu.bu.reptrack.domain.workout

/**
 * Exercises swapped for this workout only, as the routine's exercise to the one done instead.
 * The routine itself stays as it was, so next time it starts the usual way again.
 * Plain Kotlin so it is easy to test.
 */
object SessionSwaps {

    /**
     * The routine's [exercises] with the swaps in their place, in the same order. An exercise
     * that already has [logged] sets stays on the screen with its sets, just before the new one.
     */
    fun apply(exercises: List<String>, swaps: Map<String, String>, logged: Set<String> = emptySet()): List<String> =
        exercises.flatMap { name ->
            val instead = swaps[name]
            when {
                instead == null -> listOf(name)
                name in logged -> listOf(name, instead)
                else -> listOf(instead)
            }
        }.distinct()

    /**
     * The swaps after [from] is swapped for [to]. If [from] was itself swapped in, its routine
     * exercise now maps to [to]; swapping back to the routine's own exercise removes the swap.
     * Nothing changes when [to] is blank or already on the screen ([shown]).
     */
    fun record(swaps: Map<String, String>, from: String, to: String, shown: List<String>): Map<String, String> {
        val name = to.trim()
        if (name.isEmpty() || shown.any { it.equals(name, ignoreCase = true) }) return swaps
        val original = originalOf(swaps, from)
        return if (original.equals(name, ignoreCase = true)) swaps - original else swaps + (original to name)
    }

    /** The routine's own exercise that [name] stands in for, or [name] when it was not swapped in. */
    fun originalOf(swaps: Map<String, String>, name: String): String =
        swaps.entries.firstOrNull { it.value == name }?.key ?: name

    /** As a flat list (routine exercise, then the one instead), the shape SavedStateHandle can keep. */
    fun encode(swaps: Map<String, String>): ArrayList<String> =
        swaps.flatMapTo(ArrayList()) { (from, to) -> listOf(from, to) }

    fun decode(flat: List<String>?): Map<String, String> =
        flat.orEmpty().chunked(2).filter { it.size == 2 }.associate { it[0] to it[1] }
}
