package com.r42914lg.blesandbox.blewrapper.gatt

import java.util.UUID

object AirNodeGattAttributes {
    val SERVICE_UUID: UUID = UUID.fromString("0000181A-0000-1000-8000-00805f9b34fb")
    val TEMP_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A6E-0000-1000-8000-00805f9b34fb")
    val LED_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A56-0000-1000-8000-00805f9b34fb")
}
