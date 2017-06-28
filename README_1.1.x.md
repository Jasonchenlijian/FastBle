# FastBle
Android Bluetooth Low Energy 蓝牙快速开发框架。
使用回调方式处理：搜索、连接、读写、通知等一系列蓝牙操作。每一个characteristic会与一个callback形成一一对应的监听关系。

***

## Update Log
- v1.1.1（2017-05-04）
    - 优化连接异常中断后的扫描及重连机制；优化测试工具。
- v1.1.0（2017-04-30）
    - 扫描设备相关部分api稍作优化及改动，完善Demo测试工具。
- v1.0.6（2017-03-21）
	- 加入对设备名模糊搜索的功能。
- v1.0.5（2017-03-02）
	- 优化notify、indicate监听机制。
- v1.0.4（2016-12-08）
	- 增加直连指定mac地址设备的方法。
- v1.0.3（2016-11-16）
	- 优化关闭机制，在关闭连接前先移除回调。
- v1.0.2（2016-09-23）
	- 添加stopNotify和stopIndicate的方法，与stopListenCharacterCallback方法作区分。
- v1.0.1（2016-09-20）
    - 优化callback机制，一个character有且只会存在一个callback，并可以手动移除。
    - 示例代码中添加DemoActivity和OperateActivity。前者示范如何使用本框架，后者可以作为蓝牙调试工具，测试蓝牙设备。
- v1.0.0（2016-09-08) 
	- 增加设备是否支持ble的判断。
	- 修正监听不同character的时候，当其中一个character发生变化,与该character无关的callback也会回调结果的bug。

## Preview
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble0.gif) 
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble1.png) 
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble2.png) 
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble3.png)
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble4.png)
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble5.png)

## Gradle
	dependencies {
    	compile 'com.clj.fastble:FastBleLib:1.1.1'
	}

## Maven
	<dependency>
    	<groupId>com.clj.fastble</groupId>
    	<artifactId>FastBleLib</artifactId>
    	<version>1.1.1</version>
		<type>pom</type>
	</dependency>

