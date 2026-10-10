package com.r42914lg.blesandbox.coredata.model

data class LedStateRecord(
    val isOn: Boolean = false,
    val lastWriteValue: String? = if (isOn) "01" else "00"
)
