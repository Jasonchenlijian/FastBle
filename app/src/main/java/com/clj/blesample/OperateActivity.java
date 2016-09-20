package com.clj.blesample;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleGattCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.BluetoothUtil;

/**
 * Created by 陈利健 on 2016/9/20.
 * 可作为工具测试
 */
public class OperateActivity extends AppCompatActivity implements View.OnClickListener {

    private LinearLayout layout_device_list;
    private EditText et_device_name;

    private BleManager bleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operate);

        findViewById(R.id.btn_scan).setOnClickListener(this);
        findViewById(R.id.btn_connect).setOnClickListener(this);

        layout_device_list = (LinearLayout) findViewById(R.id.layout_device_list);
        et_device_name = (EditText) findViewById(R.id.et_device_name);

        bleManager = BleManager.getInstance();
        bleManager.init(this);

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
                    connectDevice(deviceName);
                }
                break;
        }
    }

    /**
     * 搜索周围蓝牙设备
     */
    private void scanDevice() {
        bleManager.scanDevice(new ListScanCallback(2000) {
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
    private void showDeviceList(BluetoothDevice[] devices) {
        layout_device_list.removeAllViews();

        for (int i = 0; devices != null && i < devices.length; i++) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.layout_item_device_list, null);

            RelativeLayout layout_item_device = (RelativeLayout) itemView.findViewById(R.id.layout_item_device);
            TextView txt_item_name = (TextView) itemView.findViewById(R.id.txt_item_name);

            txt_item_name.setText(devices[i].getName());
            layout_item_device.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

            layout_device_list.addView(itemView);
        }
    }

    /**
     * 直连某一蓝牙设备
     */
    private void connectDevice(String deviceName) {
        bleManager.connectDevice(deviceName, 2000, new BleGattCallback() {
            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothUtil.printServices(gatt);            // 打印该设备所有服务、特征值
                bleManager.getBluetoothState();               // 打印与该设备的当前状态
            }

            @Override
            public void onConnectFailure(BleException exception) {
                bleManager.handleException(exception);
            }
        });
    }


}
