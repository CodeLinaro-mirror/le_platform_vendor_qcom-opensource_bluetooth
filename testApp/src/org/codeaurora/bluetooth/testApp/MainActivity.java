/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codeaurora.bluetooth.wearos_ble_testapp;

import android.os.Build;

import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import android.bluetooth.BluetoothAdapter;
import android.app.Activity;
import android.view.View;
import android.view.Menu;
import android.content.Intent;
import android.view.MenuItem;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.view.View;
import android.widget.Button;
import android.util.*;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothManager;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattCharacteristic;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.Message;
import android.os.SystemClock;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;

import android.util.Log;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.Scanner;
import java.lang.*;
import java.util.concurrent.TimeUnit;
import java.io.*;

import libcore.io.IoUtils;


public class MainActivity extends Activity {
    public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR
                                 = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final String TAG = "MainActivity";

    /* Location permissions */
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 2;
    /* MTU size required for MTU exchange */
    public static final int MTU_SIZE_MIN = 23;
    public static final int MTU_SIZE_MAX = 512;
    public static final int TRANSPORT_LE = 2;
    public static final int TA_SM_DEV_FOUND = 2;
    /* Macros to update connection interval before Data Tx */
    public static int required_conn_interval = 0x28;
    public static int required_sup_to = 0xc80;

    public BluetoothManager mBluetoothManager;
    public BleConnectionClass mBleConnect;
    public static TestAppConnectionStateMachine mStateMachine;
    private Context mContext;
    private String conn_item;
    public int mtu_size = MTU_SIZE_MIN;

    /* Mutex required for writing characteristics and descriptors */
    private final Object write_mutex = new Object();
    private final Object discovery_mutex = new Object();

    public static boolean advPresentflag=false;
    public static boolean scanPresentflag=false;
    public static boolean pairFlag=false;
    public static int num_of_notifications = 0;
    public static boolean rxTestDone = false;
    public boolean gatt_discovery_done=false;
    public static boolean was_signalled = false;
    private boolean mRegistered = false;
    public static int connIntervalReq;
    public static long rx_start_time_stamp = 0x0;
    public static long rx_end_time_stamp = 0x0;

