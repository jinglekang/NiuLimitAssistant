package com.example.ui

import android.content.Context
import androidx.core.content.edit
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

    init {
        viewModelScope.launch {
            scannedDevices.collect { devices ->
                val targetAddress = _lastDeviceAddress.value
                if (!_isAutoConnectEnabled.value || targetAddress.isBlank()) return@collect
                if (connectionState.value != com.example.bluetooth.BLEConnectionState.DISCONNECTED) return@collect

                val targetDevice = devices.firstOrNull {
                    it.address.equals(targetAddress, ignoreCase = true)
                } ?: return@collect

                bleManager.stopScan()
                bleManager.connect(targetDevice)
            }
        }
    }

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

    fun startAutoConnectScanIfReady() {
        if (_isAutoConnectEnabled.value && _lastDeviceAddress.value.isNotBlank()) {
            bleManager.startScan()
        }
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
