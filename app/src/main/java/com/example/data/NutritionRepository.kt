package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.DailyWaterLog
import com.example.model.DateUtils
import com.example.model.MealEntry
import com.example.model.UserProfile
import com.example.model.WeightEntry
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NutritionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        }
        return uid
    }

    private fun extractFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    // --- Profile & Goals ---

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("profile").document("goals")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val fsError = extractFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveProfile(userId: String, profile: UserProfile): Result<Unit> {
        var docPath = "users/$userId/profile/goals"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("profile").document("goals")
            docPath = docRef.path

            val safeName = profile.displayName.trim().ifEmpty { "CalSnap Athlete" }.take(100)
            val safeSex = if (profile.sex in listOf("male", "female")) profile.sex else "male"
            val safeActivity = if (profile.activityLevel in listOf("sedentary", "light", "moderate", "active", "athlete")) {
                profile.activityLevel
            } else {
                "moderate"
            }
            val safeGoal = if (profile.goalType in listOf("lose", "maintain", "gain")) profile.goalType else "maintain"

            val existing = docRef.get().await()
            if (existing.exists()) {
                val updatePayload = mapOf(
                    "displayName" to safeName,
                    "calorieGoal" to profile.calorieGoal.coerceIn(800L, 8000L),
                    "proteinGoalG" to profile.proteinGoalG.coerceIn(10L, 600L),
                    "carbsGoalG" to profile.carbsGoalG.coerceIn(10L, 1000L),
                    "fatGoalG" to profile.fatGoalG.coerceIn(10L, 400L),
                    "waterGoalMl" to profile.waterGoalMl.coerceIn(500L, 8000L),
                    "currentWeightKg" to profile.currentWeightKg.coerceIn(20.0, 350.0),
                    "targetWeightKg" to profile.targetWeightKg.coerceIn(20.0, 350.0),
                    "heightCm" to profile.heightCm.coerceIn(80.0, 260.0),
                    "age" to profile.age.coerceIn(13L, 120L),
                    "sex" to safeSex,
                    "activityLevel" to safeActivity,
                    "goalType" to safeGoal,
                    "useMetricUnits" to profile.useMetricUnits,
                    "onboardingDone" to profile.onboardingDone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "calorieGoal" to profile.calorieGoal.coerceIn(800L, 8000L),
                    "proteinGoalG" to profile.proteinGoalG.coerceIn(10L, 600L),
                    "carbsGoalG" to profile.carbsGoalG.coerceIn(10L, 1000L),
                    "fatGoalG" to profile.fatGoalG.coerceIn(10L, 400L),
                    "waterGoalMl" to profile.waterGoalMl.coerceIn(500L, 8000L),
                    "currentWeightKg" to profile.currentWeightKg.coerceIn(20.0, 350.0),
                    "targetWeightKg" to profile.targetWeightKg.coerceIn(20.0, 350.0),
                    "heightCm" to profile.heightCm.coerceIn(80.0, 260.0),
                    "age" to profile.age.coerceIn(13L, 120L),
                    "sex" to safeSex,
                    "activityLevel" to safeActivity,
                    "goalType" to safeGoal,
                    "useMetricUnits" to profile.useMetricUnits,
                    "onboardingDone" to profile.onboardingDone,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- Meals ---

    fun observeMealsForDate(userId: String, dateKey: String): Flow<List<MealEntry>> = flow {
        val uid = requireUserId(userId)
        val colRef = db.collection("users").document(uid).collection("meals")
        emitAll(
            colRef.whereEqualTo("dateKey", dateKey)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MealEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.loggedAtEpochMs }
                }
                .catch { error ->
                    val fsError = extractFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeAllMeals(userId: String): Flow<List<MealEntry>> = flow {
        val uid = requireUserId(userId)
        val colRef = db.collection("users").document(uid).collection("meals")
        emitAll(
            colRef.orderBy("loggedAtEpochMs", Query.Direction.DESCENDING)
                .limit(200)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MealEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsError = extractFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun addMeal(userId: String, meal: MealEntry): Result<Unit> {
        var docPath = "users/$userId/meals"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("meals").document()
            docPath = docRef.path

            val safeName = meal.name.trim().ifEmpty { "Meal" }.take(120)
            val safeDateKey = if (Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(meal.dateKey)) {
                meal.dateKey
            } else {
                DateUtils.todayKey()
            }
            val safeMealType = if (meal.mealType in listOf("breakfast", "lunch", "dinner", "snack")) meal.mealType else "lunch"
            val safeSource = if (meal.inputSource in listOf("photo", "text", "manual")) meal.inputSource else "text"
            val safeIngredients = meal.ingredients.map { it.trim().take(140) }.filter { it.isNotEmpty() }.take(20)

            val payload = mapOf(
                "userId" to uid,
                "dateKey" to safeDateKey,
                "name" to safeName,
                "mealType" to safeMealType,
                "calories" to meal.calories.coerceIn(0L, 10000L),
                "proteinG" to meal.proteinG.coerceIn(0L, 1000L),
                "carbsG" to meal.carbsG.coerceIn(0L, 1500L),
                "fatG" to meal.fatG.coerceIn(0L, 1000L),
                "fiberG" to meal.fiberG.coerceIn(0L, 500L),
                "healthScore" to meal.healthScore.coerceIn(1L, 10L),
                "servingMultiplier" to meal.servingMultiplier.coerceIn(0.1, 10.0),
                "inputSource" to safeSource,
                "aiNotes" to meal.aiNotes.trim().take(400),
                "ingredients" to safeIngredients,
                "loggedAtEpochMs" to meal.loggedAtEpochMs.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMeal(
        userId: String,
        mealId: String,
        name: String,
        mealType: String,
        calories: Long,
        proteinG: Long,
        carbsG: Long,
        fatG: Long,
        servingMultiplier: Double
    ): Result<Unit> {
        var docPath = "users/$userId/meals/$mealId"
        return try {
            val uid = requireUserId(userId)
            require(mealId.isNotBlank()) { "Meal ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("meals").document(mealId)
            docPath = docRef.path

            val safeName = name.trim().ifEmpty { "Meal" }.take(120)
            val safeMealType = if (mealType in listOf("breakfast", "lunch", "dinner", "snack")) mealType else "lunch"

            docRef.update(
                mapOf(
                    "name" to safeName,
                    "mealType" to safeMealType,
                    "calories" to calories.coerceIn(0L, 10000L),
                    "proteinG" to proteinG.coerceIn(0L, 1000L),
                    "carbsG" to carbsG.coerceIn(0L, 1500L),
                    "fatG" to fatG.coerceIn(0L, 1000L),
                    "servingMultiplier" to servingMultiplier.coerceIn(0.1, 10.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMeal(userId: String, mealId: String): Result<Unit> {
        var docPath = "users/$userId/meals/$mealId"
        return try {
            val uid = requireUserId(userId)
            require(mealId.isNotBlank()) { "Meal ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("meals").document(mealId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Daily Hydration ---

    fun observeWaterForDate(userId: String, dateKey: String): Flow<Long> = flow {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(DailyWaterLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.waterMl ?: 0L
                    } else {
                        0L
                    }
                }
                .catch { error ->
                    val fsError = extractFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun setWaterForDate(userId: String, dateKey: String, newWaterMl: Long): Result<Unit> {
        var docPath = "users/$userId/dailyLogs/$dateKey"
        return try {
            val uid = requireUserId(userId)
            require(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(dateKey)) { "Invalid dateKey" }
            val docRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
            docPath = docRef.path
            val boundedWater = newWaterMl.coerceIn(0L, 15000L)

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "waterMl" to boundedWater,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "waterMl" to boundedWater,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- Weight Logs ---

    fun observeWeights(userId: String): Flow<List<WeightEntry>> = flow {
        val uid = requireUserId(userId)
        val colRef = db.collection("users").document(uid).collection("weights")
        emitAll(
            colRef.orderBy("recordedAtEpochMs", Query.Direction.ASCENDING)
                .limit(120)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WeightEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsError = extractFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun addWeight(
        userId: String,
        weightKg: Double,
        dateKey: String,
        note: String
    ): Result<Unit> {
        var docPath = "users/$userId/weights"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("weights").document()
            docPath = docRef.path
            val safeDate = if (Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(dateKey)) dateKey else DateUtils.todayKey()

            docRef.set(
                mapOf(
                    "userId" to uid,
                    "weightKg" to weightKg.coerceIn(20.0, 350.0),
                    "dateKey" to safeDate,
                    "note" to note.trim().take(200),
                    "recordedAtEpochMs" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteWeight(userId: String, weightId: String): Result<Unit> {
        var docPath = "users/$userId/weights/$weightId"
        return try {
            val uid = requireUserId(userId)
            require(weightId.isNotBlank()) { "Weight ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("weights").document(weightId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
