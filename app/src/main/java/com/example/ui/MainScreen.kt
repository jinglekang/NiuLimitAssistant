package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bluetooth.BLEConnectionState
import com.example.bluetooth.ScannedBleDevice

@Composable
fun MainScreen(viewModel: NiuViewModel) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    var hasEnteredControl by remember { mutableStateOf(false) }
    LaunchedEffect(connectionState) {
        when (connectionState) {
            BLEConnectionState.CONNECTED,
            BLEConnectionState.SERVICES_DISCOVERING,
            BLEConnectionState.READY -> hasEnteredControl = true

            BLEConnectionState.DISCONNECTED -> hasEnteredControl = false

            else -> Unit
        }
    }

    val showControl = hasEnteredControl && connectionState != BLEConnectionState.DISCONNECTED

    Crossfade(
        targetState = showControl,
        animationSpec = tween(durationMillis = 300)
    ) { shouldShowControl ->
        if (shouldShowControl) {
            ControlScreen(
                viewModel = viewModel,
                onDisconnectClick = { viewModel.disconnectDevice() }
            )
        } else {
            ConnectScreen(
                viewModel = viewModel,
                onDeviceClick = { device: ScannedBleDevice ->
                    viewModel.connectToDevice(device)
                }
            )
        }
    }
}
