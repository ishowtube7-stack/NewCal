package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DateUtils
import com.example.model.MealEntry
import com.example.model.MealType
import com.example.model.WeightEntry
import com.example.ui.state.DailyCalorieSummary
import com.example.ui.theme.CalorieCoral
import com.example.ui.theme.CalorieCoralSoft
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.CarbsAmberSoft
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.FatEmerald
import com.example.ui.theme.FatEmeraldSoft
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ProteinBlue
import com.example.ui.theme.ProteinBlueSoft
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SubtleStroke
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WaterCyan
import com.example.ui.theme.WaterCyanSoft
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StreakBadge(streakDays: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (streakDays > 0) CalorieCoralSoft else GraphiteElevated,
        border = BorderStroke(1.dp, if (streakDays > 0) CalorieCoral.copy(alpha = 0.5f) else SubtleStroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "Logging streak",
                tint = if (streakDays > 0) CalorieCoral else TextTertiary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${streakDays}d",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (streakDays > 0) TextPrimary else TextSecondary
                )
            )
        }
    }
}

@Composable
fun DateStripRow(
    dateKeys: List<String>,
    selectedDateKey: String,
    loggedDateKeys: Set<String>,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(dateKeys, key = { it }) { key ->
            val isSelected = key == selectedDateKey
            val hasMeals = loggedDateKeys.contains(key)
            Surface(
                modifier = Modifier
                    .width(50.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectDate(key) }
                    .testTag("date_chip_$key"),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) CalorieCoral else SlateSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) CalorieCoral else SubtleStroke
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = DateUtils.dayOfWeekShort(key),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color(0xFF140704) else TextSecondary
                    )
                    Text(
                        text = DateUtils.dayNumber(key),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isSelected) Color(0xFF140704) else TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected && hasMeals -> Color(0xFF140704)
                                    hasMeals -> FatEmerald
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun HeroCalorieMacroCard(
    consumedCalories: Int,
    calorieGoal: Int,
    proteinG: Int,
    proteinGoalG: Int,
    carbsG: Int,
    carbsGoalG: Int,
    fatG: Int,
    fatGoalG: Int,
    modifier: Modifier = Modifier
) {
    val safeGoal = calorieGoal.coerceAtLeast(1)
    val rawProgress = consumedCalories.toFloat() / safeGoal.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress.coerceIn(0f, 1.15f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "calorieRingProgress"
    )
    val remaining = calorieGoal - consumedCalories
    val isOver = remaining < 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SubtleStroke)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GraphiteElevated.copy(alpha = 0.65f),
                            SlateSurface
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Radial Calorie Ring
                Box(
                    modifier = Modifier.size(156.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ringColor = if (isOver) ErrorRose else CalorieCoral
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 15.dp.toPx()
                        val inset = strokeWidth / 2f
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val startAngle = 140f
                        val maxSweep = 260f

                        // Background Track
                        drawArc(
                            color = GraphiteElevated,
                            startAngle = startAngle,
                            sweepAngle = maxSweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Active Energy Arc
                        val sweep = (animatedProgress.coerceIn(0f, 1f)) * maxSweep
                        if (sweep > 0.5f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(ringColor, CarbsAmber, ringColor)
                                ),
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = abs(remaining).toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 32.sp,
                                color = if (isOver) ErrorRose else TextPrimary
                            )
                        )
                        Text(
                            text = if (isOver) "kcal over" else "kcal left",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOver) ErrorRose else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Consumed vs Goal Telemetry Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnergyStatRow(
                        label = "Eaten",
                        value = "$consumedCalories kcal",
                        accent = CalorieCoral
                    )
                    EnergyStatRow(
                        label = "Daily Goal",
                        value = "$calorieGoal kcal",
                        accent = TextSecondary
                    )
                    val pct = ((consumedCalories.toFloat() / safeGoal) * 100f).roundToInt()
                    EnergyStatRow(
                        label = "Progress",
                        value = "$pct%",
                        accent = if (isOver) ErrorRose else FatEmerald
                    )
                }
            }

            // Protein / Carbs / Fat Macro Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroProgressColumn(
                    label = "Protein",
                    consumed = proteinG,
                    goal = proteinGoalG,
                    accent = ProteinBlue,
                    trackBg = ProteinBlueSoft,
                    modifier = Modifier.weight(1f)
                )
                MacroProgressColumn(
                    label = "Carbs",
                    consumed = carbsG,
                    goal = carbsGoalG,
                    accent = CarbsAmber,
                    trackBg = CarbsAmberSoft,
                    modifier = Modifier.weight(1f)
                )
                MacroProgressColumn(
                    label = "Fat",
                    consumed = fatG,
                    goal = fatGoalG,
                    accent = FatEmerald,
                    trackBg = FatEmeraldSoft,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EnergyStatRow(
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
    }
}

