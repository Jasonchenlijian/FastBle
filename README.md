# FastBle
Android Bluetooth Low Energy 蓝牙快速开发框架。

>使用简单的方式进行 过滤、扫描、连接、读、写、通知订阅与取消、获取信号强度、设置最大传输单元、多设备连接等蓝牙通信功能。




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
       <version>2.2.1</version>
	   <type>pom</type>
	</dependency>

### Gradle

	compile 'com.clj.fastble:FastBleLib:2.2.1'


## 其他说明

FastBle requires at minimum Java 7 or Android 4.0.

FastBle 所有代码均可以加入混淆。



***


# 如何使用

- #### （方法说明）初始化
    
        BleManager.getInstance().init(getApplication());

- #### （方法说明）判断当前Android系统是否支持BLE

        boolean isSupportBle()

- #### （方法说明）开启或关闭蓝牙

		void enableBluetooth()
		void disableBluetooth()

- #### （方法说明）打印异常信息
	
		void handleException(BleException exception);

- #### （方法说明）是否打印日志，默认开启

	`BleManager enableLog(boolean enable)`

		BleManager.getInstance().enableLog(false);

- #### （方法说明）设置操作超时时间，默认5秒

	`BleManager setOperateTimeout(int operateTimeout)`

		BleManager.getInstance().setOperateTimeout(5000);

	此处的超时时间指的是：连接上一台设备之后，对这台设备进行 readRssi、setMtu、write、read、notify、indicate 这6种操作的超时时间。

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
	Tips：
	- 蓝牙的扫描过程会切换到主线程，所以不论在哪个线程中开启扫描，最后的结果回调都会回到主线程。

- #### （方法说明）连接

	`BluetoothGatt connect(BleDevice bleDevice, BleGattCallback bleGattCallback)`

        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
				// 开始连接
            }

            @Override
            public void onConnectFail(BleException exception) {
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
	- 如果在主线程中进行连接，所有结果回调会回到主线程。
	- 连接过程也可以切换到子线程中进行。但是在某些型号手机上，connectGatt必须在主线程才能有效。
	- 非常建议把连接过程放在主线程。

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
            public void onConnectFail(BleException exception) {
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
	Tips:
	- 在哪个线程中notify，结果就回到那个线程中回调

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
	Tips:
	- 在哪个线程中indicate，结果就回到那个线程中回调

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
                    public void onWriteSuccess() {
                        // 发送数据到设备成功
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        // 发送数据到设备失败
                    }
                });
	Tips:
	- 在哪个线程中write，结果就回到那个线程中回调
	- 在没有扩大MTU及扩大MTU无效的情况下，当遇到超过20字节的长数据需要发送的时候，需要进行分包。参数`boolean split`表示是否使用分包发送；无`boolean split`参数的`write`方法默认对超过20字节的数据进行分包发送。

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
	Tips:
	- 在哪个线程中read，结果就回到那个线程中回调

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
	- 在哪个线程中readRssi，结果就回到那个线程中回调

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
	- 在哪个线程中setMtu，结果就回到那个线程中回调

- #### （方法说明）移除对应设备及对应特征的监听

	`void removeConnectGattCallback(BleDevice bleDevice)`移除对设备原先连接状态的监听

	`void removeRssiCallback(BleDevice bleDevice)`移除对设备原先Rssi的监听

	`void removeMtuChangedCallback(BleDevice bleDevice)`移除对设备原先Mtu变化的监听

	`void removeNotifyCallback(BleDevice bleDevice, String uuid_notify)`移除对设备原先某个特征的notify的监听

	`void removeIndicateCallback(BleDevice bleDevice, String uuid_indicate)`移除对设备原先某个特征的indicate的监听

	`void removeWriteCallback(BleDevice bleDevice, String uuid_write)`移除对设备原先某个特征的write的监听

	`void removeReadCallback(BleDevice bleDevice, String uuid_read)`移除对设备原先某个特征的read的监听

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

        BleManager.getInstance().getBluetoothGatt(bleDevice);

- #### （方法说明）判断某个设备是否已连接

	`boolean isConnected(BleDevice bleDevice)`

        BleManager.getInstance().isConnected(bleDevice);

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
		

- #### （类说明）BleException

	`int getCode()` 获取异常码

	`String getDescription()` 获取异常描述
		
	异常码：

		100： 超时
		101： 连接异常
		102： 其他（异常信息可以通过异常描述获取，一般是开发过程中的操作中间步骤的异常）
		103： 设备未找到
		104： 蓝牙未启用
		105： 开启扫描过程失败





## 蓝牙操作经验及FastBle的兼容性说明

- BLE是蓝牙4.0里面的低功耗规范，Android 4.3以上的系统开始搭载BLE模块，所以FastBle也只支持4.3以上。

