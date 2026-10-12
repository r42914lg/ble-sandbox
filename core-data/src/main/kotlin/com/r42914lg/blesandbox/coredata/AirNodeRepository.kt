package com.r42914lg.blesandbox.coredata

import com.r42914lg.blesandbox.coredata.model.LedStateRecord
import com.r42914lg.blesandbox.coredata.model.TemperatureRecord
import kotlinx.coroutines.flow.Flow

interface AirNodeRepository {

    suspend fun saveTemperature(
        temperatureCelsius: Float,
        timestampMs: Long = System.currentTimeMillis()
    )

    suspend fun saveLedState(isOn: Boolean)

    suspend fun deleteTemperaturesOlderThan(cutoffMs: Long = 3_600_000): Int

    fun observeLatestTemperature(): Flow<TemperatureRecord?>

    fun observeAllTemperatures(): Flow<List<TemperatureRecord>>

    fun observeLedState(): Flow<LedStateRecord?>
}
