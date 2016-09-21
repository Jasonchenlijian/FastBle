# FastBle
Android BLE 蓝牙快速开发框架，使用回调方式处理：scan、connect、notify、indicate、write、read等一系列蓝牙操作。每一个characteristic会与一个callback形成一一对应的监听关系。

***

## Update log
- 2016-09-20
    1. 优化callback机制，一个character有且只会存在一个callback，并可以手动移除，即stop listen
    2. 示例代码中添加DemoActivity和OperateActivity。前者示范如何使用本框架，后者可以作为蓝牙调试工具，测试蓝牙设备。

- 2016-09-08 
	1. 增加设备是否支持ble的判断。
	2. 修正监听不同character的时候，当其中一个character发生变化,与该character无关的callback也会回调结果的bug。

## Preview
![效果图](http://v2.freep.cn/3tb_160921102221cern512293.png)

![效果图](http://v1.freep.cn/3tb_160921102240aiy2512293.png)

![效果图](http://v2.freep.cn/3tb_1609211024593oc6512293.png)



## Usage

- ####初始化 (默认开启蓝牙)
        bleManager = BleManager.getInstance();
        bleManager.init(this);

- #### 扫描出周围所有蓝牙可连接设备
	可获得周围蓝牙设备BluetoothDevice对象数组

        bleManager.scanDevice(new ListScanCallback(TIME_OUT) {
            @Override
            public void onDeviceFound(BluetoothDevice[] devices) {
                Log.i(TAG, "共发现" + devices.length + "台设备");
                for (int i = 0; i < devices.length; i++) {
                    Log.i(TAG, "name:" + devices[i].getName() + "------mac:" + devices[i].getAddress());
                }
                bluetoothDevices = devices;
            }

            @Override
            public void onScanTimeout() {
                super.onScanTimeout();
                Log.i(TAG, "Time Out");
            }
        });

- #### 直连某一个设备
	当搜索到周围设备之后，可以选择选择某一个设备和其连接，传入的参数即为这个BluetoothDevice对象

        bleManager.connectDevice(sampleDevice, new BleGattCallback() {
            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                Log.i(TAG, "连接成功！");
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.i(TAG, "服务被发现！");
                bleManager.getBluetoothState();               // 打印与该设备的当前状态
            }

            @Override
            public void onConnectFailure(BleException exception) {
                Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
                bleManager.handleException(exception);
            }
        });
            

- ####扫描指定名称设备、并连接
	如果你确定周围有已知名称的蓝牙设备，或只需要连接指定名称的蓝牙设备，而忽略其他名称的设备，可以选择直接对指定名称进行搜索，搜索到即连接，搜索不到则回调超时接口。

        bleManager.connectDevice(
                DEVICE_NAME,
                TIME_OUT,
                new BleGattCallback() {
                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "连接成功！");
                        gatt.discoverServices();
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "服务被发现！");
                        bleManager.getBluetoothState();               // 打印与该设备的当前状态
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                        Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
                        bleManager.handleException(exception);
                    }

                });

- ####构造某一character的callback
    	BleCharacterCallback notifyCallback_1 = new BleCharacterCallback() {
        	@Override
        	public void onSuccess(BluetoothGattCharacteristic characteristic) {
            	Log.d(TAG, "notifyCallback_1 success： " + '\n' + Arrays.toString(characteristic.getValue()));
        	}

        	@Override
        	public void onFailure(BleException exception) {
            	bleManager.handleException(exception);
        	}
    	};

- ####对这个character进行notify
	参数中的callback和uuid将会形成关联，一旦设备的此uuid对应的character发生数据变化，此callback将会回调结果。此callbak将会唯一存在，和uuid是一一对应的关系。

        bleManager.notifyDevice(UUID_SERVICE, UUID_NOTIFY_1, notifyCallback_1);

- ####不再notify这个character
    uuid作为参数，即不再监听这个uuid对应的character

        bleManager.stopListenCharacterCallback(UUID_NOTIFY_1);

- ####indicate
        bleManager.indicateDevice(UUID_SERVICE, UUID_INDICATE, indicateCallback);

- ####write
        bleManager.writeDevice(
                UUID_SERVICE,
                UUID_WRITE,
                HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                writeCallback);
- ####read
        bleManager.readDevice(
                UUID_SERVICE,
                UUID_READ,
                readCallback);

- #### 获取当前连接的状态
		boolean a = bleManager.isInScanning();
		boolean b = bleManager.isConnectingOrConnected();
		boolean c = bleManager.isConnected();
		boolean d = bleManager.isServiceDiscovered();

- ####复位（断开此次蓝牙连接，移除所有回调）
        bleManager.closeBluetoothGatt();

- ####判断设备是否支持ble
		bleManager.isSupportBle();

- ####开启或关闭蓝牙
		bleManager.enableBluetooth();
		bleManager.disableBluetooth();

- ####其他
    其他蓝牙操作可参考示例代码，或从BleManager这个类中开放的方法中找到。