## Demo
   如果想快速预览所有功能，可以直接下载apk作为测试工具使用：[FastBLE.apk](https://github.com/Jasonchenlijian/FastBle/raw/master/FastBLE.apk)

## Usage

- #### 初始化
        bleManager = new BleManager(this);

- #### 开启或关闭蓝牙
		bleManager.enableBluetooth();
		bleManager.disableBluetooth();

- #### 扫描所有设备
	可获得周围蓝牙设备对象数组

	`boolean` `scanDevice(ListScanCallback callback)`如果返回false，表示扫描失败，可能是蓝牙未打开等原因造成。根据手机的不同型号及系统版本，蓝牙权限的相关申请工作请在调用此方法前自行配置。

	`ListScanCallback(long timeoutMillis)`，传参扫描的时间；`onScanning`表示当前正在扫描状态，且搜索到一个外围设备的回调；`onScanComplete`表示扫描时间到或手动取消扫描后的回调。

	`ScanResult`表示返回的扫描结果对象。
	`BluetoothDevice` `getDevice()`: 蓝牙设备对象;
	`byte[]` `getScanRecord()`: 广播数据;
	`int` `getRssi()`: 信号强度.


        bleManager.scanDevice(new ListScanCallback(TIME_OUT) {
            @Override
            public void onScanning(ScanResult result) {
            }

            @Override
            public void onScanComplete(ScanResult[] results) {     
            }
        });

- #### 连接设备
	当搜索到周围设备之后，可以选择选择某一个设备和其连接

	`void` `connectDevice(ScanResult scanResult,boolean autoConnect,BleGattCallback callback)`

	`BleGattCallback`:`onNotFoundDevice`表示没有找到设备的回调；`onFoundDevice`表示找到设备的回调；`onConnectSuccess`表示连接成功的回调，如果需要进一步去发现服务，务必调用`gatt.discoverServices()`方法；`onServicesDiscovered`表示发现服务的回调；`onConnectFailure`表示连接断开的回调，设备连接后的操作过程中，一旦发生异常中断或主动断开，该回调会发生。


        bleManager.connectDevice(scanResult, true, new BleGattCallback() {
            @Override
            public void onNotFoundDevice() { 
            }

            @Override
            public void onFoundDevice(ScanResult scanResult) {
            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            }

            @Override
            public void onConnectFailure(BleException exception) {
            }
        });
            

- #### 扫描指定广播名的设备、并连接
	扫描周围指定广播名称的设备，搜索到第一个即连接

	`boolean` `scanNameAndConnect(String deviceName,
                                      long time_out,
                                      boolean autoConnect,
                                      BleGattCallback callback)`

        bleManager.scanNameAndConnect(
                DEVICE_NAME,
                TIME_OUT,
                false,
                new BleGattCallback() {
                    @Override
                    public void onNotFoundDevice() {
                    }

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {
                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        gatt.discoverServices();
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                    }
                });

- #### 扫描指定广播名的设备，并连接（模糊广播名）
	扫描周围指定广播名称的设备，搜索到第一个即连接

	`boolean` `scanfuzzyNameAndConnect(String deviceName,
                                      long time_out,
                                      boolean autoConnect,
                                      BleGattCallback callback)`

        bleManager.scanfuzzyNameAndConnect(
                DEVICE_NAME,
                TIME_OUT,
                false,
                new BleGattCallback() {
                    @Override
                    public void onNotFoundDevice() {
                    }

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {
                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        gatt.discoverServices();
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                    }
                });

- #### 扫描指定MAC地址的设备、并连接
	扫描周围指定MAC地址的设备，搜索到第一个即连接

	`boolean` `scanMacAndConnect(String deviceMac,
                                     long time_out,
                                     boolean autoConnect,
                                     BleGattCallback callback)`

        bleManager.scanMacAndConnect(
                DEVICE_MAC,
                TIME_OUT,
                false,
                new BleGattCallback() {
                    @Override
                    public void onNotFoundDevice() {
                    }

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {
                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        gatt.discoverServices();
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                    }
                });

- #### 停止扫描
	取消扫描操作

		bleManager.cancelScan();


- #### notify，listen data changes through callback
	参数中的callback和uuid将会形成关联，一旦设备的此uuid对应的character发生数据变化，此callback将会回调结果。此callbak将会唯一存在，和uuid是一一对应的关系。

	`boolean` `notify(String uuid_service,
                          String uuid_notify,
                          BleCharacterCallback callback)`

	`BleCharacterCallback`: `onSuccess`表示该Characteristic上发生数据变化的回调；onFailure表示发生异常的回调。

        bleManager.notify(
                UUID_SERVICE,
                UUID_NOTIFY,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                    }

                    @Override
                    public void onFailure(BleException exception) {
                    }
                });

- #### stop notify，remove callback
		bleManager.stopNotify(UUID_SERVICE, UUID_NOTIFY);

- #### indicate，listen data changes through callback

	`boolean` `indicate(String uuid_service,
                            String uuid_indicate,
                            BleCharacterCallback callback)`

        bleManager.indicate(
                UUID_SERVICE,
                UUID_INDICATE,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                    }

                    @Override
                    public void onFailure(BleException exception) {
                    }
                });

- #### stop indicate，remove callback
		bleManager.stopIndicate(UUID_SERVICE, UUID_INDICATE);

- #### write

	`boolean` `writeDevice(String uuid_service,
                               String uuid_write,
                               byte[] data,
                               BleCharacterCallback callback)`

        bleManager.writeDevice(
                UUID_SERVICE,
                UUID_WRITE,
                HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                    }

                    @Override
                    public void onFailure(BleException exception) {
                    }
                });
- #### read

	`boolean` `readDevice(String uuid_service,
                              String uuid_read,
                              BleCharacterCallback callback)`

        bleManager.readDevice(
                UUID_SERVICE,
                UUID_READ,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                    }

                    @Override
                    public void onFailure(BleException exception) {
                    }
                });

- #### manual remove callback 
    不再监听这个特征的数据变化，适用于移除notify、indicate、write、read对应的callback。

        bleManager.stopListenCharacterCallback(uuid_sample);


- #### 复位（断开此次蓝牙连接，移除所有回调）
        bleManager.closeBluetoothGatt();



## License

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




