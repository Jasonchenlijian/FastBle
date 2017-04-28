package com.clj.blesample.tool;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.clj.blesample.R;


public class EditDialog extends Dialog {

    private EditText et_write;
    private Button btn_manual_ok;
    private Button btn_manual_cancel;

    private OnDialogClickListener mListener;

    public EditDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_edit);

        setCanceledOnTouchOutside(false);

        et_write = (EditText) findViewById(R.id.et_write);
        btn_manual_ok = (Button) findViewById(R.id.btn_manual_ok);
        btn_manual_cancel = (Button) findViewById(R.id.btn_manual_cancel);

        btn_manual_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                String writeData = et_write.getText().toString().trim();
                if (TextUtils.isEmpty(writeData)) {
                    if (mListener != null) {
                        mListener.onEditErrorClick();
                    }
                } else {
                    if (mListener != null) {
                        mListener.onEditOkClick(writeData);
                    }
                }
            }
        });

        btn_manual_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    public void setOnDialogClickListener(OnDialogClickListener l) {
        mListener = l;
    }

    public interface OnDialogClickListener {
        void onEditOkClick(String writeData);

        void onEditErrorClick();

    }
}
