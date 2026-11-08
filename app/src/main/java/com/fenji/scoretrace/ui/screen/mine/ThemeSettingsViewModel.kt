package com.fenji.scoretrace.ui.screen.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeSettingsUiState(
    val themeMode: String = "system",
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = true,
    val enableLiquidGlass: Boolean = false,
    val enablePredictiveBack: Boolean = true,
)

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val state: StateFlow<ThemeSettingsUiState> = combine(
        userPreferences.themeMode,
        userPreferences.enableBlur,
        userPreferences.enableFloatingBottomBar,
        userPreferences.enableLiquidGlass,
        userPreferences.enablePredictiveBack,
    ) { themeMode, blur, floating, liquid, predictive ->
        ThemeSettingsUiState(themeMode, blur, floating, liquid, predictive)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeSettingsUiState(),
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }

    fun setEnableBlur(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setEnableBlur(enabled) }
    }

    fun setEnableFloatingBottomBar(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setEnableFloatingBottomBar(enabled) }
    }

    fun setEnableLiquidGlass(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setEnableLiquidGlass(enabled) }
    }

    fun setEnablePredictiveBack(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setEnablePredictiveBack(enabled) }
    }
}
