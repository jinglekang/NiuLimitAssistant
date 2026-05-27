package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.NiuBluetoothManager
import com.example.bluetooth.ScannedBleDevice
import com.example.data.OperationLog
import com.example.data.OperationLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NiuViewModel(
    private val context: Context,
    private val repository: OperationLogRepository
) : ViewModel() {

    val bleManager = NiuBluetoothManager(context)

    private val sharedPrefs = context.getSharedPreferences("SpeedLimitPrefs", Context.MODE_PRIVATE)

    private val _hexCommand = MutableStateFlow(
        sharedPrefs.getString("hex_command", "5A0C010100000000") ?: "5A0C010100000000"
    )
    val hexCommand = _hexCommand.asStateFlow()

    private val _isWriteNoResponse =
        MutableStateFlow(sharedPrefs.getBoolean("write_no_response", true))
    val isWriteNoResponse = _isWriteNoResponse.asStateFlow()

    val scannedDevices = bleManager.scannedDevices
    val isScanning = bleManager.isScanning
    val connectionState = bleManager.connectionState
    val connectedDevice = bleManager.connectedDevice
    val writeResult = bleManager.writeResult

    val operationLogs = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setHexCommand(command: String) {
        val clean =
            command.uppercase().filter { it.isDigit() || it in 'A'..'F' || it == ' ' || it == ':' }
        _hexCommand.value = clean
        sharedPrefs.edit().putString("hex_command", clean).apply()
    }

    fun setWriteNoResponse(enabled: Boolean) {
        _isWriteNoResponse.value = enabled
        sharedPrefs.edit().putBoolean("write_no_response", enabled).apply()
    }

    fun startScanning() {
        bleManager.startScan()
    }

    fun stopScanning() {
        bleManager.stopScan()
    }

    fun connectToDevice(device: ScannedBleDevice) {
        bleManager.connect(device)
    }

    fun disconnectDevice() {
        bleManager.disconnect()
    }

    fun restoreSpeedLimit() {
        val currentDevice = connectedDevice.value ?: return
        val currentCommand = _hexCommand.value.replace(" ", "").replace(":", "")

        bleManager.writeSpeedLimitCode(
            currentCommand,
            _isWriteNoResponse.value
        ) { success, details ->
            viewModelScope.launch {
                repository.insertLog(
                    OperationLog(
                        deviceName = currentDevice.name,
                        macAddress = currentDevice.address,
                        commandHex = _hexCommand.value,
                        isSuccess = success,
                        statusMessage = details
                    )
                )
            }
        }
    }

    fun logEvent(isSuccess: Boolean, message: String) {
        val currentDevice = connectedDevice.value ?: return
        viewModelScope.launch {
            repository.insertLog(
                OperationLog(
                    deviceName = currentDevice.name,
                    macAddress = currentDevice.address,
                    commandHex = _hexCommand.value,
                    isSuccess = isSuccess,
                    statusMessage = message
                )
            )
        }
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.stopScan()
        bleManager.disconnect()
    }

    class Factory(
        private val context: Context,
        private val repository: OperationLogRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NiuViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NiuViewModel(context, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
