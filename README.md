# FastBle
Android BLE 蓝牙开发框架，使用回调方式处理搜索、连接、notify、indicate、读、写等一系列蓝牙操作。
传参，然后回调结果，就是这么简单。

***

## Usage

- ####初始化

        bleManager = BleManager.getInstance();

        bleManager.init(this);

- ####扫描指定名称设备、并连接

        bleManager.connectDevice(
                DEVICE_NAME,
                TIME_OUT,
                new BleManagerConnectCallback() {
                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "连接成功！");
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "服务被发现！");
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                        Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
                    }
                });

- ####notify

        bleManager.notifyDevice(
                UUID_SERVICE_LISTEN,
                UUID_LISTEN_NOTIFY,
                UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                new BleManagerNotifyCallback() {
                    @Override
                    public void onNotifyDataChangeSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "特征值Notification通知数据回调： "
                                + '\n' + Arrays.toString(characteristic.getValue())
                                + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
                    }

                    @Override
                    public void onNotifyDataChangeFailure(BleException exception) {
                         Log.e(TAG, "特征值Notification通知回调失败: " + '\n' + exception.toString());
                    }

                });

- ####indicate

        bleManager.indicateDevice(
                UUID_SERVICE_LISTEN,
                UUID_LISTEN_INDICATE,
                UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                new BleManagerIndicateCallback() {
                    @Override
                    public void onIndicateDataChangeSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "特征值Indication通知数据回调： "
                                + '\n' + Arrays.toString(characteristic.getValue())
                                + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
                    }

                    @Override
                    public void onIndicateDataChangeFailure(BleException exception) {
                        Log.e(TAG, "特征值Indication通知回调失败: " + '\n' + exception.toString());
                    }
                });

- ####写指令

        bleManager.writeDevice(
                UUID_SERVICE_OPERATE,
                UUID_OPERATE_WRITE,
                UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                new BleManagerWriteCallback() {
                   @Override
                   public void onDataWriteSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "写特征值成功: "
                                + '\n' + Arrays.toString(characteristic.getValue())
                                + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
                   }

                   @Override
                   public void onDataWriteFailure(BleException exception) {
                        Log.e(TAG, "写读特征值失败: " + '\n' + exception.toString());
                   }
                });

- ####关闭操作

        bleManager.closeBluetoothGatt();

- ####其他
    其他蓝牙操作可参考示例代码，或从BleManager这个类中开放的方法中找到。