@Composable
fun MacroProgressColumn(
    label: String,
    consumed: Int,
    goal: Int,
    accent: Color,
    trackBg: Color,
    modifier: Modifier = Modifier
) {
    val safeGoal = goal.coerceAtLeast(1)
    val ratio by animateFloatAsState(
        targetValue = (consumed.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "macroBar_$label"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = GraphiteElevated,
        border = BorderStroke(1.dp, SubtleStroke)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextSecondary
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${consumed}g",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "/${goal}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(trackBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                )
            }
        }
    }
}

@Composable
fun WaterHydrationCard(
    waterMl: Long,
    waterGoalMl: Long,
    onAdjust: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeGoal = waterGoalMl.coerceAtLeast(500L)
    val progress by animateFloatAsState(
        targetValue = (waterMl.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "waterProgress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SubtleStroke)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(WaterCyanSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = "Water intake",
                            tint = WaterCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Hydration",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${waterMl} / ${safeGoal} ml",
                            style = MaterialTheme.typography.labelMedium,
                            color = WaterCyan
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onAdjust(-250L) },
                        enabled = waterMl > 0L,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .border(1.dp, SubtleStroke, CircleShape)
                            .testTag("water_minus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Subtract 250 ml water",
                            tint = if (waterMl > 0L) TextPrimary else TextTertiary
                        )
                    }
                    Button(
                        onClick = { onAdjust(250L) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WaterCyanSoft,
                            contentColor = WaterCyan
                        ),
                        border = BorderStroke(1.dp, WaterCyan.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("water_plus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "250 ml",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(GraphiteElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(listOf(WaterCyan, ProteinBlue))
                        )
                )
            }
        }
    }
}

@Composable
fun MealEntryCard(
    meal: MealEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mealType = MealType.fromWire(meal.mealType)
    val icon: ImageVector = when (mealType) {
        MealType.BREAKFAST -> Icons.Outlined.FreeBreakfast
        MealType.LUNCH -> Icons.Outlined.LunchDining
        MealType.DINNER -> Icons.Outlined.DinnerDining
        MealType.SNACK -> Icons.Outlined.BakeryDining
    }
    val scoreColor = when {
        meal.healthScore >= 8 -> FatEmerald
        meal.healthScore >= 5 -> CarbsAmber
        else -> ErrorRose
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("meal_card_${meal.id}"),
        shape = RoundedCornerShape(20.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SubtleStroke)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GraphiteElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = mealType.label,
                            tint = CalorieCoral,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = meal.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = mealType.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            val timeStr = DateUtils.formatTime(meal.loggedAtEpochMs)
                            if (timeStr.isNotEmpty()) {
                                Text("•", color = TextTertiary)
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextTertiary
                                )
                            }
                            if (meal.inputSource == "photo") {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Photo scan",
                                    tint = WaterCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                            } else if (meal.inputSource == "text") {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "AI text scan",
                                    tint = ProteinBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${meal.calories} kcal",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Score ${meal.healthScore}/10",
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroBadgePill(label = "P ${meal.proteinG}g", color = ProteinBlue, bg = ProteinBlueSoft)
                MacroBadgePill(label = "C ${meal.carbsG}g", color = CarbsAmber, bg = CarbsAmberSoft)
                MacroBadgePill(label = "F ${meal.fatG}g", color = FatEmerald, bg = FatEmeraldSoft)
                if (meal.servingMultiplier != 1.0) {
                    MacroBadgePill(
                        label = "${meal.servingMultiplier}x",
                        color = TextSecondary,
                        bg = GraphiteElevated
                    )
                }
            }
        }
    }
}

