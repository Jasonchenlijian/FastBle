package com.clj.blesample.tool.operation;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.clj.blesample.R;
import com.clj.blesample.tool.BluetoothService;

import java.util.ArrayList;
import java.util.List;

public class DeviceCharacteristicActivity extends AppCompatActivity {

    private ResultAdapter mResultAdapter;

    private BluetoothService mBluetoothService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_service);
        initView();
        bindFhrService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindFhrService();
    }

    private void initView() {
        mResultAdapter = new ResultAdapter(this);
        ListView listView_device = (ListView) findViewById(R.id.list_service);
        listView_device.setAdapter(mResultAdapter);
        listView_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothGattCharacteristic characteristic = mResultAdapter.getItem(position);
                operateCharacter(characteristic);
            }
        });
    }

    private void showData(BluetoothGattService service) {
        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            mResultAdapter.addResult(characteristic);
        }
        mResultAdapter.notifyDataSetChanged();
    }

    private void operateCharacter(BluetoothGattCharacteristic characteristic) {

    }

    private class ResultAdapter extends BaseAdapter {

        private Context context;
        private List<BluetoothGattCharacteristic> bluetoothGattCharacteristics;

        ResultAdapter(Context context) {
            this.context = context;
            bluetoothGattCharacteristics = new ArrayList<>();
        }

        public void addResult(BluetoothGattCharacteristic characteristic) {
            bluetoothGattCharacteristics.add(characteristic);
        }

        @Override
        public int getCount() {
            return bluetoothGattCharacteristics.size();
        }

        @Override
        public BluetoothGattCharacteristic getItem(int position) {
            if (position > bluetoothGattCharacteristics.size())
                return null;
            return bluetoothGattCharacteristics.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                convertView = View.inflate(context, R.layout.adapter_service, null);
                holder = new ViewHolder();
                convertView.setTag(holder);
                holder.txt_title = (TextView) convertView.findViewById(R.id.txt_title);
                holder.txt_uuid = (TextView) convertView.findViewById(R.id.txt_uuid);
                holder.txt_type = (TextView) convertView.findViewById(R.id.txt_type);
            }

            BluetoothGattCharacteristic characteristic = bluetoothGattCharacteristics.get(position);
            String uuid = characteristic.getUuid().toString();
            holder.txt_title.setText(String.valueOf("特征" + "（" + position + ")"));
            holder.txt_uuid.setText(uuid);

            holder.txt_type.setText(String.valueOf("特性" + "（" + position + ")"));
            return convertView;
        }

        class ViewHolder {
            TextView txt_title;
            TextView txt_uuid;
            TextView txt_type;
        }
    }

    private void bindFhrService() {
        Intent bindIntent = new Intent(this, BluetoothService.class);
        this.bindService(bindIntent, mFhrSCon, Context.BIND_AUTO_CREATE);
    }

    private void unbindFhrService() {
        this.unbindService(mFhrSCon);
    }

    private ServiceConnection mFhrSCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothService = ((BluetoothService.BluetoothBinder) service).getService();
            if (mBluetoothService != null)
                showData(mBluetoothService.getService());

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothService = null;
        }
    };
}
