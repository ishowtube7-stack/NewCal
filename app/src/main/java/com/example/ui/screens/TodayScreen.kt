package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DateUtils
import com.example.model.MealEntry
import com.example.model.MealType
import com.example.ui.components.DateStripRow
import com.example.ui.components.HeroCalorieMacroCard
import com.example.ui.components.MealEntryCard
import com.example.ui.components.StreakBadge
import com.example.ui.components.WaterHydrationCard
import com.example.ui.state.DashboardData
import com.example.ui.state.UiState
import com.example.ui.theme.CalorieCoral
import com.example.ui.theme.CalorieCoralSoft
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.FatEmerald
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ProteinBlue
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SubtleStroke
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    uiState: UiState<DashboardData>,
    onSelectDate: (String) -> Unit,
    onAdjustWater: (Long, Long) -> Unit,
    onOpenScan: () -> Unit,
    onOpenGoals: () -> Unit,
    onUpdateMeal: (String, String, String, Long, Long, Long, Long, Double) -> Unit,
    onDeleteMeal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inspectingMeal by remember { mutableStateOf<MealEntry?>(null) }
    val recentDates = remember { DateUtils.recentDateKeys(7) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (uiState) {
            UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CalorieCoral)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SlateSurface,
                        border = BorderStroke(1.dp, ErrorRose.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sync Error",
                                style = MaterialTheme.typography.titleLarge,
                                color = ErrorRose
                            )
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            is UiState.Success -> {
                val data = uiState.data
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = DateUtils.friendlyDate(data.selectedDateKey),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Hi ${data.profile.displayName.substringBefore(" ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StreakBadge(streakDays = data.streakDays)
                                if (!data.profile.onboardingDone) {
                                    Surface(
                                        onClick = onOpenGoals,
                                        shape = RoundedCornerShape(50),
                                        color = GraphiteElevated,
                                        border = BorderStroke(1.dp, SubtleStroke),
                                        modifier = Modifier.testTag("setup_goals_chip")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Tune,
                                                contentDescription = "Set goals",
                                                tint = ProteinBlue,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Set Goals",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7-Day Date Strip
                    item {
                        DateStripRow(
                            dateKeys = recentDates,
                            selectedDateKey = data.selectedDateKey,
                            loggedDateKeys = data.loggedDateKeys,
                            onSelectDate = onSelectDate
                        )
                    }

                    // Hero Calorie Ring & Macro Bars Card
                    item {
                        HeroCalorieMacroCard(
                            consumedCalories = data.consumedCalories,
                            calorieGoal = data.profile.calorieGoal.toInt(),
                            proteinG = data.consumedProteinG,
                            proteinGoalG = data.profile.proteinGoalG.toInt(),
                            carbsG = data.consumedCarbsG,
                            carbsGoalG = data.profile.carbsGoalG.toInt(),
                            fatG = data.consumedFatG,
                            fatGoalG = data.profile.fatGoalG.toInt()
                        )
                    }

                    // Hydration Counter
                    item {
                        WaterHydrationCard(
                            waterMl = data.waterMl,
                            waterGoalMl = data.profile.waterGoalMl,
                            onAdjust = { delta -> onAdjustWater(data.waterMl, delta) }
                        )
                    }

                    // Meals Section Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Logged Meals (${data.meals.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            TextButton(
                                onClick = onOpenScan,
                                modifier = Modifier.testTag("add_meal_text_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = CalorieCoral,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Log Meal",
                                    color = CalorieCoral,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    if (data.meals.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = SlateSurface,
                                border = BorderStroke(1.dp, SubtleStroke),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_empty_plate),
                                        contentDescription = "Empty meal log illustration",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                    Text(
                                        text = stringResource(R.string.empty_meals_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.empty_meals_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Button(
                                        onClick = onOpenScan,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CalorieCoral,
                                            contentColor = Color(0xFF140704)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("empty_state_scan_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Snap or Describe a Meal",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(data.meals, key = { it.id }) { meal ->
                            MealEntryCard(
                                meal = meal,
                                onClick = { inspectingMeal = meal }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    inspectingMeal?.let { meal ->
        MealDetailBottomSheet(
            meal = meal,
            onDismiss = { inspectingMeal = null },
            onSave = { name, mealType, calories, protein, carbs, fat, multiplier ->
                onUpdateMeal(meal.id, name, mealType, calories, protein, carbs, fat, multiplier)
                inspectingMeal = null
            },
            onDelete = {
                onDeleteMeal(meal.id)
                inspectingMeal = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealDetailBottomSheet(
    meal: MealEntry,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long, Long, Long, Double) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(meal.id) { mutableStateOf(meal.name) }
    var selectedType by remember(meal.id) { mutableStateOf(MealType.fromWire(meal.mealType)) }
    var multiplier by remember(meal.id) { mutableDoubleStateOf(meal.servingMultiplier) }

    val baseFactor = if (meal.servingMultiplier > 0.0) meal.servingMultiplier else 1.0
    val baseCalories = meal.calories / baseFactor
    val baseProtein = meal.proteinG / baseFactor
    val baseCarbs = meal.carbsG / baseFactor
    val baseFat = meal.fatG / baseFactor

    val scaledCalories = (baseCalories * multiplier).roundToLong().coerceIn(0L, 10000L)
    val scaledProtein = (baseProtein * multiplier).roundToLong().coerceIn(0L, 1000L)
    val scaledCarbs = (baseCarbs * multiplier).roundToLong().coerceIn(0L, 1500L)
    val scaledFat = (baseFat * multiplier).roundToLong().coerceIn(0L, 1000L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SlateSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Meal Details",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Meal Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Meal Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MealType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalorieCoralSoft,
                            selectedLabelColor = CalorieCoral
                        )
                    )
                }
            }

            // Portion Multiplier Selector
            Text(
                text = "Serving Multiplier",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.5, 0.75, 1.0, 1.5, 2.0).forEach { factor ->
                    FilterChip(
                        selected = kotlin.math.abs(multiplier - factor) < 0.01,
                        onClick = { multiplier = factor },
                        label = { Text("${factor}x", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalorieCoralSoft,
                            selectedLabelColor = CalorieCoral
                        )
                    )
                }
            }

            // Scaled Macro Preview
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GraphiteElevated,
                border = BorderStroke(1.dp, SubtleStroke)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MacroPreviewCell("Calories", "$scaledCalories", CalorieCoral)
                    MacroPreviewCell("Protein", "${scaledProtein}g", ProteinBlue)
                    MacroPreviewCell("Carbs", "${scaledCarbs}g", CarbsAmber)
                    MacroPreviewCell("Fat", "${scaledFat}g", FatEmerald)
                }
            }

            if (meal.aiNotes.isNotBlank()) {
                Text(
                    text = meal.aiNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            if (meal.ingredients.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Detected Breakdown",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    meal.ingredients.take(6).forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose),
                    border = BorderStroke(1.dp, ErrorRose.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("delete_meal_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete meal",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }

                Button(
                    onClick = {
                        onSave(
                            name,
                            selectedType.wire,
                            scaledCalories,
                            scaledProtein,
                            scaledCarbs,
                            scaledFat,
                            multiplier
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CalorieCoral,
                        contentColor = Color(0xFF140704)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("update_meal_button")
                ) {
                    Text("Save Changes", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun MacroPreviewCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
