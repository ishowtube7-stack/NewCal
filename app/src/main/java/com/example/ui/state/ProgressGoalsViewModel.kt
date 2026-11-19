package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NutritionRepository
import com.example.model.DateUtils
import com.example.model.MealEntry
import com.example.model.UserProfile
import com.example.model.WeightEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "ProgressGoalsVM"

data class DailyCalorieSummary(
    val dateKey: String,
    val shortDay: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

data class ProgressGoalsData(
    val profile: UserProfile,
    val weights: List<WeightEntry>,
    val weeklyCalories: List<DailyCalorieSummary>,
    val streakDays: Int,
    val totalMealsLogged: Int,
    val averageCalories7d: Int
)

class ProgressGoalsViewModel(
    private val repository: NutritionRepository,
    private val currentUserId: String,
    private val userDisplayName: String
) : ViewModel() {

    private val _feedbackMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val feedbackMessage = _feedbackMessage.asSharedFlow()

    private fun extractFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    val uiState: StateFlow<UiState<ProgressGoalsData>> = combine(
        repository.observeProfile(currentUserId),
        repository.observeWeights(currentUserId),
        repository.observeAllMeals(currentUserId)
    ) { profileOrNull, weights, allMeals ->
        val profile = profileOrNull ?: UserProfile(
            userId = currentUserId,
            displayName = userDisplayName.ifBlank { "CalSnap Athlete" }
        )

        val recentKeys = DateUtils.recentDateKeys(7)
        val byDate: Map<String, List<MealEntry>> = allMeals.groupBy { it.dateKey }
        val weekly = recentKeys.map { key ->
            val dayMeals = byDate[key].orEmpty()
            DailyCalorieSummary(
                dateKey = key,
                shortDay = DateUtils.dayOfWeekShort(key),
                calories = dayMeals.sumOf { it.calories.toInt() },
                proteinG = dayMeals.sumOf { it.proteinG.toInt() },
                carbsG = dayMeals.sumOf { it.carbsG.toInt() },
                fatG = dayMeals.sumOf { it.fatG.toInt() }
            )
        }
        val activeDays = weekly.filter { it.calories > 0 }
        val avg7d = if (activeDays.isNotEmpty()) activeDays.sumOf { it.calories } / activeDays.size else 0
        val loggedDates = byDate.keys
        val streak = DateUtils.computeStreak(loggedDates)

        ProgressGoalsData(
            profile = profile,
            weights = weights,
            weeklyCalories = weekly,
            streakDays = streak,
            totalMealsLogged = allMeals.size,
            averageCalories7d = avg7d
        )
    }
        .map<ProgressGoalsData, UiState<ProgressGoalsData>> { UiState.Success(it) }
        .catch { error ->
            val fsError = extractFirestoreError(error)
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing progress & goals", root)
            emit(UiState.Error(root.message ?: "Unable to load progress and goals."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun logWeight(weightKg: Double, note: String, currentProfile: UserProfile) {
        viewModelScope.launch {
            val res = repository.addWeight(
                userId = currentUserId,
                weightKg = weightKg,
                dateKey = DateUtils.todayKey(),
                note = note
            )
            res.onSuccess {
                repository.saveProfile(
                    currentUserId,
                    currentProfile.copy(currentWeightKg = weightKg)
                )
                _feedbackMessage.tryEmit("Weight check-in saved")
            }.onFailure { err ->
                _feedbackMessage.tryEmit(err.localizedMessage ?: "Failed to log weight.")
            }
        }
    }

    fun deleteWeight(weightId: String) {
        viewModelScope.launch {
            val res = repository.deleteWeight(currentUserId, weightId)
            res.onSuccess {
                _feedbackMessage.tryEmit("Weight entry removed")
            }.onFailure { err ->
                _feedbackMessage.tryEmit(err.localizedMessage ?: "Failed to remove weight entry.")
            }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            val res = repository.saveProfile(currentUserId, profile.copy(onboardingDone = true))
            res.onSuccess {
                _feedbackMessage.tryEmit("Nutrition goals updated")
            }.onFailure { err ->
                _feedbackMessage.tryEmit(err.localizedMessage ?: "Failed to save goals.")
            }
        }
    }
}
