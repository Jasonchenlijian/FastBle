# FastBle
Android Bluetooth Low Energy 蓝牙快速开发框架。

使用简单的方式进行搜索、连接、读写、通知的订阅与取消等一系列蓝牙操作，并实时地得到操作反馈。





# Preview
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble0.gif) 
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble1.png) 
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble2.png) 
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble3.png)
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble4.png)
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble5.png)

	

# Download
	<dependency>
       <groupId>com.clj.fastble</groupId>
       <artifactId>FastBleLib</artifactId>
       <version>1.2.0</version>
	   <type>pom</type>
	</dependency>

or Gradle:

	compile 'com.clj.fastble:FastBleLib:1.2.0'

FastBle requires at minimum Java 7 or Android 4.0.


# ProGuard
If you are using ProGuard you might need to add the following options:

	-dontwarn com.clj.fastble.**
	-keep public class com.clj.fastble.**{*;}


# 测试工具
   如果想快速预览所有功能，可以直接下载apk作为测试工具使用：[FastBLE.apk](https://github.com/Jasonchenlijian/FastBle/raw/master/FastBLE.apk)

### [查看1.1.x旧版版本API说明请点击此处](https://github.com/Jasonchenlijian/FastBle/blob/master/README_1.1.x.md)



***


# 蓝牙操作经验及FastBle的兼容性说明

- BLE是蓝牙4.0里面的低功耗规范，Android 4.3以上的系统开始搭载BLE模块，所以FastBle也只支持4.3以上。
- 不排除某些特殊设备的定制系统去除了BLE模块的情况，使用之前可以先判断当前设备是否支持BLE，再进行后续操作。
- 蓝牙设备相关程序必须使用真机才能运行。
- FastBle当前版本仅支持对BLE蓝牙进行操作，不支持经典蓝牙。
- 使用蓝牙功能，必须先声明蓝牙相关的权限。Android 6.0以上的系统，需要额外申请位置相关的权限，并且是危险权限建议在运行时动态获取。为使使用更灵活，FastBle库中并不包含权限相关的操作，使用者根据程序的实际情况在外层自行嵌套。示例代码中有相关代码演示，供参考。
- 蓝牙操作与硬件关联很大，开发过程中要保持和硬件协议的沟通，某些问题的解决需要硬件方面做一些适配。
- BLE的MTU（最大传输单元）是20字节，即一次最多能发送20个字节，若超过20个字节，建议采用分包传输的方式。
- 蓝牙连接之后，列出当前外设模块的所有service，每个service可能有一个或多个的characteristic，每一个characteristic有其对应的property（即可操作的属性类别）,假如一个characteristic的property对应的是write，那么对这个characteristic做notify处理显然是行不通的。
- 两次操作之间最好间隔一小段时间，如100ms（具体时间可以根据自己实际蓝牙外设自行尝试延长或缩短）。举例，连接成功之后，延迟100ms进行notify，成功之后延迟100ms进行write，write成功之后，notify的数据回调接口将返回外设传输过来的数据。
- FastBle中开放的蓝牙操作的相关方法均要求在主线程中执行。
- 连接及连接后的过程中，时刻关注BleGattCallback，蓝牙的连接情况会实时反映在其各个回调方法中，尤其是onDisConnected方法。
- 连接过程中，假如外设突然中断（或关闭）了蓝牙，Android设备维持的BLE连接并不会马上回调onDisConnected方法，而是会延迟一段时间才会通知连接断开，开发时需注意，假如对实时性要求较高的程序，可能需要借助其他辅助方法来判断设备是否中断，比如心跳包等。
- 蓝牙应用开发中，存在两种角色，分别是central和peripheral ,中文就是中心和外设。比如手机去连接智能设备，那手机就是central，智能设备就是peripheral。
- FastBle当前版本仅支持中心模式 （central model），即"以App作为中心，连接其他BLE外设"。把手机作为外设目前版本是行不通的。
- 连接之后的操作有：write，read，notify，indicate，response or not等。indicate和notify的区别就在于，indicate是一定会收到数据，notify有可能会丢失数据（不会有central收到数据的回应），write也分为response和no response，如果是response，那么write成功回收到peripheral的确认消息，但是会降低写入的速率，换一个角度说就是 write no response写的速率更快。
- 连接断开之后的重连很简单，在`void onDisConnected(BluetoothGatt gatt, int status, BleException exception)`调用`boolean gatt.connect()`方法即可，当外设再次处于可连接状态时，就会自动连上。
- 连接断开之后可以根据实际情况进行重连，但如果是连接失败的情况，建议不要立即重连，而是调用`void closeBluetoothGatt()`清空一下状态，并延迟一段时间等待复位，否则会把gatt阻塞，导致手机不重启蓝牙就再也无法连接任何设备的严重情况。
- 调用`bleManager.closeBluetoothGatt()`之后，最好不要紧接着调用`bleManager = null`，因为Android原生蓝牙API中的`gatt.close()`方法需要一段时间保证完成，我们建议延迟一段时间。延时操作在Android蓝牙开发中是一个重要的技巧。
- 很多Android设备是可以强制打开用户手机蓝牙的，打开蓝牙需要一段时间（部分手机上需要向用户请求）。虽然时间比较短，但也不能调用完打开蓝牙方法后直接去调用扫描方法，此时蓝牙多半是还未开启完毕状态。建议的做法是维持一个蓝牙状态的广播，调用打开蓝牙方法后，在一段时间内阻塞线程，如果在这段时间内收到蓝牙打开广播后，再进行后续操作。而后续操作过程中，如果收到蓝牙正在关闭或关闭的广播，也可以及时对当前的情况做一个妥善处理。



# 如何使用

- #### 初始化，创建操作对象
	后续的所有操作方法，均通过当前所创建的BleManager对象来完成

    `public BleManager(Context context)`
    
        bleManager = new BleManager(this);

- #### 判断当前手机是否支持BLE

        boolean isSupportBle()

- #### 开启或关闭蓝牙

		void enableBluetooth()
		void disableBluetooth()
		
- #### 查看当前蓝牙状态或连接状态
		
		boolean isBlueEnable()
		boolean isInScanning()
		boolean isConnectingOrConnected()
		boolean isConnected()
		boolean isServiceDiscovered()

- #### 打印异常信息

	`void handleException(BleException exception)`
	
		bleManager.handleException(exception);
		
- #### ListScanCallback(long timeoutMillis)

    普通扫描的Callback

		long timeoutMillis: 本次扫描的时间	
        void onScanning(ScanResult result): 当前正在扫描状态，且搜索到一个外围设备的回调
        void onScanComplete(ScanResult[] results): 扫描时间到或手动取消扫描后的回调
        
        ScanResult: 表示返回的扫描结果对象
        BluetoothDevice getDevice(): 蓝牙设备对象
        byte[] getScanRecord(): 广播数据;
        int getRssi(): 信号强度

- #### BleGattCallback

    扫描并连接的Callback

		void onFoundDevice(ScanResult scanResult): 找到设备的回调；
		void onConnecting(BluetoothGatt gatt, int status): 正在连接的回调；
		void onConnectError(BleException exception)： 连接未成功的回调，通过解析BleException来判断具体未成功的原因；
		void onConnectSuccess(BluetoothGatt gatt, int status): 连接成功的回调；
		void onServicesDiscovered(BluetoothGatt gatt, int status): 发现服务的回调；
		void onDisConnected(BluetoothGatt gatt, int status, BleException exception): 连接断开的回调，特指连接之后的断开。

- #### BleCharacterCallback

    Characteristic操作的Callback

		void onSuccess(BluetoothGattCharacteristic characteristic): 数据传输回调；
		void onFailure(BleException exception)： 操作或数据传输过程中出错；
		void onInitiatedResult(boolean result): 操作成功与否的回调；
		
- #### BleRssiCallback

    读Rssi操作的Callback

		void onSuccess(int rssi): 得到rssi数据的回调；
		void onFailure(BleException exception)： 操作或数据传输过程中出错；
		void onInitiatedResult(boolean result): 操作成功与否的回调；

- #### BleException

		int getCode(): 获取异常码；
		String getDescription()： 获取异常描述；
		
	异常码：
	- 100： 超时
	- 101： 连接异常
	- 102： 其他（异常信息可以通过异常描述获取，一般是开发过程中的操作中间步骤的异常）
	- 103： 设备未找到
	- 104： 蓝牙未启用
	- 105： 开启扫描过程失败


- #### 扫描所有设备
	可获得周围蓝牙设备对象数组

	`boolean scanDevice(ListScanCallback callback)`

	返回值表示扫描操作是否成功，具体扫描结果会显示在ListScanCallback的回调方法中。

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

	`void connectDevice(ScanResult scanResult,
                              boolean autoConnect,
                              BleGattCallback callback)`


        bleManager.connectDevice(scanResult, true, new BleGattCallback() {

            @Override
            public void onConnecting(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onConnectError(BleException exception) {

            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {

            }

        });
            

- #### 扫描指定广播名的设备、并连接
	扫描周围指定广播名称的设备，搜索到第一个即连接

	`void scanNameAndConnect(String deviceName,
                                   long time_out,
                                   boolean autoConnect,
                                   BleGattCallback callback)`

        bleManager.scanNameAndConnect(
                DEVICE_NAME,
                TIME_OUT,
                false,
                new BleGattCallback() {

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {

                    }

                    @Override
                    public void onConnecting(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onConnectError(BleException exception) {

                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {

                    }

                });

- #### 扫描指定广播名的设备，并连接（模糊广播名）
	扫描周围指定广播名称的设备，搜索到第一个即连接

	`void scanfuzzyNameAndConnect(String fuzzyName,
                                        long time_out,
                                        boolean autoConnect,
                                        BleGattCallback callback)`

        bleManager.scanfuzzyNameAndConnect(
                DEVICE_NAME,
                TIME_OUT,
                false,
                new BleGattCallback() {

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {

                    }

                    @Override
                    public void onConnecting(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onConnectError(BleException exception) {

                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {

                    }

                });

- #### 扫描指定MAC地址的设备、并连接
	扫描周围指定MAC地址的设备，搜索到第一个即连接

	`void scanMacAndConnect(String deviceMac,
                                  long time_out,
                                  boolean autoConnect,
                                  BleGattCallback callback)`

        bleManager.scanMacAndConnect(
                DEVICE_MAC,
                TIME_OUT,
                false,
                new BleGattCallback() {

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {

                    }

                    @Override
                    public void onConnecting(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onConnectError(BleException exception) {

                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {

                    }

                });;

- #### 停止扫描
	取消扫描操作

	`void cancelScan()`

		bleManager.cancelScan();


- #### 订阅通知notify
	`boolean notify(String uuid_service,
                          String uuid_notify,
                          BleCharacterCallback callback)`
                         
    返回值表示notify操作是否成功，BleCharacterCallback()中的onInitiatedResult也表示本次操作是否成功，功能一致，便于不同情况下的用途。

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

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });

- #### 取消订阅通知notify，并移除回调监听

	`boolean stopNotify(String uuid_service, String uuid_notify)`
	
	返回值表示stopNotify操作是否成功，BleCharacterCallback()中的onInitiatedResult也表示本次操作是否成功，功能一致，便于不同情况下的用途。

		bleManager.stopNotify(UUID_SERVICE, UUID_NOTIFY);

- #### 订阅通知indicate

	`boolean indicate(String uuid_service,
                            String uuid_indicate,
                            BleCharacterCallback callback)`
    
    返回值表示indicate操作是否成功，BleCharacterCallback()中的onInitiatedResult也表示本次操作是否成功，功能一致，便于不同情况下的用途。

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

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });

- #### 取消订阅通知indicate，并移除回调监听

    `boolean stopIndicate(String uuid_service, String uuid_notify)`
    
    返回值表示stopIndicate操作是否成功，BleCharacterCallback()中的onInitiatedResult也表示本次操作是否成功，功能一致，便于不同情况下的用途。
    
		bleManager.stopIndicate(UUID_SERVICE, UUID_INDICATE);

- #### 写

	`boolean writeDevice(String uuid_service,
                               String uuid_write,
                               byte[] data,
                               BleCharacterCallback callback)`

	返回值表示write操作是否成功，BleCharacterCallback()中的onInitiatedResult也表示本次操作是否成功，功能一致，便于不同情况下的用途。

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

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });
- #### 读

	`boolean readDevice(String uuid_service,
                              String uuid_read,
                              BleCharacterCallback callback)`

	返回值表示read操作是否成功，BleCharacterCallback()中的onInitiatedResult也表示本次操作是否成功，功能一致，便于不同情况下的用途。

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

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });

- #### 读外设的Rssi

	`boolean readRssi(BleRssiCallback callback)`

	返回值表示readRssi操作是否成功，BleCharacterCallback()中的onInitiatedResult也表示本次操作是否成功，功能一致，便于不同情况下的用途。

		bleManager.readRssi(new BleRssiCallback() {
            @Override
            public void onSuccess(int rssi) {

            }

            @Override
            public void onFailure(BleException exception) {

            }

            @Override
            public void onInitiatedResult(boolean result) {

            }
        });

- #### manual remove callback 

	`void stopListenCharacterCallback(String uuid)`

    不再监听这个特征的数据变化，适用于移除notify、indicate、write、read对应的callback。

        bleManager.stopListenCharacterCallback(uuid_sample);


- #### 复位（断开此次蓝牙连接，移除所有回调）

	`void closeBluetoothGatt()`

        bleManager.closeBluetoothGatt();


## 版本更新日志
- v1.2.0（2017-06-28）
    - 对扫描及连接的回调处理的API做优化处理，对所有蓝牙操作的结果做明确的回调，完善文档说明。
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
	- 初版


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




