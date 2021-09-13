![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/fastble_poster.png)

Thanks to the logo designed by [anharismail](https://github.com/anharismail)


# FastBle
Android Bluetooth Low Energy

- Filtering, scanning, linking, reading, writing, notification subscription and cancellation in a simple way.
- Supports acquiring signal strength and setting the maximum transmission unit.
- Support custom scan rules  
- Support multi device connections  
- Support reconnection  
- Support configuration timeout for conncet or operation  


### Preview
![Preview_1](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_1.png) 
![Preview_2](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_2.png) 
![Preview_3](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_3.png)
![Preview_4](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_4.png)


### APK
If you want to quickly preview all the functions, you can download APK as a test tool directly.

 [FastBLE.apk](https://github.com/Jasonchenlijian/FastBle/raw/master/FastBLE.apk) 


### Gradle

    Setp1: Add it in your root build.gradle at the end of repositories

    allprojects {
    	repositories {
    		...
    		maven { url 'https://jitpack.io' }
    	}
    }

    Step2: Add the dependency

	dependencies {
    	    implementation 'com.github.Jasonchenlijian:FastBle:2.4.0'
    }

### Jar

[FastBLE-2.4.0.jar](https://github.com/Jasonchenlijian/FastBle/raw/master/FastBLE-2.4.0.jar)


## Wiki

[中文文档](https://github.com/Jasonchenlijian/FastBle/wiki)

[Android BLE开发详解和FastBle源码解析](https://www.jianshu.com/p/795bb0a08beb)



## Usage

- #### Init
    
        BleManager.getInstance().init(getApplication());

- #### Determine whether the current Android system supports BLE

        boolean isSupportBle()

- #### Open or close Bluetooth

		void enableBluetooth()
		void disableBluetooth()

- #### Initialization configuration

        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
	            .setSplitWriteNum(20)
	            .setConnectOverTime(10000)
                .setOperateTimeout(5000);

- #### Configuration scan rules

	`void initScanRule(BleScanRuleConfig scanRuleConfig)`

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(serviceUuids)
                .setDeviceName(true, names)
                .setDeviceMac(mac)
                .setAutoConnect(isAutoConnect)
                .setScanTimeOut(10000)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);

	Tips：
	- Before scanning the device, scan rules can be configured to filter out the equipment matching the program.
	- What is not configured is the default parameter

- #### Scan

	`void scan(BleScanCallback callback)`

        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {

            }

            @Override
            public void onScanning(BleDevice bleDevice) {

            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {

            }
        });

	Tips:
	- The scanning and filtering process is carried out in the worker thread, so it will not affect the UI operation of the main thread. Eventually, every callback result will return to the main thread.。

- #### Connect with device


	`BluetoothGatt connect(BleDevice bleDevice, BleGattCallback bleGattCallback)`

        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {

            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {

            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {

            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {

            }
        });

	Tips:
	- On some types of phones, connectGatt must be effective on the main thread. It is very recommended that the connection process be placed in the main thread.
	- After connection failure, reconnect: the framework contains reconnection mechanism after connection failure, which can configure reconnection times and intervals. Of course, you can also call the `connect` method in `onConnectFail` callback automatically.
	- The connection is disconnected and reconnected: you can call the `connect` method again in the `onDisConnected` callback method.
	- In order to ensure the success rate of reconnection, it is recommended to reconnect after a period of interval.
	- When some models fail, they will be unable to scan devices for a short time. They can be connected directly through device objects or devices MAC without scanning.

- #### Connect with Mac

	`BluetoothGatt connect(String mac, BleGattCallback bleGattCallback)`

        BleManager.getInstance().connect(mac, new BleGattCallback() {
            @Override
            public void onStartConnect() {

            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {

            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {

            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {

            }
        });

	Tips:
	- This method can attempt to connect directly to the BLE device around the Mac without scanning.
	- In many usage scenarios, I suggest that APP save the Mac of the user's customary device, then use this method to connect, which will greatly improve the connection efficiency.

- #### Scan and connect

	After scanning the first equipment that meets the scanning rules, it will stop scanning and connect to the device.

	`void scanAndConnect(BleScanAndConnectCallback callback)`

        BleManager.getInstance().scanAndConnect(new BleScanAndConnectCallback() {
            @Override
            public void onScanStarted(boolean success) {

            }

            @Override
            public void onScanFinished(BleDevice scanResult) {

            }

            @Override
            public void onStartConnect() {

            }

            @Override
            public void onConnectFail(BleDevice bleDevice,BleException exception) {

            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {

            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {

            }
        }); 


- #### Cancel scan

	`void cancelScan()`

		BleManager.getInstance().cancelScan();

	Tips:
	- If this method is called, if it is still in the scan state, it will end immediately, and callback the `onScanFinished` method.


- #### Notify
	`void notify(BleDevice bleDevice,
                       String uuid_service,
                       String uuid_notify,
                       BleNotifyCallback callback)`
	`void notify(BleDevice bleDevice,
                       String uuid_service,
                       String uuid_notify,
                       boolean useCharacteristicDescriptor,
                       BleNotifyCallback callback)`
                        
        BleManager.getInstance().notify(
                bleDevice,
                uuid_service,
                uuid_characteristic_notify,
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {

                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {

                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {

                    }
                });
	

- #### Stop Notify

	`boolean stopNotify(BleDevice bleDevice,
                              String uuid_service,
                              String uuid_notify)`
	`boolean stopNotify(BleDevice bleDevice,
                              String uuid_service,
                              String uuid_notify,
                              boolean useCharacteristicDescriptor)`

		BleManager.getInstance().stopNotify(uuid_service, uuid_characteristic_notify);

- #### Indicate

	`void indicate(BleDevice bleDevice,
                         String uuid_service,
                         String uuid_indicate,
                         BleIndicateCallback callback)`
	`void indicate(BleDevice bleDevice,
                         String uuid_service,
                         String uuid_indicate,
                         boolean useCharacteristicDescriptor,
                         BleIndicateCallback callback)`

        BleManager.getInstance().indicate(
                bleDevice,
                uuid_service,
                uuid_characteristic_indicate,
                new BleIndicateCallback() {
                    @Override
                    public void onIndicateSuccess() {

                    }

                    @Override
                    public void onIndicateFailure(BleException exception) {

                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {

                    }
                });


- #### Stop Indicate

    `boolean stopIndicate(BleDevice bleDevice,
                                String uuid_service,
                                String uuid_indicate)`
	`boolean stopIndicate(BleDevice bleDevice,
                                String uuid_service,
                                String uuid_indicate,
                                boolean useCharacteristicDescriptor)`
    
		BleManager.getInstance().stopIndicate(uuid_service, uuid_characteristic_indicate);

- #### Write

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
	`void write(BleDevice bleDevice,
                      String uuid_service,
                      String uuid_write,
                      byte[] data,
                      boolean split,
                      boolean sendNextWhenLastSuccess,
                      long intervalBetweenTwoPackage,
                      BleWriteCallback callback)`

        BleManager.getInstance().write(
                bleDevice,
                uuid_service,
                uuid_characteristic_write,
                data,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {

                    }

                    @Override
                    public void onWriteFailure(BleException exception) {

                    }
                });

	Tips:
	- Without expanding MTU and expanding MTU's ineffectiveness, subcontracting is required when long data with more than 20 bytes are to be sent. The parameter `boolean split` indicates whether to use packet delivery; the `write` method without the `boolean split` parameter is subcontracted to the data by more than 20 bytes by default.
	- On the `onWriteSuccess` callback method: `current` represents the number of packets that are currently sent, and `total` represents the total packet data this time, and `justWrite` represents the successful packet that has just been sent.

- #### Read

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

                    }

                    @Override
                    public void onReadFailure(BleException exception) {

                    }
                });


- #### Get Rssi

	`void readRssi(BleDevice bleDevice, BleRssiCallback callback)`

        BleManager.getInstance().readRssi(
                bleDevice,
                new BleRssiCallback() {

                    @Override
                    public void onRssiFailure(BleException exception) {

                    }

                    @Override
                    public void onRssiSuccess(int rssi) {

                    }
                });

	Tips：
	- Obtaining the signal strength of the device must be carried out after the device is connected.
	- Some devices may not be able to read Rssi, do not callback onRssiSuccess (), and callback onRssiFailure () because of timeout.

- #### set Mtu

	`void setMtu(BleDevice bleDevice,
                       int mtu,
                       BleMtuChangedCallback callback)`

        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {

            }

            @Override
            public void onMtuChanged(int mtu) {

            }
        });

	Tips：
	- Setting up MTU requires operation after the device is connected.
	- There is no such restriction in the Android Version (API-17 to API-20). Therefore, only the equipment above API21 will expand the demand for MTU.
	- The parameter MTU of the method is set to 23, and the maximum setting is 512.
	- Not every device supports the expansion of MTU, which requires both sides of the communication, that is to say, the need for the device hardware also supports the expansion of the MTU method. After calling this method, you can see through onMtuChanged (int MTU) how much the maximum transmission unit of the device is expanded to after the final setup. If the device does not support, no matter how many settings, the final MTU will be 23.

- #### requestConnectionPriority

	`boolean requestConnectionPriority(BleDevice bleDevice,int connectionPriority)`

	Tips:
	- Request a specific connection priority. Must be one of{@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED}, {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH} or {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.

- #### Converte BleDevice object

	`BleDevice convertBleDevice(BluetoothDevice bluetoothDevice)`

	`BleDevice convertBleDevice(ScanResult scanResult)`

	Tips：
	- The completed BleDevice object is still unconnected, if necessary, advanced connection.

- #### Get all connected devices

	`List<BleDevice> getAllConnectedDevice()`

        BleManager.getInstance().getAllConnectedDevice();

- #### Get a BluetoothGatt of a connected device

	`BluetoothGatt getBluetoothGatt(BleDevice bleDevice)`

- #### Get all Service of a connected device

	`List<BluetoothGattService> getBluetoothGattServices(BleDevice bleDevice)`

- #### Get all the Characteristic of a Service

	`List<BluetoothGattCharacteristic> getBluetoothGattCharacteristics(BluetoothGattService service)`
		
- #### Determine whether a device has been connected

	`boolean isConnected(BleDevice bleDevice)`

        BleManager.getInstance().isConnected(bleDevice);

	`boolean isConnected(String mac)`

		BleManager.getInstance().isConnected(mac);

- #### Determine the current connection state of a device

	`int getConnectState(BleDevice bleDevice)`

		BleManager.getInstance().getConnectState(bleDevice);

- #### Disconnect a device

	`void disconnect(BleDevice bleDevice)`

        BleManager.getInstance().disconnect(bleDevice);

- #### Disconnect all devices

	`void disconnectAllDevice()`

        BleManager.getInstance().disconnectAllDevice();

- #### Out of use, clean up resources

	`void destroy()`

        BleManager.getInstance().destroy();


- #### HexUtil

    Data operation tool class

    `String formatHexString(byte[] data, boolean addSpace)`

	`byte[] hexStringToBytes(String hexString)`

	`char[] encodeHex(byte[] data, boolean toLowerCase)`


- #### BleDevice

    BLE device object is the smallest unit object of scanning, connection and operation in this framework.

    `String getName()` Bluetooth broadcast name

    `String getMac()` Bluetooth MAC

    `byte[] getScanRecord()` Broadcast data

    `int getRssi()` Initial signal intensity



## Contact
If you have problems and ideas to communicate with me, you can contact me in the following ways.

WeChat： chenlijian1216

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




