package com.r42914lg.blesandbox.blewrapper.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.r42914lg.blesandbox.blewrapper.model.LedState
import com.r42914lg.blesandbox.blewrapper.model.TemperatureReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data

private const val TAG = "AirNodeBleManager"

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
                if (service != null) {
                    tempCharacteristic = service.getCharacteristic(AirNodeGattAttributes.TEMP_CHARACTERISTIC_UUID)
                    ledCharacteristic = service.getCharacteristic(AirNodeGattAttributes.LED_CHARACTERISTIC_UUID)
                }
                val supported = service != null && tempCharacteristic != null && ledCharacteristic != null
                Log.d(TAG, "isRequiredServiceSupported=$supported for service ${AirNodeGattAttributes.SERVICE_UUID}")
                return supported
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
        if (bytes.size >= 4) {
            val hundredths = data.getIntValue(Data.FORMAT_SINT16, 2) ?: return
            val tempValue = hundredths.toFloat() / 100.0f
            Log.d(TAG, "Received temperature reading: $tempValue °C (hundredths=$hundredths)")
            _temperatureFlow.value = TemperatureReading(temperatureCelsius = tempValue)
        }
    }

    private fun parseLedData(data: Data) {
        val bytes = data.value ?: return
        if (bytes.isNotEmpty()) {
            val isOn = bytes[0].toInt() != 0
            val writeVal = if (isOn) "01" else "00"
            Log.d(TAG, "Read LED state: isOn=$isOn, val=$writeVal")
            _ledStateFlow.value = LedState(
                isOn = isOn,
                lastWriteTimestampMs = System.currentTimeMillis(),
                lastWriteValue = writeVal
            )
        }
    }

    fun setLedState(isOn: Boolean) {
        val char = ledCharacteristic ?: return
        val value = byteArrayOf(if (isOn) 0x01 else 0x00)
        val hexValue = if (isOn) "01" else "00"

        writeCharacteristic(
            char,
            value,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).done {
            Log.d(TAG, "Successfully wrote LED state: $hexValue")
            _ledStateFlow.value = LedState(
                isOn = isOn,
                lastWriteTimestampMs = System.currentTimeMillis(),
                lastWriteValue = hexValue
            )
        }.enqueue()
    }
}
