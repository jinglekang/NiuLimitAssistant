package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bluetooth.BLEConnectionState
import com.example.bluetooth.WriteResult
import com.example.ui.theme.NiuRed
import com.example.ui.theme.SafeGreen
import org.json.JSONArray

private data class CommandButton(
    val label: String,
    val command: String,
    val disabled: Boolean
)

private fun loadCommandButtons(context: Context): List<CommandButton> {
    val json = context.assets.open("preset_commands.json")
        .bufferedReader()
        .use { it.readText() }
    val array = JSONArray(json)
    return List(array.length()) { index ->
        val item = array.getJSONObject(index)
        CommandButton(
            label = item.getString("label"),
            command = item.optString("command"),
            disabled = item.optBoolean("disabled", false)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ControlScreen(
    viewModel: NiuViewModel,
    onDisconnectClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val mainCommandButtons = remember { loadCommandButtons(context) }

    val hexCommand by viewModel.hexCommand.collectAsStateWithLifecycle()
    val isWriteNoResponse by viewModel.isWriteNoResponse.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    val connectionError by viewModel.connectionError.collectAsStateWithLifecycle()
    val detectedModel by viewModel.detectedModel.collectAsStateWithLifecycle()
    val writeResult by viewModel.writeResult.collectAsStateWithLifecycle()
    val operationLogs by viewModel.operationLogs.collectAsStateWithLifecycle()
    var selectedCommandTab by remember { mutableIntStateOf(0) }
    var lastWriteOperation by remember { mutableStateOf("自定义") }
    var visibleLogCount by remember { mutableIntStateOf(10) }

    // 结果弹窗状态
    var showResultDialog by remember { mutableStateOf(value = false) }

    BackHandler {
        onDisconnectClick()
    }

    LaunchedEffect(connectionState) {
        if (connectionState == BLEConnectionState.CONNECTED) {
            viewModel.discoverConnectedDeviceServices()
        }
    }

    // 写入结果监听
    LaunchedEffect(writeResult) {
        if ((writeResult is WriteResult.Success) || (writeResult is WriteResult.Error)) {
            showResultDialog = true
        }
    }

    fun dismissWriteResultDialog() {
        showResultDialog = false
        viewModel.clearWriteResult()
    }

    Scaffold(
        topBar = {
            NiuTopAppBar(onSettingsClick = onSettingsClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp) // 底部留出空间给非模态提示
            ) {
            item {
                val cardBrush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.surface
                    )
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(
                        0.75.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.26f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .background(cardBrush)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "连接中",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "当前车辆连接状态",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Button(
                                onClick = onDisconnectClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(
                                        alpha = 0.92f
                                    ),
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 4.dp
                                ),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("断开连接", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        connectedDevice?.let { dev ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dev.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val statusBadgeText = when {
                                        detectedModel?.startsWith("模拟", ignoreCase = true) == true -> detectedModel
                                        connectionState == BLEConnectionState.READY -> "认证 NIU Link"
                                        else -> detectedModel
                                    }
                                    statusBadgeText?.let { badgeText ->
                                        Row(
                                            modifier = Modifier
                                                .background(
                                                    if (badgeText.startsWith("模拟")) {
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    } else {
                                                        NiuRed.copy(alpha = 0.15f)
                                                    },
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                badgeText,
                                                color = if (badgeText.startsWith("模拟")) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    NiuRed
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "MAC: ${dev.address}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (connectionState) {
                                BLEConnectionState.CONNECTING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        "正在握手蓝牙链路 (Connecting)...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                BLEConnectionState.CONNECTED -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        "蓝牙连接成功，正在校验服务特征...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                BLEConnectionState.SERVICES_DISCOVERING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        "发现底层限速服务特征组...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                BLEConnectionState.READY -> {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Ready",
                                        tint = SafeGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "安全特征通道建立完毕。车辆已就绪",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SafeGreen
                                    )
                                }

                                BLEConnectionState.FAILED -> {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        connectionError ?: "蓝牙握手失败，特征校验不成功",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                else -> {}
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = connectionState == BLEConnectionState.READY,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        border = BorderStroke(
                            0.75.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "限速写入",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "指令写入控制台",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "发射写入模式",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isWriteNoResponse) "免握手快速写 (WRITE_NO_RESPONSE)" else "请求链路应答写入 (WRITE)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Switch(
                                    checked = isWriteNoResponse,
                                    onCheckedChange = { viewModel.setWriteNoResponse(it) },
                                    modifier = Modifier.scale(0.85f)
                                )
                            }

                            val isWriting = writeResult == WriteResult.Writing
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("预设", "自定义").forEachIndexed { index, label ->
                                    val selected = selectedCommandTab == index
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clickable { selectedCommandTab = index },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.62f)
                                                .height(3.dp)
                                                .background(
                                                    if (selected) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                    RoundedCornerShape(3.dp)
                                                )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (selectedCommandTab == 0) {
                                mainCommandButtons.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { item ->
                                            Button(
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    lastWriteOperation = item.label
                                                    viewModel.writeFixedCommand(
                                                        item.label,
                                                        item.command
                                                    )
                                                },
                                                enabled = !isWriting &&
                                                        !item.disabled &&
                                                        item.command.isNotBlank(),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(42.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f),
                                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    item.label,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            } else {
                                OutlinedTextField(
                                    value = hexCommand,
                                    onValueChange = { viewModel.setHexCommand(it) },
                                    placeholder = {
                                        Text(
                                            "输入自定义Hex代码",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Ascii,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("hex_command_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val cleanHex = hexCommand.replace(" ", "").replace(":", "")
                                val canWrite = cleanHex.isNotBlank() && cleanHex.length % 2 == 0
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        lastWriteOperation = "自定义"
                                        viewModel.writeCustomCommand()
                                    },
                                    enabled = !isWriting && canWrite,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .testTag("restore_speed_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NiuRed,
                                        contentColor = Color.White,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f),
                                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isWriting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("指令安全发射中...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(
                                            Icons.Default.Done,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "一键写入",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }

                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(
                        0.75.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
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
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "历史记录",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "操作日志",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            if (operationLogs.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearLogHistory() },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            "清空历史",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (operationLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无系统写入记录",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                operationLogs.take(visibleLogCount).forEachIndexed { index, log ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (index % 2 == 0) {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
                                                },
                                                RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                0.75.dp,
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .background(
                                                            if (log.isSuccess) SafeGreen else MaterialTheme.colorScheme.error,
                                                            CircleShape
                                                        )
                                                )
                                                Text(
                                                    text = log.operationType,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                            }
                                            Text(
                                                text = log.formattedTime,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.4f
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = log.statusMessage,
                                            fontSize = 10.sp,
                                            color = if (log.isSuccess) SafeGreen else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )

                                        Spacer(modifier = Modifier.height(5.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = log.deviceName,
                                                modifier = Modifier.weight(1f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.72f
                                                ),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = log.macAddress,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.46f
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(5.dp))

                                        Text(
                                            text = log.commandHex,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .combinedClickable(
                                                    onClick = {},
                                                    onLongClick = {
                                                        clipboardManager.setText(
                                                            AnnotatedString(log.commandHex)
                                                        )
                                                        Toast.makeText(
                                                            context,
                                                            "指令已复制",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.52f
                                            ),
                                            fontFamily = FontFamily.Monospace
                                        )
                    }
                }
            }
        }
                                if (operationLogs.size > visibleLogCount) {
                                    TextButton(
                                        onClick = { visibleLogCount += 10 },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "加载更多",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
            }
                }
            }
        }

        // 操作中：底部非模态加载提示
        if (writeResult == WriteResult.Writing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        0.75.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NiuRed,
                            strokeWidth = 2.dp
                        )
                        Text(
                            "指令发射中，请靠近车辆...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
    }

    // 结果提醒弹窗
    if (showResultDialog) {
        val res = writeResult
        AlertDialog(
            onDismissRequest = { dismissWriteResultDialog() },
            icon = {
                Icon(
                    imageVector = if (res is WriteResult.Success) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier
                        .background(
                            if (res is WriteResult.Success) {
                                SafeGreen.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            },
                            CircleShape
                        )
                        .padding(8.dp)
                        .size(28.dp),
                    tint = if (res is WriteResult.Success) SafeGreen else MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(if (res is WriteResult.Success) "操作成功" else "写入失败")
            },
            text = {
                Column {
                    Text(
                        if (res is WriteResult.Success) "已成功完成【$lastWriteOperation】指令写入。"
                        else "指令执行未成功。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (res is WriteResult.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "详情: ${res.errorMsg}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "若未生效，请重启电动车电源验证。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { dismissWriteResultDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (res is WriteResult.Success) NiuRed else MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("知道了", fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