- 不排除某些特殊设备的定制系统去除了BLE模块的情况，使用之前可以先判断当前设备是否支持BLE，再进行后续操作。

- 蓝牙设备相关程序必须使用真机才能运行。

- FastBle当前版本仅支持对BLE蓝牙进行操作，不支持经典蓝牙。

- 使用蓝牙功能，必须先声明蓝牙相关的权限。Android 6.0以上的系统，需要额外申请位置相关的权限，并且是危险权限建议在运行时动态获取。为使使用更灵活，FastBle库中并不包含权限相关的操作，使用者根据程序的实际情况在外层自行嵌套。示例代码中有相关代码演示，供参考。在部分型号的Android设备上，可能还需要手动打开定位功能。

- 蓝牙操作与硬件关联很大，开发过程中要保持和硬件协议的沟通，某些问题的解决需要硬件方面做一些适配。

- 蓝牙连接之后，列出当前外设模块的所有service，每个service可能有一个或多个的characteristic，每一个characteristic有其对应的property（即可操作的属性类别）,假如一个characteristic的property对应的是write，那么对这个characteristic做notify处理显然是行不通的。

- 两次操作之间最好间隔一小段时间，如100ms（具体时间可以根据自己实际蓝牙外设自行尝试延长或缩短）。举例，连接成功之后，延迟100ms进行notify，成功之后延迟100ms进行write。

- 连接及连接后的过程中，时刻关注`onDisConnected`方法，然后做处理。

- 连接过程中，假如外设突然中断（或关闭）了蓝牙，由于某些蓝牙外设的不完善，导致Android设备维持的BLE连接并不会马上回调`onDisConnected`方法，而是会延迟一段时间才会通知连接断开，开发时需注意，假如对实时性要求较高的程序，可能需要借助其他辅助方法来判断设备是否中断，比如心跳包等。

- 蓝牙应用开发中，存在两种角色，分别是central和peripheral ,中文就是中心和外设。比如手机去连接智能设备，那手机就是central，智能设备就是peripheral。

- FastBle当前版本仅支持中心模式 （central model），即"以App作为中心，连接其他BLE外设"。把手机作为外设目前版本是行不通的。

- 连接之后的操作有：write，read，notify，indicate，response or not等。indicate和notify的区别就在于，indicate是一定会收到数据，notify有可能会丢失数据（不会有central收到数据的回应），write也分为response和no response，如果是response，那么write成功回收到peripheral的确认消息，但是会降低写入的速率，换一个角度说就是 write no response写的速率更快。

- 蓝牙数据操作是串行的，所以不等待上一次的结果回复而直接进行下一次操作是不行的。比如对一个特征值进行写入数据，直接连续写入是错误的，需要等待结果回调后，进行下一次写入。

- 很多Android设备是可以强制打开用户手机蓝牙的，打开蓝牙需要一段时间（部分手机上需要向用户请求）。虽然时间比较短，但也不能调用完打开蓝牙方法后直接去调用扫描方法，此时蓝牙多半是还未开启完毕状态。建议的做法是维持一个蓝牙状态的广播，调用打开蓝牙方法后，在一段时间内阻塞线程，如果在这段时间内收到蓝牙打开广播后，再进行后续操作。而后续操作过程中，如果收到蓝牙正在关闭或关闭的广播，也可以及时对当前的情况做一个妥善处理。

- core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle 2个字节之后，剩下的20个字节便是留给GATT的了。因此core spec规定每一个BLE设备都必须支持MTU为23。那么我们进行BLE数据相互发送的时候，一次最多能发送20个字节。如果需要发送的数据超过20个字节，有两种方法，一种是主动尝试拓宽MTU，另一种是采用分包传输的方式。

- 拓宽MTU，只有在API-21以上的Android设备才起作用，因为API-21及以下的设备没有这个限制。另外，需要关注的是并不是拓宽MTU以后就行了，还需要看你所连接的设备是否支持拓宽。


## 版本更新日志
- v2.2.1（2018-01-08）
	- 可以在子线程中进行蓝牙数据操作
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
    - 增加移除指定特征Callback的方法
    - 修正2.1.2版本上操作超时回调的bug
- v2.1.2（2017-11-29）
    - 增加setMtu方法
    - 优化操作的超时回调
- v2.1.1（2017-11-27）
    - 增加多设备连接操作
    - 优化扫描策略
    - 优化扫描、连接的结果回调，对扫描、连接、读写通知等操作的结果回调默认切换到主线程
    - 修正对同一特征值只会存在一种操作的bug
- v2.0.1（2017-11-20）
    - 优化扫描策略。
- v1.2.1（2017-06-29）
    - 小幅优化：仅仅对onDisConnected()回调方法中增加gatt和status参数，便于进行重连操作。
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




