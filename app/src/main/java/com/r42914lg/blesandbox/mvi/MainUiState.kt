package com.r42914lg.blesandbox.mvi

import com.r42914lg.blesandbox.blewrapper.model.DiscoveredDevice
import com.r42914lg.blesandbox.coredata.model.LedStateRecord
import com.r42914lg.blesandbox.coredata.model.TemperatureRecord

sealed interface MainContentState {
    data object NoDevice : MainContentState

    data class Connecting(
        val deviceAddress: String,
        val deviceName: String
    ) : MainContentState

    data class Connected(
        val deviceAddress: String,
        val deviceName: String,
        val temperature: TemperatureRecord?,
        val ledState: LedStateRecord
    ) : MainContentState

    data class Reconnecting(
        val deviceAddress: String,
        val deviceName: String,
        val lastTemperature: TemperatureRecord?,
        val ledState: LedStateRecord
    ) : MainContentState

    data class Error(
        val message: String,
        val deviceAddress: String? = null
    ) : MainContentState
}

sealed interface BottomSheetState {
    data object Hidden : BottomSheetState

    data class SetupRequired(
        val isPermissionsGranted: Boolean,
        val isLocationEnabled: Boolean,
        val isBluetoothEnabled: Boolean
    ) : BottomSheetState

    data class DevicePicker(
        val discoveredDevices: List<DiscoveredDevice> = emptyList()
    ) : BottomSheetState
}

data class MainUiState(
    val contentState: MainContentState = MainContentState.NoDevice,
    val sheetState: BottomSheetState = BottomSheetState.Hidden
) {
    val isSetupComplete: Boolean
        get() = (sheetState as? BottomSheetState.SetupRequired)?.let {
            it.isPermissionsGranted && it.isLocationEnabled && it.isBluetoothEnabled
        } ?: true
}
