package edu.bu.reptrack.domain.exercise

import edu.bu.reptrack.domain.profile.TrainingPlace

/**
 * One exercise to swap in, and why it fits: the muscles it works and what it needs. Empty
 * [equipment] means bodyweight. [exercise] is the catalog entry when the suggestion came from it.
 */
data class SwapSuggestion(
    val name: String,
    val muscles: List<String>,
    val equipment: List<String>,
    val exercise: Exercise? = null,
)

/**
 * What to do instead of an exercise: when the machine is taken, the gym is busy, or the user is
 * training at home. Our own picks for the built-in exercises come first, so there are good
 * choices even before the wger catalog is on the phone. Then come catalog exercises for the
 * same muscles. At home only exercises that need no gym equipment are suggested.
 * Plain Kotlin so it is easy to test.
 */
object SwapSuggestions {

    /** At most this many suggestions, so the list stays short enough to choose from. */
    const val LIMIT = 8

    private fun move(name: String, muscles: String, vararg equipment: String) =
        SwapSuggestion(name, muscles.split(", "), equipment.toList())

    /** Each built-in exercise, then the ones that can replace it, the closest first. */
    private val groups: List<Pair<SwapSuggestion, List<SwapSuggestion>>> = listOf(
        // ---- Gym plans ----
        move("Squat", "Quads, Glutes", "Barbell") to listOf(
            move("Goblet squat", "Quads, Glutes", "Dumbbell"),
            move("Leg press", "Quads, Glutes", "Machine"),
            move("Split squat", "Quads, Glutes"),
            move("Bodyweight squat", "Quads, Glutes"),
        ),
        move("Bench press", "Chest, Triceps", "Barbell", "Bench") to listOf(
            move("Dumbbell bench press", "Chest, Triceps", "Dumbbell", "Bench"),
            move("Machine chest press", "Chest, Triceps", "Machine"),
            move("Dumbbell floor press", "Chest, Triceps", "Dumbbell"),
            move("Push-up", "Chest, Triceps"),
        ),
        move("Barbell row", "Lats, Biceps", "Barbell") to listOf(
            move("Dumbbell row", "Lats, Biceps", "Dumbbell"),
            move("Seated cable row", "Lats, Biceps", "Cable"),
            move("Band row", "Lats, Biceps", "Resistance band"),
            move("Table row", "Lats, Biceps", "Table"),
        ),
        move("Plank", "Abs") to listOf(
            move("Dead bug", "Abs"),
            move("Hollow hold", "Abs"),
            move("Side plank", "Obliques, Abs"),
        ),
        move("Deadlift", "Hamstrings, Glutes", "Barbell") to listOf(
            move("Trap bar deadlift", "Quads, Glutes", "Trap bar"),
            move("Romanian deadlift", "Hamstrings, Glutes", "Barbell"),
            move("Dumbbell Romanian deadlift", "Hamstrings, Glutes", "Dumbbell"),
            move("Kettlebell swing", "Glutes, Hamstrings", "Kettlebell"),
        ),
        move("Overhead press", "Shoulders, Triceps", "Barbell") to listOf(
            move("Dumbbell shoulder press", "Shoulders, Triceps", "Dumbbell"),
            move("Machine shoulder press", "Shoulders, Triceps", "Machine"),
            move("Band overhead press", "Shoulders, Triceps", "Resistance band"),
            move("Pike push-up", "Shoulders, Triceps"),
        ),
        move("Lat pulldown", "Lats, Biceps", "Cable") to listOf(
            move("Pull-up", "Lats, Biceps", "Pull-up bar"),
            move("Assisted pull-up", "Lats, Biceps", "Machine"),
            move("Band pulldown", "Lats, Biceps", "Resistance band"),
            move("Table row", "Lats, Biceps", "Table"),
        ),
        move("Walking lunge", "Quads, Glutes") to listOf(
            move("Reverse lunge", "Quads, Glutes"),
            move("Dumbbell lunge", "Quads, Glutes", "Dumbbell"),
            move("Step-up", "Quads, Glutes", "Step"),
            move("Split squat", "Quads, Glutes"),
        ),
        move("Biceps curl", "Biceps", "Dumbbell") to listOf(
            move("Hammer curl", "Biceps, Forearms", "Dumbbell"),
            move("Cable curl", "Biceps", "Cable"),
            move("EZ-bar curl", "Biceps", "EZ bar"),
            move("Band curl", "Biceps", "Resistance band"),
        ),
        move("Romanian deadlift", "Hamstrings, Glutes", "Barbell") to listOf(
            move("Dumbbell Romanian deadlift", "Hamstrings, Glutes", "Dumbbell"),
            move("Lying leg curl", "Hamstrings", "Machine"),
            move("Single-leg Romanian deadlift", "Hamstrings, Glutes"),
            move("Glute bridge", "Glutes, Hamstrings"),
        ),
        move("Leg press", "Quads, Glutes", "Machine") to listOf(
            move("Hack squat", "Quads, Glutes", "Machine"),
            move("Goblet squat", "Quads, Glutes", "Dumbbell"),
            move("Leg extension", "Quads", "Machine"),
            move("Split squat", "Quads, Glutes"),
        ),
        move("Calf raise", "Calves", "Machine") to listOf(
            move("Seated calf raise", "Calves", "Machine"),
            move("Dumbbell calf raise", "Calves", "Dumbbell"),
            move("Single-leg calf raise", "Calves"),
        ),
        // ---- Home plans ----
        move("Push-up", "Chest, Triceps") to listOf(
            move("Incline push-up", "Chest, Triceps"),
            move("Knee push-up", "Chest, Triceps"),
            move("Decline push-up", "Chest, Shoulders"),
            move("Dumbbell floor press", "Chest, Triceps", "Dumbbell"),
        ),
        move("Bodyweight squat", "Quads, Glutes") to listOf(
            move("Goblet squat", "Quads, Glutes", "Dumbbell"),
            move("Split squat", "Quads, Glutes"),
            move("Jump squat", "Quads, Glutes"),
            move("Wall sit", "Quads"),
        ),
        move("Glute bridge", "Glutes, Hamstrings") to listOf(
            move("Single-leg glute bridge", "Glutes, Hamstrings"),
            move("Banded glute bridge", "Glutes, Hamstrings", "Resistance band"),
            move("Hip thrust", "Glutes, Hamstrings", "Barbell", "Bench"),
        ),
        move("Reverse lunge", "Quads, Glutes") to listOf(
            move("Walking lunge", "Quads, Glutes"),
            move("Split squat", "Quads, Glutes"),
            move("Step-up", "Quads, Glutes", "Step"),
            move("Dumbbell lunge", "Quads, Glutes", "Dumbbell"),
        ),
        move("Pike push-up", "Shoulders, Triceps") to listOf(
            move("Dumbbell shoulder press", "Shoulders, Triceps", "Dumbbell"),
            move("Band overhead press", "Shoulders, Triceps", "Resistance band"),
            move("Decline push-up", "Chest, Shoulders"),
            move("Overhead press", "Shoulders, Triceps", "Barbell"),
        ),
        move("Superman", "Lower back, Glutes") to listOf(
            move("Bird dog", "Lower back, Abs"),
            move("Glute bridge", "Glutes, Hamstrings"),
            move("Back extension", "Lower back, Glutes", "Machine"),
        ),
        move("Mountain climber", "Abs, Quads") to listOf(
            move("High knees", "Quads, Abs"),
            move("Bicycle crunch", "Abs, Obliques"),
            move("Burpee", "Quads, Chest"),
        ),
        move("Chair dip", "Triceps, Chest", "Chair") to listOf(
            move("Close-grip push-up", "Triceps, Chest"),
            move("Overhead dumbbell extension", "Triceps", "Dumbbell"),
            move("Triceps pushdown", "Triceps", "Cable"),
            move("Dips", "Triceps, Chest", "Dip bars"),
        ),
        move("Split squat", "Quads, Glutes") to listOf(
            move("Reverse lunge", "Quads, Glutes"),
            move("Step-up", "Quads, Glutes", "Step"),
            move("Goblet squat", "Quads, Glutes", "Dumbbell"),
            move("Bulgarian split squat", "Quads, Glutes", "Bench"),
        ),
        move("Table row", "Lats, Biceps", "Table") to listOf(
            move("Band row", "Lats, Biceps", "Resistance band"),
            move("Dumbbell row", "Lats, Biceps", "Dumbbell"),
            move("Pull-up", "Lats, Biceps", "Pull-up bar"),
            move("Seated cable row", "Lats, Biceps", "Cable"),
        ),
        move("Single-leg glute bridge", "Glutes, Hamstrings") to listOf(
            move("Glute bridge", "Glutes, Hamstrings"),
            move("Single-leg Romanian deadlift", "Hamstrings, Glutes"),
            move("Kettlebell swing", "Glutes, Hamstrings", "Kettlebell"),
            move("Hip thrust", "Glutes, Hamstrings", "Barbell", "Bench"),
        ),
        move("Hollow hold", "Abs") to listOf(
            move("Dead bug", "Abs"),
            move("Lying leg raise", "Abs"),
            move("Plank", "Abs"),
            move("Hanging knee raise", "Abs", "Pull-up bar"),
        ),
        move("Side plank", "Obliques, Abs") to listOf(
            move("Russian twist", "Obliques, Abs"),
            move("Bicycle crunch", "Abs, Obliques"),
            move("Plank", "Abs"),
            move("Pallof press", "Obliques, Abs", "Cable"),
        ),
    )

