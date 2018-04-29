![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/fastble_poster.png)

Thanks to the logo designed by [anharismail](https://github.com/anharismail)


# FastBle
Android Bluetooth Low Energy 蓝牙快速开发框架。

> 使用简单的方式进行 过滤、扫描、连接、读、写、通知订阅与取消   
支持获取信号强度、设置最大传输单元  
支持自定义扫描规则  
支持多设备连接  
支持重连机制  
支持配置超时机制  




# Preview
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_1.png) 
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_2.png) 
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_3.png)
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_4.png)

	


### APK

 [FastBLE.apk](https://github.com/Jasonchenlijian/FastBle/raw/master/FastBLE.apk) 如果想快速预览所有功能，可以直接下载APK作为测试工具使用.


### Maven

	<dependency>
       <groupId>com.clj.fastble</groupId>
       <artifactId>FastBleLib</artifactId>
       <version>2.3.0</version>
	   <type>pom</type>
	</dependency>

### Gradle

	compile 'com.clj.fastble:FastBleLib:2.3.0'


## 其他说明

FastBle requires at minimum Java 7 or Android 4.0.

FastBle 所有代码均可以加入混淆。

## 经验分享

[Android BLE开发详解和FastBle源码解析](https://www.jianshu.com/p/795bb0a08beb)





# 如何使用

- #### （方法说明）初始化
    
        BleManager.getInstance().init(getApplication());

- #### （方法说明）判断当前Android系统是否支持BLE

        boolean isSupportBle()

- #### （方法说明）开启或关闭蓝牙

		void enableBluetooth()
		void disableBluetooth()

- #### （方法说明）初始化配置

		BleManager.getInstance()
                .enableLog(true)					// 设置是否打印日志，默认开启
                .setReConnectCount(1, 5000)			// 设置连接时重连次数和重连间隔（毫秒），默认为0次不重连
				.setSplitWriteNum(20)				// 设置分包发送的时候，每一包的数据长度，默认20
                .setOperateTimeout(5000);			// 设置操作readRssi、setMtu、write、read、notify、indicate的超时时间

- #### （方法说明）配置扫描规则

	`void initScanRule(BleScanRuleConfig scanRuleConfig)`

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
                .setDeviceName(true, names)         // 只扫描指定广播名的设备，可选
                .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
                .setAutoConnect(isAutoConnect)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒；小于等于0表示不限制扫描时间
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
	Tips：
	- 在扫描设备之前，可以配置扫描规则，筛选出与程序匹配的设备
	- 不配置的话均为默认参数
	- 在2.1.2版本及之前，必须先配置过滤规则再扫描；在2.1.3版本之后可以无需配置，开启默认过滤规则的扫描。


- #### （方法说明）扫描

	`void scan(BleScanCallback callback)`

        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
				// 开始扫描（主线程）
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
				// 扫描到一个符合扫描规则的BLE设备（主线程）
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
				// 扫描结束，列出所有扫描到的符合扫描规则的BLE设备（主线程）
            }
        });
	Tips:
	- 扫描及过滤过程是在工作线程中进行，所以不会影响主线程的UI操作，最终每一个回调结果都会回到主线程。

- #### （方法说明）连接
通过扫描到的BleDevice对象进行连接。

	`BluetoothGatt connect(BleDevice bleDevice, BleGattCallback bleGattCallback)`

        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
				// 开始连接
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
				// 连接失败
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
				// 连接成功，BleDevice即为所连接的BLE设备
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
				// 连接中断，isActiveDisConnected表示是否是主动调用了断开连接方法
            }
        });
	Tips:
	- 在某些型号手机上，connectGatt必须在主线程才能有效。非常建议把连接过程放在主线程。
	- 连接失败后重连：框架中包含连接失败后的重连机制，可以配置重连次数和时间间隔。当然也可以自行在`onConnectFail`回调方法中延时调用`connect`方法。
	- 连接断开后重连：可以在`onDisConnected`回调方法中再次调用`connect`方法。
	- 为保证重连成功率，建议间隔一段时间之后进行重连。
	- 某些机型上连接失败后会短暂地无法扫描到设备，可以通过设备对象或设备mac直连，而不经过扫描。

