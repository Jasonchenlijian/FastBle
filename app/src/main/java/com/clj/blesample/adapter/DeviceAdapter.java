package com.clj.blesample.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.clj.blesample.R;
import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private final List<BleDevice> bleDeviceList = new ArrayList<>();
    private final Map<String, Integer> keyIndexMap = new HashMap<>();

    public void addDevice(BleDevice bleDevice) {
        String key = bleDevice.getKey();
        Integer existingIndex = keyIndexMap.get(key);
        if (existingIndex != null) {
            bleDeviceList.set(existingIndex, bleDevice);
        } else {
            keyIndexMap.put(key, bleDeviceList.size());
            bleDeviceList.add(bleDevice);
        }
    }

    public void removeDevice(BleDevice bleDevice) {
        String key = bleDevice.getKey();
        Integer index = keyIndexMap.remove(key);
        if (index != null) {
            bleDeviceList.remove(index.intValue());
            rebuildIndex();
        }
    }

    public void setScanResult(List<BleDevice> devices) {
        clearScanDevice();
        bleDeviceList.addAll(devices);
        rebuildIndex();
    }

    public void clearConnectedDevice() {
        Iterator<BleDevice> iterator = bleDeviceList.iterator();
        while (iterator.hasNext()) {
            BleDevice device = iterator.next();
            if (BleManager.getInstance().isConnected(device)) {
                iterator.remove();
            }
        }
        rebuildIndex();
    }

    public void clearScanDevice() {
        Iterator<BleDevice> iterator = bleDeviceList.iterator();
        while (iterator.hasNext()) {
            BleDevice device = iterator.next();
            if (!BleManager.getInstance().isConnected(device)) {
                iterator.remove();
            }
        }
        rebuildIndex();
    }

    private void rebuildIndex() {
        keyIndexMap.clear();
        for (int i = 0; i < bleDeviceList.size(); i++) {
            keyIndexMap.put(bleDeviceList.get(i).getKey(), i);
        }
    }

    @Override
    public int getItemCount() {
        return bleDeviceList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final BleDevice bleDevice = bleDeviceList.get(position);
        if (bleDevice == null) return;

        boolean isConnected = BleManager.getInstance().isConnected(bleDevice);
        holder.txt_name.setText(bleDevice.getName());
        holder.txt_mac.setText(bleDevice.getMac());
        holder.txt_rssi.setText(String.valueOf(bleDevice.getRssi()));

        if (isConnected) {
            holder.img_blue.setImageResource(R.mipmap.ic_blue_connected);
            holder.txt_name.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
            holder.txt_mac.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
            holder.layout_idle.setVisibility(View.GONE);
            holder.layout_connected.setVisibility(View.VISIBLE);
        } else {
            holder.img_blue.setImageResource(R.mipmap.ic_blue_remote);
            holder.txt_name.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            holder.txt_mac.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
            holder.layout_idle.setVisibility(View.VISIBLE);
            holder.layout_connected.setVisibility(View.GONE);
        }

        holder.btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onConnect(bleDevice);
            }
        });
        holder.btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onDisConnect(bleDevice);
            }
        });
        holder.btn_detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onDetail(bleDevice);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img_blue;
        TextView txt_name;
        TextView txt_mac;
        TextView txt_rssi;
        LinearLayout layout_idle;
        LinearLayout layout_connected;
        Button btn_disconnect;
        Button btn_connect;
        Button btn_detail;

        ViewHolder(View itemView) {
            super(itemView);
            img_blue = itemView.findViewById(R.id.img_blue);
            txt_name = itemView.findViewById(R.id.txt_name);
            txt_mac = itemView.findViewById(R.id.txt_mac);
            txt_rssi = itemView.findViewById(R.id.txt_rssi);
            layout_idle = itemView.findViewById(R.id.layout_idle);
            layout_connected = itemView.findViewById(R.id.layout_connected);
            btn_disconnect = itemView.findViewById(R.id.btn_disconnect);
            btn_connect = itemView.findViewById(R.id.btn_connect);
            btn_detail = itemView.findViewById(R.id.btn_detail);
        }
    }

    public interface OnDeviceClickListener {
        void onConnect(BleDevice bleDevice);
        void onDisConnect(BleDevice bleDevice);
        void onDetail(BleDevice bleDevice);
    }

    private OnDeviceClickListener mListener;

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.mListener = listener;
    }

}
