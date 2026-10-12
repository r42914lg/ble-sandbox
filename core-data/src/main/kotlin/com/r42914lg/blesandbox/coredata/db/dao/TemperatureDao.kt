package com.r42914lg.blesandbox.coredata.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.r42914lg.blesandbox.coredata.db.entity.TemperatureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemperatureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TemperatureEntity)

    @Query("SELECT * FROM temperature_readings ORDER BY timestampMs DESC LIMIT 1")
    fun observeLatest(): Flow<TemperatureEntity?>

    @Query("SELECT * FROM temperature_readings ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<TemperatureEntity>>

    @Query("DELETE FROM temperature_readings WHERE timestampMs < :cutoffTimestampMs")
    suspend fun deleteOlderThan(cutoffTimestampMs: Long): Int
}
