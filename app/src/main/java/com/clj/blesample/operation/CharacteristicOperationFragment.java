package com.clj.blesample.operation;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.clj.blesample.R;
import com.clj.blesample.BluetoothService;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;

import java.util.ArrayList;
import java.util.List;


public class CharacteristicOperationFragment extends Fragment {

    public static final int PROPERTY_READ = 1;
    public static final int PROPERTY_WRITE = 2;
    public static final int PROPERTY_WRITE_NO_RESPONSE = 3;
    public static final int PROPERTY_NOTIFY = 4;
    public static final int PROPERTY_INDICATE = 5;

    private LinearLayout layout_container;

    private List<String> childList = new ArrayList<>();

    private BluetoothService mBluetoothService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothService = ((OperationActivity) getActivity()).getBluetoothService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_characteric_operation, null);
        initView(v);
        return v;
    }

    private void initView(View v) {
        layout_container = (LinearLayout) v.findViewById(R.id.layout_container);
    }

    public void showData() {
        final BluetoothGattCharacteristic characteristic = mBluetoothService.getCharacteristic();
        final int charaProp = mBluetoothService.getCharaProp();
        String child = characteristic.getUuid().toString() + String.valueOf(charaProp);

        for (int i = 0; i < layout_container.getChildCount(); i++) {
            layout_container.getChildAt(i).setVisibility(View.GONE);
        }
        if (childList.contains(child)) {
            layout_container.findViewWithTag(characteristic.getUuid().toString()).setVisibility(View.VISIBLE);
        } else {
            childList.add(child);

            View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation, null);
            view.setTag(characteristic.getUuid().toString());
            LinearLayout layout_add = (LinearLayout) view.findViewById(R.id.layout_add);
            final TextView txt_title = (TextView) view.findViewById(R.id.txt_title);
            txt_title.setText(String.valueOf(characteristic.getUuid().toString() + "的数据变化："));
            final TextView txt = (TextView) view.findViewById(R.id.txt);
            txt.setMovementMethod(ScrollingMovementMethod.getInstance());

            switch (charaProp) {
                case PROPERTY_READ: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    Button btn = (Button) view_add.findViewById(R.id.btn);
                    btn.setText("读");
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mBluetoothService.read(
                                    characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString(),
                                    new BleCharacterCallback() {

                                        @Override
                                        public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    txt.append(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                                                    txt.append("\n");
                                                    int offset = txt.getLineCount() * txt.getLineHeight();
                                                    if (offset > txt.getHeight()) {
                                                        txt.scrollTo(0, offset - txt.getHeight());
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(final BleException exception) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    txt.append(exception.toString());
                                                    txt.append("\n");
                                                    int offset = txt.getLineCount() * txt.getLineHeight();
                                                    if (offset > txt.getHeight()) {
                                                        txt.scrollTo(0, offset - txt.getHeight());
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onInitiatedResult(boolean result) {

                                        }
                                    });
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_WRITE: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_et, null);
                    final EditText et = (EditText) view_add.findViewById(R.id.et);
                    Button btn = (Button) view_add.findViewById(R.id.btn);
                    btn.setText("写");
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String hex = et.getText().toString();
                            if (TextUtils.isEmpty(hex)) {
                                return;
                            }
                            mBluetoothService.write(
                                    characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString(),
                                    hex,
                                    new BleCharacterCallback() {

                                        @Override
                                        public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    txt.append(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                                                    txt.append("\n");
                                                    int offset = txt.getLineCount() * txt.getLineHeight();
                                                    if (offset > txt.getHeight()) {
                                                        txt.scrollTo(0, offset - txt.getHeight());
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(final BleException exception) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    txt.append(exception.toString());
                                                    txt.append("\n");
                                                    int offset = txt.getLineCount() * txt.getLineHeight();
                                                    if (offset > txt.getHeight()) {
                                                        txt.scrollTo(0, offset - txt.getHeight());
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onInitiatedResult(boolean result) {

                                        }

                                    });
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_WRITE_NO_RESPONSE: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_et, null);
                    final EditText et = (EditText) view_add.findViewById(R.id.et);
                    Button btn = (Button) view_add.findViewById(R.id.btn);
                    btn.setText("写");
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String hex = et.getText().toString();
                            if (TextUtils.isEmpty(hex)) {
                                return;
                            }
                            mBluetoothService.write(
                                    characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString(),
                                    hex,
                                    new BleCharacterCallback() {

                                        @Override
                                        public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    txt.append(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                                                    txt.append("\n");
                                                    int offset = txt.getLineCount() * txt.getLineHeight();
                                                    if (offset > txt.getHeight()) {
                                                        txt.scrollTo(0, offset - txt.getHeight());
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(final BleException exception) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    txt.append(exception.toString());
                                                    txt.append("\n");
                                                    int offset = txt.getLineCount() * txt.getLineHeight();
                                                    if (offset > txt.getHeight()) {
                                                        txt.scrollTo(0, offset - txt.getHeight());
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onInitiatedResult(boolean result) {

                                        }

                                    });
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_NOTIFY: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    final Button btn = (Button) view_add.findViewById(R.id.btn);
                    btn.setText("打开通知");
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (btn.getText().toString().equals("打开通知")) {
                                btn.setText("关闭通知");
                                mBluetoothService.notify(
                                        characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString(),
                                        new BleCharacterCallback() {

                                            @Override
                                            public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        txt.append(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                                                        txt.append("\n");
                                                        int offset = txt.getLineCount() * txt.getLineHeight();
                                                        if (offset > txt.getHeight()) {
                                                            txt.scrollTo(0, offset - txt.getHeight());
                                                        }
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure(final BleException exception) {
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        txt.append(exception.toString());
                                                        txt.append("\n");
                                                        int offset = txt.getLineCount() * txt.getLineHeight();
                                                        if (offset > txt.getHeight()) {
                                                            txt.scrollTo(0, offset - txt.getHeight());
                                                        }
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onInitiatedResult(boolean result) {

                                            }

                                        });
                            } else {
                                btn.setText("打开通知");
                                mBluetoothService.stopNotify(
                                        characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString());
                            }
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_INDICATE: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    final Button btn = (Button) view_add.findViewById(R.id.btn);
                    btn.setText("打开通知");
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (btn.getText().toString().equals("打开通知")) {
                                btn.setText("关闭通知");
                                mBluetoothService.indicate(
                                        characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString(),
                                        new BleCharacterCallback() {

                                            @Override
                                            public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        txt.append(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                                                        txt.append("\n");
                                                        int offset = txt.getLineCount() * txt.getLineHeight();
                                                        if (offset > txt.getHeight()) {
                                                            txt.scrollTo(0, offset - txt.getHeight());
                                                        }
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure(final BleException exception) {
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        txt.append(exception.toString());
                                                        txt.append("\n");
                                                        int offset = txt.getLineCount() * txt.getLineHeight();
                                                        if (offset > txt.getHeight()) {
                                                            txt.scrollTo(0, offset - txt.getHeight());
                                                        }
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onInitiatedResult(boolean result) {

                                            }

                                        });
                            } else {
                                btn.setText("打开通知");
                                mBluetoothService.stopIndicate(
                                        characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString());
                            }
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;
            }

            layout_container.addView(view);
        }
    }


}
