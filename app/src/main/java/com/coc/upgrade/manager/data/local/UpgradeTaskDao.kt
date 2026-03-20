package com.coc.upgrade.manager.data.local

import androidx.room.*
import com.coc.upgrade.manager.data.model.UpgradeTask
import kotlinx.coroutines.flow.Flow

@Dao
interface UpgradeTaskDao {
    @Query("SELECT * FROM upgrade_tasks ORDER BY endTime ASC")
    fun getAllTasks(): Flow<List<UpgradeTask>>

    @Query("SELECT * FROM upgrade_tasks ORDER BY endTime ASC")
    suspend fun getAllTasksList(): List<UpgradeTask>

    @Query("SELECT * FROM upgrade_tasks WHERE tag = :tag ORDER BY endTime ASC")
    suspend fun getTasksByTag(tag: String): List<UpgradeTask>

    @Query("SELECT * FROM upgrade_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): UpgradeTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: UpgradeTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<UpgradeTask>)

    @Update
    suspend fun updateTask(task: UpgradeTask)

    @Delete
    suspend fun deleteTask(task: UpgradeTask)

    @Query("DELETE FROM upgrade_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM upgrade_tasks")
    suspend fun deleteAllTasks()

    @Query("SELECT COUNT(*) FROM upgrade_tasks")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM upgrade_tasks WHERE done = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM upgrade_tasks WHERE done = 0")
    fun getActiveCount(): Flow<Int>
}
