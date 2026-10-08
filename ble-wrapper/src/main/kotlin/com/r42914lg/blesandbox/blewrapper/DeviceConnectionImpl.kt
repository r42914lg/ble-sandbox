package com.r42914lg.blesandbox.blewrapper

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.r42914lg.blesandbox.blewrapper.gatt.AirNodeBleManager
import com.r42914lg.blesandbox.blewrapper.gatt.AirNodeGattAttributes
import com.r42914lg.blesandbox.blewrapper.model.DeviceConnectionState
import com.r42914lg.blesandbox.blewrapper.model.DiscoveredDevice
import com.r42914lg.blesandbox.blewrapper.model.LedState
import com.r42914lg.blesandbox.blewrapper.model.TemperatureReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.observer.ConnectionObserver

class DeviceConnectionImpl(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : DeviceConnection {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }

    private val bleManager by lazy { AirNodeBleManager(context) }

    private val _connectionState = MutableStateFlow<DeviceConnectionState>(DeviceConnectionState.Disconnected)
    override val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

    override val temperatureFlow: StateFlow<TemperatureReading?>
        get() = bleManager.temperatureFlow

    override val ledStateFlow: StateFlow<LedState>
        get() = bleManager.ledStateFlow

    private val _rssiFlow = MutableStateFlow<Int?>(null)
    override val rssiFlow: StateFlow<Int?> = _rssiFlow.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private var currentTargetAddress: String? = null
    private var currentTargetName: String? = null
    private var isScanning = false

    init {
        setupConnectionObserver()
    }

    private fun setupConnectionObserver() {
        bleManager.setConnectionObserver(object : ConnectionObserver {
            @SuppressLint("MissingPermission")
            override fun onDeviceConnecting(device: android.bluetooth.BluetoothDevice) {
                _connectionState.value = DeviceConnectionState.Connecting(
                    deviceAddress = device.address,
                    deviceName = device.name ?: currentTargetName
                )
            }

            override fun onDeviceConnected(device: android.bluetooth.BluetoothDevice) {
                // do nothing - we are interested in onDeviceReady
            }

            override fun onDeviceFailedToConnect(device: android.bluetooth.BluetoothDevice, reason: Int) {
                _connectionState.value = DeviceConnectionState.Error(
                    deviceAddress = device.address,
                    message = "Failed to connect to device (reason $reason)",
                    gattStatus = reason
                )
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceReady(device: android.bluetooth.BluetoothDevice) {
                _connectionState.value = DeviceConnectionState.Connected(
                    deviceAddress = device.address,
                    deviceName = device.name ?: currentTargetName
                )
            }

            override fun onDeviceDisconnecting(device: android.bluetooth.BluetoothDevice) {
                // do nothing - we are interested in onDeviceDisconnected
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceDisconnected(device: android.bluetooth.BluetoothDevice, reason: Int) {
                if (reason == ConnectionObserver.REASON_SUCCESS || reason == ConnectionObserver.REASON_TERMINATE_LOCAL_HOST) {
                    _connectionState.value = DeviceConnectionState.Disconnected
                } else {
                    val lastTempTimestamp = temperatureFlow.value?.timestampMs
                    _connectionState.value = DeviceConnectionState.Reconnecting(
                        deviceAddress = device.address,
                        deviceName = device.name ?: currentTargetName,
                        lastReadingTimestampMs = lastTempTimestamp
                    )
                }
            }
        })
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val device = result.device ?: return
            val name = result.scanRecord?.deviceName ?: device.name ?: "AirNode"
            val newDevice = DiscoveredDevice(
                name = name,
                address = device.address,
                rssi = result.rssi
            )

            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == newDevice.address }
            if (existingIndex >= 0) {
                currentList[existingIndex] = newDevice
            } else {
                currentList.add(newDevice)
            }
            _discoveredDevices.value = currentList
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.value = DeviceConnectionState.Error(
                message = "BLE Scan failed with error code $errorCode"
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null || isScanning) {
            return
        }

        _discoveredDevices.value = emptyList()
        isScanning = true

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(AirNodeGattAttributes.SERVICE_UUID))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(filters, settings, scanCallback)
        } catch (e: SecurityException) {
            isScanning = false
            _connectionState.value = DeviceConnectionState.Error(
                message = "Missing required Bluetooth permissions to scan",
                throwable = e
            )
        } catch (e: Exception) {
            isScanning = false
            _connectionState.value = DeviceConnectionState.Error(
                message = "Failed to start BLE scan: ${e.message}",
                throwable = e
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        if (!isScanning) return

        try {
            scanner.stopScan(scanCallback)
        } catch (_: Exception) {
            // suppress
        } finally {
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(deviceAddress: String, deviceName: String?) {
        stopScan()
        currentTargetAddress = deviceAddress
        currentTargetName = deviceName

        val adapter = bluetoothAdapter ?: run {
            _connectionState.value = DeviceConnectionState.Error(
                deviceAddress = deviceAddress,
                message = "Bluetooth adapter not available"
            )
            return
        }

        val device = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: IllegalArgumentException) {
            _connectionState.value = DeviceConnectionState.Error(
                deviceAddress = deviceAddress,
                message = "Invalid MAC address: $deviceAddress",
                throwable = e
            )
            return
        }

        _connectionState.value = DeviceConnectionState.Connecting(
            deviceAddress = deviceAddress,
            deviceName = deviceName ?: device.name
        )

        coroutineScope.launch {
            bleManager.connect(device)
                .useAutoConnect(true)
                .timeout(10000)
                .retry(3, 1000)
                .enqueue()
        }
    }

    override fun disconnect() {
        stopScan()
        currentTargetAddress = null
        currentTargetName = null
        bleManager.disconnect().enqueue()
        _connectionState.value = DeviceConnectionState.Disconnected
    }

    override fun setLedState(isOn: Boolean) {
        bleManager.setLedState(isOn)
    }

    override fun retryConnection() {
        val addr = currentTargetAddress
        if (addr != null) {
            connect(addr, currentTargetName)
        }
    }
}
