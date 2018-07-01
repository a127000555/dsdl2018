package com.arvin.dsdl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

public class CharWrapper {
    public BluetoothGatt gatt;
    public BluetoothGattService service;
//    public BluetoothGattDescriptor descriptor;
    public BluetoothGattCharacteristic characteristic;
    CharWrapper(){

    }
    CharWrapper(BluetoothGatt gatt,
                BluetoothGattService service,
//                BluetoothGattDescriptor descriptor,
                BluetoothGattCharacteristic characteristic){
        this.gatt = gatt;
        this.service = service;
//        this.descriptor = descriptor;
        this.characteristic = characteristic;
    }
}
