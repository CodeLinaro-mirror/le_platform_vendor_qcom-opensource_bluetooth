/*
 * Copyright (c) 2021, The Linux Foundation. All rights reserved.
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

package vendor.qti.ancs_ble_testapp;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.widget.Toast;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.PowerManager.WakeLock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import vendor.qti.bluetooth_offload.SubscribedGattHandles;

import java.io.*;
import libcore.io.IoUtils;
import android.app.Service;
import android.app.IntentService;
import android.app.PendingIntent;

import java.util.*;
import java.util.List;
import java.util.ArrayList;

import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import vendor.qti.bluetooth_offload.NotificationOffloadAdapter;
import vendor.qti.bluetooth_offload.BluetoothOffloadCallback;
//import com.android.server;

import java.util.Arrays;

public class AncsService extends Service {
    private static final String TAG = "AncsService";

    private static BluetoothAdapter bleAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mDevice = null;

    private Context mAppContext;
    private Looper mlooper;

    public static final int TRANSPORT_LE = 2;

    // Connection States
    public static final int BLE_STATE_CONNECTING = 1;
    public static final int BLE_STATE_CONNECTED = 2;
    public static final int BLE_STATE_DISCONNECTING = 3;
    public static final int BLE_STATE_DISCONNECTED = 4;

    private static int mConnectionStatus = BLE_STATE_DISCONNECTED;
    public static NCStateMachine mStateMachine;
    public static boolean stateMachineStarted = false;
    private static boolean mReceiverRegistered = false;
    public StringBuilder printStr = new StringBuilder();

    private static IState ncPreviousState;

    public static AncsServiceMessageHandler msghandler = null;

    private static BluetoothGattCharacteristic notificationSourceChar = null;
    private static BluetoothGattCharacteristic controlPointChar = null;
    private static BluetoothGattCharacteristic dataSourceChar = null;

    private NotificationOffloadAdapter   mNotificationAdapter;

    private static final int BT_FAIL = 0;
    private static final int BT_OK = 1;
    private static final int BT_INVALID_STATE = 2;

    /* Ancs Service Actions */
    public static final int MSG_AS_REGISTER_OFFLODABLE_ADAPTER = 0;
    public static final int MSG_AS_DREGISTER_OFFLODABLE_ADAPTER = 1;
    public static final int MSG_AS_SET_SUBSCRIBED_GATTHANDLES = 2;
    public static final int MSG_AS_SET_MODE = 3;
    public static final int MSG_AS_SET_APP_CONTEXT = 4;


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.e(TAG,"inside on receive");
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) &&
                             intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                                     == BluetoothAdapter.STATE_OFF) {
                 //destroy oject
                Log.e(TAG,"BT OFF - clearing object");

                /* Stop Ancs Service msg hdlr looper*/
                printStr.setLength(0);
                printStr.append("BT is turned off !!");
                SocketServer.sendSocketData(printStr.toString());
                mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_BT_ADAPTER_OFF);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) &&
                             intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                                     == BluetoothAdapter.STATE_ON) {
                printStr.setLength(0);
                printStr.append("BT is turned on !!");
                SocketServer.sendSocketData(printStr.toString());
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mAppContext = this;
        registerReceiver(mReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),null,null);

        /* Check and prompt to user if bluetooth in not turned on */
        if (!initAdapter()) {
            Log.e(TAG, "Bluetooth is not turned on");
            showMessage("Bluetooth is not turned ON");
            return;
        }
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

    private final IBinder localBinder = new MyBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    public class MyBinder extends Binder {

        public AncsService getService() {
            return AncsService.this;

        }
    }

    /* function to check if bluetooth is turned on */
    private boolean initAdapter() {
        if(MainActivity.mBluetoothManager != null) {
            bleAdapter = MainActivity.mBluetoothManager.getAdapter();
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
        } else {
            Log.e(TAG, "mBluetoothManager is null");
            return false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "Service onStartCommand");
        IntentFilter Pairingfilter = new IntentFilter();
        Pairingfilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        Pairingfilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(mPairingReceiver, Pairingfilter,null,null);
        mReceiverRegistered = true;

        HandlerThread Thread = new HandlerThread("AncsServiceHandler");
        Thread.start();

        mlooper = Thread.getLooper();
        msghandler = new AncsServiceMessageHandler(mAppContext, mlooper);

        startNCStateMachine();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothAdapter bluetoothAdapter = MainActivity.mBluetoothManager.getAdapter();
        Log.i(TAG, "OnDestroy");
        if (bluetoothAdapter.isEnabled()) {
            stopServer();
            stopAdvertising();
        }
        /* Stopping notification consumer state machine */
        if (mStateMachine != null) {
            mStateMachine.doQuit();
        }

        /* Unregistering Paring Receiver */
        try {
            if (mReceiverRegistered) {
                unregisterReceiver(mPairingReceiver);
                mReceiverRegistered = false;
            }
        } catch (Exception E) {
            Log.d(TAG, "not able to unregister");
        }

        /* Stop Ancs Service msg hdlr looper*/
        mlooper.quitSafely();
    }

    /* function to start notification consumer state machine */
    private void startNCStateMachine() {
        if (stateMachineStarted == false) {
            mStateMachine = new NCStateMachine(mAppContext);
            Log.i(TAG, "start_nc_state_machine");
            mStateMachine.start();
            stateMachineStarted = true;
        }
    }

    /* function to print the message on display */
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /* Ancs Service Message Handler */
    public class AncsServiceMessageHandler extends Handler {
        Context mMsgContext;
        private static final String TAG = "AncsServiceMessageHandler";

        int operation_request;
        Message msg;

        public AncsServiceMessageHandler(Context contxt, Looper looper) {
            super(looper);
            mMsgContext = contxt;
        }

        @Override
        public void handleMessage(Message message) {
            Log.d(TAG, "Handler(): msg = " + message.what);

            switch (message.what) {
                case MSG_AS_REGISTER_OFFLODABLE_ADAPTER:
                    processGattRegisterOfflodableAdapter();
                    break;
                case MSG_AS_DREGISTER_OFFLODABLE_ADAPTER:
                    processGattDRegisterOfflodableAdapter();
                    break;
                case MSG_AS_SET_SUBSCRIBED_GATTHANDLES:
                    String handles = (String)message.obj;
                    if(mConnectionStatus == BLE_STATE_CONNECTED) {
                        processGattSetSubscribedGattHandles(handles);
                    } else {
                        printStr.setLength(0);
                        printStr.append("Failed! Device not connected");
                        SocketServer.sendSocketData(printStr.toString());
                    }
                    break;
                case MSG_AS_SET_MODE:
                    int mode = (int)message.obj;
                    processSetMode(mode);
                    break;
                case MSG_AS_SET_APP_CONTEXT:
                    // byte[] blob = AppContextProto.getAppContextProtoBuffer();
                    // if(blob.length > 0) {
         // Log.i(TAG,"blob length greater");
                        // processSetAppContext(blob);
                    // }
                    break;
                default:
                    Log.e(TAG, "Unknown Operation");
                    break;
            }
        }
    }

    private void processGattRegisterOfflodableAdapter() {
        Log.d(TAG, "processGattRegisterOfflodableAdapter()");
        mNotificationAdapter = new NotificationOffloadAdapter(mAppContext);
       if (mOffloadcallbacks != null) {
           Log.d(TAG,"yes");
        }
        if(BT_FAIL == mNotificationAdapter.register(mOffloadcallbacks)) {
            Log.d(TAG, "failed to register notification offload adapter");
            return;
        }
        Log.d(TAG, "Registered notification offload adapter");
        printStr.setLength(0);
        printStr.append("OffloadableApp Registered");
        SocketServer.sendSocketData(printStr.toString());
    }

    private void processGattDRegisterOfflodableAdapter() {
        Log.d(TAG, "processGattDRegisterOfflodableAdapter()");
        mNotificationAdapter.unregister();
        printStr.setLength(0);
        printStr.append("OffloadableApp Deregistered");
        SocketServer.sendSocketData(printStr.toString());
    }

    private void processGattSetSubscribedGattHandles(String handles) {
        Log.d(TAG, "processGattSetSubscribedGattHandles() Handles: " + handles);
        ArrayList<SubscribedGattHandles> list = new ArrayList<SubscribedGattHandles>();

        String[] strList = handles.split(",");
        /*ArrayList<Integer> handlesList = new ArrayList<Integer>();
        for (int j=0; j<strList.length; j++) {
            handlesList.add(Integer.parseInt(strList[j]));
        }*/
        int[] handleList = Arrays.stream(strList).mapToInt(Integer::parseInt).toArray();
        SubscribedGattHandles gattHandlesParam = new SubscribedGattHandles(mDevice.getAddress(), handleList);
        Log.d(TAG, "addr:" + mDevice.getAddress());
        //gattHandlesParam.handles = handlesList;
        //gattHandlesParam.remoteAddr = mDevice.getAddress();

        list.add(gattHandlesParam);
        mNotificationAdapter.setSubscribedGattHandles(list);
        printStr.setLength(0);
        printStr.append("setSubscribedGattHandles Done");
        SocketServer.sendSocketData(printStr.toString());
    }

    private void processSetMode(int mode) {
        Log.d(TAG, "processSetMode() mode: " + mode);

        mNotificationAdapter.transitionToPwrState(mode);
        printStr.setLength(0);
        printStr.append("sent transitionToPwrState");
        SocketServer.sendSocketData(printStr.toString());
    }

    private void processSetAppContext(byte[] blob) {
        Log.i(TAG, "processSetAppContext() blob: " + Arrays.toString(blob));
        ArrayList<Byte> blobBytes = new ArrayList<Byte>();
        for (int i = 0; i < blob.length; i++) {
            blobBytes.add(blob[i]);
        }
        //mNotificationAdapter.setAppSpecificContextInfo(blobBytes);
    }

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private void startAdvertising() {
        mBluetoothLeAdvertiser = bleAdapter.getBluetoothLeAdvertiser();
        AdvertiseData data = null;
        ParcelUuid uuid = null;

        try {
            /* get the persist property */
            Process proc =
                Runtime.getRuntime().exec("/system/bin/getprop"+" "
                +"persist.disable.external.adv");
            BufferedReader reader =
                            new BufferedReader(
                                new InputStreamReader(proc.getInputStream()));
            String readLine = reader.readLine();

            if(readLine != null) {
                if(readLine.equals("false")) {
                    /* if the value is false, add device name instead of uuid */
                    Log.w(TAG, "power testing property set to false");
                } else {
                    uuid = ParcelUuid.fromString(readLine);
                    Log.w(TAG, "uuid:"+ uuid);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception handling");
        }
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setConnectable(true)
                .setTimeout(0)
                .build();

        AdvertiseData.Builder ancsData = new AdvertiseData.Builder();

        /* set the uuid read from property */
        if(uuid != null) {
            ancsData.addServiceUuid(uuid);
        } else {
            ancsData.setIncludeDeviceName(true);
        }
        data = ancsData.build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, mAdvertiseCallback);
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private void startServer() {
        mBluetoothGattServer = MainActivity.mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
        }
    }

    /**
     * Shut down the GATT server.
     */
    private void stopServer() {
        if (mBluetoothGattServer == null) return;

        mBluetoothGattServer.close();
    }

     /**
     * Start LE Pairing
     */
    private void startPairing(){
            if(mDevice.getBondState() != BluetoothDevice.BOND_BONDED){
                Log.i(TAG, "Pairing!");
                if(!mDevice.createBond(TRANSPORT_LE)) {
                    Log.i(TAG, "couldn't start pairing");
                    printStr.setLength(0);
                    printStr.append("Pairing failed!");
                    SocketServer.sendSocketData(printStr.toString());
                }
            }
        }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
            printStr.setLength(0);
            printStr.append("Advertising started!");
            SocketServer.sendSocketData(printStr.toString());
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: " + errorCode);
            printStr.setLength(0);
            printStr.append("Advertising failed!");
            SocketServer.sendSocketData(printStr.toString());
        }
    };

    /**
     * Callback to handle incoming requests to the GATT server.
     */

    public BluetoothOffloadCallback mOffloadcallbacks = new BluetoothOffloadCallback() {

        @Override
        public void onNotifyStartDone(int status) {
            Log.d(TAG, "notifyStartDone");
        }

        @Override
        public void onNotifyStopDone(int status) {
            Log.d(TAG, "notifyStopDone");
        }

        @Override
        public int onNotifyEnableOffload(int mode) {
            Log.d(TAG, "notifyEnableOffload Mode: " + mode);
            ncPreviousState = mStateMachine.getCurrentState();
            Log.d(TAG, "CurrentState: " + ncPreviousState.getName());
            mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_OFFLOADED);
            return 0;
        }

        @Override
        public int onNotifyDisableOffload(byte[] blob) {
            Log.d(TAG, "notifyOffloadDisable blob ");
            //mofflodableappadapter.disableOffloadDone(BT_OK);
            //AncsParse.processBlob(blob);
            int size = (blob.length/8);
            int Notification_size = 8;
            Log.i(TAG," payload: " + Arrays.toString(blob));
            for (int i = 0; i<size; i++) {
               byte [] value = new byte[Notification_size];
               for(int j = 0 ; j<Notification_size ; j++) {
                   value[j] = blob[(Notification_size*i)+j];
               }
               AncsParse.processNotificationSource(value);
               Log.i(TAG," payload: " + Arrays.toString(value));
            }
            String s = new String(blob);
            Log.d(TAG, "print blob :"+ s );
            mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_ACTIVE);
            return 0;
        }

        @Override
        public void onNotifyAsyncErr(int status) {
            Log.i(TAG, "notifyAsyncErr status: " + status);
        }

        @Override
        public void onTransitionToPwrStateDone(int status) {
            Log.d(TAG, "transitionToPwrStateDone status " + status);
            printStr.setLength(0);
            printStr.append("onTransitionToPwrStateDone status:");
            printStr.append(status);
            printStr.setLength(0);
            if (status == NotificationOffloadAdapter.BT_OK) {
                printStr.append(" Offloaded");
            } else {
                printStr.append("Error while offloading");
            }
            SocketServer.sendSocketData(printStr.toString());
        }
  // @Override
        // public void onAdvStart() {
            // //Log.d(TAG, "transitionToPwrStateDone status " + status);
            // printStr.setLength(0);
            // printStr.append("transitionToPwrState Done");
            // SocketServer.sendSocketData(printStr.toString());
        // }
    };

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.i(TAG, "mGattServerCallback onConnectionStateChange device :" + device +
                    " status :" + status + " newState :" + newState);

            if (device == null || (status != BluetoothGatt.GATT_SUCCESS)) {
                Log.e(TAG, "mGattServerCallback onConnectionStateChange:Unexpected error! mstate: "
                        + newState);
                mConnectionStatus = BLE_STATE_DISCONNECTED;
                mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_GATT_SERVER_FAILED_TO_CONNECT);
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "mGattServerCallback onConnectionStateChange:CONNECTED "
                        + " remoteDevice: " + device.getAddress());
                mConnectionStatus = BLE_STATE_CONNECTED;
                mDevice = device;
                printStr.setLength(0);
                printStr.append("Connected to ");
                printStr.append(device.getName());
                SocketServer.sendSocketData(printStr.toString());
                mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_GATT_SERVER_CONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "mGattServerCallback onConnectionStateChange:DISCONNECTED "
                        + " remoteDevice: " + device.getAddress());
                mConnectionStatus = BLE_STATE_DISCONNECTED;
                mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_GATT_SERVER_DISCONNECTED);
            }
        }
    };

    private final BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "mGattCallbacks onConnectionStateChange device :" + gatt.getDevice() +
                    " status :" + status + " newState :" + newState);

            if (gatt.getDevice() == null || (status != BluetoothGatt.GATT_SUCCESS) &&
                    (mStateMachine.getCurrentState() == mStateMachine.mNCPending)) {
                Log.e(TAG, "mGattCallbacks onConnectionStateChange:Unexpected error! mstate: " + newState);
                mStateMachine.sendMessage(
                        NCStateMachine.MSG_NC_SM_GATT_FAILED_TO_CONNECT);
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "mGattCallbacks onConnectionStateChange:CONNECTED "
                        + " remoteDevice: " + gatt.getDevice().getAddress());
                Log.d(TAG, "starting discover services");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "mGattCallbacks onConnectionStateChange:DISCONNECTED "
                        + " remoteDevice: " + gatt.getDevice().getAddress());
                mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onService discovery success");

                BluetoothGattService mService = gatt.getService(AncsParse.ANCS_SERVICE_UUID);
                if (mService != null) {
                    notificationSourceChar = mService.getCharacteristic(AncsParse.ANCS_NOTIFICATION_SOURCE_UUID);
                    controlPointChar = mService.getCharacteristic(AncsParse.ANCS_CONTROL_POINT_UUID);
                    dataSourceChar = mService.getCharacteristic(AncsParse.ANCS_DATA_SOURCE_UUID);

                    // Enable Notifications
                    if(notificationSourceChar != null) {
                        setCharacteristicNotification(notificationSourceChar, true);
                        Log.d(TAG, "notificationSourceChar registered");
                    }

                    Log.d(TAG, "ANCS service found");
                    mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_ANCS_SERVICE_FOUND);
                    printStr.setLength(0);
                    List<BluetoothGattService> mServices = gatt.getServices();
                    List<BluetoothGattCharacteristic> mCharacteristics;
                    for (BluetoothGattService service : mServices) {
                        Log.d(TAG, "Found service: " + service.getUuid());
                        printStr.append("\n------------------------------------------------\n");
                        printStr.append("Service UUID:");
                        printStr.append(service.getUuid());
                        printStr.append("handle");
                        printStr.append(service.getInstanceId());
                        mCharacteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic
                                      characteristic : mCharacteristics) {
                            Log.d(TAG, "Found Char: " + characteristic.getUuid());
                            printStr.append("\nCharacteristic UUID:");
                            printStr.append(characteristic.getUuid());
                            printStr.append("handle");
                            printStr.append(characteristic.getInstanceId());
                        }
                    }
                    SocketServer.sendSocketData(printStr.toString());
                } else {
                    Log.d(TAG, "ANCS service not found");
                    mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_ANCS_SERVICE_NOT_FOUND);
                }

            } else {
                Log.d(TAG, "onServicesDiscovered failed: " + status);
                mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_FAILED_SERVICE_DISCOVERY);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            Log.d(TAG, "onCharacteristicChanged Data: " + Arrays.toString(value));

            if (characteristic == notificationSourceChar) {
                AncsParse.processNotificationSource(value);
                mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_NOTIFICATION_RECEIVED);
            } else if (characteristic == dataSourceChar) {
                AncsParse.processDataSource(value);
                mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_DATA_SOURCE_NOTIFICATION_RECEIVED);
            } else {
                Log.d(TAG, "onCharacteristicChanged unknown notification");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            if ((status == BluetoothGatt.GATT_SUCCESS)) {
                Log.i(TAG, "onCharacteristicWrite: " + status);
                mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_WRITE_RSP_RECEIVED);
            } else {
                Log.i(TAG, "write characteristic failed");
            }
        }
    };

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        mBluetoothGatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(AncsParse.CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            if (enable) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
            mBluetoothGatt.writeDescriptor(descriptor);
        } else {
            Log.e(TAG, "Descriptor not found");
        }
    }

    private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_REM_DEV_PAIRED);
                }
            } else if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        mDevice.setPairingConfirmation(true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error occurs when trying to auto pair");
                    e.printStackTrace();
                }
            }
        }
    };

    public class NCStateMachine extends StateMachine {

        public static final int MSG_NC_SM_BT_ADAPTER_OFF = 0;
        public static final int MSG_NC_SM_START_ADV = 1;
        public static final int MSG_NC_SM_STOP_ADV = 2;
        public static final int MSG_NC_SM_GATT_SERVER_CONNECTED = 3;
        public static final int MSG_NC_SM_GATT_SERVER_DISCONNECTED = 4;
        public static final int MSG_NC_SM_GATT_SERVER_FAILED_TO_CONNECT = 5;
        public static final int MSG_NC_SM_REM_DEV_PAIRED = 6;
        public static final int MSG_NC_SM_FAILED_TO_PAIR = 7;
        public static final int MSG_NC_SM_GATT_CONNECTED = 8;
        public static final int MSG_NC_SM_GATT_DISCONNECTED = 9;
        public static final int MSG_NC_SM_GATT_FAILED_TO_CONNECT = 10;
        public static final int MSG_NC_SM_ANCS_SERVICE_FOUND = 11;
        public static final int MSG_NC_SM_FAILED_SERVICE_DISCOVERY = 12;
        public static final int MSG_NC_SM_ANCS_SERVICE_NOT_FOUND = 13;
        public static final int MSG_NC_SM_NOTIFICATION_RECEIVED = 14;
        public static final int MSG_NC_SM_NOTIFICATION_ATTR = 15;
        public static final int MSG_NC_SM_APP_ATTR = 16;
        public static final int MSG_NC_SM_NOTIFICATION_ACTION = 17;
        public static final int MSG_NC_SM_DATA_SOURCE_NOTIFICATION_RECEIVED = 18;
        public static final int MSG_NC_SM_DISCONNECT = 19;
        public static final int MSG_NC_SM_WRITE_RSP_RECEIVED = 20;
        public static final int MSG_NC_SM_OFFLOADED = 21;
        public static final int MSG_NC_SM_ACTIVE = 22;

        private NCIdle mNCIdle;
        private NCPending mNCPending;
        private NCPaired mNCPaired;
        private NCNotificationReceived mNCNotificationReceived;
        private NCControlPoint mNCControlPoint;
        private NCDisconnect mNCDisconnect;
        private NCOffloaded mNCOffloaded;

        private Message messageQueued;

        StringBuilder printStr = new StringBuilder();

        private NCStateMachine(Context context) {
            super("NCStateMachine");

            mNCIdle = new NCIdle();
            mNCPending = new NCPending();
            mNCPaired = new NCPaired();
            mNCNotificationReceived = new NCNotificationReceived();
            mNCControlPoint = new NCControlPoint();
            mNCDisconnect = new NCDisconnect();
            mNCOffloaded = new NCOffloaded();

            addState(mNCIdle);
            addState(mNCPending);
            addState(mNCPaired);
            addState(mNCNotificationReceived);
            addState(mNCControlPoint);
            addState(mNCDisconnect);
            addState(mNCOffloaded);

            setInitialState(mNCIdle);
        }

        public void doQuit() {
            Log.i(TAG, "doQuit");
            synchronized (NCStateMachine.this) {
                stateMachineStarted = false;
                quitNow();
            }
        }

        private class NCIdle extends State {
            private static final String TAG = "NCIdle";

            @Override
            public void enter() {
                Log.i(TAG, "Enter" + getCurrentMessage().what);
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit" + getCurrentMessage().what);
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);
                boolean retValue = HANDLED;

                switch (message.what) {
                    case MSG_NC_SM_START_ADV:
                        startAdvertising();
                        startServer();
                        break;
                    case MSG_NC_SM_STOP_ADV:
                        stopServer();
                        stopAdvertising();
                        printStr.setLength(0);
                        printStr.append("Advertising stopped!");
                        SocketServer.sendSocketData(printStr.toString());
                        break;
                    case MSG_NC_SM_GATT_SERVER_CONNECTED:
                        stopAdvertising();
                        connectGatt();
                        transitionTo(mNCPending);
                        break;
                    case MSG_NC_SM_GATT_SERVER_DISCONNECTED:
                        printStr.setLength(0);
                        printStr.append("Disconnected!");
                        SocketServer.sendSocketData(printStr.toString());
                        break;
                    case MSG_NC_SM_GATT_SERVER_FAILED_TO_CONNECT:
                        printStr.setLength(0);
                        printStr.append("Connect failed!");
                        SocketServer.sendSocketData(printStr.toString());
                        break;
                    case MSG_NC_SM_OFFLOADED:
                        transitionTo(mNCOffloaded);
                        break;
                    case MSG_NC_SM_BT_ADAPTER_OFF:
                        transitionTo(mNCIdle);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }

            public void connectGatt() {
                if (bleAdapter != null) {
                    Log.i(TAG, "Gatt Connect");
                    mBluetoothGatt = mDevice.connectGatt(mAppContext, false, mGattCallbacks, BluetoothDevice.TRANSPORT_LE);
                }
            }
        }

        private class NCPending extends State {
            private static final String TAG = "NCPending";

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);

                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_REM_DEV_PAIRED);
                } else {
                    printStr.setLength(0);
                    printStr.append("Waiting to get paired");
                    SocketServer.sendSocketData(printStr.toString());
                    if ((MainActivity.wl != null) && (!MainActivity.wl_acquired)) {
                        MainActivity.wl.acquire();
                        MainActivity.wl_acquired = true;
                        Log.i(TAG, "wakelock acquired");
                    }
                    /* start pairing */
                    startPairing();
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
                    case MSG_NC_SM_GATT_SERVER_DISCONNECTED:
                        printStr.setLength(0);
                        printStr.append("Disconnected!");
                        SocketServer.sendSocketData(printStr.toString());
                        if ((MainActivity.wl != null) && (MainActivity.wl_acquired)) {
                            Log.i(TAG, "Releasing wakelock");
                            try {
                                MainActivity.wl.release();
                                MainActivity.wl_acquired = false;
                                Log.i(TAG, "wakelock released");
                            } catch (Throwable th) {
                                // ignoring this exception, probably wakeLock was already released
                            }
                        } else {
                            Log.e(TAG, "Wakelock reference is null");
                        }
                        transitionTo(mNCIdle);
                        break;
                    case MSG_NC_SM_REM_DEV_PAIRED:
                        printStr.setLength(0);
                        printStr.append("Device paired");
                        SocketServer.sendSocketData(printStr.toString());
                        if ((MainActivity.wl != null) && (MainActivity.wl_acquired)) {
                            Log.i(TAG, "Releasing wakelock");
                            try {
                                MainActivity.wl.release();
                                MainActivity.wl_acquired = false;
                                Log.i(TAG, "wakelock released");
                            } catch (Throwable th) {
                                // ignoring this exception, probably wakeLock was already released
                            }
                        } else {
                            // should never happen during normal workflow
                            Log.e(TAG, "Wakelock reference is null");
                        }
                        break;
                    case MSG_NC_SM_FAILED_TO_PAIR:
                        // Wait for device to be disconnected
                        printStr.setLength(0);
                        printStr.append("Pairing failed!");
                        SocketServer.sendSocketData(printStr.toString());
                        if ((MainActivity.wl != null) && (MainActivity.wl_acquired)) {
                            Log.i(TAG, "Releasing wakelock");
                            try {
                                MainActivity.wl.release();
                                MainActivity.wl_acquired = false;
                                Log.i(TAG, "wakelock released");
                            } catch (Throwable th) {
                                // ignoring this exception, probably wakeLock was already released
                            }
                        } else {
                            // should never happen during normal workflow
                            Log.e(TAG, "Wakelock reference is null");
                        }
                        break;
                    case MSG_NC_SM_GATT_CONNECTED:
                        break;
                    case MSG_NC_SM_GATT_DISCONNECTED:
                        if ((MainActivity.wl != null) && (MainActivity.wl_acquired)) {
                            Log.i(TAG, "Releasing wakelock");
                            try {
                                MainActivity.wl.release();
                                MainActivity.wl_acquired = false;
                                Log.i(TAG, "wakelock released");
                            } catch (Throwable th) {
                                // ignoring this exception, probably wakeLock was already released
                            }
                        } else {
                            Log.e(TAG, "Wakelock reference is null");
                        }
                        printStr.setLength(0);
                        printStr.append("Disconnected!");
                        SocketServer.sendSocketData(printStr.toString());
                        transitionTo(mNCIdle);
                        break;
                    case MSG_NC_SM_GATT_FAILED_TO_CONNECT:
                        printStr.setLength(0);
                        printStr.append("Connect failed!");
                        SocketServer.sendSocketData(printStr.toString());
                        transitionTo(mNCIdle);
                        break;
                    case MSG_NC_SM_ANCS_SERVICE_FOUND:
                        printStr.setLength(0);
                        printStr.append("ANCS service found");
                        SocketServer.sendSocketData(printStr.toString());
                        transitionTo(mNCPaired);
                        break;
                    case MSG_NC_SM_ANCS_SERVICE_NOT_FOUND:
                        printStr.setLength(0);
                        printStr.append("ANCS service not found");
                        SocketServer.sendSocketData(printStr.toString());
                        transitionTo(mNCDisconnect);
                        break;
                    case MSG_NC_SM_FAILED_SERVICE_DISCOVERY:
                        printStr.setLength(0);
                        printStr.append("Service discovery failed!");
                        SocketServer.sendSocketData(printStr.toString());
                        transitionTo(mNCDisconnect);
                        break;
                    case MSG_NC_SM_BT_ADAPTER_OFF:
                        transitionTo(mNCIdle);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }
        }

        private class NCPaired extends State {
            private static final String TAG = "NCPaired";

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                if(AncsParse.NotificationSourceList.size() > 0) {
                    mStateMachine.sendMessage(NCStateMachine.MSG_NC_SM_NOTIFICATION_RECEIVED);
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
                    case MSG_NC_SM_GATT_DISCONNECTED:
                        printStr.setLength(0);
                        printStr.append("Disconnected!");
                        SocketServer.sendSocketData(printStr.toString());
                        transitionTo(mNCIdle);
                        break;
                    case MSG_NC_SM_NOTIFICATION_RECEIVED:
                        if(dataSourceChar != null) {
                            setCharacteristicNotification(dataSourceChar, true);
                            Log.d(TAG, "dataSourceChar registered");
                        }
                        transitionTo(mNCNotificationReceived);
                        break;
                    case MSG_NC_SM_DISCONNECT:
                        transitionTo(mNCDisconnect);
                        break;
                    case MSG_NC_SM_OFFLOADED:
                        transitionTo(mNCOffloaded);
                        break;
                    case MSG_NC_SM_BT_ADAPTER_OFF:
                        transitionTo(mNCIdle);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }
        }

        private class NCNotificationReceived extends State {
            private static final String TAG = "NCNotificationReceived";

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);

                /*dequeue and process */
                dequeueMessage();
                if(AncsParse.NotificationSourceList.size() > 0) {
                    AncsParse.printNotificationSource();
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
                    case MSG_NC_SM_GATT_DISCONNECTED:
                        printStr.setLength(0);
                        printStr.append("Disconnected!");
                        SocketServer.sendSocketData(printStr.toString());
                        transitionTo(mNCIdle);
                        break;
                    case MSG_NC_SM_NOTIFICATION_RECEIVED:
                        AncsParse.printNotificationSource();
                        break;
                    case MSG_NC_SM_NOTIFICATION_ATTR:
                        byte[] notificationAttrCmd = AncsParse.getNotificationAttributes((AncsParse.NotificationAttr) message.obj);
                        writeToControlPointChar(notificationAttrCmd);
                        transitionTo(mNCControlPoint);
                        break;
                    case MSG_NC_SM_APP_ATTR:
                        byte[] appAttrCmd = AncsParse.getAppAttributes((String) message.obj);
                        writeToControlPointChar(appAttrCmd);
                        transitionTo(mNCControlPoint);
                        break;
                    case MSG_NC_SM_NOTIFICATION_ACTION:
                        byte[] actionCmd = AncsParse.performNotificationAction((AncsParse.NotificationAction) message.obj);
                        writeToControlPointChar(actionCmd);
                        transitionTo(mNCControlPoint);
                        break;
                    case MSG_NC_SM_DISCONNECT:
                        transitionTo(mNCDisconnect);
                        break;
                    case MSG_NC_SM_OFFLOADED:
                        transitionTo(mNCOffloaded);
                        break;
                    case MSG_NC_SM_BT_ADAPTER_OFF:
                        transitionTo(mNCIdle);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }

            public void dequeueMessage() {
                Message sendMsg;
                if(messageQueued != null) {
                    Log.i(TAG, "queued Msg: " + messageQueued.what);
                    sendMsg = AncsService.mStateMachine.obtainMessage(messageQueued.what, messageQueued.obj);
                    mStateMachine.sendMessage(sendMsg);
                    messageQueued = null;
                }
            }

            public void writeToControlPointChar(byte[] value) {
                controlPointChar.setWriteType(
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                controlPointChar.setValue(value);
                Log.i(TAG, "value written to CP: " +Arrays.toString(value));
                mBluetoothGatt.writeCharacteristic(controlPointChar);
            }
        }

        private class NCControlPoint extends State {
            private static final String TAG = "NCControlPoint";

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                if ((MainActivity.wl != null) && (!MainActivity.wl_acquired)) {
                    MainActivity.wl.acquire();
                    MainActivity.wl_acquired = true;
                    Log.i(TAG, "wakelock acquired");
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
                    case MSG_NC_SM_WRITE_RSP_RECEIVED:
                        if ((MainActivity.wl != null) && (MainActivity.wl_acquired)) {
                            Log.i(TAG, "Releasing wakelock");
                            try {
                                MainActivity.wl.release();
                                MainActivity.wl_acquired = false;
                                Log.i(TAG, "wakelock released");
                            } catch (Throwable th) {
                                // ignoring this exception, probably wakeLock was already released
                            }
                        } else {
                            // should never happen during normal workflow
                            Log.e(TAG, "Wakelock reference is null");
                        }
                        transitionTo(mNCNotificationReceived);
                        break;
                    case MSG_NC_SM_BT_ADAPTER_OFF:
                        transitionTo(mNCIdle);
                        break;
                    default:
                        /* store the message to be processed later */
                        messageQueued = new Message();
                        messageQueued.copyFrom(getCurrentMessage());
                        Log.e(TAG, "default case, adding "+ messageQueued.what);
                        break;
                }
                return retValue;
            }
        }

        private class NCOffloaded extends State {
            private static final String TAG = "NCOffloaded";

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);

                printStr.setLength(0);
                printStr.append("Offloaded");
                SocketServer.sendSocketData(printStr.toString());
                // byte[] blob = AppContextProto.getAppContextProtoBuffer();
                // if(blob.length > 0) {
     // Log.i(TAG,"blob length greater");
                    // processSetAppContext(blob);
                // }
                mNotificationAdapter.enableOffloadDone(BT_OK);
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
                    case MSG_NC_SM_ACTIVE:
                        printStr.setLength(0);
                        printStr.append("Transitioned to active");
                        SocketServer.sendSocketData(printStr.toString());
                        transitionTo(ncPreviousState);
                        break;

                    case MSG_NC_SM_BT_ADAPTER_OFF:
                       transitionTo(mNCIdle);
                       break;
                }
                return retValue;
            }
        }

        private class NCDisconnect extends State {
            private static final String TAG = "NCDisconnect";

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                // Disable Notifications
                if(notificationSourceChar != null) {
                    setCharacteristicNotification(notificationSourceChar, false);
                    Log.d(TAG, "notificationSourceChar deregistered");
                    notificationSourceChar = null;
                }
                if(dataSourceChar != null) {
                    setCharacteristicNotification(dataSourceChar, false);
                    Log.d(TAG, "dataSourceChar deregistered");
                    dataSourceChar = null;
                }
                mBluetoothGatt.disconnect();
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
                    case MSG_NC_SM_GATT_DISCONNECTED:
                        printStr.setLength(0);
                        printStr.append("Disconnected!");
                        SocketServer.sendSocketData(printStr.toString());
                        transitionTo(mNCIdle);
                        break;
                    case MSG_NC_SM_BT_ADAPTER_OFF:
                       transitionTo(mNCIdle);
                       break;
                }
                return retValue;
            }
        }
    }
}
