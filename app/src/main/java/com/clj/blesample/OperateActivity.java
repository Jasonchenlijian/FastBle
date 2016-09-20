package com.clj.blesample;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleGattCallback;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.HexUtil;

import java.util.Arrays;

/**
 * Created by 陈利健 on 2016/9/20.
 * 可作为工具测试
 */
public class OperateActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = OperateActivity.class.getSimpleName();

    private LinearLayout layout_item_connect;
    private LinearLayout layout_device_list;
    private EditText et_device_name;

    private LinearLayout layout_item_state;
    private TextView txt_device_name;
    private LinearLayout layout_character_list;

    private BleManager bleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operate);

        findViewById(R.id.btn_scan).setOnClickListener(this);
        findViewById(R.id.btn_connect).setOnClickListener(this);

        layout_item_connect = (LinearLayout) findViewById(R.id.layout_item_connect);
        layout_device_list = (LinearLayout) findViewById(R.id.layout_device_list);
        et_device_name = (EditText) findViewById(R.id.et_device_name);

        layout_item_state = (LinearLayout) findViewById(R.id.layout_item_state);
        txt_device_name = (TextView) findViewById(R.id.txt_device_name);
        layout_character_list = (LinearLayout) findViewById(R.id.layout_character_list);

        bleManager = BleManager.getInstance();
        bleManager.init(this);

        showDisConnectState();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_scan:
                scanDevice();
                break;

            case R.id.btn_connect:
                String deviceName = et_device_name.getText().toString().trim();
                if (!TextUtils.isEmpty(deviceName)) {
                    connectNameDevice(deviceName);
                }
                break;
        }
    }

    /**
     * 搜索周围蓝牙设备
     */
    private void scanDevice() {
        if (bleManager.isInScanning())
            return;

        bleManager.scanDevice(new ListScanCallback(20000) {
            @Override
            public void onDeviceFound(BluetoothDevice[] devices) {
                showDeviceList(devices);
            }

            @Override
            public void onScanTimeout() {
                super.onScanTimeout();
            }
        });
    }

    /**
     * 显示蓝牙设备列表
     */
    private void showDeviceList(final BluetoothDevice[] devices) {
        layout_device_list.removeAllViews();

        for (int i = 0; devices != null && i < devices.length; i++) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.layout_list_item_device, null);

            RelativeLayout layout_item_device = (RelativeLayout) itemView.findViewById(R.id.layout_list_item_device);
            TextView txt_item_name = (TextView) itemView.findViewById(R.id.txt_item_name);

            final BluetoothDevice device = devices[i];
            txt_item_name.setText(device.getName());
            layout_item_device.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    connectSpecialDevice(device);
                }
            });

            layout_device_list.addView(itemView);
        }
    }

    /**
     * 连接设备
     */
    private void connectSpecialDevice(final BluetoothDevice device) {
        bleManager.connectDevice(device, new BleGattCallback() {
            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                showConnectState(device.getName(), gatt);
            }

            @Override
            public void onConnectFailure(BleException exception) {
                bleManager.handleException(exception);
            }
        });
    }

    /**
     * 直连某一蓝牙设备
     */
    private void connectNameDevice(final String deviceName) {
        bleManager.connectDevice(deviceName, 20000, new BleGattCallback() {
            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                showConnectState(deviceName, gatt);
            }

            @Override
            public void onConnectFailure(BleException exception) {
                bleManager.handleException(exception);
            }
        });
    }

    /**
     * 显示未连接状态
     */
    private void showDisConnectState() {
        bleManager.closeBluetoothGatt();

        layout_item_connect.setVisibility(View.VISIBLE);
        layout_item_state.setVisibility(View.GONE);
    }

    /**
     * 显示连接状态
     */
    private void showConnectState(String deviceName, BluetoothGatt gatt) {
        bleManager.getBluetoothState();

        layout_item_connect.setVisibility(View.GONE);
        layout_item_state.setVisibility(View.VISIBLE);
        txt_device_name.setText(deviceName);

        layout_character_list.removeAllViews();
        if (gatt != null) {
            for (final BluetoothGattService service : gatt.getServices()) {
                View serviceView = LayoutInflater.from(this).inflate(R.layout.layout_list_item_service, null);
                TextView txt_service = (TextView) serviceView.findViewById(R.id.txt_service);
                txt_service.setText(service.getUuid().toString());

                layout_character_list.addView(serviceView);

                for (final BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    View characterView = LayoutInflater.from(this).inflate(R.layout.layout_list_item_character, null);
                    characterView.setTag(characteristic.getUuid().toString());
                    TextView txt_character = (TextView) characterView.findViewById(R.id.txt_character);
                    final TextView txt_properties = (TextView) characterView.findViewById(R.id.txt_properties);
                    TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);

                    txt_character.setText(characteristic.getUuid().toString());
                    txt_value.setText(Arrays.toString(characteristic.getValue()));
                    switch (characteristic.getProperties()) {
                        case 2:
                            txt_properties.setText("read");
                            txt_properties.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    startRead(service.getUuid().toString(), characteristic.getUuid().toString());
                                }
                            });
                            break;

                        case 8:
                            txt_properties.setText("write");
                            txt_properties.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    EditDialog dialog = new EditDialog(OperateActivity.this);
                                    dialog.setOnDialogClickListener(new EditDialog.OnDialogClickListener() {
                                        @Override
                                        public void onEditOkClick(String writeData) {
                                            startWrite(service.getUuid().toString(), characteristic.getUuid().toString(), writeData);
                                        }

                                        @Override
                                        public void onEditErrorClick() {
                                            Log.e(TAG, "write error");
                                        }
                                    });
                                    dialog.show();
                                }
                            });
                            break;

                        case 16:
                            txt_properties.setText("notify");
                            txt_properties.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (txt_properties.getText().toString().equals("notify")) {
                                        startNotify(service.getUuid().toString(), characteristic.getUuid().toString());
                                    } else if (txt_properties.getText().toString().equals("stop notify")) {
                                        stopListen(characteristic.getUuid().toString());
                                    }
                                }
                            });
                            break;

                        case 32:
                            txt_properties.setText("indicate");
                            txt_properties.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (txt_properties.getText().toString().equals("indicate")) {
                                        startIndicate(service.getUuid().toString(), characteristic.getUuid().toString());
                                    } else if (txt_properties.getText().toString().equals("stop indicate")) {
                                        stopListen(characteristic.getUuid().toString());
                                    }
                                }
                            });
                            break;
                    }
                    layout_character_list.addView(characterView);

                }
            }
        }
    }

    private void startNotify(String serviceUUID, final String characterUUID) {
        Log.i(TAG, "startNotify");
        boolean suc = bleManager.notifyDevice(
                serviceUUID,
                characterUUID,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "notify success： " + '\n' + Arrays.toString(characteristic.getValue()));
                        View characterView = layout_character_list.findViewWithTag(characterUUID);
                        if (characterView != null) {
                            TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);
                            if (txt_value != null) {
                                txt_value.setText(Arrays.toString(characteristic.getValue()));
                            }
                        }
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        bleManager.handleException(exception);
                    }
                });

        if (suc) {
            View characterView = layout_character_list.findViewWithTag(characterUUID);
            if (characterView != null) {
                TextView txt_properties = (TextView) characterView.findViewById(R.id.txt_properties);
                txt_properties.setText("stop notify");
            }
        }
    }

    private void startIndicate(String serviceUUID, final String characterUUID) {
        Log.i(TAG, "startIndicate");
        boolean suc = bleManager.indicateDevice(
                serviceUUID,
                characterUUID,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "indicate success： " + '\n' + Arrays.toString(characteristic.getValue()));
                        View characterView = layout_character_list.findViewWithTag(characterUUID);
                        if (characterView != null) {
                            TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);
                            if (txt_value != null) {
                                txt_value.setText(Arrays.toString(characteristic.getValue()));
                            }
                        }
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        bleManager.handleException(exception);
                    }
                });

        if (suc) {
            View characterView = layout_character_list.findViewWithTag(characterUUID);
            if (characterView != null) {
                TextView txt_properties = (TextView) characterView.findViewById(R.id.txt_properties);
                txt_properties.setText("stop indicate");
            }
        }
    }

    private void startWrite(String serviceUUID, String characterUUID, String writeData) {
        Log.i(TAG, "startWrite");
        bleManager.writeDevice(
                serviceUUID,
                characterUUID,
                HexUtil.hexStringToBytes(writeData),
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "write success: " + '\n' + Arrays.toString(characteristic.getValue()));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        bleManager.handleException(exception);
                    }
                });
    }

    private void startRead(String serviceUUID, final String characterUUID) {
        Log.i(TAG, "startRead");
        bleManager.readDevice(
                serviceUUID,
                characterUUID,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "read success: " + '\n' + Arrays.toString(characteristic.getValue()));
                        View characterView = layout_character_list.findViewWithTag(characterUUID);
                        if (characterView != null) {
                            TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);
                            if (txt_value != null) {
                                txt_value.setText(Arrays.toString(characteristic.getValue()));
                            }
                        }
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        bleManager.handleException(exception);
                    }
                });
    }

    private void stopListen(String characterUUID) {
        Log.i(TAG, "stopNotify");
        bleManager.stopListenCharacterCallback(characterUUID);
    }


}
