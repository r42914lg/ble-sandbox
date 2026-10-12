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

    @Query("SELECT * FROM temperature_readings WHERE timestampMs >= :startTimeMs ORDER BY timestampMs ASC")
    fun observeTemperaturesSince(startTimeMs: Long): Flow<List<TemperatureEntity>>

    @Query("""
        SELECT 
            CAST(AVG(id) AS INTEGER) AS id,
            CAST(AVG(temperatureCelsius) AS REAL) AS temperatureCelsius,
            CAST(AVG(timestampMs) AS INTEGER) AS timestampMs
        FROM temperature_readings
        WHERE timestampMs >= :startTimeMs
        GROUP BY (timestampMs / :bucketSizeMs)
        ORDER BY timestampMs ASC
    """)
    fun observeDownsampledTemperatures(startTimeMs: Long, bucketSizeMs: Long): Flow<List<TemperatureEntity>>

    @Query("DELETE FROM temperature_readings WHERE timestampMs < :cutoffTimestampMs")
    suspend fun deleteOlderThan(cutoffTimestampMs: Long): Int
}
