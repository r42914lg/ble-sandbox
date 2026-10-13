package com.r42914lg.blesandbox.blewrapper.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.r42914lg.blesandbox.coredata.AirNodeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data

private const val TAG = "AirNodeBleManager"

internal class AirNodeBleManager(
    context: Context,
    private val repository: AirNodeRepository,
    private val scope: CoroutineScope
) : BleManager(context) {

    private var tempCharacteristic: BluetoothGattCharacteristic? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null

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
                    enableNotifications(char)
                        .fail { _, status -> Log.w(TAG, "Failed to enable notifications: $status") }
                        .enqueue()
                }

                ledCharacteristic?.let { char ->
                    readCharacteristic(char)
                        .with { _: BluetoothDevice, data: Data -> parseLedData(data) }
                        .fail { _, status -> Log.w(TAG, "Failed to read initial LED state: $status") }
                        .enqueue()
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

            scope.launch(Dispatchers.IO) {
                repository.saveTemperature(temperatureCelsius = tempValue)
            }
        }
    }

    private fun parseLedData(data: Data) {
        val bytes = data.value ?: return
        if (bytes.isNotEmpty()) {
            val isOn = bytes[0].toInt() != 0
            Log.d(TAG, "Read LED state: isOn=$isOn")

            scope.launch(Dispatchers.IO) {
                repository.saveLedState(isOn = isOn)
            }
        }
    }

    fun setLedState(isOn: Boolean) {
        val char = ledCharacteristic ?: return
        val value = byteArrayOf(if (isOn) 0x01 else 0x00)

        writeCharacteristic(
            char,
            value,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).done {
            Log.d(TAG, "Successfully wrote LED state: $isOn")

            scope.launch(Dispatchers.IO) {
                repository.saveLedState(isOn = isOn)
            }
        }.enqueue()
    }
}
