package com.r42914lg.blesandbox.ui

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r42914lg.blesandbox.R
import com.r42914lg.blesandbox.blewrapper.model.DiscoveredDevice
import com.r42914lg.blesandbox.mvi.BottomSheetState

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirNodeBottomSheet(
    bottomSheetState: BottomSheetState,
    onDismiss: () -> Unit,
    onSelectDevice: (String, String?) -> Unit,
    onRequestPermissions: () -> Unit,
    onEnableLocation: () -> Unit,
    onEnableBluetooth: () -> Unit
) {
    val isSetupRequired = bottomSheetState is BottomSheetState.SetupRequired
    val activity = LocalContext.current as? Activity

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (isSetupRequired) {
                targetValue != SheetValue.Hidden
            } else {
                true
            }
        }
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (isSetupRequired) {
                activity?.finish()
            } else {
                onDismiss()
            }
        },
        sheetState = sheetState,
        dragHandle = if (isSetupRequired) null else { { BottomSheetDefaults.DragHandle() } },
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(isSetupRequired) {
                    if (isSetupRequired) {
                        detectVerticalDragGestures { change, _ ->
                            change.consume()
                        }
                    }
                }
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            when (bottomSheetState) {
                is BottomSheetState.SetupRequired -> {
                    SetupSheetContent(
                        state = bottomSheetState,
                        onRequestPermissions = onRequestPermissions,
                        onEnableLocation = onEnableLocation,
                        onEnableBluetooth = onEnableBluetooth
                    )
                }

                is BottomSheetState.DevicePicker -> {
                    DevicePickerSheetContent(
                        devices = bottomSheetState.discoveredDevices,
                        onSelectDevice = onSelectDevice
                    )
                }

                BottomSheetState.Hidden -> {}
            }
        }
    }
}

@Composable
fun SetupSheetContent(
    state: BottomSheetState.SetupRequired,
    onRequestPermissions: () -> Unit,
    onEnableLocation: () -> Unit,
    onEnableBluetooth: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        SetupRow(
            isDone = state.isPermissionsGranted,
            label = stringResource(R.string.allow_nearby_devices),
            doneLabel = stringResource(R.string.nearby_devices_allowed),
            onClick = onRequestPermissions
        )

        Spacer(modifier = Modifier.height(12.dp))

        SetupRow(
            isDone = state.isBluetoothEnabled,
            label = stringResource(R.string.enable_bluetooth),
            doneLabel = stringResource(R.string.bluetooth_is_on),
            onClick = onEnableBluetooth
        )

        if (!state.isLocationEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            SetupRow(
                isDone = state.isLocationEnabled,
                label = stringResource(R.string.enable_location),
                doneLabel = stringResource(R.string.location_is_on),
                onClick = onEnableLocation
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SetupRow(isDone: Boolean, label: String, doneLabel: String, onClick: () -> Unit) {
    if (isDone) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = TealSoft,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CheckIcon(color = TealAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = doneLabel,
                    color = TealAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = label, color = Color.White)
        }
    }
}

@Composable
fun CheckIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.5f)
            lineTo(size.width * 0.45f, size.height * 0.75f)
            lineTo(size.width * 0.8f, size.height * 0.25f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun DevicePickerSheetContent(
    devices: List<DiscoveredDevice>,
    onSelectDevice: (String, String?) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.picker_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingDot(color = TealAccent)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.picker_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (devices.isEmpty()) {
            Text(
                text = stringResource(R.string.picker_looking),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(devices) { device ->
                    DeviceRowItem(device = device, onSelect = onSelectDevice)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DeviceRowItem(device: DiscoveredDevice, onSelect: (String, String?) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(device.address, device.name) },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NeutralLine),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RssiSignalBars(rssi = device.rssi)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${device.address} · ${device.rssi} dBm",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = TealAccent,
                shape = RoundedCornerShape(99.dp)
            ) {
                Text(
                    text = stringResource(R.string.connect_button),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun RssiSignalBars(rssi: Int) {
    val level = when {
        rssi > -60 -> 4
        rssi > -70 -> 3
        rssi > -80 -> 2
        else -> 1
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..4) {
            val heightDp = (i * 4).dp
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(heightDp)
                    .background(
                        color = if (i <= level) MaterialTheme.colorScheme.onSurface else NeutralLine,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
