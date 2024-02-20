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

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.app.Activity;
import android.app.ActivityManager;
import android.widget.Toast;
import android.Manifest;
import android.util.Log;
import android.util.*;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.ServiceConnection;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.os.Bundle;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.Message;
import android.os.SystemClock;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothManager;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    public static int LOG_LEVEL = 6;

    public BluetoothManager mBluetoothManager;
    public static BluetoothAdapter bleAdapter;

    public static ThroughputStateMachine throughputSMClass;
    public static GattClient mgattclient;
    public static GattServer mgattserver;
    public static MainActivityMessageHandler msghandler;

    private Context mAppContext;
    public static boolean isActivityRunning = false;

    /* Location permissions */
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 2;

    public static boolean stateMachinestarted = false;

    /* Flag indicating whether we have called bind on the service. */
    public static boolean boundA = false;
    public static  boolean boundS = false;

    public static boolean mReceiverRegistered;

    public static AdvertiserService mAdvertiseService = null;
    public static ScannerService mScannerService = null;

    public static SocketServer socServer = null;

    /* Variable to keep track of calling source of scan
     (MainActivity or Gatt Client or Throughput SM) */
    public static int scan_called = 0;
    public static final int SCAN_CALLED_FROM_MAIN_ACTIVITY = 1;
    private static final int SCAN_CALLED_FROM_GATT_CLIENT = 2;
    private static final int SCAN_CALLED_FROM_THROUGHPUT_SM = 3;

    public static WakeLock wl;
    public static boolean wl_acquired=false;
    public Looper mlooper;

    /* Variable to keep track of calling source of pair request
     (MainActivity or Gatt Client or Throughput SM) */
    public static int pairing_called = 0;
    public static final int PAIRING_REQ_FROM_THROUGHPUT_SM = 1;
    public static final int PAIRING_REQ_FROM_GATT_CLIENT = 2;

    /* Main Activity Actions */
    public static final int MSG_MA_START_BLE_ADV = 0;
    public static final int MSG_MA_STOP_BLE_ADV = 1;
    public static final int MSG_MA_START_BLE_SCAN = 2;
    public static final int MSG_MA_STOP_BLE_SCAN = 3;
    public static final int MSG_MA_SCAN_DEV_FOUND = 4;
    public static final int MSG_MA_ADV_STARTED = 5;
    public static final int MSG_MA_ADV_STOPPED = 6;
    public static final int MSG_MA_MAX_ACTION_VALUE = MSG_MA_ADV_STOPPED;

    /* Gatt Client Actions */
    public static final int MSG_GC_START_BLE_CONNECT = MSG_MA_MAX_ACTION_VALUE + 1;
    public static final int MSG_GC_START_BLE_CONN_UPDATE = MSG_MA_MAX_ACTION_VALUE + 2;
    public static final int MSG_GC_START_BLE_PHY_UPDATE = MSG_MA_MAX_ACTION_VALUE + 3;
    public static final int MSG_GC_START_BLE_GATT_CONFIGURE_MTU_SIZE = MSG_MA_MAX_ACTION_VALUE + 4;
    public static final int MSG_GC_START_BLE_READ_PHY = MSG_MA_MAX_ACTION_VALUE + 5;
    public static final int MSG_GC_START_BLE_PAIR = MSG_MA_MAX_ACTION_VALUE + 6;
    public static final int MSG_GC_START_BLE_UNPAIR = MSG_MA_MAX_ACTION_VALUE + 7;
    public static final int MSG_GC_START_BLE_GATT_DISCOVER = MSG_MA_MAX_ACTION_VALUE + 8;
    public static final int MSG_GC_START_BLE_GATT_REFRESH_SERVICES = MSG_MA_MAX_ACTION_VALUE + 9;
    public static final int MSG_GC_START_BLE_GATT_WRITE_READ_CHAR = MSG_MA_MAX_ACTION_VALUE + 10;
    public static final int MSG_GC_START_BLE_GATT_WRITE_READ_DESC = MSG_MA_MAX_ACTION_VALUE + 11;
    public static final int MSG_GC_REGISTER_BLE_GATT_NOTIFICATIONS = MSG_MA_MAX_ACTION_VALUE + 12;
    public static final int MSG_GC_DEREGISTER_BLE_GATT_NOTIFICATIONS = MSG_MA_MAX_ACTION_VALUE + 13;
    public static final int MSG_GC_START_BLE_GATT_RELIABLE_WRITE = MSG_MA_MAX_ACTION_VALUE + 14;
    public static final int MSG_GC_START_BLE_GATT_ABORT_RELIABLE_WRITE = MSG_MA_MAX_ACTION_VALUE + 15;
    public static final int MSG_GC_START_BLE_GATT_DISC = MSG_MA_MAX_ACTION_VALUE + 16;
    public static final int MSG_GC_START_BLE_GATT_CANCEL_CONNECT = MSG_MA_MAX_ACTION_VALUE + 17;
    public static final int MSG_GC_MAX_ACTION_VALUE = MSG_GC_START_BLE_GATT_CANCEL_CONNECT;

    /* State Machine Actions */
    public static final int MSG_SM_START_BLE_CONNECT = MSG_GC_MAX_ACTION_VALUE + 1;
    public static final int MSG_SM_START_BLE_CONN_UPDATE = MSG_GC_MAX_ACTION_VALUE + 2;
    public static final int MSG_SM_START_BLE_PHY_UPDATE = MSG_GC_MAX_ACTION_VALUE + 3;
    public static final int MSG_SM_START_BLE_READ_PHY = MSG_GC_MAX_ACTION_VALUE + 4;
    public static final int MSG_SM_START_BLE_PAIR = MSG_GC_MAX_ACTION_VALUE + 5;
    public static final int MSG_SM_START_BLE_UNPAIR = MSG_GC_MAX_ACTION_VALUE + 6;
    public static final int MSG_SM_START_BLE_DATA_TX_TEST = MSG_GC_MAX_ACTION_VALUE + 7;
    public static final int MSG_SM_START_BLE_DATA_RX_TEST = MSG_GC_MAX_ACTION_VALUE + 8;
    public static final int MSG_SM_START_BLE_LATENCY_TEST = MSG_GC_MAX_ACTION_VALUE + 9;
    public static final int MSG_SM_START_BLE_GATT_DISC = MSG_GC_MAX_ACTION_VALUE + 10;
    public static final int MSG_SM_MAX_ACTION_VALUE = MSG_SM_START_BLE_GATT_DISC;

    /* GATT Server Actions */
    public static final int MSG_GS_START_BLE_ADD_SERVICE = MSG_SM_MAX_ACTION_VALUE + 1;
    public static final int MSG_GS_START_BLE_REMOVE_SERVICE = MSG_SM_MAX_ACTION_VALUE + 2;
    public static final int MSG_GS_START_BLE_SET_PHY = MSG_SM_MAX_ACTION_VALUE + 3;
    public static final int MSG_GS_START_BLE_READ_PHY = MSG_SM_MAX_ACTION_VALUE + 4;
    public static final int MSG_GS_START_BLE_GET_SERVICES = MSG_SM_MAX_ACTION_VALUE + 5;
    public static final int MSG_GS_START_BLE_CLEAR_SERVICES = MSG_SM_MAX_ACTION_VALUE + 6;
    public static final int MSG_GS_START_BLE_CONNECT = MSG_SM_MAX_ACTION_VALUE + 7;
    public static final int MSG_GS_START_BLE_PHY_UPDATE = MSG_SM_MAX_ACTION_VALUE + 8;
    public static final int MSG_GS_START_GET_CONNECTED_DEVICES = MSG_SM_MAX_ACTION_VALUE + 9;
    public static final int MSG_GS_MAX_ACTION_VALUE = MSG_GS_START_GET_CONNECTED_DEVICES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAppContext = getApplicationContext();

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            wl_acquired = savedInstanceState.getBoolean("wl_acquired");
            Log.d(TAG, "on create savedInstance not null");
        } else {
            Log.d(TAG, "on create savedInstance null");
            PowerManager pm = (PowerManager)mAppContext.getSystemService(
                                              Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");

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

            /* Request for location access */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSION_REQUEST_FINE_LOCATION);
            }

            socServer = SocketServer.getInstance(mAppContext);

            // Bind to the "SCANNER" service
            Log.d("CREATION", "BINDING TO SCANNER ");
            Intent intent = new Intent(this, ScannerService.class);
            startService(intent);
            bindService(intent, mscannerConnection, Context.BIND_AUTO_CREATE);

            // Bind to the "ADVERTISER" service
            Log.d("CREATION", "BINDING TO ADVERTISER");
            Intent adv_intent = new Intent(this, AdvertiserService.class);
            startService(adv_intent);
            bindService(adv_intent, madvertiserConnection, Context.BIND_AUTO_CREATE);

            IntentFilter Pairingfilter = new IntentFilter();
            Pairingfilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            registerReceiver(mPairingReceiver, Pairingfilter);
            mReceiverRegistered = true;

            HandlerThread Thread = new HandlerThread("MainActivityMessageHandler");
            Thread.start();

            mlooper = Thread.getLooper();
            /* start main activity message handler */
            msghandler = new MainActivityMessageHandler(mAppContext, mlooper);

            /* start throughput state machine */
            start_testapp_tput_state_machine();

            /* start gatt client */
            mgattclient = new GattClient(mAppContext);

            /*start gatt server*/
            mgattserver = new GattServer(mAppContext);
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

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
            manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    Log.i(TAG, "Device paired");
                    if(pairing_called == PAIRING_REQ_FROM_GATT_CLIENT){
                        Message msg = mgattclient.mGattClientHandler.obtainMessage(
                                 mgattclient.MSG_GC_REM_DEV_PAIRED, null);
                        mgattclient.mGattClientHandler.sendMessage(msg);
                    } else if(pairing_called == PAIRING_REQ_FROM_THROUGHPUT_SM) {
                        throughputSMClass.mStateMachine.sendMessage(throughputSMClass.mStateMachine
                             .MSG_TA_SM_REM_DEV_PAIRED);
                    }
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        isActivityRunning = true;
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityRunning = false;
        Log.d(TAG, "onDestroy");
        /* if wakelock is acquired data tx/rx is going on,
           so don't stop SM */
        if(!wl_acquired) {
            Log.d(TAG, "onDestroy - wakelock");
            /* Unbind from the "ADVERTISER" service */
            if (boundA) {
                unbindService(madvertiserConnection);
                boundA = false;
            }
            if(isMyServiceRunning(ScannerService.class)) {
                stopService(new Intent(this, ScannerService.class));
            }
            /* Unbind from the "SCANNER" service */
            if (boundS) {
                unbindService(mscannerConnection);
                boundS = false;
            }
            if(isMyServiceRunning(AdvertiserService.class)) {
                stopService(new Intent(this, AdvertiserService.class));
            }
            /* Stopping throughput state machine */
            if (throughputSMClass.mStateMachine != null) {
                throughputSMClass.mStateMachine.doQuit();
            }

            /* Unregistering Paring Receiver */
            try{
                if(mReceiverRegistered) {
                    unregisterReceiver(mPairingReceiver);
                    mReceiverRegistered = false;
                }
            }catch(Exception E) {
                Log.d(TAG, "not able to unregister");
            }

            /* Stop SocketServer */
            SocketServer.sendSocketData("Application is Closed... Please restart");
            socServer.closeSocketServer();

            /* stop Gatt Client handler */
            mgattclient.cleanup();

            /* stop Ble App Service msg hdlr looper*/
            mlooper.quitSafely();

        }
    }


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState");
        if(wl_acquired) {
            savedInstanceState.putBoolean("wl_acquired", true);
            Log.d(TAG, "onSaveInstanceState:wl acquired -true");
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState called");
        wl_acquired = savedInstanceState.getBoolean("wl_acquired");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    /* function to start testapp throughput state machine */
    private void start_testapp_tput_state_machine() {
        if(/*(boundA == true || boundS == true) &&*/ stateMachinestarted == false){
            throughputSMClass = new ThroughputStateMachine(mAppContext);
            Log.i("TestAppThroughputStateMachine", "make");
            throughputSMClass.mStateMachine.start();
            stateMachinestarted = true;
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

    /* Main Activity Message Handler */
    public class MainActivityMessageHandler extends Handler {
        Context mMsgContext;
        private static final String TAG = "MainActivityMessageHandler";

        int operation_request;
        Message msg;
        StringBuilder PrintStr = new StringBuilder();

        public MainActivityMessageHandler(Context contxt, Looper looper) {
            super(looper);
            mMsgContext = contxt;
            if(MainActivity.LOG_LEVEL >= 2)
                Log.d(TAG, "MainActivityMessageHandler");
        }

        @Override
        public void handleMessage(Message message) {
            if (MainActivity.LOG_LEVEL >= 2)
                Log.d(TAG, "Handler(): msg = " + message.what);
            int status;
            ReadWriteOp RdWrClass;
            Scan scnObj;
            AddServices AddServ;
            PhyUpdate phyUpdateObj;
            ConnUpdate ConnUpdateObj;

            switch (message.what) {
                case MSG_MA_START_BLE_ADV:
                    Adv adv = (Adv)message.obj;
                    mAdvertiseService.startAdvertising(adv);
                    break;
                case MSG_MA_START_BLE_SCAN:
                    scnObj = (Scan)message.obj;
                    scan_called = SCAN_CALLED_FROM_MAIN_ACTIVITY;
                    mScannerService.set_scan_parameters(scnObj);
                    PrintStr.setLength(0);
                    PrintStr.append("Scanning started!!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_MA_STOP_BLE_ADV:
                    int advId = (int)message.obj;
                    mAdvertiseService.stopAdvertising(advId);
                    break;
                case MSG_MA_STOP_BLE_SCAN:
                    scan_called = 0;
                    Log.d(TAG, "scan stop(main activity)");
                    mScannerService.stopScan();
                    PrintStr.setLength(0);
                    PrintStr.append("Scanning stopped!!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_MA_SCAN_DEV_FOUND:
                    Log.d(TAG, "scan dev found(main activity)");
                    BluetoothDevice device = (BluetoothDevice) message.obj;
                    processScanCb(device);
                    break;
                case MSG_MA_ADV_STARTED:
                    PrintStr.setLength(0);
                    String enableId = (String) message.obj;
                    PrintStr.append("Advertising started! Instance Id:");
                    PrintStr.append(enableId);
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_MA_ADV_STOPPED:
                    PrintStr.setLength(0);
                    String disableId = (String) message.obj;
                    PrintStr.append("Advertising stopped for instance Id:");
                    PrintStr.append(disableId);
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_GC_START_BLE_CONNECT:
                    scnObj = (Scan) message.obj;
                    scan_called = SCAN_CALLED_FROM_GATT_CLIENT;
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                              mgattclient.MSG_START_BLE_CONNECT, scnObj);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_GATT_CANCEL_CONNECT:
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                            mgattclient.MSG_START_CANCEL_CONNECT, null);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_CONN_UPDATE:
                    ConnUpdateObj = (ConnUpdate) message.obj;
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                              mgattclient.MSG_START_BLE_CONN_UPDATE, ConnUpdateObj);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_PHY_UPDATE:
                    phyUpdateObj = (PhyUpdate) message.obj;
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                              mgattclient.MSG_START_BLE_PHY_UPDATE, phyUpdateObj);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_GATT_CONFIGURE_MTU_SIZE:
                    int Mtu_Size = (int) message.obj;
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                          mgattclient.MSG_START_BLE_GATT_CONFIGURE_MTU_SIZE, Mtu_Size);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_READ_PHY:
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                              mgattclient.MSG_START_BLE_READ_PHY, null);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_PAIR:
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                              mgattclient.MSG_START_BLE_PAIR_DEV, null);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_UNPAIR:
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                              mgattclient.MSG_START_BLE_UNPAIR_DEV, null);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_GATT_DISCOVER:
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                              mgattclient.MSG_START_BLE_GATT_DISC, null);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_GATT_REFRESH_SERVICES:
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                                        mgattclient.MSG_START_BLE_GATT_REFRESH_SERVICES, null);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_GATT_WRITE_READ_CHAR:
                    RdWrClass = (ReadWriteOp) message.obj;
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                                        mgattclient.MSG_START_BLE_GATT_WRITE_READ_CHAR, RdWrClass);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_GATT_WRITE_READ_DESC:
                    RdWrClass = (ReadWriteOp) message.obj;
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                                        mgattclient.MSG_START_BLE_GATT_WRITE_READ_DESC, RdWrClass);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_REGISTER_BLE_GATT_NOTIFICATIONS:
                    RdWrClass = (ReadWriteOp) message.obj;
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                          mgattclient.MSG_REGISTER_BLE_GATT_NOTIFICATIONS, RdWrClass);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_DEREGISTER_BLE_GATT_NOTIFICATIONS:
                    RdWrClass = (ReadWriteOp) message.obj;
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                          mgattclient.MSG_DEREGISTER_BLE_GATT_NOTIFICATIONS, RdWrClass);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_GATT_RELIABLE_WRITE:
                    RdWrClass = (ReadWriteOp) message.obj;
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                          mgattclient.MSG_START_BLE_GATT_RELIABLE_WRITE, RdWrClass);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_GATT_ABORT_RELIABLE_WRITE:
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                          mgattclient.MSG_START_BLE_GATT_ABORT_RELIABLE_WRITE, null);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_GC_START_BLE_GATT_DISC:
                    msg = mgattclient.mGattClientHandler.obtainMessage(
                          mgattclient.MSG_START_BLE_GATT_DISCONNECT, null);
                    mgattclient.mGattClientHandler.sendMessage(msg);
                    break;
                case MSG_SM_START_BLE_CONNECT:
                    scnObj = (Scan) message.obj;
                    scan_called = SCAN_CALLED_FROM_THROUGHPUT_SM;
                    msg = throughputSMClass.mStateMachine.obtainMessage(
                          throughputSMClass.mStateMachine.MSG_TA_SM_CONNECT, scnObj);
                    throughputSMClass.mStateMachine.sendMessage(msg);
                    break;
                case MSG_SM_START_BLE_CONN_UPDATE:
                    ConnUpdateObj = (ConnUpdate) message.obj;
                    msg = throughputSMClass.mStateMachine.obtainMessage(
                    throughputSMClass.mStateMachine.MSG_TA_SM_CONN_UPDATE, ConnUpdateObj);
                    throughputSMClass.mStateMachine.sendMessage(msg);
                    break;
                case MSG_SM_START_BLE_PHY_UPDATE:
                    phyUpdateObj = (PhyUpdate) message.obj;
                    msg = throughputSMClass.mStateMachine.obtainMessage(
                    throughputSMClass.mStateMachine.MSG_TA_SM_PHY_UPDATE, phyUpdateObj);
                    throughputSMClass.mStateMachine.sendMessage(msg);
                    break;
                case MSG_SM_START_BLE_READ_PHY:
                    msg = throughputSMClass.mStateMachine.obtainMessage(
                    throughputSMClass.mStateMachine.MSG_TA_SM_READ_PHY, null);
                    throughputSMClass.mStateMachine.sendMessage(msg);
                    break;
                case MSG_SM_START_BLE_PAIR:
                    msg = throughputSMClass.mStateMachine.obtainMessage(
                    throughputSMClass.mStateMachine.MSG_TA_SM_PAIR_DEV, null);
                    throughputSMClass.mStateMachine.sendMessage(msg);
                    break;
                case MSG_SM_START_BLE_UNPAIR:
                    msg = throughputSMClass.mStateMachine.obtainMessage(
                    throughputSMClass.mStateMachine.MSG_TA_SM_UNPAIR_DEV, null);
                    throughputSMClass.mStateMachine.sendMessage(msg);
                    break;
                case MSG_SM_START_BLE_DATA_TX_TEST:
                    DataTx DataTxClass = (DataTx) message.obj;
                    msg = throughputSMClass.mStateMachine.obtainMessage(
                    throughputSMClass.mStateMachine.MSG_TA_SM_DATA_TX_TEST, DataTxClass);
                    throughputSMClass.mStateMachine.sendMessage(msg);
                    break;
                case MSG_SM_START_BLE_DATA_RX_TEST:
                    DataRx DataRxClass = (DataRx) message.obj;
                    msg = throughputSMClass.mStateMachine.obtainMessage(
                    throughputSMClass.mStateMachine.MSG_TA_SM_DATA_RX_TEST, DataRxClass);
                    throughputSMClass.mStateMachine.sendMessage(msg);
                    break;
                case MSG_SM_START_BLE_LATENCY_TEST:
                    LatencyTest LatencyTestClass = (LatencyTest) message.obj;
                    msg = throughputSMClass.mStateMachine.obtainMessage(
                    throughputSMClass.mStateMachine.MSG_TA_SM_LATENCY_TEST, LatencyTestClass);
                    throughputSMClass.mStateMachine.sendMessage(msg);
                    break;
                case MSG_SM_START_BLE_GATT_DISC:
                    msg = throughputSMClass.mStateMachine.obtainMessage(
                    throughputSMClass.mStateMachine.MSG_TA_SM_DISCONNECT, null);
                    throughputSMClass.mStateMachine.sendMessage(msg);
                    break;
                case MSG_GS_START_BLE_ADD_SERVICE:
                    AddServ = (AddServices) message.obj;
                    msg = mgattserver.mGattServerHandler.obtainMessage(
                            mgattserver.MSG_START_BLE_ADD_SERVICE, AddServ);
                    mgattserver.mGattServerHandler.sendMessage(msg);
                    break;
                case MSG_GS_START_BLE_REMOVE_SERVICE:
                    String uuid=(String)message.obj;
                    msg = mgattserver.mGattServerHandler.obtainMessage(
                            mgattserver.MSG_START_BLE_REMOVE_SERVICE, uuid);
                    mgattserver.mGattServerHandler.sendMessage(msg);
                    break;
                case MSG_GS_START_BLE_CLEAR_SERVICES:
                    msg = mgattserver.mGattServerHandler.obtainMessage(
                            mgattserver.MSG_START_BLE_CLEAR_SERVICES, null);
                    mgattserver.mGattServerHandler.sendMessage(msg);
                    break;
                case MSG_GS_START_BLE_GET_SERVICES:
                    msg = mgattserver.mGattServerHandler.obtainMessage(
                            mgattserver.MSG_START_BLE_GET_SERVICES, null);
                    mgattserver.mGattServerHandler.sendMessage(msg);
                    break;
                case MSG_GS_START_BLE_PHY_UPDATE:
                    phyUpdateObj = (PhyUpdate) message.obj;
                    msg = mgattserver.mGattServerHandler.obtainMessage(
                             mgattserver.MSG_START_BLE_PHY_UPDATE, phyUpdateObj);
                    mgattserver.mGattServerHandler.sendMessage(msg);
                    break;
                case MSG_GS_START_BLE_READ_PHY:
                    msg = mgattserver.mGattServerHandler.obtainMessage(
                              mgattserver.MSG_START_BLE_READ_PHY, null);
                     mgattserver.mGattServerHandler.sendMessage(msg);
                     break;
                case MSG_GS_START_GET_CONNECTED_DEVICES:
                    msg = mgattserver.mGattServerHandler.obtainMessage(
                             mgattserver.MSG_START_GET_CONNECTED_DEVICES, null);
                    mgattserver.mGattServerHandler.sendMessage(msg);
                    break;
                default:
                    Log.e(TAG, "Unknown Operation");
                    break;
            }
        }

        private void processScanCb(BluetoothDevice device) {
            Log.d(TAG, "processScanCb(main activity)"+scan_called);
            if(scan_called == SCAN_CALLED_FROM_GATT_CLIENT){
                msg = mgattclient.mGattClientHandler.obtainMessage(
                            mgattclient.MSG_BLE_SCAN_DEV_FOUND, device);
                mgattclient.mGattClientHandler.sendMessage(msg);
            } else if (scan_called == SCAN_CALLED_FROM_THROUGHPUT_SM){
                msg = throughputSMClass.mStateMachine.obtainMessage(
                        throughputSMClass.mStateMachine.MSG_TA_SM_DEV_FOUND, device);
                throughputSMClass.mStateMachine.sendMessage(msg);
            } else {
               /* do nothing */
            }
        }
    }
}
