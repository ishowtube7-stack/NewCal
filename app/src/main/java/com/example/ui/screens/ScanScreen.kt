package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.AiMealAnalysis
import com.example.model.MealType
import com.example.ui.state.ScanUiState
import com.example.ui.theme.CalorieCoral
import com.example.ui.theme.CalorieCoralSoft
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.ErrorRoseSoft
import com.example.ui.theme.FatEmerald
import com.example.ui.theme.FatEmeraldSoft
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ProteinBlue
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SubtleStroke
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaterCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class ScanMode(val label: String) {
    PHOTO("Photo AI"),
    DESCRIBE("Describe")
}

@Composable
fun ScanScreen(
    uiState: ScanUiState,
    selectedDateKey: String,
    onAnalyzePhoto: (Bitmap, String) -> Unit,
    onAnalyzeDescription: (String) -> Unit,
    onOpenManualEntry: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateMealType: (MealType) -> Unit,
    onUpdateMultiplier: (Double) -> Unit,
    onUpdateBaseMacros: (Int, Int, Int, Int) -> Unit,
    onSaveMeal: (String) -> Unit,
    onReset: () -> Unit,
    onMealSavedNavigateToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var modeIndex by remember { mutableIntStateOf(0) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoHint by remember { mutableStateOf("") }
    var mealDescription by remember { mutableStateOf("") }
    var cameraPermissionDenied by remember { mutableStateOf(false) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is ScanUiState.Saved) {
            selectedBitmap = null
            photoHint = ""
            mealDescription = ""
            onReset()
            onMealSavedNavigateToday()
        }
    }

    if (uiState is ScanUiState.Review || uiState is ScanUiState.Error) {
        BackHandler { onReset() }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingPhotoUri
        if (success && uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { loadScaledBitmap(context, uri) }
                if (bmp != null) {
                    selectedBitmap = bmp
                }
            }
        }
    }

    val launchCameraCapture = {
        val uri = createTempPhotoUri(context)
        pendingPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPermissionDenied = false
            launchCameraCapture()
        } else {
            cameraPermissionDenied = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { loadScaledBitmap(context, uri) }
                if (bmp != null) {
                    selectedBitmap = bmp
                    cameraPermissionDenied = false
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI Meal Studio",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Logging for $selectedDateKey",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    if (uiState is ScanUiState.Review) {
                        IconButton(onClick = onReset) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to scanner",
                                tint = TextPrimary
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onOpenManualEntry,
                            modifier = Modifier.testTag("manual_entry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = ProteinBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manual", color = ProteinBlue)
                        }
                    }
                }
            }

            when (uiState) {
                is ScanUiState.Analyzing -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = SlateSurface,
                            border = BorderStroke(1.dp, CalorieCoral.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = CalorieCoral,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "CalSnap AI is scanning...",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
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

                is ScanUiState.Error -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = ErrorRoseSoft,
                            border = BorderStroke(1.dp, ErrorRose.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Analysis Couldn't Complete",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ErrorRose
                                )
                                Text(
                                    text = uiState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Button(
                                    onClick = onReset,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CalorieCoral,
                                        contentColor = Color(0xFF140704)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Try Again")
                                }
                            }
                        }
                    }
                }

                is ScanUiState.Review, is ScanUiState.Saving -> {
                    val draft = when (uiState) {
                        is ScanUiState.Review -> uiState.draft
                        is ScanUiState.Saving -> uiState.draft
                        else -> error("unreachable")
                    }
                    val isSaving = uiState is ScanUiState.Saving
                    item {
                        ReviewMealCard(
                            draft = draft,
                            isSaving = isSaving,
                            onUpdateName = onUpdateName,
                            onUpdateMealType = onUpdateMealType,
                            onUpdateMultiplier = onUpdateMultiplier,
                            onUpdateBaseMacros = onUpdateBaseMacros,
                            onSave = { onSaveMeal(selectedDateKey) },
                            onCancel = onReset
                        )
                    }
                }

                ScanUiState.Idle, is ScanUiState.Saved -> {
                    item {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ScanMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = modeIndex == index,
                                    onClick = { modeIndex = index },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = ScanMode.entries.size
                                    ),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = CalorieCoral,
                                        activeContentColor = Color(0xFF140704),
                                        inactiveContainerColor = SlateSurface,
                                        inactiveContentColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("scan_mode_${mode.name.lowercase()}")
                                ) {
                                    Text(mode.label, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    if (modeIndex == 0) {
                        item {
                            PhotoScanCard(
                                selectedBitmap = selectedBitmap,
                                photoHint = photoHint,
                                cameraPermissionDenied = cameraPermissionDenied,
                                onHintChange = { photoHint = it },
                                onTakePhotoClick = {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        launchCameraCapture()
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                onPickGalleryClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onClearPhoto = { selectedBitmap = null },
                                onAnalyze = {
                                    selectedBitmap?.let { bmp -> onAnalyzePhoto(bmp, photoHint) }
                                }
                            )
                        }
                    } else {
                        item {
                            DescribeMealCard(
                                description = mealDescription,
                                onDescriptionChange = { mealDescription = it },
                                onAnalyze = { onAnalyzeDescription(mealDescription) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun PhotoScanCard(
    selectedBitmap: Bitmap?,
    photoHint: String,
    cameraPermissionDenied: Boolean,
    onHintChange: (String) -> Unit,
    onTakePhotoClick: () -> Unit,
    onPickGalleryClick: () -> Unit,
    onClearPhoto: () -> Unit,
    onAnalyze: () -> Unit
) {
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
            if (selectedBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Image(
                        bitmap = selectedBitmap.asImageBitmap(),
                        contentDescription = "Selected meal photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        onClick = onClearPhoto,
                        shape = RoundedCornerShape(50),
                        color = Color(0xCC0B0D12),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(GraphiteElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = CalorieCoral,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Capture or choose a food photo",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Gemini AI identifies portions, calories & macros automatically",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (cameraPermissionDenied) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GraphiteElevated,
                    border = BorderStroke(1.dp, CarbsAmber.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = stringResource(R.string.camera_permission_rationale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CarbsAmber,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onTakePhotoClick,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SubtleStroke),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("take_photo_button")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = CalorieCoral)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = onPickGalleryClick,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SubtleStroke),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("pick_photo_button")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = WaterCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", color = TextPrimary)
                }
            }

            OutlinedTextField(
                value = photoHint,
                onValueChange = onHintChange,
                label = { Text("Optional context (e.g., extra olive oil, 200g chicken)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onAnalyze,
                enabled = selectedBitmap != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalorieCoral,
                    contentColor = Color(0xFF140704)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("analyze_photo_button")
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Analyze Meal Photo",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun DescribeMealCard(
    description: String,
    onDescriptionChange: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    val quickPrompts = listOf(
        "2 scrambled eggs, avocado sourdough toast & iced oat latte",
        "Grilled chicken breast 200g, jasmine rice 1 cup & steamed broccoli",
        "Greek yogurt bowl with blueberries, granola & honey",
        "Double cheeseburger with medium fries"
    )

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
                text = "Describe what you ate",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Meal description & portions") },
                placeholder = { Text("e.g., 150g grilled salmon, 1 cup quinoa, half avocado") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("meal_description_input")
            )

            Text(
                text = "Quick Examples",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quickPrompts.forEach { sample ->
                    Surface(
                        onClick = { onDescriptionChange(sample) },
                        shape = RoundedCornerShape(12.dp),
                        color = GraphiteElevated,
                        border = BorderStroke(1.dp, SubtleStroke),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = sample,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onAnalyze,
                enabled = description.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalorieCoral,
                    contentColor = Color(0xFF140704)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("analyze_text_button")
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Calculate Nutrition with AI",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ReviewMealCard(
    draft: AiMealAnalysis,
    isSaving: Boolean,
    onUpdateName: (String) -> Unit,
    onUpdateMealType: (MealType) -> Unit,
    onUpdateMultiplier: (Double) -> Unit,
    onUpdateBaseMacros: (Int, Int, Int, Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var editingNumbers by remember { mutableStateOf(false) }
    var calInput by remember(draft.scaledCalories) { mutableStateOf(draft.scaledCalories.toString()) }
    var proteinInput by remember(draft.scaledProteinG) { mutableStateOf(draft.scaledProteinG.toString()) }
    var carbsInput by remember(draft.scaledCarbsG) { mutableStateOf(draft.scaledCarbsG.toString()) }
    var fatInput by remember(draft.scaledFatG) { mutableStateOf(draft.scaledFatG.toString()) }

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
                    text = "Review Nutrition",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = FatEmeraldSoft
                ) {
                    Text(
                        text = "Health Score ${draft.healthScore}/10",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = FatEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            OutlinedTextField(
                value = draft.name,
                onValueChange = onUpdateName,
                label = { Text("Meal Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Meal type chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MealType.entries.forEach { type ->
                    FilterChip(
                        selected = draft.mealType == type,
                        onClick = { onUpdateMealType(type) },
                        label = { Text(type.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalorieCoralSoft,
                            selectedLabelColor = CalorieCoral
                        )
                    )
                }
            }

            // Serving multiplier chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portion Scale",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                TextButton(onClick = { editingNumbers = !editingNumbers }) {
                    Text(
                        text = if (editingNumbers) "Done Editing" else "Edit Macros",
                        color = ProteinBlue
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.5, 0.75, 1.0, 1.5, 2.0).forEach { factor ->
                    FilterChip(
                        selected = kotlin.math.abs(draft.servingMultiplier - factor) < 0.01,
                        onClick = { onUpdateMultiplier(factor) },
                        label = { Text("${factor}x", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalorieCoralSoft,
                            selectedLabelColor = CalorieCoral
                        )
                    )
                }
            }

            if (editingNumbers) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = calInput,
                        onValueChange = { calInput = it.filter(Char::isDigit) },
                        label = { Text("kcal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = proteinInput,
                        onValueChange = { proteinInput = it.filter(Char::isDigit) },
                        label = { Text("Pro g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbsInput,
                        onValueChange = { carbsInput = it.filter(Char::isDigit) },
                        label = { Text("Carb g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fatInput,
                        onValueChange = { fatInput = it.filter(Char::isDigit) },
                        label = { Text("Fat g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedButton(
                    onClick = {
                        onUpdateBaseMacros(
                            calInput.toIntOrNull() ?: draft.scaledCalories,
                            proteinInput.toIntOrNull() ?: draft.scaledProteinG,
                            carbsInput.toIntOrNull() ?: draft.scaledCarbsG,
                            fatInput.toIntOrNull() ?: draft.scaledFatG
                        )
                        editingNumbers = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Custom Values")
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = GraphiteElevated,
                    border = BorderStroke(1.dp, SubtleStroke)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ReviewStat("Calories", "${draft.scaledCalories}", CalorieCoral)
                        ReviewStat("Protein", "${draft.scaledProteinG}g", ProteinBlue)
                        ReviewStat("Carbs", "${draft.scaledCarbsG}g", CarbsAmber)
                        ReviewStat("Fat", "${draft.scaledFatG}g", FatEmerald)
                    }
                }
            }

            if (draft.aiNotes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GraphiteElevated
                ) {
                    Text(
                        text = draft.aiNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (draft.ingredients.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Detected Items",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    draft.ingredients.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("Discard")
                }
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CalorieCoral,
                        contentColor = Color(0xFF140704)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_meal_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF140704),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save to Log", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStat(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

private fun createTempPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "meal_photos").apply { mkdirs() }
    val file = File(dir, "meal_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun loadScaledBitmap(context: Context, uri: Uri, maxEdge: Int = 1280): Bitmap? {
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        var sampleSize = 1
        val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        while (maxDim / sampleSize > maxEdge) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    } catch (_: Exception) {
        null
    }
}
