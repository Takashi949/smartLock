package com.example.smartlock;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    BtCom btCom;
    TextView textState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.conn).setOnClickListener(this.onClickListener);
        findViewById(R.id.lock).setOnClickListener(this.onClickListener);
        findViewById(R.id.unlock).setOnClickListener(this.onClickListener);
        findViewById(R.id.read).setOnClickListener(this.onClickListener);

        textState = (findViewById(R.id.text_angle));
    }

    @Override
    protected void onResume() {
        super.onResume();
        btCom = new BtCom(getApplicationContext(), this);
        btCom.StartAdv();
    }

    @Override
    protected void onPause() {
        super.onPause();
        btCom.StopAdv();
        btCom = null;
    }
    public void ReadResult(int isUnlock){
        Log.d("MYAPP", String.valueOf(isUnlock));
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(isUnlock == BtCom.UnlockState.UNLOCK.ordinal()){
                            textState.setText("UNLOCK");
                            textState.setBackgroundColor(Color.GREEN);
                        }else if(isUnlock == BtCom.UnlockState.LOCK.ordinal()){
                            textState.setText("LOCKING");
                            textState.setBackgroundColor(Color.RED);
                        }else if(isUnlock == BtCom.UnlockState.UNKNOWN.ordinal()){
                            textState.setText("NEED CMD");
                            textState.setBackgroundColor(Color.BLUE);
                        }
                        textState.invalidate();
                    }
                });
            }
        }).start();
    }
    View.OnClickListener onClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            if(view.getId() == R.id.conn){
                btCom.conn();
            }
            else if(view.getId() == R.id.lock) {
                btCom.lock();
            }
            else if(view.getId() ==  R.id.unlock) {
                btCom.ulock();
            }
            else if(view.getId() == R.id.read){
                btCom.read();
            }
        }
    };
    public void espFound(){
        btCom.read();
    }
}