- #### （方法说明）连接
通过已知设备Mac直接

	`BluetoothGatt connect(String mac, BleGattCallback bleGattCallback)`

        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
				// 开始连接
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
				// 连接失败
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
				// 连接成功，BleDevice即为所连接的BLE设备
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
				// 连接中断，isActiveDisConnected表示是否是主动调用了断开连接方法
            }
        });
	Tips:
	- 此方法可以不经过扫描，尝试直接连接周围复合该Mac的BLE设备。
	- 在很多使用场景，我建议APP保存用户惯用设备的Mac，然后使用该方法进行连接可以大大提高连接效率。

- #### （方法说明）扫描并连接

	扫描到首个符合扫描规则的设备后，便停止扫描，然后连接该设备。

	`void scanAndConnect(BleScanAndConnectCallback callback)`

        BleManager.getInstance().scanAndConnect(new BleScanAndConnectCallback() {
            @Override
            public void onScanStarted(boolean success) {
				// 开始扫描（主线程）
            }

            @Override
            public void onScanFinished(BleDevice scanResult) {
				// 扫描结束，结果即为扫描到的第一个符合扫描规则的BLE设备，如果为空表示未搜索到（主线程）
            }

            @Override
            public void onStartConnect() {
				// 开始连接（主线程）
            }

            @Override
            public void onConnectFail(BleDevice bleDevice,BleException exception) {
				// 连接失败（主线程）
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
				// 连接成功，BleDevice即为所连接的BLE设备（主线程）
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
				// 连接断开，isActiveDisConnected是主动断开还是被动断开（主线程）
            }
        });
	Tips:
	- 扫描及过滤过程是在工作线程中进行，所以不会影响主线程的UI操作，但每一个回调结果都会回到主线程。 连接操作会在主线中进行。  


- #### （方法说明）中止扫描
	扫描过程中，中止扫描操作

	`void cancelScan()`

		BleManager.getInstance().cancelScan();

	Tips:
	- 调用该方法后，如果当前还处在扫描状态，会立即结束，并回调`onScanFinished`方法。


- #### （方法说明）订阅通知notify
	`void notify(BleDevice bleDevice,
                       String uuid_service,
                       String uuid_notify,
                       BleNotifyCallback callback)`
                        
        BleManager.getInstance().notify(
                bleDevice,
                uuid_service,
                uuid_characteristic_notify,
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {
                        // 打开通知操作成功
                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {
                        // 打开通知操作失败
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        // 打开通知后，设备发过来的数据将在这里出现
                    }
                });
	

- #### （方法说明）取消订阅通知notify，并移除数据接收的回调监听

	`boolean stopNotify(BleDevice bleDevice,
                              String uuid_service,
                              String uuid_notify)`

		BleManager.getInstance().stopNotify(uuid_service, uuid_characteristic_notify);

- #### （方法说明）订阅通知indicate

	`void indicate(BleDevice bleDevice,
                         String uuid_service,
                         String uuid_indicate,
                         BleIndicateCallback callback)`

        BleManager.getInstance().indicate(
                bleDevice,
                uuid_service,
                uuid_characteristic_indicate,
                new BleIndicateCallback() {
                    @Override
                    public void onIndicateSuccess() {
                        // 打开通知操作成功
                    }

                    @Override
                    public void onIndicateFailure(BleException exception) {
                        // 打开通知操作失败
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        // 打开通知后，设备发过来的数据将在这里出现
                    }
                });


- #### （方法说明）取消订阅通知indicate，并移除数据接收的回调监听

    `boolean stopIndicate(BleDevice bleDevice,
                                String uuid_service,
                                String uuid_indicate)`
    
		BleManager.getInstance().stopIndicate(uuid_service, uuid_characteristic_indicate);

- #### （方法说明）写

	`void write(BleDevice bleDevice,
                      String uuid_service,
                      String uuid_write,
                      byte[] data,
                      BleWriteCallback callback)`
	`void write(BleDevice bleDevice,
                      String uuid_service,
                      String uuid_write,
                      byte[] data,
                      boolean split,
                      BleWriteCallback callback)`

        BleManager.getInstance().write(
                bleDevice,
                uuid_service,
                uuid_characteristic_write,
                data,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                        // 发送数据到设备成功
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        // 发送数据到设备失败
                    }
                });
	Tips:
	- 在没有扩大MTU及扩大MTU无效的情况下，当遇到超过20字节的长数据需要发送的时候，需要进行分包。参数`boolean split`表示是否使用分包发送；无`boolean split`参数的`write`方法默认对超过20字节的数据进行分包发送。
	- 关于`onWriteSuccess`回调方法: `current`表示当前发送第几包数据，`total`表示本次总共多少包数据，`justWrite`表示刚刚发送成功的数据包。

