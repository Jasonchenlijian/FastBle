# FastBle
Android BLE 蓝牙开发框架，使用回调方式处理搜索、连接、notify、indicate、读、写等一系列蓝牙操作。

传递参数，然后回调结果，如此。

***

## 更新日志
- 2016-09-08 
	1. 增加设备是否支持ble的判断。
	2. 修正监听不同character的时候，当其中一个character发生变化,与该特征值无关的callback也会回调结果的bug。


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
                Log.i(TAG, "搜索时间结束");
            }
        });

- #### 直连某一个设备
	当搜索到周围设备之后，可以选择选择某一个设备和其连接，传入的参数即为这个BluetoothDevice对象

        bleManager.connectDevice(sampleDevice, new BleGattCallback() {
            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                Log.i(TAG, "连接成功！");
                gatt.discoverServices();                	  // 连接上设备后搜索服务
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.i(TAG, "服务被发现！");
                BluetoothUtil.printServices(gatt);            // 打印该设备所有服务、特征值
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
                        gatt.discoverServices();                // 连接上设备后搜索服务
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "服务被发现！");
                        BluetoothUtil.printServices(gatt);      // 打印该设备所有服务、特征值
                        bleManager.getBluetoothState();         // 打印与该设备的当前状态
                   }

                   @Override
                   public void onConnectFailure(BleException exception) {
                        Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
                        bleManager.handleException(exception);
                   }
               });

- ####notify
        bleManager.notifyDevice(
                UUID_SERVICE_LISTEN,
                UUID_LISTEN_NOTIFY,
                UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "特征值Notify通知数据回调： "
                                + '\n' + Arrays.toString(characteristic.getValue()));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        Log.e(TAG, "特征值Notify通知回调失败: " + '\n' + exception.toString());
                        bleManager.handleException(exception);
                    }
                });

- ####indicate
         bleManager.indicateDevice(
                UUID_SERVICE_LISTEN,
                UUID_LISTEN_INDICATE,
                UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "特征值Indicate通知数据回调： "
                                + '\n' + Arrays.toString(characteristic.getValue()));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        Log.e(TAG, "特征值Indicate通知回调失败: " + '\n' + exception.toString());
                        bleManager.handleException(exception);
                    }
                });

- ####写指令
        bleManager.writeDevice(
                UUID_SERVICE_OPERATE,
                UUID_OPERATE_WRITE,
                UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "写特征值成功: "
                                + '\n' + Arrays.toString(characteristic.getValue()));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        Log.e(TAG, "写读特征值失败: " + '\n' + exception.toString());
                        bleManager.handleException(exception);
                    }
               });

- #### 获取当前连接的状态
		boolean a = bleManager.isInScanning();
		boolean b = bleManager.isConnectingOrConnected();
		boolean c = bleManager.isConnected();
		boolean d = bleManager.isServiceDiscovered();

- #### 必要时移除某一回调
	作为参数传入的callback将被加入callback集合不会移除，会持续保持监听。今后当该callback监听的特征值每一次发生变化时，将会触发callback回调（其他特征值发生变化不会影响）。所以当使用者不再需要此callback的时候，可自行移除。

		/**将回调实例化，而不是以匿名对象的形式*/
    	BleCharacterCallback bleCharacterCallback = new BleCharacterCallback() {
        	@Override
        	public void onSuccess(BluetoothGattCharacteristic characteristic) {
            	Log.d(TAG, "特征值Notificaty通知数据回调： "
                    + '\n' + Arrays.toString(characteristic.getValue()));
        	}

        	@Override
        	public void onFailure(BleException exception) {
            	Log.e(TAG, "特征值Notify通知回调失败: " + '\n' + exception.toString());
            	bleManager.handleException(exception);
        	}
    	};

		/**需要使用的时候，作为参数传入*/
        bleManager.notifyDevice(
                UUID_SERVICE_OPERATE,
                UUID_OPERATE_NOTIFY,
                UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                bleCharacterCallback);

        /**不需要再监听特征值变化的时候，将该回调接口对象移除*/
        bleManager.removeBleCharacterCallback(bleCharacterCallback);


- ####复位（断开此次蓝牙连接，移除所有回调）
        bleManager.closeBluetoothGatt();

- ####判断设备是否支持ble
		bleManager.isSupportBle();

- ####开启或关闭蓝牙
		bleManager.enableBluetooth();

		bleManager.disableBluetooth();

- ####其他
    其他蓝牙操作可参考示例代码，或从BleManager这个类中开放的方法中找到。

