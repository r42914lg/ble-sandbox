package com.r42914lg.blesandbox.coredata

import com.r42914lg.blesandbox.coredata.db.dao.LedStateDao
import com.r42914lg.blesandbox.coredata.db.dao.TemperatureDao
import com.r42914lg.blesandbox.coredata.db.entity.LedStateEntity
import com.r42914lg.blesandbox.coredata.db.entity.TemperatureEntity
import com.r42914lg.blesandbox.coredata.model.LedStateRecord
import com.r42914lg.blesandbox.coredata.model.TemperatureRecord
import com.r42914lg.blesandbox.coredata.model.toRecordList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AirNodeRepositoryImpl(
    private val temperatureDao: TemperatureDao,
    private val ledStateDao: LedStateDao
) : AirNodeRepository {

    override suspend fun saveTemperature(
        temperatureCelsius: Float,
        timestampMs: Long
    ) {
        val entity = TemperatureEntity(
            temperatureCelsius = temperatureCelsius,
            timestampMs = timestampMs
        )
        temperatureDao.insert(entity)
    }

    override suspend fun saveLedState(isOn: Boolean) {
        val entity = LedStateEntity(isOn = isOn)
        ledStateDao.insert(entity)
    }

    override suspend fun deleteTemperaturesOlderThan(cutoffMs: Long): Int {
        val cutoffTimestamp = System.currentTimeMillis() - cutoffMs
        return temperatureDao.deleteOlderThan(cutoffTimestamp)
    }

    override fun observeLatestTemperature(): Flow<TemperatureRecord?> {
        return temperatureDao.observeLatest().map { it?.toRecord() }
    }

    override fun observeMinuteTemperatures(): Flow<List<TemperatureRecord>> {
        val startTimeMs = System.currentTimeMillis() - 60_000L
        return temperatureDao.observeTemperaturesSince(startTimeMs)
            .map { it.toRecordList() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override fun observe10MinuteTemperatures(): Flow<List<TemperatureRecord>> {
        val startTimeMs = System.currentTimeMillis() - 600_000L
        val bucketSizeMs = 10_000L
        return temperatureDao.observeDownsampledTemperatures(startTimeMs, bucketSizeMs)
            .map { it.toRecordList() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override fun observeHourTemperatures(): Flow<List<TemperatureRecord>> {
        val startTimeMs = System.currentTimeMillis() - 3_600_000L
        val bucketSizeMs = 60_000L
        return temperatureDao.observeDownsampledTemperatures(startTimeMs, bucketSizeMs)
            .map { it.toRecordList() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override fun observeLedState(): Flow<LedStateRecord?> {
        return ledStateDao.observeLatest().map { it?.toRecord() }
    }
}
