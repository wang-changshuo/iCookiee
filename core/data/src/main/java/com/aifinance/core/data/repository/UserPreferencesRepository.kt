package com.aifinance.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppThemeMode {
    LIGHT,
    DARK,
}

interface UserPreferencesRepository {
    val themeMode: Flow<AppThemeMode>
    suspend fun setThemeMode(mode: AppThemeMode)
}

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val themeMode: Flow<AppThemeMode> = dataStore.data.map { preferences ->
        val raw = preferences[THEME_MODE_KEY] ?: AppThemeMode.LIGHT.name
        runCatching { AppThemeMode.valueOf(raw) }.getOrDefault(AppThemeMode.LIGHT)
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}