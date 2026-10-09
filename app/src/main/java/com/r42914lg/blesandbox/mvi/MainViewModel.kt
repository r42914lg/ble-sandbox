package com.r42914lg.blesandbox.mvi

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r42914lg.blesandbox.PermissionUtils
import com.r42914lg.blesandbox.blewrapper.DeviceConnection
import com.r42914lg.blesandbox.blewrapper.model.DeviceConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

data class ReadinessState(
    val isPermissionsGranted: Boolean = false,
    val isLocationEnabled: Boolean = true,
    val isBluetoothEnabled: Boolean = false
) {
    val isSetupComplete: Boolean
        get() = isPermissionsGranted && isLocationEnabled && isBluetoothEnabled
}

class MainViewModel(
    private val deviceConnection: DeviceConnection,
    context: Context
) : ViewModel() {

    private val readinessFlow = MutableStateFlow(
        ReadinessState(
            isPermissionsGranted = PermissionUtils.hasRequiredPermissions(context),
            isLocationEnabled = PermissionUtils.isLocationEnabled(context),
            isBluetoothEnabled = PermissionUtils.isBluetoothEnabled(context)
        )
    )

    private val sheetExplicitOpenFlow = MutableStateFlow(true)

    private val contentStateFlow = combine(
        deviceConnection.connectionState,
        deviceConnection.temperatureFlow,
        deviceConnection.ledStateFlow
    ) { connState, temp, led ->
        when (connState) {
            is DeviceConnectionState.Disconnected -> MainContentState.NoDevice
            is DeviceConnectionState.Connecting -> MainContentState.Connecting(
                deviceAddress = connState.deviceAddress,
                deviceName = connState.deviceName ?: "AirNode"
            )
            is DeviceConnectionState.Connected -> MainContentState.Connected(
                deviceAddress = connState.deviceAddress,
                deviceName = connState.deviceName ?: "AirNode",
                temperature = temp,
                ledState = led
            )
            is DeviceConnectionState.Reconnecting -> MainContentState.Reconnecting(
                deviceAddress = connState.deviceAddress,
                deviceName = connState.deviceName ?: "AirNode",
                lastTemperature = temp,
                ledState = led
            )
            is DeviceConnectionState.Error -> MainContentState.Error(
                message = connState.message,
                deviceAddress = connState.deviceAddress
            )
        }
    }

    private val sheetStateFlow = combine(
        readinessFlow,
        sheetExplicitOpenFlow,
        deviceConnection.discoveredDevices
    ) { readiness, isSheetOpen, devices ->
        when {
            !readiness.isSetupComplete -> BottomSheetState.SetupRequired(
                isPermissionsGranted = readiness.isPermissionsGranted,
                isLocationEnabled = readiness.isLocationEnabled,
                isBluetoothEnabled = readiness.isBluetoothEnabled
            )
            isSheetOpen -> BottomSheetState.DevicePicker(
                devices.filter { it.name.contains("AirNode-C3") }
            )
            else -> BottomSheetState.Hidden
        }
    }.onEach { sheet ->
        if (sheet is BottomSheetState.DevicePicker) {
            deviceConnection.startScan()
        } else {
            deviceConnection.stopScan()
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        contentStateFlow,
        sheetStateFlow
    ) { content, sheet ->
        MainUiState(contentState = content, sheetState = sheet)
    }.onStart {
        checkReadiness(context)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun onScreenAction(action: MainScreenAction) {
        when (action) {
            is MainScreenAction.CheckReadiness -> checkReadiness(action.context)
            MainScreenAction.OpenBottomSheet -> sheetExplicitOpenFlow.value = true
            MainScreenAction.CloseBottomSheet -> sheetExplicitOpenFlow.value = false
            is MainScreenAction.ConnectToDevice -> {
                sheetExplicitOpenFlow.value = false
                deviceConnection.connect(action.address, action.name)
            }
            MainScreenAction.Disconnect -> {
                deviceConnection.disconnect()
                sheetExplicitOpenFlow.value = true
            }
            is MainScreenAction.ToggleLed -> deviceConnection.setLedState(action.isOn)
            MainScreenAction.RetryConnection -> deviceConnection.retryConnection()
        }
    }

    private fun checkReadiness(context: Context) {
        readinessFlow.value = ReadinessState(
            isPermissionsGranted = PermissionUtils.hasRequiredPermissions(context),
            isLocationEnabled = PermissionUtils.isLocationEnabled(context),
            isBluetoothEnabled = PermissionUtils.isBluetoothEnabled(context)
        )
    }

    override fun onCleared() {
        super.onCleared()
        deviceConnection.stopScan()
    }
}
