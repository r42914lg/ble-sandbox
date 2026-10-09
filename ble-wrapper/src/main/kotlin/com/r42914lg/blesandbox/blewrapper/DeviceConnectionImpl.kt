package com.r42914lg.blesandbox.blewrapper

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.r42914lg.blesandbox.blewrapper.gatt.AirNodeBleManager
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
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.ble.observer.ConnectionObserver

private const val TAG = "DeviceConnectionImpl"

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
    private var isDeviceReady = false

    init {
        setupConnectionObserver()
    }

    private fun setupConnectionObserver() {
        bleManager.setConnectionObserver(object : ConnectionObserver {
            @SuppressLint("MissingPermission")
            override fun onDeviceConnecting(device: android.bluetooth.BluetoothDevice) {
                isDeviceReady = false
                Log.d(TAG, "onDeviceConnecting: ${device.address}")
                _connectionState.value = DeviceConnectionState.Connecting(
                    deviceAddress = device.address,
                    deviceName = device.name ?: currentTargetName
                )
            }

            override fun onDeviceConnected(device: android.bluetooth.BluetoothDevice) {
                Log.d(TAG, "onDeviceConnected: ${device.address}")
                // Connected at link layer, discovering services...
            }

            override fun onDeviceFailedToConnect(device: android.bluetooth.BluetoothDevice, reason: Int) {
                isDeviceReady = false
                Log.e(TAG, "onDeviceFailedToConnect: ${device.address}, reason=$reason")
                _connectionState.value = DeviceConnectionState.Error(
                    deviceAddress = device.address,
                    message = "Failed to connect to device (reason $reason)",
                    gattStatus = reason
                )
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceReady(device: android.bluetooth.BluetoothDevice) {
                isDeviceReady = true
                Log.d(TAG, "onDeviceReady: ${device.address}")
                _connectionState.value = DeviceConnectionState.Connected(
                    deviceAddress = device.address,
                    deviceName = device.name ?: currentTargetName
                )
            }

            override fun onDeviceDisconnecting(device: android.bluetooth.BluetoothDevice) {
                Log.d(TAG, "onDeviceDisconnecting: ${device.address}")
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceDisconnected(device: android.bluetooth.BluetoothDevice, reason: Int) {
                Log.d(TAG, "onDeviceDisconnected: ${device.address}, reason=$reason, wasReady=$isDeviceReady")
                if (reason == ConnectionObserver.REASON_SUCCESS || reason == ConnectionObserver.REASON_TERMINATE_LOCAL_HOST) {
                    // user disconnects
                    isDeviceReady = false
                    _connectionState.value = DeviceConnectionState.Disconnected
                } else if (isDeviceReady) {
                    // signal lost
                    val lastTempTimestamp = temperatureFlow.value?.timestampMs
                    _connectionState.value = DeviceConnectionState.Reconnecting(
                        deviceAddress = device.address,
                        deviceName = device.name ?: currentTargetName,
                        lastReadingTimestampMs = lastTempTimestamp
                    )
                } else {
                    // initial setup never succeeded
                    isDeviceReady = false
                    _connectionState.value = DeviceConnectionState.Error(
                        deviceAddress = device.address,
                        message = "Could not establish connection to device (GATT status $reason)",
                        gattStatus = reason
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
            val name = result.scanRecord?.deviceName ?: device.name ?: "Unknown Device"
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
            Log.e(TAG, "onScanFailed with errorCode: $errorCode")
            _connectionState.value = DeviceConnectionState.Error(
                message = "BLE Scan failed with error code $errorCode"
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        Log.d(TAG, "startScan requested, scanner=$scanner, isScanning=$isScanning")
        if (scanner == null || isScanning) {
            return
        }

        _discoveredDevices.value = emptyList()
        isScanning = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            Log.d(TAG, "Calling scanner.startScan...")
            scanner.startScan(null, settings, scanCallback)
        } catch (e: SecurityException) {
            isScanning = false
            Log.e(TAG, "SecurityException in startScan", e)
            _connectionState.value = DeviceConnectionState.Error(
                message = "Missing required Bluetooth permissions to scan",
                throwable = e
            )
        } catch (e: Exception) {
            isScanning = false
            Log.e(TAG, "Exception in startScan", e)
            _connectionState.value = DeviceConnectionState.Error(
                message = "Failed to start BLE scan: ${e.message}",
                throwable = e
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        Log.d(TAG, "stopScan requested, isScanning=$isScanning, scanner=$scanner")
        if (!isScanning || scanner == null) {
            isScanning = false
            return
        }

        try {
            scanner.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in stopScan", e)
        } finally {
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(deviceAddress: String, deviceName: String?) {
        Log.d(TAG, "connect called for address: $deviceAddress, name: $deviceName")
        stopScan()

        coroutineScope.launch {
            if (bleManager.isConnected) {
                try {
                    bleManager.disconnect().suspend()
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while disconnecting previous session", e)
                }
            }

            currentTargetAddress = deviceAddress
            currentTargetName = deviceName
            isDeviceReady = false

            val adapter = bluetoothAdapter
            if (adapter == null) {
                _connectionState.value = DeviceConnectionState.Error(
                    deviceAddress = deviceAddress,
                    message = "Bluetooth adapter not available"
                )
                return@launch
            }

            val device = try {
                adapter.getRemoteDevice(deviceAddress)
            } catch (e: IllegalArgumentException) {
                _connectionState.value = DeviceConnectionState.Error(
                    deviceAddress = deviceAddress,
                    message = "Invalid MAC address: $deviceAddress",
                    throwable = e
                )
                return@launch
            }

            _connectionState.value = DeviceConnectionState.Connecting(
                deviceAddress = deviceAddress,
                deviceName = deviceName ?: device.name
            )

            bleManager.connect(device)
                .useAutoConnect(false)
                .timeout(10000)
                .retry(3, 1000)
                .enqueue()
        }
    }

    override fun disconnect() {
        Log.d(TAG, "disconnect called")
        stopScan()
        currentTargetAddress = null
        currentTargetName = null
        isDeviceReady = false
        bleManager.disconnect().enqueue()
        _connectionState.value = DeviceConnectionState.Disconnected
    }

    override fun setLedState(isOn: Boolean) {
        Log.d(TAG, "setLedState: $isOn")
        bleManager.setLedState(isOn)
    }

    override fun retryConnection() {
        Log.d(TAG, "retryConnection called")
        val addr = currentTargetAddress
        if (addr != null) {
            connect(addr, currentTargetName)
        }
    }
}
