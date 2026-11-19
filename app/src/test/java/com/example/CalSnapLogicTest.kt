package com.example

import com.example.model.ActivityLevel
import com.example.model.AiMealAnalysis
import com.example.model.BiologicalSex
import com.example.model.DateUtils
import com.example.model.GoalCalculator
import com.example.model.GoalType
import com.example.model.MacroPreset
import com.example.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalSnapLogicTest {

    @Test
    fun goalCalculator_mifflinStJeor_computesExpectedTargets() {
        val calc = GoalCalculator.calculate(
            weightKg = 75.0,
            heightCm = 178.0,
            age = 30,
            sex = BiologicalSex.MALE,
            activityLevel = ActivityLevel.MODERATE,
            goalType = GoalType.LOSE,
            preset = MacroPreset.BALANCED
        )
        // BMR = 10*75 + 6.25*178 - 5*30 + 5 = 750 + 1112.5 - 150 + 5 = 1717.5 -> 1718
        assertEquals(1718, calc.bmr)
        assertTrue(calc.tdee > calc.bmr)
        assertEquals(calc.tdee - 450, calc.calorieGoal)
        assertTrue(calc.proteinGoalG > 0 && calc.carbsGoalG > 0 && calc.fatGoalG > 0)
    }

    @Test
    fun computeStreak_countsConsecutiveDaysIncludingTodayOrYesterday() {
        val today = DateUtils.daysAgoKey(0)
        val yesterday = DateUtils.daysAgoKey(1)
        val twoDaysAgo = DateUtils.daysAgoKey(2)
        val fourDaysAgo = DateUtils.daysAgoKey(4)

        assertEquals(3, DateUtils.computeStreak(setOf(today, yesterday, twoDaysAgo, fourDaysAgo)))
        assertEquals(2, DateUtils.computeStreak(setOf(yesterday, twoDaysAgo)))
        assertEquals(0, DateUtils.computeStreak(setOf(fourDaysAgo)))
    }

    @Test
    fun aiMealAnalysis_scalesCaloriesAndMacrosByServingMultiplier() {
        val analysis = AiMealAnalysis(
            name = "Chicken Bowl",
            mealType = MealType.LUNCH,
            baseCalories = 500,
            baseProteinG = 40,
            baseCarbsG = 50,
            baseFatG = 14,
            baseFiberG = 8,
            healthScore = 8,
            servingMultiplier = 1.5,
            inputSource = "photo",
            aiNotes = "Balanced protein bowl",
            ingredients = listOf("Chicken breast 150g")
        )
        assertEquals(750, analysis.scaledCalories)
        assertEquals(60, analysis.scaledProteinG)
        assertEquals(75, analysis.scaledCarbsG)
        assertEquals(21, analysis.scaledFatG)
    }
}