    private val byName: Map<String, Pair<SwapSuggestion, List<SwapSuggestion>>> =
        groups.associateBy { CatalogMatch.squash(it.first.name) }

    /** The built-in exercise names that have our own picks. */
    val builtInNames: List<String> get() = groups.map { it.first.name }

    /**
     * Our own picks for [name]. For a built-in exercise, its group. For one of the picks, the
     * exercise it replaces comes first, then the other picks, so swapping back is one tap.
     */
    fun handPicked(name: String): List<SwapSuggestion> {
        val key = CatalogMatch.squash(name)
        byName[key]?.let { return it.second }
        val (original, picks) = groups.firstOrNull { (_, picks) -> picks.any { CatalogMatch.squash(it.name) == key } }
            ?: return emptyList()
        return listOf(original) + picks.filter { CatalogMatch.squash(it.name) != key }
    }

    /**
     * Up to [limit] exercises to do instead of [name], best first. [exclude] holds names already
     * in the workout, so the same exercise is not offered twice. At home, anything that needs
     * gym equipment is left out.
     */
    fun suggest(
        name: String,
        catalog: List<Exercise>,
        place: TrainingPlace,
        exclude: Collection<String> = emptyList(),
        limit: Int = LIMIT,
    ): List<SwapSuggestion> {
        val skip = (exclude + name).map(CatalogMatch::squash).toMutableSet()
        val fits = { equipment: List<String> -> place == TrainingPlace.GYM || fitsHome(equipment) }

        val picks = handPicked(name).filter { CatalogMatch.squash(it.name) !in skip && fits(it.equipment) }
        // A catalog entry with another name can be the same exercise as a pick ("Dumbbell Bench
        // Press" and "Bench Press Dumbbell"), so the catalog's own match for each pick is skipped too.
        val pickedIds = picks.mapNotNull { CatalogMatch.find(it.name, catalog)?.id }.toSet()
        skip += picks.map { CatalogMatch.squash(it.name) }

        val fromCatalog = CatalogMatch.find(name, catalog)
            ?.let { CatalogMatch.sameMuscles(it, catalog, exclude = emptyList(), limit = catalog.size) }
            .orEmpty()
            .filter { it.id !in pickedIds && CatalogMatch.squash(it.name) !in skip && fits(it.equipment) }
            .map { SwapSuggestion(it.name, it.muscles, bodyweightAsEmpty(it.equipment), it) }

        return (picks + fromCatalog).distinctBy { CatalogMatch.squash(it.name) }.take(limit)
    }

    /**
     * True when the equipment is found at home: nothing, dumbbells, a kettlebell, a band, a mat,
     * or furniture. Barbells, machines, cables, benches and bars need a gym.
     */
    fun fitsHome(equipment: List<String>): Boolean =
        equipment.all { item -> words(item).any { word -> HOME_EQUIPMENT.any { word.startsWith(it) } } }

    /** wger says "none (bodyweight exercise)" for no equipment; here that is an empty list. */
    fun bodyweightAsEmpty(equipment: List<String>): List<String> =
        equipment.filterNot { item -> words(item).let { it.firstOrNull() == "none" || "bodyweight" in it } }

    // Matched on whole words, so "Adjustable bench" is not a table and "Machine" is not a mat.
    private val HOME_EQUIPMENT = listOf(
        "none", "bodyweight", "body", "dumbbell", "kettlebell", "band", "mat", "chair", "table", "step",
    )

    private fun words(text: String): List<String> =
        text.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
}
