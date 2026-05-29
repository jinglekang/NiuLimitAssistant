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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bluetooth.BLEConnectionState
import com.example.bluetooth.ScannedBleDevice

private object MainRoute {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun MainScreen(viewModel: NiuViewModel) {
    val navController = rememberNavController()
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

    NavHost(
        navController = navController,
        startDestination = MainRoute.HOME
    ) {
        composable(MainRoute.HOME) {
            Crossfade(
                targetState = showControl,
                animationSpec = tween(durationMillis = 300)
            ) { shouldShowControl ->
                if (shouldShowControl) {
                    ControlScreen(
                        viewModel = viewModel,
                        onDisconnectClick = { viewModel.disconnectDevice() },
                        onSettingsClick = { navController.navigate(MainRoute.SETTINGS) }
                    )
                } else {
                    ConnectScreen(
                        viewModel = viewModel,
                        onSettingsClick = { navController.navigate(MainRoute.SETTINGS) },
                        onDeviceClick = { device: ScannedBleDevice ->
                            viewModel.connectToDevice(device)
                        }
                    )
                }
            }
        }

        composable(MainRoute.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