- #### （方法说明）读

	`void read(BleDevice bleDevice,
                     String uuid_service,
                     String uuid_read,
                     BleReadCallback callback)`

        BleManager.getInstance().read(
                bleDevice,
                uuid_service,
                uuid_characteristic_read,
                new BleReadCallback() {
                    @Override
                    public void onReadSuccess(byte[] data) {
                        // 读特征值数据成功
                    }

                    @Override
                    public void onReadFailure(BleException exception) {
                        // 读特征值数据失败
                    }
                });


- #### （方法说明）获取设备的信号强度Rssi

	`void readRssi(BleDevice bleDevice, BleRssiCallback callback)`

        BleManager.getInstance().readRssi(
                bleDevice,
                new BleRssiCallback() {

                    @Override
                    public void onRssiFailure(BleException exception) {
                        // 读取设备的信号强度失败
                    }

                    @Override
                    public void onRssiSuccess(int rssi) {
                        // 读取设备的信号强度成功
                    }
                });

	Tips：
	- 获取设备的信号强度，需要在设备连接之后进行。
	- 某些设备可能无法读取Rssi，不会回调onRssiSuccess(),而会因为超时而回调onRssiFailure()。

- #### （方法说明）设置最大传输单元MTU

	`void setMtu(BleDevice bleDevice,
                       int mtu,
                       BleMtuChangedCallback callback)`

        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                // 设置MTU失败
            }

            @Override
            public void onMtuChanged(int mtu) {
                // 设置MTU成功，并获得当前设备传输支持的MTU值
            }
        });

	Tips：
	- 设置MTU，需要在设备连接之后进行操作。
	- 默认每一个BLE设备都必须支持的MTU为23。
	- MTU为23，表示最多可以发送20个字节的数据。
	- 在Android 低版本(API-17 到 API-20)上，没有这个限制。所以只有在API21以上的设备，才会有拓展MTU这个需求。
	- 该方法的参数mtu，最小设置为23，最大设置为512。
	- 并不是每台设备都支持拓展MTU，需要通讯双方都支持才行，也就是说，需要设备硬件也支持拓展MTU该方法才会起效果。调用该方法后，可以通过onMtuChanged(int mtu)查看最终设置完后，设备的最大传输单元被拓展到多少。如果设备不支持，可能无论设置多少，最终的mtu还是23。

- #### （方法说明）自行构建BleDevice对象

	`BleDevice convertBleDevice(BluetoothDevice bluetoothDevice)`通过BluetoothDevice对象构建

	`BleDevice convertBleDevice(ScanResult scanResult)`通过ScanResult对象构建

	对于BLE设备扫描，官方API上提供了很多种方法，功能丰富，包括过滤规则、后台扫描等情况。FastBle框架中默认使用的是API21以下的兼容性扫描方式，建议有其他特殊需求开发者可以根据官方提供的[其他方法](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner.html)自定义扫描流程。然后利用FastBle框架中的方法对扫描到的设备进行连接等后续操作。

	需要注意的是：
	- 构建完成的BleDevice对象依然是未连接状态，如需操作，先进行连接。


- #### （方法说明）获取所有已连接设备

	`List<BleDevice> getAllConnectedDevice()`

        BleManager.getInstance().getAllConnectedDevice();

- #### （方法说明）获取某个已连接设备的BluetoothGatt

	`BluetoothGatt getBluetoothGatt(BleDevice bleDevice)`

- #### （方法说明）获取某个已连接设备的所有Service

	`List<BluetoothGattService> getBluetoothGattServices(BleDevice bleDevice)`

- #### （方法说明）获取某个Service的所有Characteristic

	`List<BluetoothGattCharacteristic> getBluetoothGattCharacteristics(BluetoothGattService service)`
		
- #### （方法说明）判断某个设备是否已连接

	`boolean isConnected(BleDevice bleDevice)`

        BleManager.getInstance().isConnected(bleDevice);

	`boolean isConnected(String mac)`

		BleManager.getInstance().isConnected(mac);

