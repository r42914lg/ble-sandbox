package com.r42914lg.blesandbox.coredata.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.r42914lg.blesandbox.coredata.db.entity.LedStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LedStateEntity)

    @Query("SELECT * FROM led_state WHERE id = 1")
    fun observeLatest(): Flow<LedStateEntity?>
}
