# 小牛限速助手

小牛限速助手是一款面向小牛电动车的 Android 蓝牙工具，用于扫描附近的 `NIU Link` BLE 设备、建立 GATT 连接，并向指定服务特征写入合规限速恢复指令。

## 支持型号

目前本工具已完成以下型号的蓝牙特征匹配与自动化识别：
- **MT Sport**
- **MT City**

> 注：其他型号由于蓝牙 Service/Characteristic UUID 可能不同，目前暂不支持。

## 主要功能

- 扫描附近 BLE 设备，并优先展示名称包含 `NIU` / `NIU Link` 的设备
- 连接真实蓝牙设备并展示连接状态
- 手动连接后记住上次设备，支持扫描命中后自动连接
- 校验目标 GATT Service / Characteristic
- 支持 `WRITE` 和 `WRITE_NO_RESPONSE` 两种写入方式
- 支持自定义 Hex 指令和预设指令快速填充
- 记录车辆名称、MAC 地址、写入指令、结果和时间
- 内置合规声明，限定用于合法合规限速还原场景

## 技术栈

- Kotlin (2.2.10)
- Jetpack Compose
- Android BLE / GATT
- Room (数据库存储日志)
- Retrofit / OkHttp (网络请求)
- Gradle Kotlin DSL / Version Catalog

## 快速上手

### 1. 环境准备
- 使用 Android Studio (建议 Jellyfish 或更新版本) 打开项目。
- 等待 Gradle Sync 完成。
- 准备一台支持 BLE 的 Android 真机 (Android 6.0+)。

### 2. 签名配置
项目使用自定义签名，请根据需要配置：

#### Debug 签名
Debug 构建需要根目录存在 `debug.keystore`。如果本地缺失，可从 `debug.keystore.base64` 恢复：
```powershell
$base64 = Get-Content -Raw -Path debug.keystore.base64
[IO.File]::WriteAllBytes((Join-Path (Get-Location) 'debug.keystore'), [Convert]::FromBase64String($base64.Trim()))
```

#### Release 签名
默认读取根目录的 `release.jks`。也可通过环境变量配置：

- `KEYSTORE_PATH`: keystore 文件路径；不配置时默认使用项目根目录的 `release.jks`
- `STORE_PASSWORD`: keystore 密码
- `KEY_PASSWORD`: release 签名 key 密码
- `KEY_ALIAS`: release 签名 key alias

PowerShell 示例：

```powershell
$env:KEYSTORE_PATH="D:\path\to\release.jks"
$env:STORE_PASSWORD="PASSWORD"
$env:KEY_PASSWORD="PASSWORD"
$env:KEY_ALIAS="RELEASE"
.\gradlew.bat assembleRelease
```

### 3. 编译与运行
你可以直接通过 Android Studio 的 **Run** 按钮运行，也可以使用自动化脚本或命令行：

- **自动化一键构建 (推荐)**: 运行 `.\package.ps1`，成功后 APK 会存放在 `build/release/` 目录下。
- **Debug 编译**: `.\gradlew.bat assembleDebug`
- **Release 编译**: `.\gradlew.bat assembleRelease`

## 权限说明

应用需要蓝牙扫描与连接权限，已在清单文件中声明：

- **Android 12+**: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`
- **Android 11 及以下**: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- **硬件要求**: 必须具备蓝牙低功耗硬件支持。

```xml
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
```

## 合规声明

本工具仅限执法检验、出厂配置检验、车主合法合规限速还原等场景使用。不得用于非法提速、规避监管或其他违反当地法律法规的用途。
