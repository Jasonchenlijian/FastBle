package com.clj.blesample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_operate).setOnClickListener(this);
        findViewById(R.id.btn_demo).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_operate:
                startActivity(new Intent(MainActivity.this, OperateActivity.class));
                break;

            case R.id.btn_demo:
                startActivity(new Intent(MainActivity.this, DemoActivity.class));
                break;
        }
    }
}
