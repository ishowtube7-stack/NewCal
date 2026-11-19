package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class MealType(val wire: String, val label: String) {
    BREAKFAST("breakfast", "Breakfast"),
    LUNCH("lunch", "Lunch"),
    DINNER("dinner", "Dinner"),
    SNACK("snack", "Snack");

    companion object {
        fun fromWire(raw: String?): MealType =
            entries.firstOrNull { it.wire.equals(raw, ignoreCase = true) } ?: LUNCH

        fun guessForCurrentHour(): MealType {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when {
                hour in 5..10 -> BREAKFAST
                hour in 11..15 -> LUNCH
                hour in 17..21 -> DINNER
                else -> SNACK
            }
        }
    }
}

enum class BiologicalSex(val wire: String, val label: String) {
    MALE("male", "Male"),
    FEMALE("female", "Female");

    companion object {
        fun fromWire(raw: String?): BiologicalSex =
            entries.firstOrNull { it.wire.equals(raw, ignoreCase = true) } ?: MALE
    }
}

enum class ActivityLevel(val wire: String, val label: String, val multiplier: Double) {
    SEDENTARY("sedentary", "Sedentary", 1.2),
    LIGHT("light", "Lightly Active", 1.375),
    MODERATE("moderate", "Moderately Active", 1.55),
    ACTIVE("active", "Very Active", 1.725),
    ATHLETE("athlete", "Athlete", 1.9);

    companion object {
        fun fromWire(raw: String?): ActivityLevel =
            entries.firstOrNull { it.wire.equals(raw, ignoreCase = true) } ?: MODERATE
    }
}

enum class GoalType(val wire: String, val label: String, val calorieDelta: Int) {
    LOSE("lose", "Lose Fat", -450),
    MAINTAIN("maintain", "Maintain", 0),
    GAIN("gain", "Build Muscle", 300);

    companion object {
        fun fromWire(raw: String?): GoalType =
            entries.firstOrNull { it.wire.equals(raw, ignoreCase = true) } ?: MAINTAIN
    }
}

