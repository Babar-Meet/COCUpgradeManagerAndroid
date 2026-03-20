package com.coc.upgrade.manager.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coc.upgrade.manager.data.model.UpgradeTask
import com.coc.upgrade.manager.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showSettings by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var jsonInput by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var replaceUpgrade by remember { mutableStateOf("") }
    var replaceLevel by remember { mutableStateOf("") }
    var replaceDays by remember { mutableStateOf("") }
    var replaceHours by remember { mutableStateOf("") }
    var replaceMinutes by remember { mutableStateOf("") }
    var replaceSeconds by remember { mutableStateOf("") }
    var selectedTaskForReplace by remember { mutableStateOf<UpgradeTask?>(null) }

    // Form state
    var playerTag by remember { mutableStateOf(uiState.playerTag) }
    var selectedTh by remember { mutableStateOf("") }
    var upgradeName by remember { mutableStateOf("") }
    var level by remember { mutableStateOf(1) }
    var days by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }

    LaunchedEffect(uiState.playerTag) {
        playerTag = uiState.playerTag
    }

    // Handle import/export results
    LaunchedEffect(Unit) {
        viewModel.importExportResult.collectLatest { result ->
            when (result) {
                is ImportExportResult.Success -> {
                    // Show snackbar or toast
                }
                is ImportExportResult.Error -> {
                    // Show error
                }
                is ImportExportResult.ExportReady -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("COC Backup", result.json)
                    clipboard.setPrimaryClip(clip)
                }
            }
        }
    }

    if (showSettings) {
        SettingsScreen(
            onBack = { showSettings = false },
            onExport = { viewModel.exportToClipboard() },
            onClearAll = { showClearConfirmDialog = true }
        )
    } else {
        Scaffold(
            containerColor = BgDark,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "⚔️ Clash Builder",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BgDark,
                        titleContentColor = Primary,
                        actionIconContentColor = Primary
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Statistics Section
                item {
                    StatisticsSection(
                        totalCount = uiState.totalCount,
                        completedCount = uiState.completedCount,
                        activeCount = uiState.activeCount,
                        thStats = uiState.thStats
                    )
                }

                // Add Upgrades Section
                item {
                    AddUpgradeSection(
                        playerTag = playerTag,
                        onPlayerTagChange = {
                            playerTag = it
                            viewModel.setPlayerTag(it)
                        },
                        selectedTh = selectedTh,
                        onThChange = { selectedTh = it },
                        upgradeName = upgradeName,
                        onUpgradeNameChange = { upgradeName = it },
                        level = level,
                        onLevelChange = { level = it },
                        days = days,
                        onDaysChange = { days = it },
                        hours = hours,
                        onHoursChange = { hours = it },
                        minutes = minutes,
                        onMinutesChange = { minutes = it },
                        seconds = seconds,
                        onSecondsChange = { seconds = it },
                        inputMethod = uiState.inputMethod,
                        onInputMethodChange = { viewModel.setInputMethod(it) },
                        onAddTask = {
                            viewModel.addTask(
                                th = selectedTh,
                                upgrade = upgradeName,
                                level = if (level > 0) level else null,
                                days = days.toIntOrNull() ?: 0,
                                hours = hours.toIntOrNull() ?: 0,
                                minutes = minutes.toIntOrNull() ?: 0,
                                seconds = seconds.toIntOrNull() ?: 0
                            )
                            // Reset form
                            upgradeName = ""
                            level = 1
                            days = ""
                            hours = ""
                            minutes = ""
                            seconds = ""
                        },
                        onShowJsonDialog = { showJsonDialog = true },
                        onProcessClipboard = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString()
                                if (!text.isNullOrBlank()) {
                                    viewModel.processJsonImport(text)
                                }
                            }
                        }
                    )
                }

                // Filter Buttons
                item {
                    FilterButtonsSection(
                        currentFilter = uiState.currentFilter,
                        onFilterChange = { viewModel.setFilter(it) }
                    )
                }

                // Task List
                if (uiState.tasks.isEmpty()) {
                    item {
                        EmptyState()
                    }
                } else {
                    val groupedTasks = if (uiState.currentFilter == FilterType.BY_TH) {
                        uiState.tasks.groupBy { it.th }
                    } else {
                        mapOf("" to uiState.tasks)
                    }

                    groupedTasks.forEach { (th, tasks) ->
                        if (uiState.currentFilter == FilterType.BY_TH && th.isNotEmpty()) {
                            item {
                                Text(
                                    "Town Hall $th",
                                    color = Primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        
                        items(tasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                currentTime = uiState.currentTime,
                                onDelete = { viewModel.deleteTask(task) },
                                onReplace = {
                                    selectedTaskForReplace = task
                                    replaceUpgrade = task.upgrade
                                    replaceLevel = task.level?.toString() ?: ""
                                    replaceDays = ""
                                    replaceHours = ""
                                    replaceMinutes = ""
                                    replaceSeconds = ""
                                    showReplaceDialog = true
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // JSON Import Dialog
    if (showJsonDialog) {
        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title = { Text("Paste JSON") },
            text = {
                Column {
                    Text("Paste your Clash of Clans game export JSON or backup JSON:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("Paste JSON here...") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (jsonInput.isNotBlank()) {
                            viewModel.processJsonImport(jsonInput)
                            jsonInput = ""
                            showJsonDialog = false
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Confirm Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Tasks") },
            text = { Text("Are you sure you want to delete all upgrades?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllTasks()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Delete All", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Replace Dialog
    if (showReplaceDialog && selectedTaskForReplace != null) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("Replace Upgrade") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = replaceUpgrade,
                        onValueChange = { replaceUpgrade = it },
                        label = { Text("Upgrade Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = replaceLevel,
                        onValueChange = { replaceLevel = it },
                        label = { Text("Level") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = replaceDays,
                            onValueChange = { replaceDays = it },
                            label = { Text("Days") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = replaceHours,
                            onValueChange = { replaceHours = it },
                            label = { Text("Hours") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = replaceMinutes,
                            onValueChange = { replaceMinutes = it },
                            label = { Text("Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = replaceSeconds,
                            onValueChange = { replaceSeconds = it },
                            label = { Text("Sec") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val taskToReplace = selectedTaskForReplace
                        if (taskToReplace != null) {
                            val index = uiState.tasks.indexOf(taskToReplace)
                            if (index >= 0) {
                                viewModel.replaceTask(
                                    index = index,
                                    upgrade = replaceUpgrade,
                                    level = replaceLevel.toIntOrNull(),
                                    days = replaceDays.toIntOrNull() ?: 0,
                                    hours = replaceHours.toIntOrNull() ?: 0,
                                    minutes = replaceMinutes.toIntOrNull() ?: 0,
                                    seconds = replaceSeconds.toIntOrNull() ?: 0
                                )
                            }
                        }
                        showReplaceDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
    onClearAll: () -> Unit
) {
    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = Primary,
                    navigationIconContentColor = Primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Data Management",
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Button(
                        onClick = onExport,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Backup", color = BgDark)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = onClearAll,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Danger)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Tasks")
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsSection(
    totalCount: Int,
    completedCount: Int,
    activeCount: Int,
    thStats: List<ThStats>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "📊 Statistics",
                color = Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = totalCount.toString(), label = "Total")
                StatItem(value = completedCount.toString(), label = "Completed", highlight = true)
                StatItem(value = activeCount.toString(), label = "Active")
            }

            if (thStats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Border)
                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(thStats) { thStat ->
                        ThStatItem(
                            thLevel = thStat.thLevel,
                            timeRemaining = thStat.nextCompletionTime
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, highlight: Boolean = false) {
    val backgroundColor = if (highlight) {
        Brush.horizontalGradient(listOf(Primary.copy(alpha = 0.2f), Success.copy(alpha = 0.1f)))
    } else {
        Brush.horizontalGradient(listOf(BgCardLight, BgCardLight))
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (highlight) Primary else Border,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = if (highlight) Primary else Text,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            label,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
fun ThStatItem(thLevel: String, timeRemaining: Long) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgCardLight)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "TH$thLevel",
            color = Primary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            formatTimeRemaining(timeRemaining - System.currentTimeMillis()),
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUpgradeSection(
    playerTag: String,
    onPlayerTagChange: (String) -> Unit,
    selectedTh: String,
    onThChange: (String) -> Unit,
    upgradeName: String,
    onUpgradeNameChange: (String) -> Unit,
    level: Int,
    onLevelChange: (Int) -> Unit,
    days: String,
    onDaysChange: (String) -> Unit,
    hours: String,
    onHoursChange: (String) -> Unit,
    minutes: String,
    onMinutesChange: (String) -> Unit,
    seconds: String,
    onSecondsChange: (String) -> Unit,
    inputMethod: String,
    onInputMethodChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onShowJsonDialog: () -> Unit,
    onProcessClipboard: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "⚒️ Add Upgrades",
                color = Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tab Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                FilterChip(
                    selected = inputMethod == "manual",
                    onClick = { onInputMethodChange("manual") },
                    label = { Text("✍️ Manual") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = BgDark
                    )
                )
                FilterChip(
                    selected = inputMethod == "json",
                    onClick = { onInputMethodChange("json") },
                    label = { Text("📋 JSON") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = BgDark
                    )
                )
            }

            if (inputMethod == "manual") {
                // Manual Entry Form
                OutlinedTextField(
                    value = playerTag,
                    onValueChange = onPlayerTagChange,
                    label = { Text("Player Tag") },
                    placeholder = { Text("#YOURTAG") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                var thExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = thExpanded,
                    onExpandedChange = { thExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedTh.isEmpty()) "Select TH" else "TH $selectedTh",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Town Hall") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = thExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = thExpanded,
                        onDismissRequest = { thExpanded = false }
                    ) {
                        (1..18).forEach { th ->
                            DropdownMenuItem(
                                text = { Text("TH $th") },
                                onClick = {
                                    onThChange(th.toString())
                                    thExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                var upgradeExpanded by remember { mutableStateOf(false) }
                val upgradeList = listOf(
                    "Cannon", "Archer Tower", "Mortar", "Air Defense",
                    "Wizard Tower", "Air Sweeper", "Hidden Tesla", "Bomb Tower",
                    "X-Bow", "Inferno Tower", "Eagle Artillery", "Scattershot",
                    "Builder's Hut", "Spell Tower", "Monolith", "Multi-Archer Tower",
                    "Ricochet Cannon", "Multi-Gear Tower", "Firespitter",
                    "Bomb", "Spring Trap", "Giant Bomb", "Air Bomb",
                    "Seeking Air Mine", "Skeleton Trap", "Tornado Trap", "Giga Bomb",
                    "Gold Mine", "Elixir Collector", "Gold Storage", "Elixir Storage",
                    "Dark Elixir Drill", "Dark Elixir Storage",
                    "CC", "Army Camp", "Dark Barracks", "Laboratory",
                    "Spell Factory", "Hero Hall", "Dark Spell Factory",
                    "Blacksmith", "Workshop", "Pet House",
                    "BK", "Queen", "Warden", "Minion Prince", "RC", "DD"
                )
                ExposedDropdownMenuBox(
                    expanded = upgradeExpanded,
                    onExpandedChange = { upgradeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = upgradeName,
                        onValueChange = onUpgradeNameChange,
                        label = { Text("Upgrade") },
                        placeholder = { Text("Enter upgrade name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upgradeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = upgradeExpanded,
                        onDismissRequest = { upgradeExpanded = false }
                    ) {
                        upgradeList.forEach { upgrade ->
                            DropdownMenuItem(
                                text = { Text(upgrade) },
                                onClick = {
                                    onUpgradeNameChange(upgrade)
                                    upgradeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Text("Level: $level", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { onLevelChange(it.toInt()) },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = days,
                        onValueChange = onDaysChange,
                        label = { Text("DD") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hours,
                        onValueChange = onHoursChange,
                        label = { Text("HH") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = onMinutesChange,
                        label = { Text("MM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = onSecondsChange,
                        label = { Text("SS") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAddTask,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Upgrade", color = BgDark)
                }
            } else {
                // JSON Import Buttons
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onProcessClipboard,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Process from Clipboard", color = BgDark)
                    }
                    
                    OutlinedButton(
                        onClick = onShowJsonDialog,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Paste & Process")
                    }
                }
            }
        }
    }
}

@Composable
fun FilterButtonsSection(
    currentFilter: FilterType,
    onFilterChange: (FilterType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentFilter == FilterType.SOONEST,
            onClick = { onFilterChange(FilterType.SOONEST) },
            label = { Text("⏱️ Soonest") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Primary,
                selectedLabelColor = BgDark
            )
        )
        FilterChip(
            selected = currentFilter == FilterType.BY_TH,
            onClick = { onFilterChange(FilterType.BY_TH) },
            label = { Text("🏰 By TH") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Primary,
                selectedLabelColor = BgDark
            )
        )
    }
}

@Composable
fun TaskCard(
    task: UpgradeTask,
    currentTime: Long,
    onDelete: () -> Unit,
    onReplace: () -> Unit
) {
    val remaining = task.endTime - currentTime
    val isDone = remaining <= 0

    val borderColor by animateColorAsState(
        targetValue = if (isDone) Success else Primary,
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) Success.copy(alpha = 0.1f) else BgCard
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val thLevel = task.th.toIntOrNull() ?: 0
            val imageRes = if (thLevel in 1..18) "th${thLevel}_max" else "th1_max"

            Box(
                modifier = Modifier
                    .size(65.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, Primary, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                val context = LocalContext.current
                val resId = remember(imageRes) {
                    context.resources.getIdentifier(imageRes, "drawable", context.packageName)
                }
                
                if (resId != 0) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = "TH${task.th}",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("TH?", color = Primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(task.upgrade, color = Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    buildString {
                        append("Town Hall ${task.th.ifEmpty { "?" }}")
                        if (task.tag.isNotEmpty()) append(" • ${task.tag}")
                        if (task.category != null) append(" • ${task.category}")
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                if (task.level != null) {
                    Surface(
                        color = Primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "Level ${task.level}",
                            color = Primary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isDone) {
                    Text("✅", fontSize = 18.sp)
                } else {
                    Text(formatTimeRemaining(remaining), color = Success, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Remaining", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Danger)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete", color = Danger)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onReplace,
                colors = ButtonDefaults.buttonColors(containerColor = Success)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = BgDark)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Replace", color = BgDark)
            }
        }
    }
}

@Composable
fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚔️", fontSize = 60.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Upgrades Planned", color = Text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Add your first upgrade to get started!", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

fun formatTimeRemaining(millis: Long): String {
    val remaining = maxOf(0L, millis)
    val totalSeconds = remaining / 1000
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return buildString {
        if (days > 0) append("${days}d ")
        if (hours > 0 || days > 0) append("${hours}h ")
        if (minutes > 0 || hours > 0 || days > 0) append("${minutes}m ")
        append("${seconds}s")
    }
}
