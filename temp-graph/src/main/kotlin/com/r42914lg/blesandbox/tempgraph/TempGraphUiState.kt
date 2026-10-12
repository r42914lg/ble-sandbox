package com.r42914lg.blesandbox.tempgraph

import com.r42914lg.blesandbox.coredata.model.TemperatureRecord

data class TempGraphUiState(
    val timeWindow: TimeWindow = TimeWindow.MINUTE,
    val readings: List<TemperatureRecord> = emptyList(),
    val minTemp: Float = 15.0f,
    val maxTemp: Float = 35.0f
)
