package com.r42914lg.blesandbox.ui

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.r42914lg.blesandbox.PermissionUtils
import com.r42914lg.blesandbox.blewrapper.model.LedState
import com.r42914lg.blesandbox.blewrapper.model.TemperatureReading
import com.r42914lg.blesandbox.mvi.BottomSheetState
import com.r42914lg.blesandbox.mvi.MainContentState
import com.r42914lg.blesandbox.mvi.MainScreenAction
import com.r42914lg.blesandbox.mvi.MainUiState
import com.r42914lg.blesandbox.mvi.MainViewModel
import org.koin.androidx.compose.koinViewModel

val TealAccent = Color(0xFF0B7A85)
val TealSoft = Color(0xFFD6EEF0)
val WarnAmber = Color(0xFF9A5B00)
val WarnAmberSoft = Color(0xFFFFF8E1)
val BadRed = Color(0xFFB3261E)
val BadRedSoft = Color(0xFFFADFDC)
val NeutralLine = Color(0xFFD3DBE2)

@Composable
fun MainScreen() {
    val viewModel: MainViewModel = koinViewModel()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var hasRequestedPermission by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenAction(MainScreenAction.CheckReadiness(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasRequestedPermission = true
        viewModel.onScreenAction(MainScreenAction.CheckReadiness(context))
    }

    val handleRequestPermissions: () -> Unit = {
        val activity = context as? Activity
        if (hasRequestedPermission && activity != null && !PermissionUtils.shouldShowRationale(activity)) {
            PermissionUtils.openAppSettings(context)
        } else {
            hasRequestedPermission = true
            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
        }
    }

    MainScreenContent(
        uiState = uiState,
        onOpenSheet = { viewModel.onScreenAction(MainScreenAction.OpenBottomSheet) },
        onCloseSheet = { viewModel.onScreenAction(MainScreenAction.CloseBottomSheet) },
        onSelectDevice = { addr, name -> viewModel.onScreenAction(MainScreenAction.ConnectToDevice(addr, name)) },
        onDisconnect = { viewModel.onScreenAction(MainScreenAction.Disconnect) },
        onToggleLed = { isOn -> viewModel.onScreenAction(MainScreenAction.ToggleLed(isOn)) },
        onRetry = { viewModel.onScreenAction(MainScreenAction.RetryConnection) },
        onRequestPermissions = handleRequestPermissions,
        onEnableLocation = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
        onEnableBluetooth = {
            try {
                context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } catch (_: SecurityException) {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
        }
    )
}

@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onOpenSheet: () -> Unit,
    onCloseSheet: () -> Unit,
    onSelectDevice: (String, String?) -> Unit,
    onDisconnect: () -> Unit,
    onToggleLed: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onRequestPermissions: () -> Unit,
    onEnableLocation: () -> Unit,
    onEnableBluetooth: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 50.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
        ) {
            TopHeaderBar(
                contentState = uiState.contentState
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val content = uiState.contentState) {
                    is MainContentState.NoDevice -> {
                        EmptyStateView(onOpenSheet = onOpenSheet)
                    }

                    is MainContentState.Connecting -> {
                        ActiveDeviceView(
                            deviceName = content.deviceName,
                            deviceAddress = content.deviceAddress,
                            temperature = null,
                            ledState = LedState(),
                            isConnecting = true,
                            isReconnecting = false,
                            onToggleLed = onToggleLed,
                            onChangeDevice = onDisconnect
                        )
                    }

                    is MainContentState.Connected -> {
                        ActiveDeviceView(
                            deviceName = content.deviceName,
                            deviceAddress = content.deviceAddress,
                            temperature = content.temperature,
                            ledState = content.ledState,
                            isConnecting = false,
                            isReconnecting = false,
                            onToggleLed = onToggleLed,
                            onChangeDevice = onDisconnect
                        )
                    }

                    is MainContentState.Reconnecting -> {
                        ActiveDeviceView(
                            deviceName = content.deviceName,
                            deviceAddress = content.deviceAddress,
                            temperature = content.lastTemperature,
                            ledState = content.ledState,
                            isConnecting = false,
                            isReconnecting = true,
                            onToggleLed = onToggleLed,
                            onChangeDevice = onDisconnect
                        )
                    }

                    is MainContentState.Error -> {
                        ErrorStateView(
                            message = content.message,
                            onRetry = onRetry,
                            onOpenSheet = onOpenSheet
                        )
                    }
                }
            }
        }

        if (uiState.sheetState !is BottomSheetState.Hidden) {
            AirNodeBottomSheet(
                bottomSheetState = uiState.sheetState,
                onDismiss = onCloseSheet,
                onSelectDevice = onSelectDevice,
                onRequestPermissions = onRequestPermissions,
                onEnableLocation = onEnableLocation,
                onEnableBluetooth = onEnableBluetooth
            )
        }
    }
}

@Composable
fun EmptyStateView(onOpenSheet: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No device yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose the AirNode you want to read.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onOpenSheet,
            colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
        ) {
            Text(text = "Choose device", color = Color.White)
        }
    }
}

@Composable
fun ActiveDeviceView(
    deviceName: String,
    deviceAddress: String,
    temperature: TemperatureReading?,
    ledState: LedState,
    isConnecting: Boolean,
    isReconnecting: Boolean,
    onToggleLed: (Boolean) -> Unit,
    onChangeDevice: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            val cardBorderColor = if (isReconnecting) WarnAmber else NeutralLine
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, cardBorderColor),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsingDot(
                            color = when {
                                isReconnecting -> WarnAmber
                                isConnecting -> Color.Gray
                                else -> TealAccent
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TEMPERATURE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        val tempText = when {
                            isConnecting -> "--.-"
                            temperature != null -> "%.1f".format(temperature.temperatureCelsius)
                            else -> "--.-"
                        }
                        Text(
                            text = tempText,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = if (isReconnecting) WarnAmber else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "°C",
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val noteText = when {
                        isConnecting -> "Connecting to $deviceName..."
                        isReconnecting -> "Connection lost. Last reading available."
                        else -> "Updates every second"
                    }
                    Text(
                        text = noteText,
                        fontSize = 13.sp,
                        color = if (isReconnecting) WarnAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, NeutralLine),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Onboard LED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val lastWriteText = ledState.lastWriteValue?.let { "last write $it" } ?: "no write yet"
                        Text(
                            text = "GPIO 8 · $lastWriteText",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = ledState.isOn,
                        onCheckedChange = onToggleLed,
                        enabled = !isConnecting && !isReconnecting,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TealAccent
                        )
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onChangeDevice,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Change device", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit,
    onOpenSheet: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BadRedSoft),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Couldn’t connect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BadRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                ) {
                    Text(text = "Try again", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenSheet,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Choose another device", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
