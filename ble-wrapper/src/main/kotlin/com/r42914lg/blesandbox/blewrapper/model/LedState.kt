package com.r42914lg.blesandbox.blewrapper.model

data class LedState(
    val isOn: Boolean = false,
    val lastWriteTimestampMs: Long? = null,
    val lastWriteValue: String? = null
)
