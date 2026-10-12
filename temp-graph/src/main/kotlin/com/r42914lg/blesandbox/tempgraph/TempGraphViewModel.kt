package com.r42914lg.blesandbox.tempgraph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r42914lg.blesandbox.coredata.AirNodeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TempGraphViewModel(
    private val repository: AirNodeRepository
) : ViewModel() {

    val selectedWindowFlow = MutableStateFlow(TimeWindow.MINUTE)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TempGraphUiState> = selectedWindowFlow.flatMapLatest { window ->
        when (window) {
            TimeWindow.MINUTE -> repository.observeMinuteTemperatures()
            TimeWindow.TEN_MINUTES -> repository.observe10MinuteTemperatures()
            TimeWindow.HOUR -> repository.observeHourTemperatures()
        }.map { list ->
            val minVal = (list.minOfOrNull { it.temperatureCelsius } ?: 15.0f).coerceAtMost(15.0f)
            val maxVal = (list.maxOfOrNull { it.temperatureCelsius } ?: 35.0f).coerceAtLeast(35.0f)
            TempGraphUiState(
                timeWindow = window,
                readings = list,
                minTemp = minVal,
                maxTemp = maxVal
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TempGraphUiState()
    )

    fun selectTimeWindow(window: TimeWindow) {
        selectedWindowFlow.value = window
    }
}
