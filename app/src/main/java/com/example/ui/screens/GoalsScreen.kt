package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.ActivityLevel
import com.example.model.BiologicalSex
import com.example.model.GoalCalculator
import com.example.model.GoalType
import com.example.model.MacroPreset
import com.example.model.UserProfile
import com.example.ui.state.ProgressGoalsData
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
import com.example.ui.theme.WaterCyan
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(
    uiState: UiState<ProgressGoalsData>,
    userEmail: String?,
    onSaveProfile: (UserProfile) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(uiState.message, color = ErrorRose)
                }
            }

            is UiState.Success -> {
                val profile = uiState.data.profile
                var useMetric by remember(profile.userId, profile.useMetricUnits) {
                    mutableStateOf(profile.useMetricUnits)
                }
                var sex by remember(profile.userId, profile.sex) {
                    mutableStateOf(BiologicalSex.fromWire(profile.sex))
                }
                var ageText by remember(profile.userId, profile.age) {
                    mutableStateOf(profile.age.toString())
                }
                var heightText by remember(profile.userId, profile.heightCm, useMetric) {
                    val v = if (useMetric) profile.heightCm else profile.heightCm / 2.54
                    mutableStateOf(v.roundToInt().toString())
                }
                var weightText by remember(profile.userId, profile.currentWeightKg, useMetric) {
                    val v = if (useMetric) profile.currentWeightKg else profile.currentWeightKg * 2.20462
                    mutableStateOf(String.format("%.1f", v))
                }
                var targetWeightText by remember(profile.userId, profile.targetWeightKg, useMetric) {
                    val v = if (useMetric) profile.targetWeightKg else profile.targetWeightKg * 2.20462
                    mutableStateOf(String.format("%.1f", v))
                }
                var activity by remember(profile.userId, profile.activityLevel) {
                    mutableStateOf(ActivityLevel.fromWire(profile.activityLevel))
                }
                var goalType by remember(profile.userId, profile.goalType) {
                    mutableStateOf(GoalType.fromWire(profile.goalType))
                }
                var preset by remember { mutableStateOf(MacroPreset.BALANCED) }

                var calorieText by remember(profile.calorieGoal) { mutableStateOf(profile.calorieGoal.toString()) }
                var proteinText by remember(profile.proteinGoalG) { mutableStateOf(profile.proteinGoalG.toString()) }
                var carbsText by remember(profile.carbsGoalG) { mutableStateOf(profile.carbsGoalG.toString()) }
                var fatText by remember(profile.fatGoalG) { mutableStateOf(profile.fatGoalG.toString()) }
                var waterGoalMl by remember(profile.waterGoalMl) { mutableIntStateOf(profile.waterGoalMl.toInt()) }

                val weightKg = (weightText.toDoubleOrNull() ?: 72.0).let { if (useMetric) it else it / 2.20462 }.coerceIn(20.0, 350.0)
                val targetKg = (targetWeightText.toDoubleOrNull() ?: 70.0).let { if (useMetric) it else it / 2.20462 }.coerceIn(20.0, 350.0)
                val heightCm = (heightText.toDoubleOrNull() ?: 175.0).let { if (useMetric) it else it * 2.54 }.coerceIn(80.0, 260.0)
                val ageVal = (ageText.toIntOrNull() ?: 28).coerceIn(13, 120)

                val previewCalc = GoalCalculator.calculate(
                    weightKg = weightKg,
                    heightCm = heightCm,
                    age = ageVal,
                    sex = sex,
                    activityLevel = activity,
                    goalType = goalType,
                    preset = preset
                )

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Goals & Calculator",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                    }

                    // Calculator Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = SlateSurface,
                            border = BorderStroke(1.dp, SubtleStroke),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Personalized TDEE Calculator",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (useMetric) "Metric" else "Imperial",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary
                                        )
                                        Switch(
                                            checked = useMetric,
                                            onCheckedChange = { useMetric = it }
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BiologicalSex.entries.forEach { s ->
                                        FilterChip(
                                            selected = sex == s,
                                            onClick = { sex = s },
                                            label = { Text(s.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CalorieCoralSoft,
                                                selectedLabelColor = CalorieCoral
                                            )
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ageText,
                                        onValueChange = { ageText = it.filter(Char::isDigit) },
                                        label = { Text("Age") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = heightText,
                                        onValueChange = { heightText = it.filter(Char::isDigit) },
                                        label = { Text(if (useMetric) "Height (cm)" else "Height (in)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = weightText,
                                        onValueChange = { weightText = it },
                                        label = { Text(if (useMetric) "Current (kg)" else "Current (lbs)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = targetWeightText,
                                        onValueChange = { targetWeightText = it },
                                        label = { Text(if (useMetric) "Target (kg)" else "Target (lbs)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text("Activity Level", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ActivityLevel.entries.forEach { level ->
                                        FilterChip(
                                            selected = activity == level,
                                            onClick = { activity = level },
                                            label = { Text(level.label, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CalorieCoralSoft,
                                                selectedLabelColor = CalorieCoral
                                            )
                                        )
                                    }
                                }

                                Text("Goal Pace", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GoalType.entries.forEach { g ->
                                        FilterChip(
                                            selected = goalType == g,
                                            onClick = { goalType = g },
                                            label = { Text(g.label, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CalorieCoralSoft,
                                                selectedLabelColor = CalorieCoral
                                            )
                                        )
                                    }
                                }

                                Text("Macro Split", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MacroPreset.entries.forEach { p ->
                                        FilterChip(
                                            selected = preset == p,
                                            onClick = { preset = p },
                                            label = { Text(p.label, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CalorieCoralSoft,
                                                selectedLabelColor = CalorieCoral
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = GraphiteElevated,
                                    border = BorderStroke(1.dp, SubtleStroke)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Estimated BMR ${previewCalc.bmr} kcal • TDEE ${previewCalc.tdee} kcal",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "Recommended: ${previewCalc.calorieGoal} kcal  (${previewCalc.proteinGoalG}P / ${previewCalc.carbsGoalG}C / ${previewCalc.fatGoalG}F)",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = FatEmerald
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                calorieText = previewCalc.calorieGoal.toString()
                                                proteinText = previewCalc.proteinGoalG.toString()
                                                carbsText = previewCalc.carbsGoalG.toString()
                                                fatText = previewCalc.fatGoalG.toString()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("apply_calculated_goals_button")
                                        ) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = CalorieCoral)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Apply Recommended Targets", color = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Daily Targets Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = SlateSurface,
                            border = BorderStroke(1.dp, SubtleStroke),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Daily Nutrition Targets",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = calorieText,
                                        onValueChange = { calorieText = it.filter(Char::isDigit) },
                                        label = { Text("Calories (kcal)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = proteinText,
                                        onValueChange = { proteinText = it.filter(Char::isDigit) },
                                        label = { Text("Protein (g)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = carbsText,
                                        onValueChange = { carbsText = it.filter(Char::isDigit) },
                                        label = { Text("Carbs (g)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = fatText,
                                        onValueChange = { fatText = it.filter(Char::isDigit) },
                                        label = { Text("Fat (g)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Daily Water Goal", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                        Text(
                                            "$waterGoalMl ml",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = WaterCyan
                                        )
                                    }
                                    Slider(
                                        value = waterGoalMl.toFloat(),
                                        onValueChange = { waterGoalMl = ((it / 250f).roundToInt() * 250).coerceIn(1000, 5000) },
                                        valueRange = 1000f..5000f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = WaterCyan,
                                            activeTrackColor = WaterCyan
                                        )
                                    )
                                }

                                Button(
                                    onClick = {
                                        val updated = profile.copy(
                                            calorieGoal = (calorieText.toLongOrNull() ?: profile.calorieGoal).coerceIn(800L, 8000L),
                                            proteinGoalG = (proteinText.toLongOrNull() ?: profile.proteinGoalG).coerceIn(10L, 600L),
                                            carbsGoalG = (carbsText.toLongOrNull() ?: profile.carbsGoalG).coerceIn(10L, 1000L),
                                            fatGoalG = (fatText.toLongOrNull() ?: profile.fatGoalG).coerceIn(10L, 400L),
                                            waterGoalMl = waterGoalMl.toLong().coerceIn(500L, 8000L),
                                            currentWeightKg = weightKg,
                                            targetWeightKg = targetKg,
                                            heightCm = heightCm,
                                            age = ageVal.toLong(),
                                            sex = sex.wire,
                                            activityLevel = activity.wire,
                                            goalType = goalType.wire,
                                            useMetricUnits = useMetric,
                                            onboardingDone = true
                                        )
                                        onSaveProfile(updated)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CalorieCoral,
                                        contentColor = Color(0xFF140704)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("save_goals_button")
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Goals", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    // Account Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = SlateSurface,
                            border = BorderStroke(1.dp, SubtleStroke),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = userEmail ?: "Google Account Connected",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                OutlinedButton(
                                    onClick = onSignOut,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, ErrorRose.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign out",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sign Out")
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }
}
