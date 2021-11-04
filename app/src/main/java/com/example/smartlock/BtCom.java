package com.example.smartlock;

import static androidx.core.content.ContextCompat.getSystemService;

import static java.security.AccessController.getContext;

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
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

public class BtCom {
    enum UnlockState{
        LOCK,UNLOCK,UNKNOWN
    };
    final UUID service_uuid = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914c");
    final UUID charc_uuid = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9");

    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;

    private BluetoothDevice btesp;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;

    private final Context context;
    private MainActivity mainActivity;
    private LockTile lockTile;

    boolean isEspFound = false;

    ArrayList<ScanFilter> scanFilters;
    ScanSettings scanSettings;

    BtCom(Context context, MainActivity mainActivity){
        this.mainActivity = mainActivity;
        this.context = context;
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(service_uuid.toString())).build();
        scanFilters = new ArrayList<>();
        scanFilters.add(scanFilter);
        scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
    }
    BtCom(Context context, LockTile mainActivity){
        this.lockTile = mainActivity;
        this.context = context;
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(service_uuid.toString())).build();
        scanFilters = new ArrayList<>();
        scanFilters.add(scanFilter);
        scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
    }

    private void callResult(int isUnlock){
        if(mainActivity != null)mainActivity.ReadResult(isUnlock);
        else if(lockTile != null){
            lockTile.readResult(isUnlock);
        }
    }
    private void callFound(){
        if(lockTile != null)lockTile.espFound();
        else if(mainActivity != null)mainActivity.espFound();
    }
    private void callLost(){
        if(lockTile != null)lockTile.espLost();
    }

    void StartAdv(){
        bluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters, scanSettings, scanCallback);
    }
    void StopAdv(){
        isEspFound =false;
        bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
    }
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("MYAPP", String.valueOf(errorCode));
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d("MYAPP", "発見");
            btesp = result.getDevice();

            btesp.connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
        }
    };
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("MYAPP", "dev found");
            if(newState == BluetoothProfile.STATE_CONNECTED || newState == BluetoothProfile.STATE_CONNECTING){
                Log.d("MYAPP", "サービスを、検索");
                bluetoothGatt = gatt;
                gatt.discoverServices();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                Log.d("MYAPP", "サービスを、削除");
                callLost();
                isEspFound = false;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d("MYAPP", "discovered");
            if (status == BluetoothGatt.GATT_SUCCESS){
                bluetoothGattCharacteristic = gatt.getService(service_uuid) .getCharacteristic(charc_uuid);
                if (bluetoothGattCharacteristic != null){
                    bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                    Log.d("MYAPP", "charc found");
                    isEspFound =true;
                    callFound();
                }
            }
            else if(status == BluetoothGatt.GATT_FAILURE)Log.e("MYAPP", "ディスカバーフェイル");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                bluetoothGatt.readCharacteristic(characteristic);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                int isUnlock = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                Log.d("MYAPP", String.valueOf(isUnlock));
                callResult(isUnlock);
            }
        }
    };


    void conn(){
        this.StartAdv();
    }
    void lock(){
        if (!isEspFound) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothGattCharacteristic.setValue("lock1");
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                    Log.d("MYAPP", "lock1");
                } catch (Exception e) {
                    Log.e("MYAPP", e.getMessage());
                }
            }
        }).start();
    }
    void ulock(){
        if (!isEspFound) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothGattCharacteristic.setValue("unlock1");
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                    Log.d("MYAPP", "unlock1");
                } catch (Exception e) {
                    Log.e("MYAPP", e.getMessage());
                }
            }
        }).start();
    }
    void read() {
        if(isEspFound){
            bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
        }
    }
}
