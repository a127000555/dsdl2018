package com.arvin.dsdl;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTING;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    // Initializes Bluetooth adapter.
    Bitmap bmp = null;

    Context mContext = (Context) this;
    final int REQUEST_ENABLE_BT = 13;
    final String ID_TITLE = "TITLE", ID_SUBTITLE = "SUBTITLE";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
    ArrayList<HashMap<String,String>> ble_list = new ArrayList();
    ArrayList<HashMap<String,String>> characteristic_list = new ArrayList();
    ArrayList<HashMap<String,String>> result_list = new ArrayList();
    HashMap<String,CharWrapper> UUIDToCharWrapper = new HashMap<>();
    String nowTxid = "";
    SimpleAdapter adapter ,adapter2 , resultAdapter;
    BluetoothDevice myDevice ;
    BluetoothGatt remoteGatt ;
    private String getBluetoothMacAddress() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String bluetoothMacAddress = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
            try {
                Field mServiceField = bluetoothAdapter.getClass().getDeclaredField("mService");
                mServiceField.setAccessible(true);

                Object btManagerService = mServiceField.get(bluetoothAdapter);

                if (btManagerService != null) {
                    bluetoothMacAddress = (String) btManagerService.getClass().getMethod("getAddress").invoke(btManagerService);
                }
            } catch (Exception e) {
            }
        } else {
            bluetoothMacAddress = bluetoothAdapter.getAddress();
        }
        return bluetoothMacAddress;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("request permission" , "request successful.");
                    // Permission granted, yay! Start the Bluetooth device scan.
                } else {
                    // Alert the user that this application requires the location permission to perform the scan.
                    Toast.makeText(MainActivity.this,"你最好是給我開藍芽喔",Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener(){
                    //                    @Override 
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

    }
    String mBluetoothDeviceAddress;
    int mConnectionState;
    @Override
    protected void onResume() {
        super.onResume();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        final BluetoothLeScanner mbluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();



        ListView listview = (ListView) findViewById(R.id.bleList);

        adapter= new SimpleAdapter(
                this,
                ble_list,
                android.R.layout.simple_list_item_2,
                new String[] { ID_TITLE, ID_SUBTITLE },
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView arg0, View arg1, int arg2,
                                    long arg3) {
                // TODO Auto-generated method stub
                ListView listView = (ListView) arg0;
//                TextView t = findViewById(R.id.chosen_addr);
//                t.setText( ((HashMap<String,String>)listView.getItemAtPosition(arg2)).get(ID_SUBTITLE));
                final AlertDialog alertDialog = getAlertDialog("確定連線？",((HashMap<String,String>)listView.getItemAtPosition(arg2)).get(ID_SUBTITLE));
                alertDialog.show();

            }
        });
        ListView listview2 = (ListView) findViewById(R.id.characteristic_list);
        adapter2= new SimpleAdapter(
                this,
                characteristic_list,
                android.R.layout.simple_list_item_2,
                new String[] { ID_TITLE, ID_SUBTITLE },
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        listview2.setAdapter(adapter2);
        listview2.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView arg0, View arg1, int arg2,
                                    long arg3) {
                // TODO Auto-generated method stub
                ListView listView = (ListView) arg0;
                final String targetUuid = ((HashMap<String,String>)listView.getItemAtPosition(arg2)).get(ID_TITLE).split("\n")[0];


                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Read or Write?");
                final EditText edit = new EditText(MainActivity.this);
                edit.setText("get wallet address");
                builder.setView(edit);
                builder.setPositiveButton("Write", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e("WriteCharacteristic" , targetUuid);
                        CharWrapper cw =  UUIDToCharWrapper.get(targetUuid);
                        Log.e("peep",cw.toString());
                        for( BluetoothGattDescriptor b : cw.characteristic.getDescriptors()){
                            Log.e("WriteCharacteristic","Descriptor:" + b.getUuid());
                        }

                        cw.gatt.setCharacteristicNotification(cw.characteristic,true);
                        cw.characteristic.setValue(edit.getText().toString().getBytes());
                        Boolean write_result = cw.gatt.writeCharacteristic(cw.characteristic);

                        Log.e("WriteCharacteristic" ,"Result:"  + write_result);
                        if (write_result == true) {
                            Toast.makeText(MainActivity.this, "Update list to get your value." , Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(MainActivity.this, "Did you have read property?" , Toast.LENGTH_SHORT).show();

                        }

                    }
                });
                builder.setNegativeButton("Read", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Log.e("ReadCharacteristic" , targetUuid);
                        CharWrapper cw =  UUIDToCharWrapper.get(targetUuid);
                        Log.e("peep",cw.toString());
                        Boolean read_result = cw.gatt.readCharacteristic(cw.characteristic);
                        Log.e("ReadCharacteristic" ,"Result:"  + read_result);
                        if (read_result == true) {
                            Toast.makeText(MainActivity.this, "Update list to get your value." , Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(MainActivity.this, "Did you have read property?" , Toast.LENGTH_SHORT).show();

                        }
                    }
                });
                builder.setNeutralButton("Paste", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e("WriteCharacteristic" , targetUuid);
                        CharWrapper cw =  UUIDToCharWrapper.get(targetUuid);
                        Log.e("peep",cw.toString());
                        for( BluetoothGattDescriptor b : cw.characteristic.getDescriptors()){
                            Log.e("WriteCharacteristic","Descriptor:" + b.getUuid());
                        }
                        String s = ((TextView)findViewById(R.id.from_server)).getText().toString();
                        String a = "";
                        if(s.length() > 16) {
                            a = s.substring(0, 16);
                            String b = s.substring(16,s.length());
                            ((TextView)findViewById(R.id.from_server)).setText(b);
                        }else{
                            a = s;
                            ((TextView)findViewById(R.id.from_server)).setText("");
                        }
                        cw.gatt.setCharacteristicNotification(cw.characteristic,true);
                        cw.characteristic.setValue(s.getBytes());
                        Boolean write_result = cw.gatt.writeCharacteristic(cw.characteristic);

                        Log.e("WriteCharacteristic" ,"Result:"  + write_result);
                        if (write_result == true) {
                            Toast.makeText(MainActivity.this, "Update list to get your value." , Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(MainActivity.this, "Did you have read property?" , Toast.LENGTH_SHORT).show();

                        }

                    }
                });
                builder.setCancelable(true);
                AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();



            }
        });


        final ListView resultListview = new ListView(MainActivity.this);// (ListView) findViewById(R.id.result_list);

        resultAdapter= new SimpleAdapter(
                this,
                result_list,
                android.R.layout.simple_list_item_2,
                new String[] { ID_TITLE, ID_SUBTITLE },
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        resultListview.setAdapter(resultAdapter);
        String address = null;

//            address = android.provider.Settings.Secure.getString(MainActivity.this.getContentResolver(), "bluetooth_address");
        address = getBluetoothMacAddress();
        Log.e("address" , "get :" + address);
        myDevice = mBluetoothAdapter.getRemoteDevice(address);

        if (myDevice == null) {
            Log.w("connect", "Device not found.  Unable to connect.");
            Toast.makeText(MainActivity.this,"Connect to phone ble failed..." , Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(MainActivity.this,"Connected to phone ble!" , Toast.LENGTH_SHORT).show();
        }

        Log.e("connect" , myDevice.toString());
        mGatt = myDevice.connectGatt(MainActivity.this,true,mGattCallback);
//        Log.e("connect", "Trying to create a new connection." + mGatt.toString());
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        Button startScanBtn = findViewById(R.id.startScanButton);
        Button stopScanBtn = findViewById(R.id.stopScanButton);
        Button updateCharListBtn =  findViewById(R.id.updateCharList);
        Button fromPiToServer = findViewById(R.id.from_pi_to_server);
        ImageButton refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v){
                ((TextView)findViewById(R.id.from_pi)).setText("");
            }
        });

        ImageButton star = findViewById(R.id.star);
        star.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v){
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("txid: " + nowTxid);
                final ImageView img = new ImageView(MainActivity.this);

                new Thread(){
                    @Override
                    public void run(){
                        URL url = null;
                        int hash = 0;
                        for(char c : nowTxid.toCharArray()){
                            hash = ( (hash * 129) + c )% 2000;
                        }
                        hash = (hash + 1512) % 2000;
                        int choose = hash + 18000;
                        // 19512 is good.
                        try {
                            url = new URL("https://www.csie.ntu.edu.tw/~b05902127/img/" + choose+ ".jpg");
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }

                        try {
                            bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }
                }.start();
                while(bmp == null){
                    Log.e("web","while-wating...");
                }
                Log.e("web","loaded!");
                img.setImageBitmap(bmp);


//                img.setImageResource(R.drawable.ic_launcher_background);
                builder.setView(img);
                builder.setPositiveButton("Confirmed", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();
            }
        });
        fromPiToServer.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v){
                String sendMsg = ((TextView)findViewById(R.id.from_pi)).getText().toString();
                String result = sendMsgToServer(sendMsg);
                Log.e("se" , "set up!");
            }
        });
        startScanBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                mbluetoothLeScanner.startScan(scanCallback);
                Toast.makeText(MainActivity.this,"掃描之前，請先膜拜麻麻三次<(_ _)>",Toast.LENGTH_LONG).show();
            }
        });
        stopScanBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                mbluetoothLeScanner.stopScan(scanCallback);
                Toast.makeText(MainActivity.this,"Stop Scanning",Toast.LENGTH_SHORT).show();

            }
        });
        updateCharListBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                adapter2.notifyDataSetChanged();
                resultAdapter.notifyDataSetChanged();
