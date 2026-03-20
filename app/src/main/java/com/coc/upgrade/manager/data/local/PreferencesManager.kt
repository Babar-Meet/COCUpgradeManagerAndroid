package com.coc.upgrade.manager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "coc_upgrade_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private val PLAYER_TAG_KEY = stringPreferencesKey("player_tag")
        private val INPUT_METHOD_KEY = stringPreferencesKey("input_method")
    }

    val playerTag: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PLAYER_TAG_KEY] ?: ""
    }

    val inputMethod: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[INPUT_METHOD_KEY] ?: "json"
    }

    suspend fun setPlayerTag(tag: String) {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_TAG_KEY] = tag
        }
    }

    suspend fun setInputMethod(method: String) {
        context.dataStore.edit { preferences ->
            preferences[INPUT_METHOD_KEY] = method
        }
    }
}
