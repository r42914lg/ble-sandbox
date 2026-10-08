package com.r42914lg.blesandbox.blewrapper

import com.r42914lg.blesandbox.blewrapper.model.DeviceConnectionState
import com.r42914lg.blesandbox.blewrapper.model.DiscoveredDevice
import com.r42914lg.blesandbox.blewrapper.model.LedState
import com.r42914lg.blesandbox.blewrapper.model.TemperatureReading
import kotlinx.coroutines.flow.StateFlow

interface DeviceConnection {
    val connectionState: StateFlow<DeviceConnectionState>
    val temperatureFlow: StateFlow<TemperatureReading?>
    val ledStateFlow: StateFlow<LedState>
    val rssiFlow: StateFlow<Int?>
    val discoveredDevices: StateFlow<List<DiscoveredDevice>>

    fun startScan()
    fun stopScan()
    fun connect(deviceAddress: String, deviceName: String? = null)
    fun disconnect()
    fun retryConnection()

    fun setLedState(isOn: Boolean)
}