//                resultListview.setSelection(resultAdapter.getCount() - 1);
            }
        });
        Button webConnectBtn = findViewById(R.id.web_connect);
        webConnectBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                sendMsgToServer("Do you know piepie is god?");
            }
        });
    }
    String msgFromServer;
    public String sendMsgToServer(final String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("給定現在的port");    //设置对话框标题
        final EditText edit = new EditText(MainActivity.this);
        edit.setText("44445");
        builder.setView(edit);
        builder.setPositiveButton("Confirmed", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                        Toast.makeText(MainActivity.this, "你输入的是: " + edit.getText().toString(), Toast.LENGTH_SHORT).show();
                try {
                    String stringPort = edit.getText().toString();

                    int port;
                    try{
                        port = Integer.parseInt(stringPort);

                    }catch(Exception e){
                        Toast.makeText(MainActivity.this,"你是不是很天才？",Toast.LENGTH_SHORT).show();
                        return ;
                    }
                    String rawTransaction = Webclient.throwMessageToWeb(port, msg);
                    msgFromServer= rawTransaction;
                    Log.e("web", "get (" + rawTransaction + ")");
                    ((TextView)findViewById(R.id.from_server)).setText(rawTransaction);
                    Log.e("web","setitng");

                }catch (IOException e ){

                }

            }
        });
        builder.setNegativeButton("Canceled", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "我看你是更天才了:)", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        return msgFromServer;
    }

    BluetoothGatt mGatt;

    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onMtuChanged(android.bluetooth.BluetoothGatt gatt, int mtu, int status){
            Log.d("BLE","onMtuChanged mtu="+mtu+",status="+status);
            super.onMtuChanged(gatt, mtu, status);

            Log.d("BLE","onMtuChanged mtu="+mtu+",status="+status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e("onConnectionStateChange" , "onConnectionStateChange" + gatt.toString() + "newState:" + newState +":" + gatt.getDevice().getAddress());

            if (newState == STATE_CONNECTED){
                gatt.discoverServices();
                Log.e("onConnectionStateChange" , "DiscoverServices");
            }else if (newState == STATE_DISCONNECTED){

            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                remoteGatt = gatt;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int mtu = 185;
                    Log.e("mtu", "try to set mtu");
                    try {
                        Log.e("mtu", "request " + mtu + " mtu:" + remoteGatt.requestMtu(mtu));
                    }catch(Exception e){
                        Log.e("mtu" , e.toString());
                    }
                    Log.e("mtu", "try to set mtu finished");
                }

                Log.e("onServicesDiscovered" , "onServicesDiscovered"+gatt.toString() + ", status=" + "Success!");
//                Log.e("onServicesDiscovered" , "ADDR:" + gatt.getDevice().getAddress() + ",UUID:" + gatt.getDevice().getUuids().toString());
                java.util.List<BluetoothGattService> o = gatt.getServices();
                for(BluetoothGattService b : o ){
                    // In service
                    java.util.List<BluetoothGattCharacteristic> i = b.getCharacteristics();
                    for(BluetoothGattCharacteristic c : i){
                        // In characteristic
                        c.getUuid();
                        c = b.getCharacteristic(c.getUuid());
                        String s = c.getStringValue(0);
                        int Intproperties = c.getProperties();
                        String properties = "";
                        if ((Intproperties & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {

                            properties += "Read";
                        }

                        if ((Intproperties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)
                            properties += "/Write";
                        if ((Intproperties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)
                            properties += "/Notify";

                        Log.e("onServicesDiscovered" , "Properties:" + properties);
                        Log.e("onServicesDiscovered" ,  "");
                        HashMap<String,String> item = new HashMap<String,String>();
                        String bType = b.getUuid().toString();
                        if(bType.substring(4,8).equals("1800")){
                            bType = "Generic Access";
                        }else if(bType.substring(4,8).equals("1801")) {
                            bType = "Generic Attribute";
                        }else{
                            bType = "Custom Characteristic";
                        }

                        item.put(ID_TITLE,c.getUuid().toString() + "\n" +bType);
                        item.put(ID_SUBTITLE,properties);
                        characteristic_list.add(item);
                        Log.e("onServicesDiscovered" , item.toString());

                        CharWrapper cw = new CharWrapper(gatt,b,c);
                        UUIDToCharWrapper.put(c.getUuid().toString() , cw);
                        Log.e("onServicesDiscovered" ,"put key:" +  c.getUuid().toString());
                        Log.e("peep",UUIDToCharWrapper.toString());
                    }
                }

            } else {
                Log.w("onServicesDiscovered", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.e("readmtu", ""+gatt.requestMtu(500));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String s = "";
                int now_offset = 0;
                while(true) {
                    String tmp = characteristic.getStringValue(now_offset);
                    now_offset += tmp.length();
                    if (tmp.length() == 0){
                        break;
                    }else{
                        Log.e("onCharacteristicRead" , "length:" + tmp.length());
                    }
                    s += tmp;

                }
                Log.e("onCharacteristicRead" , "length:"  + now_offset);
                String toH = String.format("%x", new BigInteger(1, s.getBytes()));
                String origin = ((TextView)findViewById(R.id.from_pi)).getText().toString();
                ((TextView)findViewById(R.id.from_pi)).setText(origin+s);
                Log.e("Final","text:" + ((TextView)findViewById(R.id.from_pi)).getText().toString());
                HashMap<String,String> item = new HashMap<String,String>();
                item.put(ID_TITLE,characteristic.getUuid().toString());
                item.put(ID_SUBTITLE,s + " | " + toH);
                result_list.add(item);

                Log.e("onCharacteristicRead" , "onCharacteristicRead:"  + s + ",hex:" + toH);
                try {
                    Log.e("onCharacteristicRead", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0).toString());
                }catch(Exception e){}
                try{
                    Log.e("onCharacteristicRead",characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT,0).toString());
                }catch(Exception e){}
                    //                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

            }else{
                switch(status){
                    case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                        Log.e("onCharacteristicRead" , "GATT_READ_NOT_PERMITTED");
                        break;

                    case BluetoothGatt.GATT_FAILURE:
                        Log.e("onCharacteristicRead" , "GATT_FAILURE");
                        break;

                    case BluetoothGatt.GATT_INVALID_OFFSET:
                        Log.e("onCharacteristicRead" , "GATT_INVALID_OFFSET");
                        break;
                    case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                        Log.e("onCharacteristicRead" , "GATT_REQUEST_NOT_SUPPORTED");
                        break;

                    default:
                        Log.e("onCharacteristicRead" , "NANI!? " + status);
                }
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            Log.e("onCharacteristicChanged" , "onCharacteristicChanged:"  + characteristic.toString());
//
        }
        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            Log.e("onCharacteristicWrite" ,"onCharacteristicWrite/" + characteristic.getStringValue(0) +"/" + status );
            switch (status){
                case BluetoothGatt.GATT_SUCCESS:
                    Log.e("onCharacteristicWrite", "Write Success");
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    Log.e("onCharacteristicWrite", "Write Failure");
                    break;
                case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                    Log.e("onCharacteristicWrite", "Not permitted");
                    break;
                default:
                    Log.e("onCharacteristicWrite", "Nani!? " + status);
                    break;

            }
        };

    };
    private ScanCallback scanCallback=new ScanCallback(){
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            byte[] scanData=result.getScanRecord().getBytes();
            Log.e("TAG","Address:" + result.getDevice().getAddress());
            Log.e("TAG","onScanResult :"+result.getScanRecord().toString());
//            if (result.getDevice().getName() == null ){
//                return ;
//            }
            for (HashMap<String,String> i : ble_list){
                if (i.get(ID_SUBTITLE).equals(result.getDevice().getAddress())){
                    return ;
                }
            }
            HashMap<String,String> item = new HashMap<String,String>();
            item.put(ID_TITLE,
                    (result.getDevice().getName() == null ?
                            "UNKNOWN SERVICE!" : result.getDevice().getName()) +
                            " (RSSI:" + result.getRssi() + ")"
            );
            item.put(ID_SUBTITLE,result.getDevice().getAddress());
            ble_list.add(item);
            adapter.notifyDataSetChanged();

        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("onScanFailed" , "ScanFailed... (" + errorCode + ")" );
        }
    };
    private AlertDialog getAlertDialog(String title, final String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String address = message;
                Log.e("connect","Get Address"+address);
                if (address.equals("None")){
                    Toast.makeText(MainActivity.this,"No, you cannot connect empty device."  , Toast.LENGTH_SHORT).show();
                    return ;
                }
                Toast.makeText(MainActivity.this,"Try to connect..." , Toast.LENGTH_SHORT).show();
                BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(address);
                if (remoteDevice == null) {
                    Log.w("connect", "Device not found.  Unable to connect.");
                    Toast.makeText(MainActivity.this,"Connect failed..." , Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this,"Device Connected!" , Toast.LENGTH_SHORT).show();
                }
                Log.e("connect" , remoteDevice.toString());
                mGatt = remoteDevice.connectGatt(MainActivity.this,false,mGattCallback);
//                mGatt.requestMtu(512);
                Log.e("connect", "Trying to create a new connection." + mGatt.toString());
                mBluetoothDeviceAddress = address;
                mConnectionState = STATE_CONNECTING;

//                Toast.makeText(MainActivity.this, "您按下OK按鈕", Toast.LENGTH_SHORT).show();

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "Canceled.", Toast.LENGTH_SHORT).show();
            }
        });
        return builder.create();
    }



}//258467319