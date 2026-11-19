package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.DateUtils
import com.example.model.UserProfile
import com.example.ui.components.WeeklyCalorieChart
import com.example.ui.components.WeightTrendChart
import com.example.ui.state.ProgressGoalsData
import com.example.ui.state.UiState
import com.example.ui.theme.CalorieCoral
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.FatEmerald
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ProteinBlue
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SubtleStroke
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProgressScreen(
    uiState: UiState<ProgressGoalsData>,
    onLogWeight: (Double, String, UserProfile) -> Unit,
    onDeleteWeight: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showWeightDialog by remember { mutableStateOf(false) }

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
                val data = uiState.data
                val useMetric = data.profile.useMetricUnits
                val unitLabel = if (useMetric) "kg" else "lbs"
                val latestWeightKg = data.weights.lastOrNull()?.weightKg ?: data.profile.currentWeightKg
                val displayCurrent = if (useMetric) latestWeightKg else latestWeightKg * 2.20462
                val displayTarget = if (useMetric) data.profile.targetWeightKg else data.profile.targetWeightKg * 2.20462

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Progress & Trends",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                    }

                    // KPI Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiCard(
                                title = "Active Streak",
                                value = "${data.streakDays} days",
                                subtitle = "${data.totalMealsLogged} meals logged",
                                accent = CalorieCoral,
                                icon = Icons.Filled.LocalFireDepartment,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "7-Day Avg",
                                value = "${data.averageCalories7d}",
                                subtitle = "kcal / active day",
                                accent = FatEmerald,
                                icon = Icons.Filled.MonitorWeight,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Weight Trend Card
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
                                    Column {
                                        Text(
                                            text = "Weight Journey",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Now ${String.format("%.1f", displayCurrent)} $unitLabel • Goal ${String.format("%.1f", displayTarget)} $unitLabel",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    Button(
                                        onClick = { showWeightDialog = true },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ProteinBlue,
                                            contentColor = Color(0xFF041428)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("log_weight_button")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Log Weight", style = MaterialTheme.typography.labelLarge)
                                    }
                                }

                                WeightTrendChart(
                                    weights = data.weights,
                                    targetWeightKg = data.profile.targetWeightKg,
                                    useMetric = useMetric
                                )
                            }
                        }
                    }

                    // 7-Day Calorie Adherence Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = SlateSurface,
                            border = BorderStroke(1.dp, SubtleStroke),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "7-Day Calorie Intake",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Goal ${data.profile.calorieGoal} kcal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                                WeeklyCalorieChart(
                                    weekly = data.weeklyCalories,
                                    calorieGoal = data.profile.calorieGoal.toInt()
                                )
                            }
                        }
                    }

                    // Recent Weight Check-ins
                    if (data.weights.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent Check-ins",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        items(data.weights.reversed().take(10), key = { it.id }) { entry ->
                            val shownVal = if (useMetric) entry.weightKg else entry.weightKg * 2.20462
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SlateSurface,
                                border = BorderStroke(1.dp, SubtleStroke),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${String.format("%.1f", shownVal)} $unitLabel",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = buildString {
                                                append(DateUtils.friendlyDate(entry.dateKey))
                                                if (entry.note.isNotBlank()) append(" • ${entry.note}")
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    IconButton(onClick = { onDeleteWeight(entry.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete weight entry",
                                            tint = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }

                if (showWeightDialog) {
                    LogWeightDialog(
                        initialWeight = displayCurrent,
                        useMetric = useMetric,
                        onDismiss = { showWeightDialog = false },
                        onConfirm = { inputWeight, note ->
                            val kg = if (useMetric) inputWeight else inputWeight / 2.20462
                            onLogWeight(kg.coerceIn(20.0, 350.0), note, data.profile)
                            showWeightDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SubtleStroke)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun LogWeightDialog(
    initialWeight: Double,
    useMetric: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var weightText by remember { mutableStateOf(String.format("%.1f", initialWeight)) }
    var noteText by remember { mutableStateOf("") }
    val unitLabel = if (useMetric) "kg" else "lbs"
    val parsed = weightText.toDoubleOrNull()
    val valid = parsed != null && parsed in 20.0..770.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteElevated,
        title = { Text("Log Weight Check-In", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight ($unitLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (parsed != null) onConfirm(parsed, noteText) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalorieCoral,
                    contentColor = Color(0xFF140704)
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
