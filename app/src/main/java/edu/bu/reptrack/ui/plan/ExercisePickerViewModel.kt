package edu.bu.reptrack.ui.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.exercise.ExerciseRepository
import edu.bu.reptrack.data.plan.PlanRepository
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.domain.exercise.CatalogMatch
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.domain.exercise.SwapSuggestion
import edu.bu.reptrack.domain.exercise.SwapSuggestions
import edu.bu.reptrack.domain.exercise.filterExercises
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.workout.RoutineEdits
import edu.bu.reptrack.domain.workout.CustomPlan
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.workout.findRoutine
import edu.bu.reptrack.ui.navigation.Routes
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the exercise picker needs to draw itself. */
data class ExercisePickerUiState(
    val isLoading: Boolean = true,
    val routineName: String = "",
    /** The exercise being swapped out. Null when adding a new one. */
    val replacing: String? = null,
    val query: String = "",
    /** When swapping: what to do instead, best first, each with the muscles and equipment that make it fit. */
    val suggestions: List<SwapSuggestion> = emptyList(),
    /** Where the routine is done. At home, the suggestions need no gym equipment. */
    val place: TrainingPlace = TrainingPlace.GYM,
    /** Catalog exercises that match the search and are not in the routine or the suggestions. */
    val results: List<Exercise> = emptyList(),
    /** The search text, offered as the user's own exercise when the catalog has nothing by that name. */
    val customName: String? = null,
    val catalogEmpty: Boolean = false,
    val saveFailed: Boolean = false,
    /** Swapping during a workout, for an exercise of the routine: it can be changed in the routine too. */
    val canKeep: Boolean = false,
    /** "Keep for next time" is ticked: the routine changes as well as this workout. */
    val keep: Boolean = false,
    /** During a workout: the exercise swapped out and the one picked, for the workout to use. */
    val swapped: Pair<String, String>? = null,
    /** The change is saved: the screen closes. */
    val done: Boolean = false,
)

/**
 * Picks an exercise from the wger catalog to add to a routine, or to swap one out. A swap puts
 * the new exercise in the old one's place and leaves the rest of the routine as it was. The
 * user can also type the name of an exercise the catalog does not have.
 *
 * Swapping suggests what to do instead first (see SwapSuggestions), for the gym or for home.
 * Opened from a workout, the swap is for that workout only, unless "Keep for next time" is ticked.
 */
