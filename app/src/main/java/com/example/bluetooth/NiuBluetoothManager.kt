package com.example.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class BLEConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    SERVICES_DISCOVERING,
    READY,
    FAILED
}

sealed class WriteResult {
    object Idle : WriteResult()
    object Writing : WriteResult()
    object Success : WriteResult()
    data class Error(val errorMsg: String) : WriteResult()
}

data class ScannedBleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val device: BluetoothDevice?,
    val isNiuLink: Boolean = false
)

class NiuBluetoothManager(private val context: Context) {
    private val TAG = "NiuBluetooth"

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScannedBleDevice>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(BLEConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<ScannedBleDevice?>(null)
    val connectedDevice = _connectedDevice.asStateFlow()

    private val _writeResult = MutableStateFlow<WriteResult>(WriteResult.Idle)
    val writeResult = _writeResult.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private var pendingLogCallback: ((Boolean, String) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    val SERVICE_UUID: UUID = UUID.fromString("8ec94e30-f315-4f60-9fb8-838830daea51")
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("8ec94e32-f315-4f60-9fb8-838830daea51")

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val device = result.device
            val name = result.scanRecord?.deviceName ?: device.name ?: "Unknown Device"
            val address = device.address ?: "00:00:00:00:00:00"
            val rssi = result.rssi
            val isNiu = name.startsWith("NIU Link", ignoreCase = true) || name.contains(
                "NIU",
                ignoreCase = true
            )

            val currentList = _scannedDevices.value
            if (currentList.none { it.address == address }) {
                val newDevice = ScannedBleDevice(
                    name = name,
                    address = address,
                    rssi = rssi,
                    device = device,
                    isNiuLink = isNiu
                )
                _scannedDevices.value = (currentList + newDevice).sortedWith(
                    compareByDescending<ScannedBleDevice> { it.isNiuLink }
                        .thenByDescending { it.rssi }
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error $errorCode")
            _isScanning.value = false
        }
    }

    fun startScan() {
        if (_isScanning.value) return
        cleanup()

        _scannedDevices.value = emptyList()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing Bluetooth scan permission")
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null")
            return
        }

        try {
            _isScanning.value = true
            scanner.startScan(scanCallback)
            mainHandler.postDelayed({
                stopScan()
            }, 10000)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth scan permission", e)
            _isScanning.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scan due to exception", e)
            _isScanning.value = false
        }
    }

    fun stopScan() {
        if (!_isScanning.value) return
        _isScanning.value = false

        if (!hasBluetoothScanPermission()) {
            Log.e(TAG, "Missing Bluetooth scan permission while stopping scan")
            return
        }

        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth scan permission while stopping scan", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop BLE scan", e)
        }
    }

    fun connect(scannedDevice: ScannedBleDevice) {
        if (_connectionState.value != BLEConnectionState.DISCONNECTED) {
            cleanup()
        }

        _connectedDevice.value = scannedDevice
        _connectionState.value = BLEConnectionState.CONNECTING
        _writeResult.value = WriteResult.Idle

        val device = scannedDevice.device
        if (device == null) {
            _connectionState.value = BLEConnectionState.FAILED
            return
        }

        if (!hasBluetoothConnectPermission()) {
            Log.e(TAG, "Missing Bluetooth connect permission")
            _connectionState.value = BLEConnectionState.FAILED
            return
        }

        try {
            activeGatt = device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth connect permission", e)
            _connectionState.value = BLEConnectionState.FAILED
        } catch (e: Exception) {
            Log.e(TAG, "Gatt connection exception: ${e.message}", e)
            _connectionState.value = BLEConnectionState.FAILED
        }
    }

    fun disconnect() {
        cleanup()
    }

    private fun cleanup() {
        _connectionState.value = BLEConnectionState.DISCONNECTED
        _connectedDevice.value = null
        _writeResult.value = WriteResult.Idle

        activeGatt?.let { gatt ->
            try {
                if (hasBluetoothConnectPermission()) {
                    gatt.disconnect()
                }
                gatt.close()
            } catch (e: SecurityException) {
                Log.e(TAG, "Missing Bluetooth connect permission while cleaning up gatt", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up gatt", e)
            }
            activeGatt = null
        }
        writeCharacteristic = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT error: status $status")
                _connectionState.value = BLEConnectionState.FAILED
                cleanup()
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT connected")
                _connectionState.value = BLEConnectionState.CONNECTED
                _connectionState.value = BLEConnectionState.SERVICES_DISCOVERING
                if (!hasBluetoothConnectPermission()) {
                    Log.e(TAG, "Missing Bluetooth connect permission while discovering services")
                    _connectionState.value = BLEConnectionState.FAILED
                    cleanup()
                    return
                }

                try {
                    gatt.discoverServices()
                } catch (e: SecurityException) {
                    Log.e(TAG, "Missing Bluetooth connect permission while discovering services", e)
                    _connectionState.value = BLEConnectionState.FAILED
                    cleanup()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "GATT disconnected")
                cleanup()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)

                if (characteristic != null) {
                    writeCharacteristic = characteristic
                    _connectionState.value = BLEConnectionState.READY
                    Log.d(TAG, "Target Service and Characteristic found!")
                } else {
                    Log.e(TAG, "Target characteristic or service not found!")
                    _connectionState.value = BLEConnectionState.FAILED
                }
            } else {
                Log.e(TAG, "Service discovery failed with status $status")
                _connectionState.value = BLEConnectionState.FAILED
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid == CHARACTERISTIC_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _writeResult.value = WriteResult.Success
                    pendingLogCallback?.invoke(true, "写入成功")
                    Log.i(TAG, "Speed limit reset command written successfully!")
                } else {
                    _writeResult.value = WriteResult.Error("写入错误: GATT status $status")
                    pendingLogCallback?.invoke(false, "GATT写入失败: status $status")
                }
                pendingLogCallback = null
            }
        }
    }

    fun writeSpeedLimitCode(
        hexString: String,
        isWriteNoResponse: Boolean = false,
        onLogReady: (Boolean, String) -> Unit
    ) {
        val cleanHex = hexString.replace(" ", "").replace(":", "")
        val bytes = try {
            hexStringToByteArray(cleanHex)
        } catch (e: Exception) {
            _writeResult.value = WriteResult.Error("非法的Hex字节格式: ${e.localizedMessage}")
            onLogReady(false, "非法的Hex字节格式")
            return
        }

        _writeResult.value = WriteResult.Writing

        val gatt = activeGatt
        val char = writeCharacteristic
        if (gatt == null || char == null) {
            _writeResult.value = WriteResult.Error("设备未就绪，特征码未包含")
            onLogReady(false, "设备未就绪或无法读取UUID特征集")
            return
        }

        if (!hasBluetoothConnectPermission()) {
            val errMsg = "缺少蓝牙连接权限"
            _writeResult.value = WriteResult.Error(errMsg)
            onLogReady(false, errMsg)
            return
        }

        val writeType = if (isWriteNoResponse) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
        char.writeType = writeType

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val status = gatt.writeCharacteristic(char, bytes, writeType)
                if (status == 0) {
                    if (isWriteNoResponse) {
                        _writeResult.value = WriteResult.Success
                        onLogReady(true, "写入成功")
                    } else {
                        pendingLogCallback = onLogReady
                    }
                } else {
                    val errMsg = "发送失败 (AGP Status code $status)"
                    _writeResult.value = WriteResult.Error(errMsg)
                    onLogReady(false, errMsg)
                }
            } else {
                @Suppress("DEPRECATION")
                char.value = bytes
                @Suppress("DEPRECATION")
                val success = gatt.writeCharacteristic(char)
                if (success) {
                    if (isWriteNoResponse) {
                        _writeResult.value = WriteResult.Success
                        onLogReady(true, "写入成功")
                    } else {
                        pendingLogCallback = onLogReady
                    }
                } else {
                    val errMsg = "写入失败 (低版本SDK接口拒绝)"
                    _writeResult.value = WriteResult.Error(errMsg)
                    onLogReady(false, errMsg)
                }
            }
        } catch (e: SecurityException) {
            val errMsg = "缺少蓝牙连接权限"
            Log.e(TAG, errMsg, e)
            _writeResult.value = WriteResult.Error(errMsg)
            onLogReady(false, errMsg)
        }
    }

    private fun hasBluetoothScanPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        if (len % 2 != 0) {
            throw IllegalArgumentException("Hex字符串长度必须是偶数")
        }
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            val h = Character.digit(s[i], 16)
            val l = Character.digit(s[i + 1], 16)
            if (h == -1 || l == -1) {
                throw IllegalArgumentException("包含非法的Hex字符: ${s[i]}${s[i + 1]}")
            }
            data[i / 2] = ((h shl 4) + l).toByte()
            i += 2
        }
        return data
    }
}
