package com.r42914lg.blesandbox.blewrapper.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.r42914lg.blesandbox.blewrapper.model.LedState
import com.r42914lg.blesandbox.blewrapper.model.TemperatureReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data

class AirNodeBleManager(context: Context) : BleManager(context) {

    private var tempCharacteristic: BluetoothGattCharacteristic? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null

    private val _temperatureFlow = MutableStateFlow<TemperatureReading?>(null)
    val temperatureFlow: StateFlow<TemperatureReading?> = _temperatureFlow.asStateFlow()

    private val _ledStateFlow = MutableStateFlow(LedState())
    val ledStateFlow: StateFlow<LedState> = _ledStateFlow.asStateFlow()

    override fun getGattCallback(): BleManagerGattCallback {
        return object : BleManagerGattCallback() {

            override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                val service = gatt.getService(AirNodeGattAttributes.SERVICE_UUID)
                return if (service != null) {
                    tempCharacteristic = service.getCharacteristic(AirNodeGattAttributes.TEMP_CHARACTERISTIC_UUID)
                    ledCharacteristic = service.getCharacteristic(AirNodeGattAttributes.LED_CHARACTERISTIC_UUID)
                    true
                } else {
                    false
                }
            }

            override fun initialize() {
                tempCharacteristic?.let { char ->
                    setNotificationCallback(char).with { _: BluetoothDevice, data: Data ->
                        parseTemperatureData(data)
                    }
                    enableNotifications(char).enqueue()
                }

                ledCharacteristic?.let { char ->
                    readCharacteristic(char).with { _: BluetoothDevice, data: Data ->
                        parseLedData(data)
                    }.enqueue()
                }
            }

            override fun onServicesInvalidated() {
                tempCharacteristic = null
                ledCharacteristic = null
            }
        }
    }

    private fun parseTemperatureData(data: Data) {
        val bytes = data.value ?: return
        if (bytes.isNotEmpty()) {
            val tempValue = when {
                bytes.size >= 4 -> data.getFloatValue(Data.FORMAT_FLOAT, 0) ?: 20.0f
                bytes.size >= 2 -> (data.getIntValue(Data.FORMAT_SINT16, 0) ?: 200).toFloat() / 10f
                else -> (bytes[0].toInt() and 0xFF).toFloat()
            }
            _temperatureFlow.value = TemperatureReading(temperatureCelsius = tempValue)
        }
    }

    private fun parseLedData(data: Data) {
        val bytes = data.value ?: return
        if (bytes.isNotEmpty()) {
            val isOn = bytes[0].toInt() != 0
            val writeVal = if (isOn) "01" else "00"
            _ledStateFlow.value = LedState(
                isOn = isOn,
                lastWriteTimestampMs = System.currentTimeMillis(),
                lastWriteValue = writeVal
            )
        }
    }

    fun setLedState(isOn: Boolean) {
        val char = ledCharacteristic ?: return
        val value = if (isOn) byteArrayOf(0x01) else byteArrayOf(0x00)
        val hexValue = if (isOn) "01" else "00"

        writeCharacteristic(
            char,
            value,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).done {
            _ledStateFlow.value = LedState(
                isOn = isOn,
                lastWriteTimestampMs = System.currentTimeMillis(),
                lastWriteValue = hexValue
            )
        }.enqueue()
    }
}
