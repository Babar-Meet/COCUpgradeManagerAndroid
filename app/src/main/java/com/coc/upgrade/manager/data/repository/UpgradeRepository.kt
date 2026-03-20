package com.coc.upgrade.manager.data.repository

import com.coc.upgrade.manager.data.local.PreferencesManager
import com.coc.upgrade.manager.data.local.UpgradeTaskDao
import com.coc.upgrade.manager.data.model.UpgradeTask
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpgradeRepository @Inject constructor(
    private val upgradeTaskDao: UpgradeTaskDao,
    private val preferencesManager: PreferencesManager
) {
    val allTasks: Flow<List<UpgradeTask>> = upgradeTaskDao.getAllTasks()
    
    val totalCount: Flow<Int> = upgradeTaskDao.getTotalCount()
    val completedCount: Flow<Int> = upgradeTaskDao.getCompletedCount()
    val activeCount: Flow<Int> = upgradeTaskDao.getActiveCount()
    
    val playerTag: Flow<String> = preferencesManager.playerTag
    val inputMethod: Flow<String> = preferencesManager.inputMethod

    suspend fun getAllTasksList(): List<UpgradeTask> = upgradeTaskDao.getAllTasksList()

    suspend fun getTasksByTag(tag: String): List<UpgradeTask> = upgradeTaskDao.getTasksByTag(tag)

    suspend fun getTaskById(id: Long): UpgradeTask? = upgradeTaskDao.getTaskById(id)

    suspend fun insertTask(task: UpgradeTask): Long = upgradeTaskDao.insertTask(task)

    suspend fun insertTasks(tasks: List<UpgradeTask>) = upgradeTaskDao.insertTasks(tasks)

    suspend fun updateTask(task: UpgradeTask) = upgradeTaskDao.updateTask(task)

    suspend fun deleteTask(task: UpgradeTask) = upgradeTaskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Long) = upgradeTaskDao.deleteTaskById(id)

    suspend fun deleteAllTasks() = upgradeTaskDao.deleteAllTasks()

    suspend fun setPlayerTag(tag: String) = preferencesManager.setPlayerTag(tag)

    suspend fun setInputMethod(method: String) = preferencesManager.setInputMethod(method)
}
