package com.coc.upgrade.manager.di

import android.content.Context
import androidx.room.Room
import com.coc.upgrade.manager.data.local.PreferencesManager
import com.coc.upgrade.manager.data.local.UpgradeDatabase
import com.coc.upgrade.manager.data.local.UpgradeTaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUpgradeDatabase(
        @ApplicationContext context: Context
    ): UpgradeDatabase {
        return Room.databaseBuilder(
            context,
            UpgradeDatabase::class.java,
            UpgradeDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideUpgradeTaskDao(database: UpgradeDatabase): UpgradeTaskDao {
        return database.upgradeTaskDao()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager {
        return PreferencesManager(context)
    }
}
