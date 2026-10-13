package com.r42914lg.blesandbox.coredata.model

import com.r42914lg.blesandbox.coredata.db.entity.TemperatureEntity

data class TemperatureRecord(
    val id: Long = 0,
    val temperatureCelsius: Float,
    val timestampMs: Long
)

internal fun List<TemperatureEntity>.toRecordList(): List<TemperatureRecord> = map { it.toRecord() }
