package com.example.ui

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.NiuBluetoothManager
import com.example.bluetooth.BLEConnectionState
import com.example.bluetooth.ScannedBleDevice
import com.example.data.OperationLog
import com.example.data.OperationLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NiuViewModel(
    context: Context,
    private val repository: OperationLogRepository
) : ViewModel() {

    val bleManager = NiuBluetoothManager(context.applicationContext)

    private val sharedPrefs =
        context.applicationContext.getSharedPreferences("SpeedLimitPrefs", Context.MODE_PRIVATE)

    private val _hexCommand = MutableStateFlow(
        sharedPrefs.getString("hex_command", "") ?: ""
    )
    val hexCommand = _hexCommand.asStateFlow()

    private val _isWriteNoResponse =
        MutableStateFlow(sharedPrefs.getBoolean("write_no_response", true))
    val isWriteNoResponse = _isWriteNoResponse.asStateFlow()

    private val _isAutoConnectEnabled =
        MutableStateFlow(sharedPrefs.getBoolean("auto_connect_enabled", true))
    val isAutoConnectEnabled = _isAutoConnectEnabled.asStateFlow()

    private val _lastDeviceAddress =
        MutableStateFlow(sharedPrefs.getString("last_device_address", "") ?: "")
    val lastDeviceAddress = _lastDeviceAddress.asStateFlow()

    private val _lastDeviceName =
        MutableStateFlow(sharedPrefs.getString("last_device_name", "") ?: "")
    val lastDeviceName = _lastDeviceName.asStateFlow()

    private val _onlyShowNiu = MutableStateFlow(true)
    val onlyShowNiu = _onlyShowNiu.asStateFlow()

    private var autoReconnectAttempted = false

    fun setOnlyShowNiu(value: Boolean) {
        _onlyShowNiu.value = value
    }

    val scannedDevices = bleManager.scannedDevices
    val isScanning = bleManager.isScanning
    val connectionState = bleManager.connectionState
    val connectedDevice = bleManager.connectedDevice
    val connectionError = bleManager.connectionError
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
        sharedPrefs.edit {
            putString("hex_command", clean)
        }
    }

    fun setWriteNoResponse(enabled: Boolean) {
        _isWriteNoResponse.value = enabled
        sharedPrefs.edit {
            putBoolean("write_no_response", enabled)
        }
    }

    fun setAutoConnectEnabled(enabled: Boolean) {
        _isAutoConnectEnabled.value = enabled
        sharedPrefs.edit {
            putBoolean("auto_connect_enabled", enabled)
        }
    }

    fun tryReconnectLastDeviceOnce(hasRequiredPermissions: Boolean): Boolean {
        if (!hasRequiredPermissions) return false
        if (autoReconnectAttempted) return false
        autoReconnectAttempted = true
        if (!_isAutoConnectEnabled.value || _lastDeviceAddress.value.isBlank()) return false
        if (connectionState.value != BLEConnectionState.DISCONNECTED) return false

        bleManager.connectByAddress(
            _lastDeviceAddress.value,
            _lastDeviceName.value.ifBlank { "已保存设备" }
        )
        return true
    }

    fun startScanning() {
        bleManager.startScan()
    }

    fun stopScanning() {
        bleManager.stopScan()
    }

    fun connectToDevice(device: ScannedBleDevice) {
        _lastDeviceAddress.value = device.address
        _lastDeviceName.value = device.name
        sharedPrefs.edit {
            putString("last_device_address", device.address)
            putString("last_device_name", device.name)
        }
        bleManager.connect(device)
    }

    fun discoverConnectedDeviceServices() {
        bleManager.discoverServices()
    }

    fun disconnectDevice() {
        bleManager.disconnect()
    }

    fun writeCustomCommand() {
        val currentCommand = _hexCommand.value.replace(" ", "").replace(":", "")
        writeCommand(currentCommand, _hexCommand.value, "自定义")
    }

    fun writeFixedCommand(operationType: String, command: String) {
        val currentCommand = command.replace(" ", "").replace(":", "")
        writeCommand(currentCommand, command, operationType)
    }

    private fun writeCommand(
        commandToWrite: String,
        commandForLog: String,
        operationType: String
    ) {
        val currentDevice = connectedDevice.value ?: return
        bleManager.writeSpeedLimitCode(
            commandToWrite,
            _isWriteNoResponse.value
        ) { success, details ->
            viewModelScope.launch {
                repository.insertLog(
                    OperationLog(
                        deviceName = currentDevice.name,
                        macAddress = currentDevice.address,
                        operationType = operationType,
                        commandHex = commandForLog,
                        isSuccess = success,
                        statusMessage = details
                    )
                )
            }
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
        context: Context,
        private val repository: OperationLogRepository
    ) : ViewModelProvider.Factory {
        private val applicationContext = context.applicationContext

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NiuViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NiuViewModel(applicationContext, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
