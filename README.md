<div align="center">

![FastBle](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/fastble_poster.png)

# FastBle

**Android Bluetooth Low Energy fast development framework**

[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Version](https://img.shields.io/badge/Version-2.5.0-orange.svg)](https://github.com/Jasonchenlijian/FastBle)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)

Filtering, scanning, connecting, reading, writing, notification — all in a simple way.

[Wiki](https://github.com/Jasonchenlijian/FastBle/wiki) · [Download APK](https://github.com/Jasonchenlijian/FastBle/raw/master/FastBLE.apk) · [Source Analysis](https://www.jianshu.com/p/795bb0a08beb)

</div>

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Preview](#preview)
- [Getting Started](#getting-started)
- [Usage](#usage)
  - [Init](#init)
  - [Scan](#scan)
  - [Connect](#connect)
  - [Notify / Indicate](#notify--indicate)
  - [Read / Write](#read--write)
  - [RSSI / MTU](#rssi--mtu)
  - [Other APIs](#other-apis)
- [Changelog](#changelog)
- [Star History](#star-history)
- [Contact](#contact)
- [License](#license)

---

## Features

- Scan with custom filter rules (name / MAC / UUID)
- Multi-device simultaneous connections
- Read, write, notify, indicate operations
- Signal strength (RSSI) reading & MTU configuration
- Auto reconnection with configurable retry count and interval
- Configurable timeouts for connect & operations
- Modern `BluetoothLeScanner` + `ScanCallback` API internally
- Supports Android 5.0 ~ 15 (API 21 ~ 35)

## Requirements

| Item | Minimum |
|:-----|:--------|
| Android SDK | API 21 (Android 5.0) |
| Java | 17 |
| Android Studio | Ladybug (2024.2) or later |

## Preview

<p align="center">
<img src="https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_1.png" width="24%" />
<img src="https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_2.png" width="24%" />
<img src="https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_3.png" width="24%" />
<img src="https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_4.png" width="24%" />
</p>


## Getting Started

### Gradle

**Step 1.** Add JitPack to your root `build.gradle` (or `settings.gradle`):

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2.** Add the dependency:

```gradle
dependencies {
    implementation 'com.github.Jasonchenlijian:FastBle:2.5.0'
}
```

### Permissions

FastBle declares all required permissions in its own manifest. **You do NOT need to add them again.**

However, you **must** request runtime permissions before scanning:

```java
private void checkPermissions() {
    List<String> denied = new ArrayList<>();

    // Android 12+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PERMISSION_GRANTED)
            denied.add(Manifest.permission.BLUETOOTH_SCAN);
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PERMISSION_GRANTED)
            denied.add(Manifest.permission.BLUETOOTH_CONNECT);
    }

    // All versions
    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED)
        denied.add(Manifest.permission.ACCESS_FINE_LOCATION);

    if (!denied.isEmpty()) {
        requestPermissions(denied.toArray(new String[0]), REQUEST_CODE);
    } else {
        startScan();
    }
}
```

> **Note:** On Android 6.0 ~ 11, GPS must be enabled for BLE scanning. On Android 12+, this is no longer required.


## Usage

### Init

```java
BleManager.getInstance().init(getApplication());
BleManager.getInstance()
        .enableLog(true)
        .setReConnectCount(1, 5000)
        .setSplitWriteNum(20)
        .setConnectOverTime(10000)
        .setOperateTimeout(5000);
```

### Scan

```java
// Configure scan rules
BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
        .setServiceUuids(serviceUuids)
        .setDeviceName(true, names)
        .setDeviceMac(mac)
        .setAutoConnect(isAutoConnect)
        .setScanTimeOut(10000)
        .build();
BleManager.getInstance().initScanRule(scanRuleConfig);

// Start scanning
BleManager.getInstance().scan(new BleScanCallback() {
    @Override
    public void onScanStarted(boolean success) { }

    @Override
    public void onScanning(BleDevice bleDevice) { }

    @Override
    public void onScanFinished(List<BleDevice> scanResultList) { }
});
```

### Connect

```java
// Connect by BleDevice
BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
    @Override
    public void onStartConnect() { }

    @Override
    public void onConnectFail(BleDevice bleDevice, BleException exception) { }

    @Override
    public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) { }

    @Override
    public void onDisConnected(boolean isActiveDisConnected, BleDevice device,
                               BluetoothGatt gatt, int status) { }
});

// Or connect directly by MAC address
BleManager.getInstance().connect(mac, bleGattCallback);

// Or scan and auto-connect the first matched device
BleManager.getInstance().scanAndConnect(bleScanAndConnectCallback);
```

### Notify / Indicate

```java
// Subscribe to notifications
BleManager.getInstance().notify(bleDevice, uuid_service, uuid_notify,
        new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() { }

            @Override
            public void onNotifyFailure(BleException exception) { }

            @Override
            public void onCharacteristicChanged(byte[] data) { }
        });

// Stop notifications
BleManager.getInstance().stopNotify(bleDevice, uuid_service, uuid_notify);

// Indicate works the same way
BleManager.getInstance().indicate(bleDevice, uuid_service, uuid_indicate, bleIndicateCallback);
BleManager.getInstance().stopIndicate(bleDevice, uuid_service, uuid_indicate);
```

### Read / Write

```java
// Write data
BleManager.getInstance().write(bleDevice, uuid_service, uuid_write, data,
        new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int current, int total, byte[] justWrite) { }

            @Override
            public void onWriteFailure(BleException exception) { }
        });

// Read data
BleManager.getInstance().read(bleDevice, uuid_service, uuid_read,
        new BleReadCallback() {
            @Override
            public void onReadSuccess(byte[] data) { }

            @Override
            public void onReadFailure(BleException exception) { }
        });
```

> **Tip:** Data longer than 20 bytes is automatically split into packets. Use `write(bleDevice, uuid_service, uuid_write, data, split, callback)` to control this behavior.

### RSSI / MTU

```java
// Read signal strength (device must be connected)
BleManager.getInstance().readRssi(bleDevice, new BleRssiCallback() {
    @Override
    public void onRssiSuccess(int rssi) { }

    @Override
    public void onRssiFailure(BleException exception) { }
});

// Set MTU (range: 23 ~ 512, requires device support)
BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
    @Override
    public void onMtuChanged(int mtu) { }

    @Override
    public void onSetMTUFailure(BleException exception) { }
});
```

### Other APIs

| Method | Description |
|:-------|:------------|
| `isSupportBle()` | Check if device supports BLE |
| `enableBluetooth()` | Enable Bluetooth (deprecated on Android 13+) |
| `getAllConnectedDevice()` | Get all connected devices |
| `isConnected(bleDevice)` | Check connection state |
| `getConnectState(bleDevice)` | Get detailed connection state |
| `getBluetoothGatt(bleDevice)` | Get BluetoothGatt object |
| `getBluetoothGattServices(bleDevice)` | Get all services |
| `getBluetoothGattCharacteristics(service)` | Get all characteristics |
| `disconnect(bleDevice)` | Disconnect a device |
| `disconnectAllDevice()` | Disconnect all devices |
| `destroy()` | Release all resources |
| `convertBleDevice(bluetoothDevice)` | Convert to BleDevice object |
| `requestConnectionPriority(bleDevice, priority)` | Request connection priority |


## Changelog

### v2.5.0
- **Build:** Gradle 8.13, AGP 8.5.0, compileSdk/targetSdk 35, minSdk 21, Java 17
- **Android 12+:** Add `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` runtime permissions
- **Scan API:** Replace deprecated `startLeScan()` with `BluetoothLeScanner` + `ScanCallback`
- **Thread safety:** Use `ConcurrentHashMap` for GATT callback maps
- **Bug fixes:** Null device name crash, `SplitWriter` cleanup order, `Parcel.readParcelable` deprecation
- **Cleanup:** Remove unnecessary API version checks (minSdk is now 21)


## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=Jasonchenlijian/FastBle&type=Date)](https://star-history.com/#Jasonchenlijian/FastBle&Date)


## Contact

| Channel | |
|:--------|:-|
| WeChat | chenlijian1216 |
| Email | jasonchenlijian@gmail.com |


## License

```
Copyright 2016 chenlijian

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