enum class MacroPreset(val label: String, val proteinPct: Int, val carbsPct: Int, val fatPct: Int) {
    BALANCED("Balanced (30/40/30)", 30, 40, 30),
    HIGH_PROTEIN("High Protein (38/35/27)", 38, 35, 27),
    LOW_CARB("Low Carb (35/20/45)", 35, 20, 45)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "CalSnap Athlete",
    val calorieGoal: Long = 2100L,
    val proteinGoalG: Long = 158L,
    val carbsGoalG: Long = 210L,
    val fatGoalG: Long = 70L,
    val waterGoalMl: Long = 2500L,
    val currentWeightKg: Double = 72.0,
    val targetWeightKg: Double = 70.0,
    val heightCm: Double = 175.0,
    val age: Long = 28L,
    val sex: String = BiologicalSex.MALE.wire,
    val activityLevel: String = ActivityLevel.MODERATE.wire,
    val goalType: String = GoalType.MAINTAIN.wire,
    val useMetricUnits: Boolean = true,
    val onboardingDone: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MealEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = DateUtils.todayKey(),
    val name: String = "",
    val mealType: String = MealType.LUNCH.wire,
    val calories: Long = 0L,
    val proteinG: Long = 0L,
    val carbsG: Long = 0L,
    val fatG: Long = 0L,
    val fiberG: Long = 0L,
    val healthScore: Long = 7L,
    val servingMultiplier: Double = 1.0,
    val inputSource: String = "text",
    val aiNotes: String = "",
    val ingredients: List<String> = emptyList(),
    val loggedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyWaterLog(
    @DocumentId val dateKey: String = "",
    val userId: String = "",
    val waterMl: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WeightEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val weightKg: Double = 70.0,
    val dateKey: String = DateUtils.todayKey(),
    val note: String = "",
    val recordedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AiMealAnalysis(
    val name: String,
    val mealType: MealType,
    val baseCalories: Int,
    val baseProteinG: Int,
    val baseCarbsG: Int,
    val baseFatG: Int,
    val baseFiberG: Int,
    val healthScore: Int,
    val servingMultiplier: Double = 1.0,
    val inputSource: String,
    val aiNotes: String,
    val ingredients: List<String>
) {
    val scaledCalories: Int get() = (baseCalories * servingMultiplier).roundToInt().coerceIn(0, 10000)
    val scaledProteinG: Int get() = (baseProteinG * servingMultiplier).roundToInt().coerceIn(0, 1000)
    val scaledCarbsG: Int get() = (baseCarbsG * servingMultiplier).roundToInt().coerceIn(0, 1500)
    val scaledFatG: Int get() = (baseFatG * servingMultiplier).roundToInt().coerceIn(0, 1000)
    val scaledFiberG: Int get() = (baseFiberG * servingMultiplier).roundToInt().coerceIn(0, 500)
}

data class CalculatedGoals(
    val bmr: Int,
    val tdee: Int,
    val calorieGoal: Int,
    val proteinGoalG: Int,
    val carbsGoalG: Int,
    val fatGoalG: Int
)

object GoalCalculator {
    fun calculate(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        sex: BiologicalSex,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        preset: MacroPreset = MacroPreset.BALANCED
    ): CalculatedGoals {
        val safeWeight = weightKg.coerceIn(20.0, 350.0)
        val safeHeight = heightCm.coerceIn(80.0, 260.0)
        val safeAge = age.coerceIn(13, 120)

        // Mifflin-St Jeor equation
        val sexOffset = if (sex == BiologicalSex.MALE) 5.0 else -161.0
        val bmr = ((10.0 * safeWeight) + (6.25 * safeHeight) - (5.0 * safeAge) + sexOffset).roundToInt()
        val tdee = (bmr * activityLevel.multiplier).roundToInt()
        val targetCalories = (tdee + goalType.calorieDelta).coerceIn(1200, 6000)

        val proteinG = ((targetCalories * (preset.proteinPct / 100.0)) / 4.0).roundToInt().coerceIn(10, 600)
        val carbsG = ((targetCalories * (preset.carbsPct / 100.0)) / 4.0).roundToInt().coerceIn(10, 1000)
        val fatG = ((targetCalories * (preset.fatPct / 100.0)) / 9.0).roundToInt().coerceIn(10, 400)

        return CalculatedGoals(
            bmr = bmr,
            tdee = tdee,
            calorieGoal = targetCalories,
            proteinGoalG = proteinG,
            carbsGoalG = carbsG,
            fatGoalG = fatG
        )
    }
}

object DateUtils {
    private val keyFormatter: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val shortDayFormatter: SimpleDateFormat
        get() = SimpleDateFormat("EEE", Locale.US)

    private val monthDayFormatter: SimpleDateFormat
        get() = SimpleDateFormat("MMM d", Locale.US)

    private val timeFormatter: SimpleDateFormat
        get() = SimpleDateFormat("h:mm a", Locale.US)

    fun todayKey(): String = keyFormatter.format(Calendar.getInstance().time)

    fun daysAgoKey(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return keyFormatter.format(cal.time)
    }

    fun recentDateKeys(count: Int = 7): List<String> {
        return (count - 1 downTo 0).map { daysAgoKey(it) }
    }

    fun dayOfWeekShort(dateKey: String): String {
        val date = keyFormatter.parse(dateKey) ?: return ""
        return shortDayFormatter.format(date).uppercase(Locale.US)
    }

    fun dayNumber(dateKey: String): String {
        return dateKey.takeLast(2).trimStart('0').ifEmpty { "0" }
    }

    fun friendlyDate(dateKey: String): String {
        val today = todayKey()
        if (dateKey == today) return "Today"
        if (dateKey == daysAgoKey(1)) return "Yesterday"
        val date = keyFormatter.parse(dateKey) ?: return dateKey
        return monthDayFormatter.format(date)
    }

    fun formatTime(epochMs: Long): String {
        if (epochMs <= 0L) return ""
        return timeFormatter.format(java.util.Date(epochMs))
    }

    fun computeStreak(loggedDateKeys: Set<String>): Int {
        if (loggedDateKeys.isEmpty()) return 0
        val today = todayKey()
        var streak = 0
        var offset = if (loggedDateKeys.contains(today)) 0 else 1
        while (offset < 365) {
            val key = daysAgoKey(offset)
            if (loggedDateKeys.contains(key)) {
                streak++
                offset++
            } else {
                break
            }
        }
        return streak
    }
}
