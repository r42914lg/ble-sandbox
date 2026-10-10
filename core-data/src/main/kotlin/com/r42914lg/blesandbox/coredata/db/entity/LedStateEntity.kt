package com.r42914lg.blesandbox.coredata.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.r42914lg.blesandbox.coredata.model.LedStateRecord

@Entity(tableName = "led_state")
data class LedStateEntity(
    @PrimaryKey val id: Int = 1,
    val isOn: Boolean
) {
    fun toRecord(): LedStateRecord = LedStateRecord(isOn = isOn)
}