@Composable
fun MacroBadgePill(
    label: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun WeightTrendChart(
    weights: List<WeightEntry>,
    targetWeightKg: Double,
    useMetric: Boolean,
    modifier: Modifier = Modifier
) {
    if (weights.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GraphiteElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log your first weigh-in to see your trend curve",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        return
    }

    val recent = weights.takeLast(14)
    val values = recent.map { if (useMetric) it.weightKg else it.weightKg * 2.20462 }
    val targetVal = if (useMetric) targetWeightKg else targetWeightKg * 2.20462
    val minVal = (minOf(values.min(), targetVal) - 2.0).coerceAtLeast(0.0)
    val maxVal = maxOf(values.max(), targetVal) + 2.0
    val span = (maxVal - minVal).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            val w = size.width
            val h = size.height

            // Target dashed line
            val targetY = (h - ((targetVal - minVal) / span) * h).toFloat().coerceIn(0f, h)
            drawLine(
                color = FatEmerald.copy(alpha = 0.65f),
                start = Offset(0f, targetY),
                end = Offset(w, targetY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
            )

            val points = values.mapIndexed { idx, v ->
                val x = if (values.size == 1) w / 2f else (idx.toFloat() / (values.size - 1)) * w
                val y = (h - ((v - minVal) / span) * h).toFloat().coerceIn(4f, h - 4f)
                Offset(x, y)
            }

            if (points.size >= 2) {
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, h)
                    lineTo(points.first().x, h)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(ProteinBlue.copy(alpha = 0.32f), Color.Transparent)
                    )
                )
                drawPath(
                    path = linePath,
                    color = ProteinBlue,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            points.forEachIndexed { idx, pt ->
                drawCircle(
                    color = SlateSurface,
                    radius = 5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = if (idx == points.lastIndex) CalorieCoral else ProteinBlue,
                    radius = 3.5.dp.toPx(),
                    center = pt
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Target: ${String.format("%.1f", targetVal)} ${if (useMetric) "kg" else "lbs"}",
                style = MaterialTheme.typography.labelSmall,
                color = FatEmerald
            )
            Text(
                text = "Latest: ${String.format("%.1f", values.last())} ${if (useMetric) "kg" else "lbs"}",
                style = MaterialTheme.typography.labelSmall,
                color = CalorieCoral
            )
        }
    }
}

@Composable
fun WeeklyCalorieChart(
    weekly: List<DailyCalorieSummary>,
    calorieGoal: Int,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(calorieGoal, weekly.maxOfOrNull { it.calories } ?: 0, 500).toFloat()
    val todayKey = DateUtils.todayKey()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(top = 8.dp)
        ) {
            val w = size.width
            val h = size.height
            val barSlot = w / weekly.size.coerceAtLeast(1)
            val barWidth = (barSlot * 0.48f).coerceAtMost(28.dp.toPx())

            // Goal dashed line
            val goalY = h - ((calorieGoal.toFloat() / maxVal) * h).coerceIn(0f, h)
            drawLine(
                color = TextTertiary.copy(alpha = 0.7f),
                start = Offset(0f, goalY),
                end = Offset(w, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )

            weekly.forEachIndexed { index, day ->
                val centerX = barSlot * index + barSlot / 2f
                val barHeight = ((day.calories.toFloat() / maxVal) * h).coerceAtLeast(
                    if (day.calories > 0) 6.dp.toPx() else 2.dp.toPx()
                )
                val topLeft = Offset(centerX - barWidth / 2f, h - barHeight)
                val barColor = when {
                    day.calories == 0 -> GraphiteElevated
                    day.calories > calorieGoal -> ErrorRose
                    day.dateKey == todayKey -> CalorieCoral
                    else -> FatEmerald
                }
                drawRoundRect(
                    color = barColor,
                    topLeft = topLeft,
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekly.forEach { day ->
                Text(
                    text = day.shortDay.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.dateKey == todayKey) CalorieCoral else TextSecondary
                )
            }
        }
    }
}
