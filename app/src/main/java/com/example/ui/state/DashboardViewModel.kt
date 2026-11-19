package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NutritionRepository
import com.example.model.DateUtils
import com.example.model.MealEntry
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "DashboardVM"

data class DashboardData(
    val profile: UserProfile,
    val selectedDateKey: String,
    val meals: List<MealEntry>,
    val waterMl: Long,
    val consumedCalories: Int,
    val consumedProteinG: Int,
    val consumedCarbsG: Int,
    val consumedFatG: Int,
    val consumedFiberG: Int,
    val streakDays: Int,
    val loggedDateKeys: Set<String>
)

class DashboardViewModel(
    private val repository: NutritionRepository,
    private val currentUserId: String,
    private val userDisplayName: String
) : ViewModel() {

    private val _selectedDateKey = MutableStateFlow(DateUtils.todayKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _feedbackMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val feedbackMessage = _feedbackMessage.asSharedFlow()

    private fun extractFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mealsForSelectedDate = _selectedDateKey.flatMapLatest { dateKey ->
        repository.observeMealsForDate(currentUserId, dateKey)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val waterForSelectedDate = _selectedDateKey.flatMapLatest { dateKey ->
        repository.observeWaterForDate(currentUserId, dateKey)
    }

    val uiState: StateFlow<UiState<DashboardData>> = combine(
        repository.observeProfile(currentUserId),
        _selectedDateKey,
        mealsForSelectedDate,
        waterForSelectedDate,
        repository.observeAllMeals(currentUserId)
    ) { profileOrNull, dateKey, meals, waterMl, allMeals ->
        val effectiveProfile = profileOrNull ?: UserProfile(
            userId = currentUserId,
            displayName = userDisplayName.ifBlank { "CalSnap Athlete" }
        )
        val loggedDates = allMeals.map { it.dateKey }.toSet()
        val streak = DateUtils.computeStreak(loggedDates)

        DashboardData(
            profile = effectiveProfile,
            selectedDateKey = dateKey,
            meals = meals,
            waterMl = waterMl,
            consumedCalories = meals.sumOf { it.calories.toInt() },
            consumedProteinG = meals.sumOf { it.proteinG.toInt() },
            consumedCarbsG = meals.sumOf { it.carbsG.toInt() },
            consumedFatG = meals.sumOf { it.fatG.toInt() },
            consumedFiberG = meals.sumOf { it.fiberG.toInt() },
            streakDays = streak,
            loggedDateKeys = loggedDates
        )
    }
        .map<DashboardData, UiState<DashboardData>> { UiState.Success(it) }
        .catch { error ->
            val fsError = extractFirestoreError(error)
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing dashboard state", root)
            emit(UiState.Error(root.message ?: "Unable to sync your nutrition dashboard."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun adjustWater(currentWaterMl: Long, deltaMl: Long) {
        val dateKey = _selectedDateKey.value
        val updated = (currentWaterMl + deltaMl).coerceIn(0L, 15000L)
        viewModelScope.launch {
            val res = repository.setWaterForDate(currentUserId, dateKey, updated)
            res.onFailure { err ->
                _feedbackMessage.tryEmit(err.localizedMessage ?: "Failed to update water intake.")
            }
        }
    }

    fun updateMeal(
        mealId: String,
        name: String,
        mealType: String,
        calories: Long,
        proteinG: Long,
        carbsG: Long,
        fatG: Long,
        servingMultiplier: Double
    ) {
        viewModelScope.launch {
            val res = repository.updateMeal(
                userId = currentUserId,
                mealId = mealId,
                name = name,
                mealType = mealType,
                calories = calories,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
                servingMultiplier = servingMultiplier
            )
            res.onSuccess {
                _feedbackMessage.tryEmit("Meal updated")
            }.onFailure { err ->
                _feedbackMessage.tryEmit(err.localizedMessage ?: "Failed to update meal.")
            }
        }
    }

    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            val res = repository.deleteMeal(currentUserId, mealId)
            res.onSuccess {
                _feedbackMessage.tryEmit("Meal removed")
            }.onFailure { err ->
                _feedbackMessage.tryEmit(err.localizedMessage ?: "Failed to remove meal.")
            }
        }
    }
}
