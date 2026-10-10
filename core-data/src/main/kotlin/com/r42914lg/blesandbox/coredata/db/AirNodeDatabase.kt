package com.r42914lg.blesandbox.coredata.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.r42914lg.blesandbox.coredata.db.dao.LedStateDao
import com.r42914lg.blesandbox.coredata.db.dao.TemperatureDao
import com.r42914lg.blesandbox.coredata.db.entity.LedStateEntity
import com.r42914lg.blesandbox.coredata.db.entity.TemperatureEntity

@Database(
    entities = [TemperatureEntity::class, LedStateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AirNodeDatabase : RoomDatabase() {
    abstract fun temperatureDao(): TemperatureDao
    abstract fun ledStateDao(): LedStateDao
}
