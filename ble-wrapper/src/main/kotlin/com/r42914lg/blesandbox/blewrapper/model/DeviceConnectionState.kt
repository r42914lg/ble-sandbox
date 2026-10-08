package com.r42914lg.blesandbox.blewrapper.model

sealed interface DeviceConnectionState {
    data object Disconnected : DeviceConnectionState

    data class Connecting(
        val deviceAddress: String,
        val deviceName: String? = null,
    ) : DeviceConnectionState

    data class Connected(
        val deviceAddress: String,
        val deviceName: String? = null,
    ) : DeviceConnectionState

    data class Reconnecting(
        val deviceAddress: String,
        val deviceName: String? = null,
        val lastReadingTimestampMs: Long? = null,
    ) : DeviceConnectionState

    data class Error(
        val deviceAddress: String? = null,
        val message: String,
        val gattStatus: Int? = null,
        val throwable: Throwable? = null,
    ) : DeviceConnectionState
}
