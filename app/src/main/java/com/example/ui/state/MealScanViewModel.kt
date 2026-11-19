package com.example.ui.state

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NutritionAiRepository
import com.example.data.NutritionRepository
import com.example.model.AiMealAnalysis
import com.example.model.DateUtils
import com.example.model.MealEntry
import com.example.model.MealType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Analyzing(val message: String) : ScanUiState
    data class Review(val draft: AiMealAnalysis) : ScanUiState
    data class Saving(val draft: AiMealAnalysis) : ScanUiState
    data class Saved(val mealName: String) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class MealScanViewModel(
    private val aiRepository: NutritionAiRepository,
    private val nutritionRepository: NutritionRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun analyzePhoto(bitmap: Bitmap, contextHint: String = "") {
        viewModelScope.launch {
            _uiState.value = ScanUiState.Analyzing("Identifying ingredients & portion sizes...")
            val result = aiRepository.analyzeMealPhoto(bitmap, contextHint)
            result.onSuccess { analysis ->
                _uiState.value = ScanUiState.Review(analysis)
            }.onFailure { error ->
                _uiState.value = ScanUiState.Error(
                    error.localizedMessage ?: "Could not analyze photo. Please try again."
                )
            }
        }
    }

    fun analyzeDescription(description: String) {
        viewModelScope.launch {
            _uiState.value = ScanUiState.Analyzing("Calculating calories & macro breakdown...")
            val result = aiRepository.analyzeMealText(description)
            result.onSuccess { analysis ->
                _uiState.value = ScanUiState.Review(analysis)
            }.onFailure { error ->
                _uiState.value = ScanUiState.Error(
                    error.localizedMessage ?: "Could not analyze description. Please try again."
                )
            }
        }
    }

    fun openManualEntry() {
        _uiState.value = ScanUiState.Review(
            AiMealAnalysis(
                name = "",
                mealType = MealType.guessForCurrentHour(),
                baseCalories = 350,
                baseProteinG = 25,
                baseCarbsG = 35,
                baseFatG = 12,
                baseFiberG = 5,
                healthScore = 7,
                servingMultiplier = 1.0,
                inputSource = "manual",
                aiNotes = "Custom manual nutrition entry.",
                ingredients = emptyList()
            )
        )
    }

    fun updateDraftName(name: String) {
        val current = (_uiState.value as? ScanUiState.Review)?.draft ?: return
        _uiState.value = ScanUiState.Review(current.copy(name = name))
    }

    fun updateDraftMealType(mealType: MealType) {
        val current = (_uiState.value as? ScanUiState.Review)?.draft ?: return
        _uiState.value = ScanUiState.Review(current.copy(mealType = mealType))
    }

    fun updateServingMultiplier(multiplier: Double) {
        val current = (_uiState.value as? ScanUiState.Review)?.draft ?: return
        _uiState.value = ScanUiState.Review(current.copy(servingMultiplier = multiplier.coerceIn(0.25, 5.0)))
    }

    fun updateDraftBaseMacros(calories: Int, protein: Int, carbs: Int, fat: Int) {
        val current = (_uiState.value as? ScanUiState.Review)?.draft ?: return
        _uiState.value = ScanUiState.Review(
            current.copy(
                baseCalories = calories.coerceIn(0, 10000),
                baseProteinG = protein.coerceIn(0, 1000),
                baseCarbsG = carbs.coerceIn(0, 1500),
                baseFatG = fat.coerceIn(0, 1000),
                servingMultiplier = 1.0
            )
        )
    }

    fun saveMeal(dateKey: String = DateUtils.todayKey()) {
        val draft = (_uiState.value as? ScanUiState.Review)?.draft ?: return
        val finalName = draft.name.trim().ifEmpty { "Logged Meal" }
        viewModelScope.launch {
            _uiState.value = ScanUiState.Saving(draft)
            val entry = MealEntry(
                userId = currentUserId,
                dateKey = dateKey,
                name = finalName,
                mealType = draft.mealType.wire,
                calories = draft.scaledCalories.toLong(),
                proteinG = draft.scaledProteinG.toLong(),
                carbsG = draft.scaledCarbsG.toLong(),
                fatG = draft.scaledFatG.toLong(),
                fiberG = draft.scaledFiberG.toLong(),
                healthScore = draft.healthScore.toLong(),
                servingMultiplier = draft.servingMultiplier,
                inputSource = draft.inputSource,
                aiNotes = draft.aiNotes,
                ingredients = draft.ingredients,
                loggedAtEpochMs = System.currentTimeMillis()
            )
            val result = nutritionRepository.addMeal(currentUserId, entry)
            result.onSuccess {
                _uiState.value = ScanUiState.Saved(finalName)
            }.onFailure { error ->
                _uiState.value = ScanUiState.Error(
                    error.localizedMessage ?: "Failed to save meal to cloud."
                )
            }
        }
    }

    fun reset() {
        _uiState.value = ScanUiState.Idle
    }
}