- #### （方法说明）判断某个设备的当前连接状态

	`int getConnectState(BleDevice bleDevice)`

		BleManager.getInstance().getConnectState(bleDevice);

- #### （方法说明）断开某个设备

	`void disconnect(BleDevice bleDevice)`

        BleManager.getInstance().disconnect(bleDevice);

- #### （方法说明）断开所有设备

	`void disconnectAllDevice()`

        BleManager.getInstance().disconnectAllDevice();

- #### （方法说明）退出使用，清理资源

	`void destroy()`

        BleManager.getInstance().destroy();


- #### （类说明）HexUtil

    数据操作工具类

    `String formatHexString(byte[] data, boolean addSpace)`
	byte[]转String，参数addSpace表示每一位之间是否增加空格，常用于打印日志。

	`byte[] hexStringToBytes(String hexString)`
	String转byte[]

	`char[] encodeHex(byte[] data, boolean toLowerCase)`
	byte[]转char[]，参数toLowerCase表示大小写


- #### （类说明）BleDevice

    BLE设备对象，作为本框架中的扫描、连接、操作的最小单元对象。

    `String getName()` 蓝牙广播名

    `String getMac()` 蓝牙Mac地址

    `byte[] getScanRecord()` 广播数据

    `int getRssi()` 初始信号强度


## 版本更新日志
- v2.3.0（2018-04-29）
	- 增加通过mac直连的方法
	- 增加连接失败后重连api
	- 修改若干bug
- v2.2.4（2018-02-02）
	- 优化扫描大量蓝牙设备时的效率
	- 优化在工作线程中进行数据交互的逻辑
	- 优化大数据发送时的内存管理
- v2.2.3（2018-01-23）
	- 新增分包发发送的进度回调
- v2.2.2（2018-01-09）
	- 可以在工作线程中进行蓝牙数据操作
	- 添加长数据分包发送的方法
- v2.1.7（2017-12-26）
	- 优化高并发情况下的数据返回
- v2.1.6（2017-12-20）
	- 修正UUID必须小写的Bug
	- 定义默认的扫描超时时间为10秒
- v2.1.5（2017-12-10）
	- 增加对自定义扫描设备的支持
	- 扫描过程增加onLeScan方法回调
- v2.1.4（2017-12-01）
    - 增加多设备连接操作
    - 优化扫描策略
    - 优化扫描、连接的结果回调，对扫描、连接、读写通知等操作的结果回调默认切换到主线程
    - 修正对同一特征值只会存在一种操作的bug    
    - 增加setMtu方法
    - 优化操作的超时回调
    - 增加移除指定特征Callback的方法
- v2.0.1（2017-11-20）
    - 优化扫描策略。
- v1.2.1（2017-06-28）
    - 对扫描及连接的回调处理的API做优化处理，对所有蓝牙操作的结果做明确的回调，完善文档说明
- v1.1.1（2017-05-04）
    - 优化连接异常中断后的扫描及重连机制；优化测试工具
- v1.1.0（2017-04-30）
    - 扫描设备相关部分api稍作优化及改动，完善Demo测试工具
- v1.0.6（2017-03-21）
	- 加入对设备名模糊搜索的功能
- v1.0.5（2017-03-02）
	- 优化notify、indicate监听机制
- v1.0.4（2016-12-08）
	- 增加直连指定mac设备的方法
- v1.0.3（2016-11-16）
	- 优化关闭机制，在关闭连接前先移除回调
- v1.0.2（2016-09-23）
	- 添加stopNotify和stopIndicate的方法，与stopListenCharacterCallback方法作区分
- v1.0.1（2016-09-20）
    - 优化callback机制，一个character有且只会存在一个callback，并可以手动移除
    - 添加示例代码和测试工具
- v1.0.0（2016-09-08) 
	- 初版

## Donations
如果此框架对你帮助很大，并且你很想支持库的后续开发和维护，那么你可以扫描下方捐赠二维码支持我一下，我将不胜感激。

![二维码](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/donations.png)


## Contact
如果你有技术方面问题与想法想与我沟通，可以通过下面的方式联系我。

QQ： 1033526540

Email： jasonchenlijian@gmail.com

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