class ExercisePickerViewModel(
    private val planRepository: PlanRepository,
    catalog: ExerciseRepository,
    profiles: ProfileRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val routineId: String = savedState.get<String>(Routes.ARG_ROUTINE_ID).orEmpty()

    /** The exercise to swap, by its place in the routine. -1: add a new one. */
    private val replaceIndex: Int = savedState.get<Int>(Routes.ARG_REPLACE_INDEX) ?: -1

    /** The exercise to swap during a workout, by name, since it may be a swap from earlier in that workout. */
    private val workoutExercise: String? = savedState.get<String>(Routes.ARG_EXERCISE_NAME)?.takeIf { it.isNotBlank() }

    /** The routine's own exercise in that place. "Keep for next time" changes this one. */
    private val workoutOriginal: String? = savedState.get<String>(Routes.ARG_ORIGINAL)?.takeIf { it.isNotBlank() } ?: workoutExercise

    private val query = savedState.getStateFlow(KEY_QUERY, "")
    private val keep = savedState.getStateFlow(KEY_KEEP, false)
    private val inputs = combine(query, keep) { text, keep -> text to keep }

    /** A built-in routine is for the gym or for home. For the user's own routines, where they said they train. */
    private val place = profiles.profile
        .map { profile -> StarterRoutines.placeOf(routineId) ?: profile?.place ?: TrainingPlace.GYM }
        .catch { emit(StarterRoutines.placeOf(routineId) ?: TrainingPlace.GYM) }

    private data class Local(
        val saveFailed: Boolean = false,
        val swapped: Pair<String, String>? = null,
        val done: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    /** The routine's exercises right now, for saving. */
    private var current: List<String> = StarterRoutines.find(routineId)?.exercises.orEmpty()

    private val routine = combine(
        planRepository.observeRoutineEdits().catch { emit(emptyMap()) },
        planRepository.observeCustomPlan().catch { emit(CustomPlan()) },
    ) { edits, custom -> findRoutine(routineId, edits, custom) }

    val uiState: StateFlow<ExercisePickerUiState> = combine(
        routine,
        catalog.observeExercises().catch { emit(emptyList()) },
        inputs,
        local,
        place,
    ) { found, all, (text, keepIt), picked, where ->
        val exercises = found?.exercises.orEmpty()
        current = exercises
        val replacing = workoutExercise ?: exercises.getOrNull(replaceIndex)
        // During a workout the routine's own exercise was swapped out, so it can be picked again.
        val skip = exercises.filterNot { it.equals(workoutOriginal, ignoreCase = true) } + listOfNotNull(replacing)
        val inRoutine = skip.map(CatalogMatch::squash).toSet()
        val suggestions = if (replacing != null && text.isBlank()) {
            SwapSuggestions.suggest(replacing, all, where, exclude = skip)
        } else {
            emptyList()
        }
        val suggested = suggestions.map { CatalogMatch.squash(it.name) }.toSet()
        val available = all.filter { CatalogMatch.squash(it.name).let { key -> key !in inRoutine && key !in suggested } }
        val typed = text.trim()
        val custom = typed.takeIf { name ->
            name.isNotEmpty() &&
                all.none { CatalogMatch.squash(it.name) == CatalogMatch.squash(name) } &&
                CatalogMatch.squash(name) !in inRoutine
        }
        ExercisePickerUiState(
            isLoading = false,
            routineName = found?.name.orEmpty(),
            replacing = replacing,
            query = text,
            suggestions = suggestions,
            place = where,
            results = filterExercises(available, text, category = null),
            customName = custom,
            catalogEmpty = all.isEmpty(),
            saveFailed = picked.saveFailed,
            canKeep = workoutOriginal != null && exercises.any { it.equals(workoutOriginal, ignoreCase = true) },
            keep = keepIt,
            swapped = picked.swapped,
            done = picked.done,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExercisePickerUiState(),
    )

    fun onQueryChange(text: String) {
        savedState[KEY_QUERY] = text
    }

    fun onKeepChange(keep: Boolean) {
        savedState[KEY_KEEP] = keep
    }

    /** Adds [name], or puts it in place of the exercise being swapped, then closes. */
    fun pick(name: String) {
        if (workoutExercise != null) {
            pickForWorkout(workoutExercise, workoutOriginal ?: workoutExercise, name)
            return
        }
        val changed = if (replaceIndex >= 0) RoutineEdits.replace(current, replaceIndex, name) else RoutineEdits.add(current, name)
        save(changed, swapped = null)
    }

    /**
     * During a workout the swap goes back to the workout. With "Keep for next time" ticked the
     * routine is changed too, so the screen only closes once that is saved.
     */
    private fun pickForWorkout(from: String, original: String, name: String) {
        val swapped = from to name.trim()
        val index = current.indexOfFirst { it.equals(original, ignoreCase = true) }
        if (!keep.value || index < 0) {
            local.update { it.copy(swapped = swapped, done = true) }
            return
        }
        save(RoutineEdits.replace(current, index, name), swapped)
    }

    private fun save(changed: List<String>, swapped: Pair<String, String>?) {
        if (changed == current) {
            local.update { it.copy(swapped = swapped, done = true) }
            return
        }
        local.update { it.copy(saveFailed = false) }
        viewModelScope.launch {
            try {
                planRepository.setRoutineExercises(routineId, changed)
                local.update { it.copy(swapped = swapped, done = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                local.update { it.copy(saveFailed = true) }
            }
        }
    }

    companion object {
        private const val KEY_QUERY = "picker_query"
        private const val KEY_KEEP = "picker_keep"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                ExercisePickerViewModel(
                    planRepository = container.planRepository,
                    catalog = container.exerciseRepository,
                    profiles = container.profileRepository,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}