    public AdvertiserService mAdvertiseService = null;
    private BluetoothDevice mDevice = null;
    public ScannerService mScannerService=null;
    public static BluetoothAdapter bleAdapter;
    /* Flag indicating whether we have called bind on the service. */
    private boolean boundA=false;
    private boolean boundS=false;
    private boolean statMachinestarted=false;
    private static final int  MAX_ADV_SETS_SUPPORTED = 16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //moveTaskToBack(true);
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "mBluetoothManager is null");
            return;
        }
        /* Check and prompt to user if bluetooth in not turned on */
        if (!initAdapter()) {
            Log.e(TAG, "Bluetooth is not turned on");
            showMessage("Bluetooth is not turned ON");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_REQUEST_FINE_LOCATION);
        }
        /* Create a queue to hold the flags */
        try {
            TextParse T=new TextParse();
            Log.d("Queue CREATED", "SUCCESSFULLY");
            Object o;
            int p=T.objects.size();
            Log.d("size", "size: "+p);
        } catch (Exception e) {
            Log.d("Exception thrown", "Couldn't create Queue");
            e.printStackTrace();
        }

        if(scanPresentflag==true){
            // Bind to the "SCANNER" service
            Log.d("CREATION", "BINDING TO SCANNER ");
            Intent intent = new Intent(this, ScannerService.class);
            startService(intent);
            bindService(intent, mscannerConnection, Context.BIND_AUTO_CREATE);
        }

        if(advPresentflag==true) {
            // Bind to the "ADVERTISER" service
            Log.d("CREATION", "BINDING TO ADVERTISER");
            Intent adv_intent = new Intent(this, AdvertiserService.class);
            startService(adv_intent);
            bindService(adv_intent, madvertiserConnection, Context.BIND_AUTO_CREATE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                            int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission granted!");
                } else {
                    Log.e(TAG, "Needs location permission");
                }
            }
        }
    }

    /* function to check if bluetooth is turned on */
    private boolean initAdapter() {
        bleAdapter = mBluetoothManager.getAdapter();
        if (bleAdapter == null) {
            Log.e(TAG, "bleAdapter is null");
            return false;
        }
        boolean isBtEnabled = bleAdapter.isEnabled();
        if (!isBtEnabled) {
            return false;
        } else {
            Log.e(TAG, "bt is enabled");
            showMessage("Bluetooth is ON");
        }
        return true;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED) {
                   Log.i(TAG, "Device paired");
                   mStateMachine.sendMessage(TestAppConnectionStateMachine
                        .TA_REM_DEV_PAIRED);
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mBleConnect = new BleConnectionClass(getApplicationContext());
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mRegistered = true;
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop called");
        super.onStop();
        // Unbind from the "ADVERTISER" service
        if (boundA) {
            unbindService(madvertiserConnection);
            boundA = false;
            advPresentflag = false;
        }
        //Unbind from the "SCANNER" service
        if (boundS) {
            unbindService(mscannerConnection);
            boundS = false;
            scanPresentflag = false;
        }
        /*Stopping state machine */
        if (mStateMachine != null) {
            mStateMachine.doQuit();
        }
        if(mRegistered) {
            unregisterReceiver(mReceiver);
            mRegistered = false;
        }
    }

    /* function to start testapp state machine */
    private void start_testapp_state_machine() {
        if((boundA == true || boundS == true) && statMachinestarted == false){
          mStateMachine = new TestAppConnectionStateMachine(mContext);
          Log.i("TestAppConnectionStateMachine", "make");
          mStateMachine.start();
        }
    }

    /* Advertiser connection service */
    private ServiceConnection madvertiserConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            AdvertiserService.LocalBinder binderA=(AdvertiserService.LocalBinder) service;
            mAdvertiseService = binderA.getService();
            boundA = true;
            start_testapp_state_machine();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mAdvertiseService = null;
            boundA = false;
        }
    };

    private ServiceConnection mscannerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the scanner service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a scanner, so here we get a client-side
            // representation of that from the raw IBinder object.
            ScannerService.LocalBinder binderS = (ScannerService.LocalBinder) service;
            mScannerService = binderS.getService();
            boundS = true;
            start_testapp_state_machine();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mScannerService = null;
            boundS = false;
        }
    };

    /* function to print the message on display */
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /* Connection Class */
    public class BleConnectionClass {
        private static final String TAG = "BleConnectionClass";

        private BluetoothGatt mBluetoothGatt;
        private BluetoothGattService mService;
        private BluetoothGattCharacteristic mCharacteristic;
        private BluetoothGattCharacteristic mreadChar;
        private Context context;
        private int mState;
        private int GATT_SUCCESS = 0x00;

        public BleConnectionClass(Context context) {
            this.context = context;
        }

        /**
         * GATT callbacks
         */
        private final BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.i(TAG, "onConnectionStateChange device :" + gatt.getDevice() +
                      " status :" + status + " newState :" + newState);
                mState = newState;
                if (gatt.getDevice() == null || status != GATT_SUCCESS) {
                    Log.e(TAG, "onConnectionStateChange:Unexpected error! mstate: " +  mState);
                    mStateMachine.sendMessage(
                                    TestAppConnectionStateMachine.TA_REM_DEV_FAILED_TO_CONNECT);
                    return;
                }
                int bondState = mDevice.getBondState();
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "onConnectionStateChange:DISCONNECTED "
                            + " remoteDevice: " + gatt.getDevice().getAddress());
                    /*Send Message to SM */
                    mStateMachine.sendMessage(
                                    TestAppConnectionStateMachine.TA_REM_DEV_DISCONNECTED);
                    if ((bondState == BluetoothDevice.BOND_BONDED)&& (pairFlag == true)) {
                        mDevice.removeBond();
                        Log.i(TAG, "Device unpaired");
                        pairFlag = false;
                    }
                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "onConnectionStateChange:CONNECTED "
                            + " remoteDevice: " + gatt.getDevice().getAddress());
                    /*Send Message to SM*/
                    mStateMachine.sendMessage(TestAppConnectionStateMachine.TA_REM_DEV_CONNECTED);
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                       Log.i(TAG, "Device paired");
                    }
                    Log.d(TAG, "starting discover services");
                    gatt.discoverServices();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "onService discovery success");
                    gatt_discovery_done = true;
                    synchronized (discovery_mutex) {
                        discovery_mutex.notifyAll();
                    }
                } else {
                        Log.d(TAG, "onServicesDiscovered received: " + status);
                }
            }

            @Override
            public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency,
                                                int timeout, int status) {
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "on Conn updated:"
                         + " interval=" + interval + " latency=" + latency
                        + " timeout=" + timeout + " status=" + status);
                    if(interval == connIntervalReq){
                        Log.d(TAG, "Conn Update Interval matched");
                        mStateMachine.sendMessage(
                                        TestAppConnectionStateMachine.TA_CONNECTION_UPDATED);
                      connIntervalReq = 0;
                    }
                } else {
                    Log.i(TAG, "conn update failed");
                }
            }

            @Override
            public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy,
                                        int status) {
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "on Phy updated:"
                         + " tx phy " + txPhy + " rx phy " + rxPhy +" status " + status);
                    mStateMachine.sendMessage(TestAppConnectionStateMachine.TA_PHY_UPDATED);
                } else {
                    Log.i(TAG, "phy update failed");
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic,
                                                int status){
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "Characteristic read is "+ characteristic.getValue().toString());
                } else {
                    Log.i(TAG, "Char read failed");
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic,
                                                int status) {
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "onCharacteristicWrite: " + status);
                } else {
                    Log.i(TAG, "write characteristic failed");
                }
                synchronized (write_mutex) {
                    was_signalled = true;
                    write_mutex.notify();
                }
             }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt,
                                            BluetoothGattDescriptor desc, int status) {
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "onDescriptorWrite: " + status);
                } else {
                    Log.i(TAG, "write descriptor failed");
                }
                synchronized (write_mutex) {
                    was_signalled = true;
                    write_mutex.notifyAll();
                    Log.d(TAG, "Mutex unlock");
                }
             }

            @Override
            public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                if(status == GATT_SUCCESS){
                    Log.i(TAG, "Read Phy: Tx Phy-"+txPhy+"Rx Phy:"+rxPhy);
                    mStateMachine.sendMessage(
                                   TestAppConnectionStateMachine.TA_PHY_READ);
                } else{
                    Log.i(TAG, "Read Phy failed");
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {
                num_of_notifications ++;
                String charValue = characteristic.getStringValue(0);
                if (charValue.contains("start")) {
                    rx_start_time_stamp = SystemClock.elapsedRealtime();
                    Log.d(TAG, "rx_start_time_stamp:"+rx_start_time_stamp);
                } else {
                    rx_end_time_stamp = SystemClock.elapsedRealtime();
                }
            }

            @Override
            public void onMtuChanged (BluetoothGatt gatt, int mtu, int status) {
               if (status == GATT_SUCCESS) {
                    Log.i(TAG, "Gatt updated MTU" + mtu);
                    mtu_size = mtu;
                    // Message to SM
                    mStateMachine.sendMessage(TestAppConnectionStateMachine.TA_MTU_EXCHANGE_DONE);
                }
            }
        };

        public void connect(BluetoothDevice device){
            if(bleAdapter!=null) {
                Log.i(TAG, "Gatt Connect");
                mDevice = device;
                mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallbacks,TRANSPORT_LE);
            }
        }

        public void pair(){
            if(mDevice.getBondState() != BluetoothDevice.BOND_BONDED){
                Log.i(TAG, "Pairing!");
                if(!mDevice.createBond(TRANSPORT_LE)) {
                    Log.i(TAG, "couldn't start pairing");
                }
            }
        }

        public BluetoothGattService getGattService(UUID serv_uuid){
            BluetoothGattService mService = mBluetoothGatt.getService(serv_uuid);
            if(mService != null){
                Log.d(TAG, "Get Gatt Service");
            } else {
                Log.d(TAG, "Get Gatt Service not found");
            }
            return mService;
        }
    }

    /* function to start service discovery if not yet started */
    public void wait_for_gatt_service_discovery() {
        Log.d(TAG, "Wait for service disc, thread name:" + Thread.currentThread().getName() +
                "thread id:" + Thread.currentThread().getId());
        if(!gatt_discovery_done){
            synchronized (discovery_mutex) {
                try {
                    Log.d(TAG,"discovery lock ");
                    discovery_mutex.wait();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Interrupted while waiting for operation to complete");
                }
            }
        }
    }

    public class TestAppConnectionStateMachine extends StateMachine {

        public static final int TA_BT_ADAPTER_OFF = 1;
        public static final int TA_SM_ADV_STOPPED = 3;
        public static final int TA_REM_DEV_CONNECTED = 4;
        public static final int TA_REM_DEV_FAILED_TO_CONNECT = 5;
        public static final int TA_DEV_DISCONNECT = 6;
        public static final int TA_REM_DEV_DISCONNECTED = 7;
        public static final int TA_TX_TEST_DONE = 8;
        public static final int TA_RX_TEST_DONE = 9;
        public static final int TA_LAT_TEST_DONE = 10;
        public static final int TA_CONNECTION_UPDATED = 11;
        public static final int TA_PHY_UPDATED = 12;
        public static final int TA_PHY_READ = 13;
        public static final int TA_MTU_EXCHANGE_DONE = 14;
        public static final int TA_REM_DEV_PAIRED = 15;

        TextParse txtParse;
        // Test App Connection states.
        private TAIdle mTAIdle;
        private TAConnectPending mTAConnectPending;
        private TAConnected mTAConnected;
        private TADataTx mTADataTx;
        private TADataRx mTADataRx;
        private TALatencyMeasurement mTALatencyMeasurement;
        private TADisconnect mTADisconnect;
        private Context mContext;

        private TestAppConnectionStateMachine(Context context) {
            super("TestAppConnectionStateMachine");
            mContext = context;
            statMachinestarted = true;

            mTAIdle = new TAIdle();
            mTAConnectPending = new TAConnectPending();
            mTAConnected = new TAConnected();
            mTADataTx = new TADataTx();
            mTADataRx = new TADataRx();
            mTALatencyMeasurement = new TALatencyMeasurement();
            mTADisconnect = new TADisconnect();

            addState(mTAIdle);
            addState(mTAConnectPending);
            addState(mTAConnected);
            addState(mTADataTx);
            addState(mTADataRx);
            addState(mTALatencyMeasurement);
            addState(mTADisconnect);

            setInitialState(mTAIdle);
        }

        public void doQuit() {
            Log.i("TestAppConnectionStateMachine", "Quit");
            synchronized (TestAppConnectionStateMachine.this) {
                statMachinestarted = false;
                quitNow();
            }
        }

        private class TAIdle extends State {
            private static final String TAG = "TAIdle";
            ArrayList<ScanFilter> mfilter;
            ScanSettings settings;

            @Override
            public void enter() {
                Log.i(TAG, "Enter ");
                for(int i=1; i <= MAX_ADV_SETS_SUPPORTED; i++){
                    if(!txtParse.objects.isEmpty()) {
                        String flag = (String)txtParse.objects.peek();
                       /* according to the flag send the message to
                          respective service along with class obj */
                       if("advflag".equals(flag)){
                           txtParse.objects.remove();
                           Log.i(TAG, "advflag set, service bound:" + boundA);
                           Adv adv = (Adv)txtParse.objects.remove();
                           mAdvertiseService.startAdvertising(adv);
                           showMessage("Enabling Advertising!");
                       }
                    }
                }
                if(!txtParse.objects.isEmpty()) {
                    String flag = (String)txtParse.objects.peek();
                   if("scanflag".equals(flag)){
                       txtParse.objects.remove();
                       Log.i(TAG, "scanflag set,service bound: " + boundS);
                       Scan scn = (Scan)txtParse.objects.remove();
                       set_scan_parameters(scn);
                       mScannerService.startScan(mfilter, settings);
                   }
                }
                else {
                    Log.d(TAG, "Queue is Empty. Nothing more to do!");
                }
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit");
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);

                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                        break;
                    case TA_SM_DEV_FOUND:
                        BluetoothDevice device = (BluetoothDevice) message.obj;
                        processSMDevFoundEvent(device);
                        break;
                    case TA_SM_ADV_STOPPED:
                         /* ?? Adv has stopped */
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }

            private void processSMDevFoundEvent(BluetoothDevice device) {
                Log.i(TAG, "matchFoundEvent Address:" + device.getAddress());
                if(!txtParse.objects.isEmpty()) {
                    if("Connectflag".equals(txtParse.objects.remove())){
                        mBleConnect.connect(device);
                        transitionTo(mTAConnectPending);
                    }
                }
                else {
                    Log.d(TAG, "Queue is Empty!");
                }
            }

            private void set_scan_parameters(Scan scn) {
                ScanParams params = new ScanParams(scn.DeviceName, scn.DeviceAddress,
                                                    scn.ServiceUuid, scn.SvcMaskUuid,
                                                    scn.ManufacturerId, scn.ManufacturerData,
                                                    scn.ManuMaskData, scn.ServiceDataUuid,
                                                    scn.ServiceData, scn.SvcDataMask,
                                                    scn.ScanMode, scn.CallbackType, scn.ResultType,
                                                    scn.NumOfAdvMatches, scn.MatchMode,
                                                    scn.ReportDelay, scn.ScanTO);
                Log.i(TAG, "cbtpe:" + scn.CallbackType + "scan mode:" + scn.ScanMode +
                        "noOfadvmatches:" + scn.NumOfAdvMatches + "match mode:" + scn.MatchMode +
                        "result type:" + scn.ResultType + "report delay:" + scn.ReportDelay);
                if(params == null) {
                    Log.i(TAG, "params NULL");
                }
                mfilter = params.parseScanFilter();
                if(mfilter == null) {
                    Log.i(TAG, "mfilter NULL");
                }
                settings = params.getScanSettings();
                if(settings == null) {
                    Log.i(TAG, "settings NULL");
                }
            }
        }

        private class TAConnectPending extends State {
            private static final String TAG = "TAConnectPending";

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);
                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                        break;
                    case TA_REM_DEV_CONNECTED:
                        showMessage("Device Connected");
                        if((!txtParse.objects.isEmpty()) && (pairFlag == true)) {
                            if("Pairflag".equals(txtParse.objects.peek())){
                                txtParse.objects.remove();
                                mBleConnect.pair();
                            }
                        }
                        else {
                            Log.d(TAG, "Queue is Empty!");
                        }
                        mScannerService.stopScan();
                        transitionTo(mTAConnected);
                        break;
                    case TA_REM_DEV_FAILED_TO_CONNECT:
                        Log.i(TAG, "Connection failed to establish, please try again");
                        transitionTo(mTAIdle);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }
        }

        private class TAConnected extends State {
            private static final String TAG = "TAConnected";
            private int DATA_TX = 1;
            private int DATA_RX = 2;
            private int LATENCY_MEASUREMENT = 3;
            private int CONN_UPDATE = 4;
            private int DISCONNECT = 5;

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                if((mDevice.getBondState() == BluetoothDevice.BOND_BONDED)||
                    (mDevice.getBondState() == BluetoothDevice.BOND_NONE)){
                    change_state_acc_next_item_in_queue();
                }
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);

                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_REM_DEV_PAIRED:
                        showMessage("Remote device paired");
                        change_state_acc_next_item_in_queue();
                        break;
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                        break;
                    case TA_CONNECTION_UPDATED:
                        Log.d(TAG, "CONNECTION PARAM UPDATED");
                        change_state_acc_next_item_in_queue();
                        break;
                    case TA_PHY_UPDATED:
                        Log.d(TAG, "PHY UPDATED");
                        change_state_acc_next_item_in_queue();
                        break;
                    case TA_PHY_READ:
                        Log.d(TAG, "PHY READ");
                        change_state_acc_next_item_in_queue();
                        break;
                    case TA_DEV_DISCONNECT:
                        transitionTo(mTADisconnect);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }

            private void processConnUpdateReq(){
                /* Pop Connection update parameters */
                ConnUpdate ConnUpdateClass = (ConnUpdate)txtParse.objects.remove();
                Log.i(TAG, "Conn Update");
                /* Android version check */
                try {
                    if (Build.VERSION.SDK_INT >= 28) {
                        mBleConnect.mBluetoothGatt.requestLeConnectionUpdate(
                                                    ConnUpdateClass.ConnIntervalMin,
                                                    ConnUpdateClass.ConnIntervalMax,
                                                    ConnUpdateClass.ConnSlaveLatency,
                                                    ConnUpdateClass.ConnSupTO, 0, 0);
                        connIntervalReq = ConnUpdateClass.ConnIntervalMin;
                    } else {
                        showMessage("Conn Update can't be done");
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Interrupted while waiting for operation to complete");
                }
            }

            private void processReadPhyReq(){
                /* Pop Connection update parameters */
                Log.i(TAG, "Read Phy");
                mBleConnect.mBluetoothGatt.readPhy();
            }

            private void processPhyUpdateReq(){
                /* Pop Connection update parameters */
                PhyUpdate phyUpdate = (PhyUpdate)txtParse.objects.remove();
                Log.i(TAG, "Phy Update");
                mBleConnect.mBluetoothGatt.setPreferredPhy(phyUpdate.txPhy,
                                            phyUpdate.rxPhy, phyUpdate.phyOpt);
            }

            private void change_state_acc_next_item_in_queue() {
                if(!txtParse.objects.isEmpty()) {
                    conn_item = (String)txtParse.objects.remove();
                    Log.d(TAG,"conn_item_flag: " + conn_item.toString());
                 } else {
                    Log.d(TAG, "Queue is Empty!");
                }

                switch(conn_item)
                {
                    case "DataTxflag":
                        Log.d(TAG,"DataTxflag");
                        /* Change Connection Interval required for Data Tx */
                        mBleConnect.mBluetoothGatt.requestLeConnectionUpdate(
                                                required_conn_interval,required_conn_interval,0,
                                                required_sup_to, 0, 0);
                        Log.d(TAG, "Conn Update requested");
                        connIntervalReq = required_conn_interval;
                        transitionTo(mTADataTx);
                        break;
                    case "DataRxflag":
                        Log.d(TAG,"DataRxflag");
                        transitionTo(mTADataRx);
                        break;
                    case "LatencyTestflag":
                        Log.d(TAG,"LatencyTestflag");
                        transitionTo(mTALatencyMeasurement);
                        break;
                    case "ConnUpdateflag":
                        Log.d(TAG,"ConnUpdateflag");
                        processConnUpdateReq();
                        break;
                    case "PhyUpdateflag":
                        Log.d(TAG,"PhyUpdateflag");
                        processPhyUpdateReq();
                        break;
                    case "ReadPhyflag":
                        Log.d(TAG,"ReadPhyflag");
                        processReadPhyReq();
                        break;
                    case "Disconnectflag":
                        /*Disconnect*/
                        Log.d(TAG,"Disconnectflag");
                        transitionTo(mTADisconnect);
                        break;
                    default:
                        Log.i(TAG, "Unknown Operation");
                        break;
                }
            }
        }

        private class TADataTx extends State {
            private static final String TAG = "TADataTx";
            DataTx DataTxClass;
            DataTxthread tt = new DataTxthread();
            Thread t = new Thread(tt);

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                /* read parameters */
                DataTxClass = (DataTx)txtParse.objects.remove();
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);
                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                    case TA_DEV_DISCONNECT:
                        transitionTo(mTADisconnect);
                        break;
                    case TA_TX_TEST_DONE:
                        showMessage("Data Tx Ended");
                        Log.d(TAG, "state change to connected");
                        transitionTo(mTAConnected);
                        break;
                    case TA_REM_DEV_FAILED_TO_CONNECT:
                        break;
                    case TA_MTU_EXCHANGE_DONE:
                        Log.d(TAG,"MTU size exchanged");
                        /* Start the thread */
                        if(!t.isAlive()) {
                            t.start();
                            showMessage("Data Tx Thread Started");
                        } else {
                            Log.i(TAG, "Thread is already running");
                        }
                        break;
                     case TA_CONNECTION_UPDATED:
                        Log.d(TAG, "CONNECTION PARAM UPDATED. MTU size:"+DataTxClass.Mtu_Size);
                        /*MTU exchange*/
                        if(mBleConnect.mBluetoothGatt.requestMtu(DataTxClass.Mtu_Size)) {
                            Log.i(TAG, "MTU size requested to max size");
                        }
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }

            public class DataTxthread implements Runnable {

                @Override
                public void run() {
                    Log.i(TAG, "data tx thread start");
                    final UUID UUID_TX_SERVICE = UUID.fromString(DataTxClass.txService);
                    final UUID UUID_TX_CHAR = UUID.fromString(DataTxClass.txChar);

                    wait_for_gatt_service_discovery();
                    /* wait for 500ms for DLE event to be received */
                    try {
                        Thread.sleep(500);
                    } catch(InterruptedException e){
                        Log.e(TAG, "error in thread sleep");
                    }

                    int mtu_intr_size = DataTxClass.Packet_Size;
                    /* Set the packet size to maximum possible if it exceeds mtu size */
                    if(mtu_intr_size > (DataTxClass.Mtu_Size - 3)) {
                        mtu_intr_size = DataTxClass.Mtu_Size - 3;
                    }
                    char[] str = new char[mtu_intr_size];
                    long length = mtu_intr_size * (DataTxClass.Num_Packets);
                    BluetoothGattService mService =
                                            mBleConnect.mBluetoothGatt.getService(UUID_TX_SERVICE);
                    if (mService != null) {
                        BluetoothGattCharacteristic mCharacteristic =
                                                        mService.getCharacteristic(UUID_TX_CHAR);
                        if (mCharacteristic != null) {
                            /* Filling the array with data */
                            str[0]  = 41; str[1] = 42; str[2] = 43;
                            Arrays.fill(str,3, mtu_intr_size-1,(char)'d');
                            try {
                                Process proc =
                                    Runtime.getRuntime().exec("/system/bin/getprop"+" "
                                                            + "bt.tx_test.enable");
                                BufferedReader reader =
                                                new BufferedReader(
                                                    new InputStreamReader(proc.getInputStream()));
                                String readLine = reader.readLine();
                                /* If system property is set to false,
                                   continue with sending the data from the app */
                                if(readLine.equals("false")) {
                                    long tx_start_time_stamp = SystemClock.elapsedRealtime();
                                    mCharacteristic.setWriteType(
                                            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                                    Log.d(TAG, "chardata len:"+length+
                                            "tx start time:"+tx_start_time_stamp);
                                    for (long i = 1; i <= (DataTxClass.Num_Packets - 1) ; i++) {
                                        /*Max packet size that can be sent using
                                         write without response is MTU-3 Bytes*/
                                        mCharacteristic.setValue(String.valueOf(str));
                                        mBleConnect.mBluetoothGatt.writeCharacteristic(
                                                                            mCharacteristic);
                                        synchronized (write_mutex) {
                                            // Wait for write response
                                            if(!was_signalled) {
                                                try {
                                                    write_mutex.wait();
                                                } catch (InterruptedException e) {
                                                    Log.d(TAG, "Interrupted while waiting");
                                                }
                                            }
                                            was_signalled = false;
                                        }
                                     }
                                     /* Write the last packet with response */
                                    mCharacteristic.setWriteType(
                                                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                                    mCharacteristic.setValue(String.valueOf(str));
                                    mBleConnect.mBluetoothGatt.writeCharacteristic(
                                                                    mCharacteristic);
                                    long tx_intr_time_stamp = SystemClock.elapsedRealtime();
                                    Log.d(TAG, "Intr time stamp:"+tx_intr_time_stamp);
                                    synchronized (write_mutex) {
                                        // Wait for write response
                                        if(!was_signalled){
                                            try {
                                                write_mutex.wait();
                                            } catch (InterruptedException e) {
                                                Log.d(TAG, "Interrupted while waiting");
                                            }
                                        }
                                        was_signalled = false;
                                    }
                                    long tx_end_time_stamp = SystemClock.elapsedRealtime();
                                    Log.d(TAG, "chardata len:"+length +
                                            "tx end time:"+tx_end_time_stamp);
                                    float txTput = (((float) length /
                                            ((float) (tx_end_time_stamp - tx_start_time_stamp)))
                                                *8 *1000);
                                    float txTputk = txTput / 1000;
                                    float txTputm = txTputk / 1000;
                                    Log.d(TAG, "Tx tput in kbps: " + txTputk +
                                            " in mbps: " + txTputm);
                                    /* Another Tx Throughput value,
                                        since response could take some time */
                                    Log.d(TAG, "chardata len:"+length + "tx intr time:"
                                                            +tx_intr_time_stamp);
                                    float txTputr = (((float) length /
                                            ((float) (tx_intr_time_stamp - tx_start_time_stamp)))
                                            *8 *1000);
                                    float txTputkr = txTputr / 1000;
                                    float txTputmr = txTputkr / 1000;
                                    Log.d(TAG, "Intr Tx tput in kbps: "+txTputkr+
                                                " in mbps: "+txTputmr);
                                } else {
                                    /* If Property is set to true, send one packet from app,
                                       rest of the packets from bta */
                                    mCharacteristic.setWriteType(
                                              BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                                    mCharacteristic.setValue(String.valueOf(str));
                                    mBleConnect.mBluetoothGatt.writeCharacteristic(
                                                                    mCharacteristic);
                                    synchronized (write_mutex) {
                                        // Wait for write response
                                        if(!was_signalled){
                                            try {
                                                write_mutex.wait();
                                            } catch (InterruptedException e) {
                                                Log.d(TAG, "Interrupted while waiting");
                                            }
                                        }
                                        was_signalled = false;
                                    }
                                }
                                /* Signal SM that TX Test is done*/
                                mStateMachine.sendMessage(
                                            TestAppConnectionStateMachine.TA_TX_TEST_DONE);
                            } catch (IOException e) {
                                Log.e(TAG, "Exception handling");;
                            }
                        } else {
                            Log.e(TAG, "Characteristic is null!");
                        }
                    } else {
                        Log.d(TAG, "Service with UUID not found");
                    }
                }
            }
        }

        private class TADataRx extends State {
            private static final String TAG = "TADataRx";
            DataRx DataRxClass;

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                /* read parameters */
                DataRxClass = (DataRx)txtParse.objects.remove();
                DataRxthread rt = new DataRxthread();
                Thread t = new Thread(rt);
                if(!t.isAlive()) {
                    t.start();
                    showMessage("Data Rx Thread Started");
                } else {
                    Log.i(TAG, "Thread is already running");
                }
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);
                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                    case TA_DEV_DISCONNECT:
                        transitionTo(mTADisconnect);
                        break;
                    case TA_RX_TEST_DONE:
                        showMessage("Data Rx Ended");
                        Log.d(TAG, "state change to connected");
                        transitionTo(mTAConnected);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }

            public class DataRxthread implements Runnable {

                public void run() {
                    Log.i(TAG, "data rx thread start");
                    final UUID UUID_RX_SERVICE = UUID.fromString(DataRxClass.rxService);
                    final UUID UUID_RX_CHAR = UUID.fromString(DataRxClass.rxChar);
                    final UUID UUID_CCCD = CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR;
                    int rx_data_size = 244;
                    wait_for_gatt_service_discovery();
                    //battery service and characteristic
                    BluetoothGattService mService =
                                            mBleConnect.mBluetoothGatt.getService(UUID_RX_SERVICE);
                    if (mService != null) {
                        BluetoothGattCharacteristic mreadChar =
                                                        mService.getCharacteristic(UUID_RX_CHAR);
                        if (mreadChar != null) {
                            Log.i(TAG, "Found Characteristic: " + mreadChar.getUuid().toString());
                            //enable cccd to send notification
                            mBleConnect.mBluetoothGatt.setCharacteristicNotification(
                                                                    mreadChar, true);
                            BluetoothGattDescriptor descriptor =
                                                            mreadChar.getDescriptor(UUID_CCCD);
                            if (descriptor != null) {
                                //start of writing
                                descriptor.setValue(
                                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mBleConnect.mBluetoothGatt.writeDescriptor(descriptor);
                                synchronized (write_mutex) {
                                    // Wait for write response
                                    if(!was_signalled){
                                        try {
                                            write_mutex.wait();
                                        } catch (InterruptedException e) {
                                            Log.d(TAG, "Interrupted while waiting");
                                        }
                                    }
                                    was_signalled = false;
                                }
                            } else {
                                Log.e(TAG, "Descriptor not found");
                            }
                            /* write notifications time to the characteristic */
                            mreadChar.setValue(
                                    (int)DataRxClass.NotificationsTime,
                                    BluetoothGattCharacteristic.FORMAT_UINT32,0);
                            mBleConnect.mBluetoothGatt.writeCharacteristic(mreadChar);
                            synchronized (write_mutex) {
                                // Wait for write response
                                if(!was_signalled) {
                                    try {
                                        write_mutex.wait();
                                    } catch (InterruptedException e) {
                                        Log.d(TAG, "Interrupted while waiting");
                                    }
                                }
                                was_signalled = false;
                            }
                            /* wait for DataRxClass.NotificationsTime seconds
                               before disabling notifications */
                            try {
                                Log.d(TAG, "Sleep for :"+DataRxClass.NotificationsTime+
                                        " in sec");
                                Thread.sleep((DataRxClass.NotificationsTime)*1000);
                            }
                            catch(InterruptedException e){
                                Log.e(TAG, "error in thread sleep");
                            }

                            //disable cccd
                            mBleConnect.mBluetoothGatt.setCharacteristicNotification(
                                                    mreadChar, false);
                            descriptor.setValue(
                                       BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                            mBleConnect.mBluetoothGatt.writeDescriptor(descriptor);
                            synchronized (write_mutex) {
                            //Wait for write response
                                if(!was_signalled){
                                    try {
                                        Log.d(TAG, "mutex lock");
                                        write_mutex.wait();
                                    }
                                    catch (InterruptedException e) {
                                        Log.d(TAG, "Interrupted while waiting");
                                    }
                                }
                                was_signalled = false;
                            }
                            Log.d(TAG, "start time"+rx_start_time_stamp+"end time:"+
                                rx_end_time_stamp+"num of notifications:"+num_of_notifications);
                            float rxTput = (((float) num_of_notifications * rx_data_size *8*1000)
                                        /((float) (rx_end_time_stamp - rx_start_time_stamp)));
                            float rxTputk = rxTput / 1000;
                            float rxTputm = rxTputk / 1000;
                            Log.d(TAG, "Rx tput in kbps: " + rxTputk + " in mbps: " + rxTputm);
                            rx_start_time_stamp = 0;
                            rx_end_time_stamp = 0;
                            num_of_notifications = 0;
                            /* Signal SM that TX Test is done*/
                              mStateMachine.sendMessage(
                                            TestAppConnectionStateMachine.TA_RX_TEST_DONE);
                        } else {
                            Log.d(TAG, "Characteristic is null!");
                        }
                    } else {
                        Log.d(TAG, "Service with uuid is not found");
                    }
                }
            }
        }

        private class TALatencyMeasurement extends State {
            private static final String TAG = "TALatencyMeasurement";
            LatencyTest LatClass;

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                LatClass = (LatencyTest)txtParse.objects.remove();
                LatencyTestthread lt = new LatencyTestthread();
                Thread t = new Thread(lt);
                if(!t.isAlive()) {
                    t.start();
                    showMessage("Latency Measurement Started");
                } else {
                    Log.i(TAG, "Thread is already running");
                }
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);
                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                    case TA_DEV_DISCONNECT:
                        transitionTo(mTADisconnect);
                        break;
                    case TA_LAT_TEST_DONE:
                        showMessage("Latency Test Ended");
                        Log.d(TAG, "state change to connected");
                        transitionTo(mTAConnected);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }
            class LatencyTestthread implements Runnable {

                public void run() {
                    final UUID UUID_LAT_SERVICE = UUID.fromString(LatClass.latService);
                    final UUID UUID_LAT_CHAR = UUID.fromString(LatClass.latChar);
                    Log.i(TAG, "Latency test start");
                    wait_for_gatt_service_discovery();
                    String str = "LatencyTest";
                    BluetoothGattService mService =
                                        mBleConnect.mBluetoothGatt.getService(UUID_LAT_SERVICE);
                    if (mService != null) {
                        BluetoothGattCharacteristic mCharacteristic =
                                                    mService.getCharacteristic(UUID_LAT_CHAR);
                        if (mCharacteristic != null) {
                            mCharacteristic.setWriteType(
                                                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            mCharacteristic.setValue(str);
                            long latency_start_time_stamp = SystemClock.elapsedRealtime();
                            mBleConnect.mBluetoothGatt.writeCharacteristic(mCharacteristic);
                            synchronized (write_mutex) {
                                // Wait for write response
                                if(!was_signalled) {
                                    try {
                                        write_mutex.wait();
                                    } catch (InterruptedException e) {
                                        Log.d(TAG, "Interrupted while waiting");
                                    }
                                }
                                was_signalled = false;
                            }
                            long latency_end_time_stamp = SystemClock.elapsedRealtime();
                            float latency = (float) (latency_end_time_stamp -
                                                        latency_start_time_stamp);
                            Log.d(TAG, "Latency in msec " + latency);
                            mStateMachine.sendMessage(
                                            TestAppConnectionStateMachine.TA_LAT_TEST_DONE);
                        } else {
                            Log.e(TAG, "Characteristic is null!");
                        }
                    } else {
                        Log.d(TAG, "Service with UUID not found ");
                    }
                }
            }
        }

        private class TADisconnect extends State {
            private static final String TAG = "TADisconnect";

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                disconnect();
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }

            private void disconnect() {
                mBleConnect.mBluetoothGatt.disconnect();
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);
                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                    case TA_REM_DEV_DISCONNECTED:
                        transitionTo(mTAIdle);
                        showMessage("Device Disconnected");
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }
        }
    }
}
