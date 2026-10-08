package com.r42914lg.blesandbox.mvi

import android.content.Context

sealed interface MainScreenAction {
    data class CheckReadiness(val context: Context) : MainScreenAction
    data object OpenBottomSheet : MainScreenAction
    data object CloseBottomSheet : MainScreenAction
    data class ConnectToDevice(val address: String, val name: String? = null) : MainScreenAction
    data object Disconnect : MainScreenAction
    data class ToggleLed(val isOn: Boolean) : MainScreenAction
    data object RetryConnection : MainScreenAction
}
