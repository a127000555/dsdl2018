package com.arvin.dsdl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

public class BLE {
    // BLEDemo主要代碼
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothGatt mBtGatt = null;
    private int mState = 0;
    private Context mContext;
    private BluetoothGattCharacteristic mWriteCharacteristic = null;
    private BluetoothGattCharacteristic mReadCharacteristric = null;

    private final String TAG = "BLE_Demo";

    // 設備連接狀態
    private final int CONNECTED = 0x01;
    private final int DISCONNECTED = 0x02;
    private final int CONNECTTING = 0x03;

    // 讀寫相關的Service、Characteristic的UUID
    public static final UUID TRANSFER_SERVICE_READ = UUID.fromString("34567817-2432-5678-1235-3c1d5ab44e17");
    public static final UUID TRANSFER_SERVICE_WRITE = UUID.fromString("34567817-2432-5678-1235-3c1d5ab44e18");
    public static final UUID TRANSFER_CHARACTERISTIC_READ = UUID.fromString("23487654-5678-1235-2432-3c1d5ab44e94");
    public static final UUID TRANSFER_CHARACTERISTIC_WRITE = UUID.fromString("23487654-5678-1235-2432-3c1d5ab44e93");

    // BLE設備連接通信過程中回調
    private BluetoothGattCallback mBtGattCallback = new BluetoothGattCallback() {

        // 連接狀態發生改變時的回調
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mState = CONNECTED;
                Log.d(TAG, "connected OK");
                mBtGatt.discoverServices();
            } else if (newState == BluetoothGatt.GATT_FAILURE) {
                mState = DISCONNECTED;
                Log.d(TAG, "connect failed");
            }
        }

        // 遠端設備中的服務可用時的回調
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService btGattWriteService = mBtGatt
                        .getService(TRANSFER_SERVICE_WRITE);
                BluetoothGattService btGattReadService = mBtGatt
                        .getService(TRANSFER_SERVICE_READ);
                if (btGattWriteService != null) {
                    mWriteCharacteristic = btGattWriteService
                            .getCharacteristic(TRANSFER_CHARACTERISTIC_WRITE);
                }
                if (btGattReadService != null) {
                    mReadCharacteristric = btGattReadService
                            .getCharacteristic(TRANSFER_CHARACTERISTIC_READ);
                    if (mReadCharacteristric != null) {
                        mBtGatt.readCharacteristic(mReadCharacteristric);
                    }
                }
            }
        }

        // 某Characteristic的狀態為可讀時的回調
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBtGatt.readCharacteristic(characteristic);

                // 訂閱遠端設備的characteristic，
                // 當此characteristic發生改變時當回調mBtGattCallback中的onCharacteristicChanged方法
                mBtGatt.setCharacteristicNotification(mReadCharacteristric,
                        true);
                BluetoothGattDescriptor descriptor = mReadCharacteristric
                        .getDescriptor(UUID
                                .fromString("00002902-0000-1000-8000-00805f9b34fb"));
                if (descriptor != null) {
                    byte[] val = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                    descriptor.setValue(val);
                    mBtGatt.writeDescriptor(descriptor);
                }
            }
        }

        // 寫入Characteristic成功與否的回調
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {

            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    Log.d(TAG, "write data success");
                    break;// 寫入成功
                case BluetoothGatt.GATT_FAILURE:
                    Log.d(TAG, "write data failed");
                    break;// 寫入失敗
                case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                    Log.d(TAG, "write not permitted");
                    break;// 沒有寫入的權限
            }
        }

        // 訂閱了遠端設備的Characteristic信息後，
        // 當遠端設備的Characteristic信息發生改變後,回調此方法
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            mBtGatt.readCharacteristic(characteristic);
        }

    };

}
