package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.AiMealAnalysis
import com.example.model.MealType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "NutritionAiRepo"

class NutritionAiRepository {

    private val mealSchema = Schema.obj(
        properties = mapOf(
            "mealName" to Schema.string(description = "Short, clear title of the meal (e.g., Grilled Chicken Avocado Bowl)"),
            "mealType" to Schema.enumeration(
                values = listOf("breakfast", "lunch", "dinner", "snack"),
                description = "Most likely meal period"
            ),
            "calories" to Schema.integer(description = "Estimated total calories in kcal"),
            "proteinGrams" to Schema.integer(description = "Estimated total protein in grams"),
            "carbsGrams" to Schema.integer(description = "Estimated total carbohydrates in grams"),
            "fatGrams" to Schema.integer(description = "Estimated total fat in grams"),
            "fiberGrams" to Schema.integer(description = "Estimated dietary fiber in grams"),
            "healthScore" to Schema.integer(description = "Nutritional quality score from 1 (least nutritious) to 10 (whole/balanced)"),
            "nutritionTip" to Schema.string(description = "One concise, constructive nutrition observation under 160 characters"),
            "ingredients" to Schema.array(
                items = Schema.string(description = "Single food item with estimated portion and calories, e.g. 'Grilled Salmon (170g) • 350 kcal'"),
                description = "List of detected or described food items (max 12)"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mealSchema
                temperature = 0.2f
            }
        )
    }

    suspend fun analyzeMealPhoto(bitmap: Bitmap, contextHint: String = ""): Result<AiMealAnalysis> {
        return try {
            val scaledBitmap = withContext(Dispatchers.Default) {
                downscaleBitmap(bitmap, maxEdgePx = 1024)
            }
            val promptHint = if (contextHint.isNotBlank()) {
                "Additional user context about ingredients/portion: \"$contextHint\". "
            } else {
                ""
            }
            val promptText = """
                You are an expert sports nutritionist and calorie estimator.
                Analyze the food in this photo carefully and estimate realistic total calories and macronutrients for the entire visible serving.
                ${promptHint}If there is no food visible, return mealName="Unrecognized Meal" with 0 calories and explain briefly in nutritionTip.
                Return accurate JSON strictly matching the schema.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(scaledBitmap)
                        text(promptText)
                    }
                )
            }
            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            Result.success(parseAnalysis(rawJson, inputSource = "photo"))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Photo meal analysis timed out", e)
            Result.failure(IllegalStateException("Meal analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Photo meal analysis failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Unable to analyze meal photo."))
        }
    }

    suspend fun analyzeMealText(description: String): Result<AiMealAnalysis> {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter a meal description first."))
        }
        return try {
            val promptText = """
                You are an expert sports nutritionist and calorie estimator.
                Estimate realistic calories, protein, carbs, fat, fiber, health score (1-10), and itemized food breakdown for this meal description:
                "$trimmed"
                Use standard USDA-like portion estimates when exact weights aren't specified.
                Return accurate JSON strictly matching the schema.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(promptText)
            }
            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            Result.success(parseAnalysis(rawJson, inputSource = "text"))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Text meal analysis timed out", e)
            Result.failure(IllegalStateException("Meal analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Text meal analysis failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Unable to analyze meal description."))
        }
    }

    private fun parseAnalysis(rawJson: String, inputSource: String): AiMealAnalysis {
        val json = JSONObject(rawJson)
        val mealName = json.optString("mealName", "Logged Meal").trim().take(120).ifEmpty { "Logged Meal" }
        val mealType = MealType.fromWire(json.optString("mealType", MealType.guessForCurrentHour().wire))
        val calories = json.optInt("calories", 0).coerceIn(0, 10000)
        val protein = json.optInt("proteinGrams", 0).coerceIn(0, 1000)
        val carbs = json.optInt("carbsGrams", 0).coerceIn(0, 1500)
        val fat = json.optInt("fatGrams", 0).coerceIn(0, 1000)
        val fiber = json.optInt("fiberGrams", 0).coerceIn(0, 500)
        val healthScore = json.optInt("healthScore", 7).coerceIn(1, 10)
        val tip = json.optString("nutritionTip", "").trim().take(400)

        val ingredientsArr = json.optJSONArray("ingredients")
        val ingredients = buildList {
            if (ingredientsArr != null) {
                val count = minOf(ingredientsArr.length(), 20)
                for (i in 0 until count) {
                    val item = ingredientsArr.optString(i, "").trim().take(140)
                    if (item.isNotEmpty()) add(item)
                }
            }
        }

        return AiMealAnalysis(
            name = mealName,
            mealType = mealType,
            baseCalories = calories,
            baseProteinG = protein,
            baseCarbsG = carbs,
            baseFatG = fat,
            baseFiberG = fiber,
            healthScore = healthScore,
            servingMultiplier = 1.0,
            inputSource = inputSource,
            aiNotes = tip,
            ingredients = ingredients
        )
    }

    private fun downscaleBitmap(source: Bitmap, maxEdgePx: Int): Bitmap {
        val maxDim = max(source.width, source.height)
        if (maxDim <= maxEdgePx) return source
        val scale = maxEdgePx.toFloat() / maxDim.toFloat()
        val newWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
        val newHeight = (source.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }
}
