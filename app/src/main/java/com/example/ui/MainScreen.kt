package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bluetooth.BLEConnectionState
import com.example.bluetooth.ScannedBleDevice

@Composable
fun MainScreen(viewModel: NiuViewModel) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    val isConnected = connectionState != BLEConnectionState.DISCONNECTED

    Crossfade(
        targetState = isConnected,
        animationSpec = tween(durationMillis = 300)
    ) { connected ->
        if (connected) {
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
