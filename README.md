# 小牛限速助手

小牛限速助手是一款面向小牛电动车的 Android 蓝牙工具，用于扫描附近的 `NIU Link` BLE 设备、建立 GATT 连接，并向指定服务特征写入合规限速恢复指令。

本项目当前仅保留真机蓝牙流程，不包含模拟设备或模拟写入模式。

## 主要功能

- 扫描附近 BLE 设备，并优先展示名称包含 `NIU` / `NIU Link` 的设备
- 连接真实蓝牙设备并展示连接状态
- 校验目标 GATT Service / Characteristic
- 支持 `WRITE` 和 `WRITE_NO_RESPONSE` 两种写入方式
- 支持自定义 Hex 指令和预设指令快速填充
- 记录车辆名称、MAC 地址、写入指令、结果和时间
- 本地保存操作日志，支持清空历史记录
- 内置合规声明，限定用于合法合规限速还原场景

## 技术栈

- Kotlin
- Jetpack Compose
- Android BLE / GATT
- Room
- Gradle Kotlin DSL

## 权限说明

应用需要蓝牙扫描与连接权限：

- Android 12 及以上：`BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`
- Android 11 及以下：`ACCESS_FINE_LOCATION`、`ACCESS_COARSE_LOCATION`

项目声明依赖 BLE 硬件：

```xml
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
```

## 本地运行

1. 使用 Android Studio 打开项目根目录。
2. 等待 Gradle Sync 完成。
3. 确认根目录存在 `debug.keystore`。
4. 连接支持 BLE 的 Android 真机。
5. 运行 `app` 的 debug 构建。

也可以在命令行验证 debug 构建：

```powershell
.\gradlew.bat assembleDebug
```

## Debug 签名

debug 构建使用项目内的自定义签名配置：

```kotlin
debug {
  signingConfig = signingConfigs.getByName("debugConfig")
}
```

对应 keystore 路径为：

```text
debug.keystore
```

如果本地没有该文件，可以从仓库中的 `debug.keystore.base64` 恢复：

```powershell
$base64 = Get-Content -Raw -Path debug.keystore.base64
[IO.File]::WriteAllBytes((Join-Path (Get-Location) 'debug.keystore'), [Convert]::FromBase64String($base64.Trim()))
```

`debug.keystore` 已被 `.gitignore` 忽略，不应提交到仓库。

## Release 签名

release 构建默认读取：

```text
my-upload-key.jks
```

也可以通过环境变量指定：

```text
KEYSTORE_PATH
STORE_PASSWORD
KEY_PASSWORD
```

当前如果缺少 release keystore，`build` 任务会在 `packageRelease` 阶段失败；只验证调试包时使用：

```powershell
.\gradlew.bat assembleDebug
```

## 合规声明

本工具仅限执法检验、出厂配置检验、车主合法合规限速还原等场景使用。不得用于非法提速、规避监管或其他违反当地法律法规的用途。
