package com.coc.upgrade.manager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.coc.upgrade.manager.data.model.UpgradeTask

@Database(
    entities = [UpgradeTask::class],
    version = 1,
    exportSchema = false
)
abstract class UpgradeDatabase : RoomDatabase() {
    abstract fun upgradeTaskDao(): UpgradeTaskDao

    companion object {
        const val DATABASE_NAME = "coc_upgrade_db"
    }
}
