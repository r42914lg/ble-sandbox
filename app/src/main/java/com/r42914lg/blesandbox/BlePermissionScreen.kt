package com.r42914lg.blesandbox

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@SuppressLint("MissingPermission")
@Composable
fun BlePermissionScreen() {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(PermissionUtils.hasRequiredPermissions(context)) }
    var isLocationEnabled by remember { mutableStateOf(PermissionUtils.isLocationEnabled(context)) }
    var isBluetoothEnabled by remember { mutableStateOf(PermissionUtils.isBluetoothEnabled(context)) }

    fun updateStates() {
        hasPermissions = PermissionUtils.hasRequiredPermissions(context)
        isLocationEnabled = PermissionUtils.isLocationEnabled(context)
        isBluetoothEnabled = PermissionUtils.isBluetoothEnabled(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateStates()
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
        updateStates()
    }

    BlePermissionContent(
        hasPermissions = hasPermissions,
        isLocationEnabled = isLocationEnabled,
        isBluetoothEnabled = isBluetoothEnabled,
        onRequestPermissions = {
            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
        },
        onEnableLocation = {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        },
        onEnableBluetooth = {
            try {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                context.startActivity(enableBtIntent)
            } catch (e: SecurityException) {
                val btSettingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                context.startActivity(btSettingsIntent)
            }
        }
    )
}

@Composable
fun BlePermissionContent(
    hasPermissions: Boolean,
    isLocationEnabled: Boolean,
    isBluetoothEnabled: Boolean,
    onRequestPermissions: () -> Unit = {},
    onEnableLocation: () -> Unit = {},
    onEnableBluetooth: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BLE Readiness Check",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        StatusCard(
            title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Bluetooth Permissions (Android 12+)" else "Location Permission (Android <= 11)",
            isGranted = hasPermissions,
            description = if (hasPermissions) "Required permissions granted" else "Permissions needed for BLE scanning & connecting",
            buttonText = "Grant Permissions",
            onButtonClick = onRequestPermissions
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            StatusCard(
                title = "Location Services (GPS)",
                isGranted = isLocationEnabled,
                description = if (isLocationEnabled) "Location Services enabled" else "Location Services must be turned ON for BLE scanning on older Android versions",
                buttonText = "Enable Location",
                onButtonClick = onEnableLocation
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        StatusCard(
            title = "Bluetooth Adapter",
            isGranted = isBluetoothEnabled,
            description = if (isBluetoothEnabled) "Bluetooth is ON" else "Bluetooth is turned OFF",
            buttonText = "Enable Bluetooth",
            onButtonClick = onEnableBluetooth
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (hasPermissions && isLocationEnabled && isBluetoothEnabled) {
            Text(
                text = "All systems ready for BLE operations!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    isGranted: Boolean,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall)

            if (!isGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onButtonClick) {
                    Text(text = buttonText)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BlePermissionScreenPreviewNotGranted() {
    MaterialTheme {
        Surface {
            BlePermissionContent(
                hasPermissions = false,
                isLocationEnabled = false,
                isBluetoothEnabled = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BlePermissionScreenPreviewReady() {
    MaterialTheme {
        Surface {
            BlePermissionContent(
                hasPermissions = true,
                isLocationEnabled = true,
                isBluetoothEnabled = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatusCardGrantedPreview() {
    MaterialTheme {
        StatusCard(
            title = "Bluetooth Permissions",
            isGranted = true,
            description = "Required permissions granted",
            buttonText = "Grant Permissions",
            onButtonClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatusCardNotGrantedPreview() {
    MaterialTheme {
        StatusCard(
            title = "Bluetooth Permissions",
            isGranted = false,
            description = "Permissions needed for BLE scanning & connecting",
            buttonText = "Grant Permissions",
            onButtonClick = {}
        )
    }
}
