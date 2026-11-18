package com.tk.quicksearch.search.core

import androidx.lifecycle.viewModelScope
import com.tk.quicksearch.search.appSettings.BooleanSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val booleanSettingWrites = Dispatchers.IO.limitedParallelism(1)

internal fun SearchViewModel.setBooleanSetting(setting: BooleanSetting, enabled: Boolean) {
    viewModelScope.launch(booleanSettingWrites) {
        userPreferences.setBooleanSetting(setting, enabled)
        updateConfigState { state ->
            state.copy(booleanSettings = state.booleanSettings + (setting.preferenceKey to enabled))
        }
    }
}
