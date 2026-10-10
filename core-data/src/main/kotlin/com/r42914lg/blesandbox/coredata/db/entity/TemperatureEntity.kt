package com.r42914lg.blesandbox.coredata.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.r42914lg.blesandbox.coredata.model.TemperatureRecord

@Entity(tableName = "temperature_readings")
data class TemperatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val temperatureCelsius: Float,
    val timestampMs: Long
) {
    fun toRecord(): TemperatureRecord = TemperatureRecord(
        id = id,
        temperatureCelsius = temperatureCelsius,
        timestampMs = timestampMs
    )
}
