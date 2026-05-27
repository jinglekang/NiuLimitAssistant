package com.example.ui

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bluetooth.BLEConnectionState
import com.example.bluetooth.ScannedBleDevice
import com.example.ui.theme.NiuRed
import com.example.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    viewModel: NiuViewModel,
    onDeviceClick: (ScannedBleDevice) -> Unit
) {
    val context = LocalContext.current

    val scannedDevices by viewModel.scannedDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isAutoConnectEnabled by viewModel.isAutoConnectEnabled.collectAsStateWithLifecycle()
    val lastDeviceAddress by viewModel.lastDeviceAddress.collectAsStateWithLifecycle()
    val lastDeviceName by viewModel.lastDeviceName.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectionError by viewModel.connectionError.collectAsStateWithLifecycle()

    val onlyShowNiu by viewModel.onlyShowNiu.collectAsStateWithLifecycle()
    val isConnecting = connectionState == BLEConnectionState.CONNECTING
    var connectingDeviceAddress by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(connectionState) {
        if (!isConnecting) {
            connectingDeviceAddress = null
        }
    }

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = requiredPermissions.all { results[it] == true }
        if (allGranted) {
            val reconnectStarted = viewModel.tryReconnectLastDeviceOnce(true)
            if (!reconnectStarted) {
                viewModel.startScanning()
            }
            Toast.makeText(
                context,
                if (reconnectStarted) "蓝牙权限已获取，正在连接上次设备" else "蓝牙权限已获取，开始扫描",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(context, "需要蓝牙权限以扫描附近的电动车", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermissions = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.tryReconnectLastDeviceOnce(hasPermissions)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppLogo()
                        Column {
                            Text(
                                text = "小牛限速助手",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "车辆合规限速一键恢复系统",
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "自动连接上次设备",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lastDeviceAddress.isBlank()) {
                                    "手动连接一次后会记住设备"
                                } else {
                                    (lastDeviceName.ifBlank { "已保存设备" }) + " / " + lastDeviceAddress
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = isAutoConnectEnabled,
                            onCheckedChange = { viewModel.setAutoConnectEnabled(it) },
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "扫描附近小牛电动车",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "自动扫描识别'NIU Link*'前缀设备",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (isScanning) {
                                        viewModel.stopScanning()
                                    } else {
                                        permissionLauncher.launch(requiredPermissions)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isScanning) MaterialTheme.colorScheme.error.copy(
                                        alpha = 0.1f
                                    ) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = 14.dp,
                                    vertical = 6.dp
                                ),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("scan_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = MaterialTheme.colorScheme.error,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("停止", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "开始扫描",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "过滤条件: " + if (onlyShowNiu) "仅显示NIU小牛车辆" else "显示全部蓝牙设备",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            TextButton(
                                onClick = { viewModel.setOnlyShowNiu(!onlyShowNiu) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(
                                    text = if (onlyShowNiu) "显示全部" else "仅设NIU",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (connectionState == BLEConnectionState.FAILED && !connectionError.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = connectionError ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val filteredList = scannedDevices.filter {
                            !onlyShowNiu || it.isNiuLink
                        }

                        if (filteredList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .background(
                                        MaterialTheme.colorScheme.background,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isScanning) "正在努力寻找附近的设备..." else "未在扫描，请点击上方按钮开始",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filteredList.take(6).forEach { dev ->
                                    val isLastDevice =
                                        dev.address.equals(lastDeviceAddress, ignoreCase = true)
                                    val isConnectingThisDevice =
                                        isConnecting && dev.address == connectingDeviceAddress
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.background,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable(enabled = !isConnecting) {
                                                connectingDeviceAddress = dev.address
                                                onDeviceClick(dev)
                                            }
                                            .padding(12.dp)
                                            .testTag("device_item_${dev.address}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(
                                                        if (dev.isNiuLink) NiuRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.1f
                                                        ),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (dev.isNiuLink) Icons.Default.Lock else Icons.Default.Settings,
                                                    contentDescription = null,
                                                    tint = if (dev.isNiuLink) NiuRed else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = dev.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val deviceTags = listOfNotNull(
                                                    if (dev.isNiuLink) "NIU车辆" else null,
                                                    if (isLastDevice) "上次连接" else null
                                                )
                                                if (deviceTags.isNotEmpty()) {
                                                    Text(
                                                        text = deviceTags.joinToString(" · "),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (dev.isNiuLink) NiuRed else SafeGreen
                                                    )
                                                    Spacer(modifier = Modifier.height(1.dp))
                                                }
                                                Text(
                                                    text = "MAC: " + dev.address,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.5f
                                                    )
                                                )
                                            }
                                        }

                                        if (isConnectingThisDevice) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Text(
                                                    text = "连接中",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "${dev.rssi} dBm",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (dev.rssi > -65) SafeGreen else MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.4f
                                                    )
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "连接",
                                                    tint = MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.6f
                                                    ),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.6f
                        )
                    ),
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "合规警示",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "合规声明与法律警示",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "根据中华人民共和国国家标准《电动自行车安全技术规范》(GB17761-2018)规定，电动自行车设计最高时速不得超过25km/h。此工具严禁用于非法提速改装，仅限执法检验、出厂配置检验和车主合法合规限速还原。",
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}
