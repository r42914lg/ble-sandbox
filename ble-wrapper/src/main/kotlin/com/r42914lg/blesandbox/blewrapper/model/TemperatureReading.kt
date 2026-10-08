package com.r42914lg.blesandbox.blewrapper.model

data class TemperatureReading(
    val temperatureCelsius: Float,
    val timestampMs: Long = System.currentTimeMillis()
)
