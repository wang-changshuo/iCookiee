package com.aifinance.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AppThemeMode
import com.aifinance.core.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = userPreferencesRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppThemeMode.LIGHT,
    )
}