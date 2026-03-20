package com.coc.upgrade.manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coc.upgrade.manager.data.model.UpgradeTask
import com.coc.upgrade.manager.data.repository.UpgradeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

enum class FilterType {
    SOONEST, BY_TH
}

data class ThStats(
    val thLevel: String,
    val nextCompletionTime: Long
)

data class MainUiState(
    val tasks: List<UpgradeTask> = emptyList(),
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val activeCount: Int = 0,
    val totalTimeRemaining: Long = 0,
    val thStats: List<ThStats> = emptyList(),
    val playerTag: String = "",
    val inputMethod: String = "json",
    val currentFilter: FilterType = FilterType.SOONEST,
    val showReplaceDialog: Boolean = false,
    val replaceTaskIndex: Int = -1,
    val currentTime: Long = System.currentTimeMillis()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UpgradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _importExportResult = MutableSharedFlow<ImportExportResult>()
    val importExportResult: SharedFlow<ImportExportResult> = _importExportResult.asSharedFlow()

    init {
        observeTasks()
        observeCounts()
        observePreferences()
        startTimerUpdate()
    }

    private fun observeTasks() {
        viewModelScope.launch {
            repository.allTasks.collect { tasks ->
                val filteredTasks = filterTasks(tasks, _uiState.value.currentFilter)
                _uiState.update { state ->
                    state.copy(
                        tasks = filteredTasks,
                        thStats = calculateThStats(tasks),
                        totalTimeRemaining = calculateTotalTimeRemaining(tasks)
                    )
                }
            }
        }
    }

    private fun observeCounts() {
        viewModelScope.launch {
            repository.totalCount.collect { count ->
                _uiState.update { it.copy(totalCount = count) }
            }
        }
        viewModelScope.launch {
            repository.completedCount.collect { count ->
                _uiState.update { it.copy(completedCount = count) }
            }
        }
        viewModelScope.launch {
            repository.activeCount.collect { count ->
                _uiState.update { it.copy(activeCount = count) }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            repository.playerTag.collect { tag ->
                _uiState.update { it.copy(playerTag = tag) }
            }
        }
        viewModelScope.launch {
            repository.inputMethod.collect { method ->
                _uiState.update { it.copy(inputMethod = method) }
            }
        }
    }

    private fun startTimerUpdate() {
        viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(currentTime = System.currentTimeMillis()) }
                delay(1000)
            }
        }
    }

    fun setPlayerTag(tag: String) {
        viewModelScope.launch {
            repository.setPlayerTag(tag)
        }
    }

    fun setInputMethod(method: String) {
        viewModelScope.launch {
            repository.setInputMethod(method)
        }
    }

    fun setFilter(filter: FilterType) {
        _uiState.update { state ->
            val filteredTasks = filterTasks(state.tasks, filter)
            state.copy(currentFilter = filter, tasks = filteredTasks)
        }
    }

    private fun filterTasks(tasks: List<UpgradeTask>, filter: FilterType): List<UpgradeTask> {
        return when (filter) {
            FilterType.SOONEST -> tasks.sortedBy { it.endTime }
            FilterType.BY_TH -> tasks.sortedWith(
                compareBy<UpgradeTask> { it.th.toIntOrNull() ?: Int.MAX_VALUE }
                    .thenBy { it.endTime }
            )
        }
    }

    private fun calculateThStats(tasks: List<UpgradeTask>): List<ThStats> {
        val now = System.currentTimeMillis()
        val activeTasks = tasks.filter { !it.done && it.endTime > now }
        
        return activeTasks
            .groupBy { it.th }
            .mapNotNull { (th, thTasks) ->
                val firstCompletion = thTasks.minByOrNull { it.endTime }?.endTime
                if (firstCompletion != null && th.isNotEmpty()) {
                    ThStats(th, firstCompletion)
                } else null
            }
            .sortedBy { it.thLevel.toIntOrNull() ?: Int.MAX_VALUE }
    }

    private fun calculateTotalTimeRemaining(tasks: List<UpgradeTask>): Long {
        val now = System.currentTimeMillis()
        return tasks
            .filter { !it.done && it.endTime > now }
            .sumOf { it.endTime - now }
    }

    fun addTask(
        th: String,
        upgrade: String,
        level: Int?,
        days: Int,
        hours: Int,
        minutes: Int,
        seconds: Int
    ) {
        viewModelScope.launch {
            val totalMillis = ((days * 24L * 60 * 60) + 
                (hours * 60L * 60) + 
                (minutes * 60L) + 
                seconds) * 1000
            
            if (th.isNotEmpty() && upgrade.isNotEmpty() && totalMillis > 0) {
                val task = UpgradeTask(
                    tag = _uiState.value.playerTag,
                    th = th,
                    upgrade = upgrade,
                    level = level,
                    endTime = System.currentTimeMillis() + totalMillis,
                    done = false
                )
                repository.insertTask(task)
            }
        }
    }

    fun deleteTask(task: UpgradeTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            repository.deleteAllTasks()
        }
    }

    fun hideReplaceDialog() {
        _uiState.update { it.copy(showReplaceDialog = false, replaceTaskIndex = -1) }
    }

    fun replaceTask(
        index: Int,
        upgrade: String,
        level: Int?,
        days: Int,
        hours: Int,
        minutes: Int,
        seconds: Int
    ) {
        viewModelScope.launch {
            val tasks = _uiState.value.tasks.toMutableList()
            if (index in tasks.indices) {
                val totalMillis = ((days * 24L * 60 * 60) + 
                    (hours * 60L * 60) + 
                    (minutes * 60L) + 
                    seconds) * 1000
                
                if (upgrade.isNotEmpty() && totalMillis > 0) {
                    val updatedTask = tasks[index].copy(
                        upgrade = upgrade,
                        level = level,
                        endTime = System.currentTimeMillis() + totalMillis,
                        done = false
                    )
                    repository.updateTask(updatedTask)
                    hideReplaceDialog()
                }
            }
        }
    }

    fun processJsonImport(jsonString: String) {
        viewModelScope.launch {
            try {
                val json = JSONObject(jsonString)
                
                // Check if it's a backup
                val type = json.optString("type")
                if (type == "COC_UPGRADE_MANAGER_BACKUP" || type == "COC_UPGRAD_MANAGER_BACKUP") {
                    // Import backup
                    val tasksArray = json.getJSONArray("tasks")
                    val tasks = mutableListOf<UpgradeTask>()
                    
                    for (i in 0 until tasksArray.length()) {
                        val taskJson = tasksArray.getJSONObject(i)
                        val task = UpgradeTask(
                            tag = taskJson.optString("tag", ""),
                            th = taskJson.optString("th", ""),
                            upgrade = taskJson.optString("upgrade", ""),
                            level = taskJson.optInt("level").takeIf { it > 0 },
                            endTime = taskJson.optLong("endTime", System.currentTimeMillis()),
                            done = taskJson.optBoolean("done", false),
                            code = taskJson.optString("code", null),
                            category = taskJson.optString("category", null)
                        )
                        if (task.upgrade.isNotEmpty()) {
                            tasks.add(task)
                        }
                    }
                    
                    repository.deleteAllTasks()
                    repository.insertTasks(tasks.sortedBy { it.endTime })
                    
                    // Update player tag if present
                    val playerTag = json.optString("playerTag", "")
                    if (playerTag.isNotEmpty()) {
                        repository.setPlayerTag(playerTag)
                    }
                    
                    _importExportResult.emit(ImportExportResult.Success("Imported ${tasks.size} tasks from backup"))
                    return@launch
                }
                
                // Process game export JSON
                val incomingTag = json.optString("tag", _uiState.value.playerTag)
                if (incomingTag.isNotEmpty()) {
                    repository.setPlayerTag(incomingTag)
                }
                
                // Find TH level
                val buildings = json.optJSONArray("buildings")
                var thLevel = ""
                if (buildings != null) {
                    for (i in 0 until buildings.length()) {
                        val building = buildings.getJSONObject(i)
                        if (building.optInt("data") == 1000001) {
                            thLevel = building.optInt("lvl").toString()
                            break
                        }
                    }
                }
                
                val extractedTasks = extractActiveUpgradesFromCocJson(json, thLevel, incomingTag)
                
                if (extractedTasks.isEmpty()) {
                    _importExportResult.emit(ImportExportResult.Error("No active upgrades found in JSON"))
                    return@launch
                }
                
                val existingTasks = repository.getAllTasksList()
                val existingTags = existingTasks.map { it.tag }.filter { it.isNotEmpty() }.toSet()
                
                val newTasks = if (existingTasks.isEmpty()) {
                    extractedTasks
                } else if (existingTags.contains(incomingTag)) {
                    // Replace same account
                    existingTasks.filter { it.tag != incomingTag } + extractedTasks
                } else {
                    // Add new account
                    existingTasks + extractedTasks
                }
                
                repository.deleteAllTasks()
                repository.insertTasks(newTasks.sortedBy { it.endTime })
                
                _importExportResult.emit(ImportExportResult.Success("Imported ${extractedTasks.size} active upgrades"))
            } catch (e: Exception) {
                _importExportResult.emit(ImportExportResult.Error("Invalid JSON: ${e.message}"))
            }
        }
    }

    private fun extractActiveUpgradesFromCocJson(json: JSONObject, thLevel: String, tag: String): List<UpgradeTask> {
        val categories = listOf(
            "buildings", "traps", "units", "siege_machines", "heroes",
            "spells", "pets", "equipment", "buildings2", "traps2", "units2", "heroes2"
        )
        
        val results = mutableListOf<UpgradeTask>()
        val now = System.currentTimeMillis()
        
        for (category in categories) {
            val items = json.optJSONArray(category) ?: continue
            
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val timer = item.optInt("timer", 0)
                
                if (timer <= 0) continue
                
                val code = item.optInt("data")
                val level = item.optInt("lvl").takeIf { it > 0 }
                val name = getCocNameById(code)
                
                results.add(
                    UpgradeTask(
                        tag = tag,
                        th = thLevel,
                        code = code.toString(),
                        category = getCocCategoryLabel(category),
                        upgrade = name,
                        level = level,
                        endTime = now + (timer * 1000L),
                        done = false
                    )
                )
            }
        }
        
        return results.sortedBy { it.endTime }
    }

    private fun getCocNameById(id: Int): String {
        val names = mapOf(
            // Buildings
            1000000 to "Army Camp",
            1000001 to "Town Hall",
            1000002 to "Elixir Collector",
            1000003 to "Elixir Storage",
            1000004 to "Gold Mine",
            1000005 to "Gold Storage",
            1000006 to "Barracks",
            1000007 to "Laboratory",
            1000008 to "Cannon",
            1000009 to "Archer Tower",
            1000010 to "Wall",
            1000011 to "Wizard Tower",
            1000012 to "Air Defense",
            1000013 to "Mortar",
            1000014 to "Clan Castle",
            1000015 to "Builder's Hut",
            1000019 to "Hidden Tesla",
            1000020 to "Spell Factory",
            1000021 to "X-Bow",
            1000023 to "Dark Elixir Drill",
            1000024 to "Dark Elixir Storage",
            1000026 to "Dark Barracks",
            1000027 to "Inferno Tower",
            1000028 to "Air Sweeper",
            1000029 to "Dark Spell Factory",
            1000059 to "Workshop",
            1000068 to "Pet House",
            1000070 to "Blacksmith",
            1000071 to "Hero Hall",
            1000072 to "Spell Tower",
            1000077 to "Monolith",
            1000084 to "Multi-Archer Tower",
            1000085 to "Ricochet Cannon",
            1000089 to "Firespitter",
            // Traps
            12000000 to "Bomb",
            12000001 to "Spring Trap",
            12000002 to "Giant Bomb",
            12000005 to "Air Bomb",
            12000006 to "Seeking Air Mine",
            12000008 to "Skeleton Trap",
            12000016 to "Tornado Trap",
            12000020 to "Giga Bomb",
            // Heroes
            28000000 to "Barbarian King",
            28000001 to "Archer Queen",
            28000002 to "Grand Warden",
            28000004 to "Royal Champion",
            28000006 to "Minion Prince",
            28000007 to "Dragon Duck"
        )
        return names[id] ?: "Unknown ($id)"
    }

    private fun getCocCategoryLabel(category: String): String {
        val labels = mapOf(
            "buildings" to "Building",
            "traps" to "Trap",
            "units" to "Troop",
            "siege_machines" to "Siege",
            "heroes" to "Hero",
            "spells" to "Spell",
            "pets" to "Pet",
            "equipment" to "Equipment",
            "buildings2" to "BB Building",
            "traps2" to "BB Trap",
            "units2" to "BB Troop",
            "heroes2" to "BB Hero"
        )
        return labels[category] ?: category
    }

    fun exportBackup(): String {
        val tasks = _uiState.value.tasks
        val backup = JSONObject().apply {
            put("type", "COC_UPGRADE_MANAGER_BACKUP")
            put("version", 1)
            put("exportedAt", java.time.Instant.now().toString())
            put("playerTag", _uiState.value.playerTag)
            put("tasks", JSONArray().apply {
                tasks.forEach { task ->
                    put(JSONObject().apply {
                        put("tag", task.tag)
                        put("th", task.th)
                        put("upgrade", task.upgrade)
                        task.level?.let { put("level", it) }
                        put("endTime", task.endTime)
                        put("done", task.done)
                        task.code?.let { put("code", it) }
                        task.category?.let { put("category", it) }
                    })
                }
            })
        }
        return backup.toString(2)
    }

    fun exportToClipboard() {
        viewModelScope.launch {
            val backup = exportBackup()
            _importExportResult.emit(ImportExportResult.ExportReady(backup))
        }
    }
}

sealed class ImportExportResult {
    data class Success(val message: String) : ImportExportResult()
    data class Error(val message: String) : ImportExportResult()
    data class ExportReady(val json: String) : ImportExportResult()
}
