# FastBle
Android Bluetooth Low Energy 蓝牙快速开发框架。
使用回调方式处理：scan、connect、notify、indicate、write、read等一系列蓝牙操作。每一个characteristic会与一个callback形成一一对应的监听关系。

***

## Update log
- 2017-03-02
	1. 优化notify、indicate监听机制。
	
- 2016-12-08
	1. 增加直连指定mac地址设备的方法。

- 2016-11-16
	1. 优化关闭机制，在关闭连接前先移除回调。

- 2016-09-23
	1. 添加stopNotify和stopIndicate的方法，与stopListenCharacterCallback方法作区分。

- 2016-09-20
    1. 优化callback机制，一个character有且只会存在一个callback，并可以手动移除。
    2. 示例代码中添加DemoActivity和OperateActivity。前者示范如何使用本框架，后者可以作为蓝牙调试工具，测试蓝牙设备。

- 2016-09-08 
	1. 增加设备是否支持ble的判断。
	2. 修正监听不同character的时候，当其中一个character发生变化,与该character无关的callback也会回调结果的bug。

## Preview
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble1.png) 
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble2.png) 
![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/ble3.png)

## Gradle
	dependencies {
    	compile 'com.clj.fastble:FastBleLib:1.0.4'
	}

## Maven
	<dependency>
    	<groupId>com.clj.fastble</groupId>
    	<artifactId>FastBleLib</artifactId>
    	<version>1.0.4</version>
		<type>pom</type>
	</dependency>


## Usage

- ####初始化
        bleManager = new BleManager(this);

- ####判断设备是否支持BLE
		bleManager.isSupportBle();

- ####开启或关闭蓝牙
		bleManager.enableBluetooth();
		bleManager.disableBluetooth();

- #### 扫描出周围所有蓝牙可连接设备
	可获得周围蓝牙设备BluetoothDevice对象数组

        bleManager.scanDevice(new ListScanCallback(TIME_OUT) {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                super.onLeScan(device, rssi, scanRecord);
                Log.i(TAG, "发现设备：" + device.getName());
            }

            @Override
            public void onDeviceFound(BluetoothDevice[] devices) {
                Log.i(TAG, "共发现" + devices.length + "台设备");
                for (int i = 0; i < devices.length; i++) {
                    Log.i(TAG, "name:" + devices[i].getName() + "------mac:" + devices[i].getAddress());
                }
                bluetoothDevices = devices;
            }
        });

- #### 直连某一个设备
	当搜索到周围设备之后，可以选择选择某一个设备和其连接，传入的参数即为这个BluetoothDevice对象

        bleManager.connectDevice(sampleDevice, new BleGattCallback() {
			@Override
            public void onNotFoundDevice() {
                Log.i(TAG, "未发现设备！");
            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                Log.i(TAG, "连接成功！");
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.i(TAG, "服务被发现！");
                bleManager.getBluetoothState();
            }

            @Override
            public void onConnectFailure(BleException exception) {
                Log.i(TAG, "连接失败或连接中断：" + exception.toString());
                bleManager.handleException(exception);
            }
        });
            

- ####扫描指定名称的设备、并连接
	如果你确定周围有已知名称的蓝牙设备，或只需要连接指定名称的蓝牙设备，而忽略其他名称的设备，可以选择直接对指定名称进行搜索，搜索到即连接，搜索不到则回调超时接口。

        bleManager.scanNameAndConnect(
                DEVICE_NAME,
                TIME_OUT,
                new BleGattCallback() {
					@Override
            		public void onNotFoundDevice() {
                		Log.i(TAG, "未发现设备！");
            		}

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "连接成功！");
                        gatt.discoverServices();
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "服务被发现！");
                        bleManager.getBluetoothState(); 
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                        Log.i(TAG, "连接失败或连接中断：" + exception.toString());
                        bleManager.handleException(exception);
                    }

                });

- ####扫描指定MAC地址的设备、并连接
	如果你确定周围有已知地址的蓝牙设备，或只需要连接指定地址的蓝牙设备，而忽略其他地址的设备，可以选择直接对指定名称进行搜索，搜索到即连接，搜索不到则回调超时接口。

        bleManager.scanMacAndConnect(
                DEVICE_MAC,
                TIME_OUT,
                false,
                new BleGattCallback() {
            		public void onNotFoundDevice() {
                		Log.i(TAG, "未发现设备！");
            		}

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "连接成功！");
                        gatt.discoverServices();
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "服务被发现！");
                        bleManager.getBluetoothState();
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                        Log.i(TAG, "连接失败或连接中断：" + exception.toString());
                        bleManager.handleException(exception);
                    }

                });


- ####notify，listen data changes through callback
	参数中的callback和uuid将会形成关联，一旦设备的此uuid对应的character发生数据变化，此callback将会回调结果。此callbak将会唯一存在，和uuid是一一对应的关系。

        bleManager.notify(
                UUID_SERVICE,
                UUID_NOTIFY,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "notify result： "
                                + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        bleManager.handleException(exception);
                    }
                });

- ####stop notify，remove callback
		bleManager.stopNotify(UUID_SERVICE, UUID_NOTIFY);

- ####indicate，listen data changes through callback
        bleManager.indicate(
                UUID_SERVICE,
                UUID_INDICATE,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "indicate result： "
                                + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        Log.e(TAG, "indicate: " + exception.toString());
                        bleManager.handleException(exception);
                    }
                });

- ####stop indicate，remove callback
		bleManager.stopIndicate(UUID_SERVICE, UUID_INDICATE);

- ####write
        bleManager.writeDevice(
                UUID_SERVICE,
                UUID_WRITE,
                HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "write result: "
                                + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        Log.e(TAG, "write: " + exception.toString());
                        bleManager.handleException(exception);
                    }
                });
- ####read
        bleManager.readDevice(
                UUID_SERVICE,
                UUID_WRITE,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "read result: "
                                + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        Log.e(TAG, "read: " + exception.toString());
                        bleManager.handleException(exception);
                    }
                });

- ####manual remove callback 
    uuid作为参数，即不再监听这个uuid对应的character的数据变化，适用于移除notify、indicate、write、read对应的callback。

        bleManager.stopListenCharacterCallback(UUID_NOTIFY);


- #### 获取当前连接的状态
		bleManager.isInScanning();
		bleManager.isConnectingOrConnected();
		bleManager.isConnected();
		bleManager.isServiceDiscovered();

- ####复位（断开此次蓝牙连接，移除所有回调）
        bleManager.closeBluetoothGatt();


- ####其他
    其他蓝牙操作可参考示例代码，或从BleManager这个类中开放的方法中找到。

