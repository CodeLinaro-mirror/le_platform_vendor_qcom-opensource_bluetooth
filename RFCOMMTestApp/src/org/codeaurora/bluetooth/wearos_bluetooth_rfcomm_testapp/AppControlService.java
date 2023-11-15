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
 *
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 *
 */

package org.codeaurora.bluetooth.wearos_bluetooth_rfcomm_testapp;

import static android.widget.Toast.makeText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import java.util.UUID;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import android.app.Notification;
import java.text.DecimalFormat;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;

public class AppControlService extends Service {
    private static final String TAG = "BluetoothTxRxApp Service";
    private static BluetoothAdapter bluetoothAdapter = BluetoothAdapter
            .getDefaultAdapter();
    BluetoothDevice btDeviceToConnect = null;
    public BluetoothSocket mmSocket;

    private Context mContext;
    ConfigFileParser parser;
    AppControlStateMachine mAppControlStateMachine;
    AppControlStateMachine mAppControlStateMachineforGAP;
    private ConnectThread connectThread;
    private ReadyToAcceptThread readyToAcceptThread;
    public AcceptThread mAcceptThread = null;
    public ServerConnectedThread mServerConnectedThread = null;
    private static final String name = "BluetoothRFCommNotApp";
    private static String remoteAddr;
    private static String serviceId;
    private static String appId;
    private Looper mlooper;
    public static final int DEFAULT_DATA_PATTERN = 1;
    public static final int BINARY_DATA_PATTERN  = 2;
    public static final int PRBS9_DATA_PATTERN   = 3;
    public static final int RFCOMM_PACKET_HEADER = 5;
    private FileOutputStream fos = null;

    public AppControlService() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(TAG, "Service onCreate");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            String NOTIFICATION_CHANNEL_ID = "org.codeaurora.bluetooth.wearos_bluetooth_rfcomm_testapp";
            String channelName = "RFCOMM TestApp Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(chan);
            //NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification.Builder notificationBuilder = new Notification.Builder(this,NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
            .setContentTitle("RFCOMM Test App")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build();
            startForeground(1337, notification);
        }else{
            Intent notificationIntent = new Intent(this, AppControlActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

            Notification notification = new Notification.Builder(this)
            .setContentTitle("RFCOMM Test App")
            .setContentText("Running...!!!")
            .setContentIntent(pendingIntent).build();
            startForeground(1337, notification);
        }
        parser = new ConfigFileParser();
        if (!initAdapter()) {
            Log.d(TAG, "Unexpected error: Turning off BT");
        }
        registerReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO do something useful
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "Service onStartCommand");
        Utils.mAppControlService = AppControlService.this;
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mContext = null;
        unregisterReceiver(eventReceiver);
        if (mAppControlStateMachine != null) {
            mAppControlStateMachine.cleanUp();
        }
    }

    private final IBinder localBinder = new MyBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    public class MyBinder extends Binder {

        public AppControlService getService() {
            return AppControlService.this;

        }
    }

    private boolean initAdapter() {
        Log.d(TAG, "initAdapter()");
        if (bluetoothAdapter == null) {
            Log.d(TAG, "initAdapter: Bluetooth not supported");
            return false;
        } else if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enabling bluetooth ");
            Intent enableBtIntent = new Intent(
                    bluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBtIntent);
            Log.d(TAG, "Bluetooth enabled returning true");
            return true;
        } else {
            Log.d(TAG, "initAdapter: Bluetooth has been enabled now");
            return true;
        }
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mContext.registerReceiver(eventReceiver, intentFilter);
    }

    private void registerReceiverForGap() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(eventReceiverForGap, intentFilter);
    }

    public void initServerSocket () {
        Log.d(TAG, "initServerSocket: enter()");
        if (mAcceptThread == null) {
            Log.d(TAG, "Creating Server Accept thread");
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    private BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Inside onReceive");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Socket Disconnected");
            }
        }
    };

    private BroadcastReceiver eventReceiverForGap = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Inside eventReceiverForGap");
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                SocketServer.sendSocketData("Discovery Started");
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                SocketServer.sendSocketData("Discovery Ended");
                if(mAppControlStateMachineforGAP != null)
                {
                   Message message = Message.obtain();
                   message.what = Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_END_DISCOVERY;
                   mAppControlStateMachineforGAP.sendMessage(message);
                }
            }
        }
    };

    private void manageServerSocket(BluetoothSocket socket) {
        Log.d(TAG, "manageMyConnectedSocket()");
        mServerConnectedThread = new ServerConnectedThread(socket);
        mServerConnectedThread.start();
    }

    private class ServerConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final OutputStream mOutputStream;
        private final InputStream mInputStream;

        public ServerConnectedThread(BluetoothSocket socket) {
            mSocket = socket;
            OutputStream tmpOut = null;
            InputStream tmpIn = null;
            try {
                tmpOut = mSocket.getOutputStream();
                tmpIn = mSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "ServerConnectedThread: Socket'getOutputStream/InputStream method failed", e);
            }
            mInputStream = tmpIn;
            mOutputStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "ServerConnectedThread Running run()");
            byte[] buffer = new byte[1024]; //buffer to store the stream
            int bytes;
            while (true) {
                try {
                    bytes = mInputStream.read(buffer);
                    String incomingMsg = new String(buffer, 0, bytes);
                    Log.d(TAG, "Received data in Server socket "+ incomingMsg);
                } catch (IOException e) {
                    Log.e(TAG, "ServerConnectedThread: could not read the incoming message" + e.getMessage());
                    break;
                }
            }
        }

        /* API for main application to write messages */
        public void write(byte[] data) {
            String text = new String(data, Charset.defaultCharset());
            Log.d(TAG, "ServerConnectedThread: data to be written: " + text);
            try {
                mOutputStream.write(data);
            } catch (IOException e) {
                Log.e(TAG, "ServerConnectedThread: could not write to the OutputStream" + e.getMessage());
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ServerConnectedThread: Could not close the connect socket", e);
            }
        }

        public void disconnect() {
            Log.d(TAG, "disconnect()");
            if(mSocket != null) {
                try {
                    InputStream I=mSocket.getInputStream();
                    OutputStream O=mSocket.getOutputStream();
                    if(I!=null)
                        I.close();
                    if(O!=null)
                        O.close();
                    mSocket.close();
                } catch (Exception e) {
                    Log.e(TAG, "ServerConnectedThread: Could not close the input and output streams");
                }
            }
        }
    }

    /*Thread to Create a Server RFCOMM channel and listen for accept*/
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                Log.d(TAG, "AcceptThread: Setting up BluetoothServerSocket for listening to connection requests from client");
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(name, Utils.UUIDConstants.APP_SERVER_UUID);
            } catch (IOException e) {
                SocketServer.sendSocketData("Error while creating Server Socket");
                Log.e(TAG, "AcceptThread: Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "AcceptThread Running run()");
            BluetoothSocket socket = null;

            //Keep listening till exception occurs
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                    Log.d(TAG, "AcceptThread: Connection was made, BluetoothSocket returned");
                } catch (IOException e) {
                    SocketServer.sendSocketData("Error while accepting a Server Socket");
                    Log.e(TAG, "AcceptThread: Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    manageServerSocket(socket);
                    try {
                        mmServerSocket.close();
                        break;
                    } catch (IOException e) {
                        Log.e(TAG, "AcceptThread: Socket's close() method failed", e);
                        break;
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: Could not close the connect socket", e);
            }
        }
    }

    public void startStateMachineForGAP() {
        mAppControlStateMachineforGAP = new AppControlStateMachine(
                AppControlService.this);
        Log.d(TAG, "Starting App State Machine");
        mAppControlStateMachineforGAP.start();
        Utils.appControlStateMachineforGAP = mAppControlStateMachineforGAP;
        registerReceiverForGap();
    }

    public void stopStateMachineForGAP() {
        mAppControlStateMachineforGAP.cleanUp();
        mAppControlStateMachineforGAP = null;
        unregisterReceiver(eventReceiverForGap);
    }

    protected void initializeOutgoingConnection(Connect connect) {
        if(connect.bdAddress != null && connect.uuid != null)
        {
            btDeviceToConnect = bluetoothAdapter.getRemoteDevice(connect.bdAddress.toUpperCase());
            mAppControlStateMachine = new AppControlStateMachine(AppControlService.this);
            Log.d(TAG, "Starting App State Machine for "+connect.bdAddress.toUpperCase()+" "+connect.uuid.toUpperCase());
            mAppControlStateMachine.start();
            String btAddr_uuid_direction = connect.bdAddress.toUpperCase()+" "+connect.uuid.toUpperCase()+ " "+ "CLIENT";
            Utils.btAddrUUIDToStateMachineMap.put(btAddr_uuid_direction,mAppControlStateMachine);
            Message message = Message.obtain();
            message.what = Utils.StateMachineMessageConstants.STATE_READY_TO_CONNECT;
            mAppControlStateMachine.sendMessage(message);
            startConnectionProcess(btDeviceToConnect,connect.uuid);
        }
        else
        {
            SocketServer.sendSocketData("Unable to Process BT Address...Please Try again");
            return;
        }
    }

    public void startConnectionProcess(BluetoothDevice bt_device, String uuid_to_connect) {
        Log.d(TAG, "startConnectionProcess for "+bt_device.getAddress() + " "+uuid_to_connect);
        connectThread = new ConnectThread(bt_device,uuid_to_connect);
        connectThread.start();
    }

    public void closeConnection(String disconnectedHandle) {
        Log.d(TAG, "closeConnection :: "+disconnectedHandle);
        String[] tmp = disconnectedHandle.split(" ",3);
        SocketServer.sendSocketData("Device is Disconnected");
        SocketServer.sendSocketData("Remote Device  :"+tmp[0]);
        SocketServer.sendSocketData("UUID           :"+tmp[1]);
        SocketServer.sendSocketData("Direction      :"+tmp[2]);
        AppControlStateMachine appControlStateMachine = (AppControlStateMachine)Utils.btAddrUUIDToStateMachineMap.remove(disconnectedHandle);
        if(appControlStateMachine != null){
            Log.d(TAG, "closeConnection :: releasing State Machine");
            appControlStateMachine.cleanUp();
            appControlStateMachine = null;
        }

        BluetoothSocket socket = (BluetoothSocket)Utils.btAddrUUIDToBTSocketMap.remove(disconnectedHandle);
        try {
            if (socket != null) {
                Log.d(TAG, "closeConnection :: releasing Bluetooth Socket");
                if (socket.getInputStream() != null) {
                    socket.getInputStream().close();
                }
                if (socket.getOutputStream() != null) {
                    socket.getOutputStream().close();
                }
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not close the socket", e);
        }

        if(Utils.btAddrUUIDToBTSocketMap.size() == 0){
            Log.d(TAG,"All Connections are closed. Move to Connections Screen");
            if(Utils.isSppConnection){
              SocketServer.processOutputState = SocketServer.BLUETOOTH_SPP_CONNECT_MENU;
              SocketServer.mainMenuState = SocketServer.BLUETOOTH_SPP_CONNECT_MENU;
            }
            else
            {
            SocketServer.processOutputState = SocketServer.THROUGHPUT_TESTING_MENU;
            SocketServer.mainMenuState = SocketServer.THROUGHPUT_TESTING_MENU;
            }
            SocketServer.updateSocketClient();
        }else{
            Log.d(TAG,"Connections are available. Refresh Device Selection Screen");
            SocketServer.processOutputState = SocketServer.DEVICE_SELECTION_MENU;
            SocketServer.mainMenuState = SocketServer.DEVICE_SELECTION_MENU;
            SocketServer.updateSocketClient();
        }
    }

    public void closeConnection() {
        Log.d(TAG, "closeConnection");
        if (connectThread != null) {
            connectThread.cancel();
        }
        if (readyToAcceptThread != null) {
            readyToAcceptThread.cancel();
        }
        if (mServerConnectedThread != null) {
            mServerConnectedThread.cancel();
            mServerConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
    }


    private class ConnectThread extends Thread {
        private static final String TAG = "BluetoothTxRxApp Connect Thread";
        BluetoothDevice mDevice;
        String mUuid;

        public ConnectThread(BluetoothDevice device, String uuid) {
            mDevice = device;
            mUuid = uuid;
            BluetoothSocket tmp = null;
            if(mDevice == null)
            {
                Log.d(TAG, "ConnectThread: Remote Device is null");
                SocketServer.sendSocketData("Remote Device is Null... Please close and restart the Process");
                return;
            }
            else
            {
                Log.d(TAG, "ConnectThread: Device is not null");
            }

            try {
                Log.d(TAG, "Creating a BluetoothSocket to create a connection to a remote device with uuid " + mUuid);
                tmp = mDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(mUuid));
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            } catch (Exception e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }

            if(tmp == null)
            {
                Log.d(TAG, "Socket is null");
                SocketServer.sendSocketData("Communication Socket is not created... Please close and restart the Process");
                return;
            }
            else
            {
                Log.d(TAG, "Socket is not null");
            }

            mmSocket = tmp;
        }

        @Override
        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                Log.d(TAG, "connecting to a remote device");
                if(mmSocket != null)
                {
                    mmSocket.connect();
                }
                else
                {
                    Log.d(TAG, "Socket is null");
                    SocketServer.sendSocketData("Communication Socket is not created... Please close and restart the Process");
                    return;
                }
                Log.d(TAG, "Socket Connected");
                Utils.btAddrUUIDToBTSocketMap.put(mDevice.getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+" CLIENT", mmSocket);
                if(Utils.isSppConnection){
                    Message message = Message.obtain();
                    message.what = Utils.StateMachineMessageConstants.STATE_SPP_CONNECTED;
                    message.obj = mDevice.getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "CLIENT";
                    if(Utils.btAddrUUIDToStateMachineMap.get(mDevice.getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "CLIENT") != null){
                            Utils.btAddrUUIDToStateMachineMap.get(mDevice.getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "CLIENT").sendMessage(message);
                        }
                }
                else
                {
                    Log.d(TAG,"Utils.isThroughputStateMachineUnderProcessing :: "+Utils.isThroughputStateMachineUnderProcessing);
                    if (Utils.isThroughputStateMachineUnderProcessing == true)
                    {
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_CONNECTED;
                        message.obj = mDevice.getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "CLIENT";
                        if(Utils.btAddrUUIDToStateMachineMap.get(mDevice.getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "CLIENT") != null){
                            Utils.btAddrUUIDToStateMachineMap.get(mDevice.getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "CLIENT").sendMessage(message);
                        }
                    }
                }

            } catch (IOException connectException) {
                Log.e(TAG, "Unable to connect; close the socket and return",
                        connectException);
                try {
                    mmSocket.close();
                    if(Utils.isSppConnection)
                    {
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_CONNECTION_FAILED;
                            if(Utils.btAddrUUIDToStateMachineMap.get(mDevice.getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "CLIENT") != null){
                                Utils.btAddrUUIDToStateMachineMap.get(mDevice.getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "CLIENT").sendMessage(message);
                            }
                    }
                    else {
                    Log.d(TAG,"Utils.isThroughputStateMachineUnderProcessing :: "+Utils.isThroughputStateMachineUnderProcessing);
                    if (Utils.isThroughputStateMachineUnderProcessing == true) {
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_CONNECTION_FAILED;
                        if(Utils.btAddrUUIDToStateMachineMap.get(mDevice.getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "CLIENT") != null){
                            Utils.btAddrUUIDToStateMachineMap.get(mDevice.getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "CLIENT").sendMessage(message);
                            }
                        }
                    }
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket",
                            closeException);
                }
                return;
            }
        }

        public void cancel() {
            try {
                if (mmSocket != null) {
                    if (mmSocket.getInputStream() != null) {
                        mmSocket.getInputStream().close();
                    }
                    if (mmSocket.getOutputStream() != null) {
                        mmSocket.getOutputStream().close();
                    }
                    mmSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    public void startTxOperation(Tx txParam) {
        Runnable txOperationRunnable = new TxOperationRunnable(txParam);
        new Thread(txOperationRunnable).start();
    }

    private class TxOperationRunnable implements Runnable {

        Tx mTx;

        public TxOperationRunnable(Tx tx) {
            mTx = tx;
        }

        public void run() {
            int mChunkSize = mTx.chunkSize;
            int mPattern = mTx.pattern;
            BluetoothSocket mSocket = mTx.socket;
            String[] tmp = mTx.bt_addr_uuid.split(" ",3);
            String mUuid = tmp[2];
            OutputStream outputStream = null;
            long tx_start_time, tx_end_time;
            try
            {
               outputStream = mSocket.getOutputStream();
            }
            catch (IOException e)
            {
               Log.e(TAG, "Error occurred when creating output stream", e);
            }
            int chunksize = mChunkSize;
            //chunksize = chunksize * 1024;
            Log.d(TAG, "chunkSize is :: " + chunksize);
            Log.d(TAG, "pattern is :: " + mPattern);
            StringBuilder sb = new StringBuilder(1024);

            if(mPattern == DEFAULT_DATA_PATTERN) {
               for(int i = 0; i < 1024 - RFCOMM_PACKET_HEADER; i++) {
                   sb.append('a');
              }
            }

            else if(mPattern == BINARY_DATA_PATTERN) {
               for(int i = 0; i < 1024 - RFCOMM_PACKET_HEADER; i++) {
                   if(i%2 == 0)
                       sb.append('1');
                   else
                       sb.append('0');
                }
            }
            else
            {
                int start = 0x02;
                int a = start;
                int i;
                for(i = 0;i<1024 - RFCOMM_PACKET_HEADER; i++) {
                   int newbit = (((a >> 9) ^ (a >> 6)) & 1);
                   a = ((a << 1) | newbit) & 0x7ff;
                   int b = a & 1;
                   sb.append(Integer.toString(b));
                }
            }
            String senttext = sb.toString();
            String start = "Start";
            String end = "End";
            bluetoothAdapter.cancelDiscovery();
            tx_start_time = SystemClock.elapsedRealtime();
            Log.d(TAG, "start time: " + tx_start_time);
            Log.d(TAG, "senttext.length() :: " + senttext.length());
            try {
                int i =1;
                outputStream.write(start.getBytes());
                while(i <= chunksize)
                {
                    String header = String.format("$%04d", i);
                    header += senttext;
                    outputStream.write(header.getBytes());
                    i++;
                }
                outputStream.write(end.getBytes());
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
                Message message = Message.obtain();
                message.what = Utils.StateMachineMessageConstants.STATE_DATA_TX_FAILED;
                if(Utils.btAddrUUIDToStateMachineMap.get(mTx.bt_addr_uuid) != null){
                    Utils.btAddrUUIDToStateMachineMap.get(mTx.bt_addr_uuid).sendMessage(message);
                }
            }
            tx_end_time = SystemClock.elapsedRealtime();
            Log.d(TAG, "end time: " + tx_end_time);
            Log.d(TAG, "end_time - start_time: "
                    + (tx_end_time - tx_start_time));

            // throughput calculations
            float TxTput = (((float) senttext.length() + RFCOMM_PACKET_HEADER) * 8 * 1000 * chunksize)
                    / (tx_end_time - tx_start_time);
            float TxTputk = TxTput / 1000;
            Log.d(TAG, "write: Through put (send) is (in kbps): " + TxTputk);
            SocketServer.sendSocketData("Tx Results");
            SocketServer.sendSocketData("----------");
            SocketServer.sendSocketData("Device Address       : "+tmp[0]);
            SocketServer.sendSocketData("Connection UUID      : "+tmp[1]);
            SocketServer.sendSocketData("Connection Direction : "+tmp[2]);
            SocketServer.sendSocketData("Throughput (in kbps) : "+TxTputk);
            Message message = Message.obtain();
            message.what = Utils.StateMachineMessageConstants.STATE_END_DATA_TX;
            message.obj = mTx;
            mAppControlStateMachine.sendMessage(message);

        }
    }

    public void startRxOperation(Rx rxParam) {
        OutputStream outputStream = null;
        Log.d(TAG, "startRxOperation");
        try {
            if(rxParam.socket != null){
                outputStream = rxParam.socket.getOutputStream();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        Runnable rxOperationRunnable = new RxOperationRunnable(rxParam);
        new Thread(rxOperationRunnable).start();
    }

    private class RxOperationRunnable implements Runnable {

        Rx mRx;

        public RxOperationRunnable(Rx rx) {
            mRx = rx;
        }

        public void run() {
            InputStream inputStream = null;
            BluetoothSocket mSocket = mRx.socket;
            String[] tmp = mRx.bt_addr_uuid.split(" ",3);
            long rx_start_time = 0, rx_end_time = 0;
            byte[] mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()
            int totalBytes = 0; // totalBytes received
            int pktStartIdx=0;
            int prvSrlNo=0;
            int curSrlNo = 0;
            int pktMissingIdx = 0;
            String incompPkt="";
            File file = null;
            try
            {
                inputStream = mSocket.getInputStream();
            }
            catch (IOException e)
            {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            while (true) {
                try {
                    while((numBytes = inputStream.read(mmBuffer, 0, mmBuffer.length)) != -1) {
                    totalBytes = totalBytes + numBytes;
                    String incomingMsg = new String(mmBuffer, 0, numBytes);
                        if(Utils.isSppFileTransferOngoing == true){
                            if(incomingMsg.contains("SPP_START_SENDING_FILE")){
                                file = new File(Environment.getExternalStorageDirectory() + "/" + "test_spp_from_remote.txt");
                                boolean created = file.createNewFile();
                                Log.d(TAG,"is file created :: "+created);
                                new FileOutputStream(file,false).close();
                                fos = new FileOutputStream(file,false);
                            }
                            Log.d(TAG,"writing to file is :: "+incomingMsg);
                            fos.write(mmBuffer,0,numBytes);
                            if(incomingMsg.contains("SPP_END_SENDING_FILE")){
                                SocketServer.sendSocketData("File Receive Complete");
                                SocketServer.sendSocketData("---------------------");
                                SocketServer.sendSocketData("Device Address       : "+tmp[0]);
                                SocketServer.sendSocketData("Connection UUID      : "+tmp[1]);
                                SocketServer.sendSocketData("Connection Direction : "+tmp[2]);
                                Message message = Message.obtain();
                                message.what = Utils.StateMachineMessageConstants.STATE_END_RECEIVE_FILE;
                                message.obj = mRx;
                                mAppControlStateMachine.sendMessage(message);
                            }
                        }
                        else
                        {
                            if (incomingMsg.contains("Start")) {
                                rx_start_time = SystemClock.elapsedRealtime();
                                Log.d(TAG, "start_time: " + rx_start_time);
                                pktStartIdx=0;
                                prvSrlNo=0;
                                curSrlNo=0;
                                pktMissingIdx=0;
                            }
                            Log.d(TAG, "Incoming msg received in ClientSocket "+incomingMsg);
                            pktStartIdx = 0;
                            pktMissingIdx = incompPkt.indexOf("$",0);
                            if(pktMissingIdx == 0){
                                incomingMsg = incompPkt + incomingMsg;
                                numBytes += incompPkt.length();
                                incompPkt="";
                            }
                            while(pktStartIdx != -1){
                                pktStartIdx = incomingMsg.indexOf("$", pktStartIdx);
                                if(pktStartIdx >= 0 )
                                {
                                    if(numBytes >= pktStartIdx+5 )
                                    {
                                        curSrlNo = Integer.parseInt(incomingMsg.substring(pktStartIdx+1 ,pktStartIdx+5));
                                        if(curSrlNo == prvSrlNo + 1) {
                                            Log.d(TAG, "Received Packet " +curSrlNo);
                                            prvSrlNo = curSrlNo;
                                        }
                                        else{
                                            while(curSrlNo > prvSrlNo){
                                                prvSrlNo++;
                                                Log.d(TAG, "Missed Packet " + prvSrlNo);
                                            }
                                        }
                                        pktStartIdx++;
                                    }
                                    else
                                    {
                                        incompPkt = incomingMsg.substring(pktStartIdx, numBytes);
                                        pktStartIdx = -1;
                                    }
                                }
                            }
                            if (incomingMsg.contains("end") || incomingMsg.contains("nd") || incomingMsg.contains("d")) {
                                rx_end_time = SystemClock.elapsedRealtime();
                                Log.d(TAG, "end_time: " + rx_end_time);
                                Log.d(TAG, "end_time - start_time = "
                                        + ((rx_end_time - rx_start_time) / 1000));
                                Log.d(TAG, "totalBytes = " + totalBytes);
                                Log.d(TAG, "totalBits =  " + (totalBytes * 8));
                                // throughput calculation start
                                float RxTput = ((float) totalBytes * 8 * 1000)
                                        / (rx_end_time - rx_start_time);
                                float RxTputk = RxTput / 1000;
                                // throughput calculation end
                                Log.d(TAG,
                                        "write: Through put (receive) is approximately(in kbps): "
                                                + RxTputk);
                                SocketServer.sendSocketData("Rx Results");
                                SocketServer.sendSocketData("----------");
                                SocketServer.sendSocketData("Device Address       : "+tmp[0]);
                                SocketServer.sendSocketData("Connection UUID      : "+tmp[1]);
                                SocketServer.sendSocketData("Connection Direction : "+tmp[2]);
                                SocketServer.sendSocketData("Throughput (in kbps) : "+RxTputk);
                                totalBytes = 0;
                                Message message = Message.obtain();
                                message.what = Utils.StateMachineMessageConstants.STATE_END_DATA_RX;
                                message.obj = mRx;
                                mAppControlStateMachine.sendMessage(message);
                            }
                        }
                    }
                    if(Utils.isSppFileTransferOngoing == true){
                        Log.d(TAG,"inside SPP_END_SENDING_FILE");
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_END_RECEIVE_FILE;
                        mAppControlStateMachine.sendMessage(message);
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    Message message = Message.obtain();
                    message.what = Utils.StateMachineMessageConstants.STATE_DISCONNECTED;
                    message.obj = mRx.bt_addr_uuid;
                    if(Utils.btAddrUUIDToStateMachineMap.get(mRx.bt_addr_uuid) != null){
                        Utils.btAddrUUIDToStateMachineMap.get(mRx.bt_addr_uuid).sendMessage(message);
                    }
                    break;
                }
            }
        }
    };

    public void setScanMode(int scanMode) {
        Log.d(TAG, "scanMode is :: " + scanMode);
        int result = -1;
        switch (scanMode) {
        case 0:
            SocketServer.sendSocketData("Setting Scan Mode :: SCAN_MODE_NONE");
            result = bluetoothAdapter
                    .setScanMode(BluetoothAdapter.SCAN_MODE_NONE);
            break;
        case 1:
            SocketServer
            .sendSocketData("Setting Scan Mode :: SCAN_MODE_CONNECTABLE");
            result = bluetoothAdapter
                    .setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
            break;
        case 2:
            SocketServer
            .sendSocketData("Setting Scan Mode :: SCAN_MODE_CONNECTABLE_DISCOVERABLE");
            result = bluetoothAdapter
                    .setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
            break;
        default:
            break;
        }
        if (BluetoothStatusCodes.SUCCESS == result) {
            SocketServer.sendSocketData("Set Scan Mode Successful");
        } else {
            SocketServer
            .sendSocketData("Set Scan Mode Failed...Please try again");
        }
        Message message = Message.obtain();
        message.what = Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_END_SCAN_MODE;
        mAppControlStateMachineforGAP.sendMessage(message);
        SocketServer.processOutputState = SocketServer.CONNECTION_TEST_MENU;
        SocketServer.mainMenuState = SocketServer.CONNECTION_TEST_MENU;
        SocketServer.updateSocketClient();
    }

    public void startOnOffTestCase(int count) {
        int iterationCount = 1;
        Log.d(TAG, "startOnOffTestCase onOffCount is ::" + count);
        if (count > 0) {
            for (int i = count; i > 0; i--) {
                try {
                    SocketServer.sendSocketData("Iteration " + iterationCount);
                    if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        runCommand(Utils.AppControlConstants.BT_OFF_COMMAND);
                        SocketServer
                        .sendSocketData("BT is turning Off...Please wait..!!");
                        Thread.sleep(5000);
                        checkForDumpsys();
                        runCommand(Utils.AppControlConstants.BT_ON_COMMAND);
                        SocketServer
                        .sendSocketData("BT is turning On...Please wait..!!");
                        Thread.sleep(8000);
                        checkForDumpsys();
                    } else {
                        runCommand(Utils.AppControlConstants.BT_ON_COMMAND);
                        SocketServer
                        .sendSocketData("BT is turning On...Please wait..!!");
                        Thread.sleep(8000);
                        checkForDumpsys();
                        runCommand(Utils.AppControlConstants.BT_OFF_COMMAND);
                        SocketServer
                        .sendSocketData("BT is turning Off...Please wait..!!");
                        Thread.sleep(5000);
                        checkForDumpsys();
                    }
                    iterationCount++;
                } catch (IOException e) {
                    Log.e(TAG, "There is an exception when running command");
                    e.printStackTrace();
                    SocketServer
                    .sendSocketData("BT OFF->ON Test case failed. Please check logcat for more Info.");
                } catch (InterruptedException e) {
                    Log.e(TAG, "There is an exception when running command");
                    e.printStackTrace();
                    SocketServer
                    .sendSocketData("BT OFF->ON Test case failed. Please check logcat for more Info.");
                }
            }
        }
        Message message = Message.obtain();
        message.what = Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_END_OFF_ON;
        mAppControlStateMachineforGAP.sendMessage(message);
        SocketServer.processOutputState = SocketServer.CONNECTION_TEST_MENU;
        SocketServer.mainMenuState = SocketServer.CONNECTION_TEST_MENU;
        SocketServer.updateSocketClient();
    }

    private Process runCommand(String commandToRun) throws IOException,
    InterruptedException {
        Log.d(TAG, "Running command ::" + commandToRun);
        Process pr = Runtime.getRuntime().exec(commandToRun);
        pr.waitFor();
        return pr;
    }

    private void checkForDumpsys() {
        String generateDumpSys = "dumpsys bluetooth_manager";
        try {
            Process dumpsysPr = runCommand(generateDumpSys);
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(dumpsysPr.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Log.d(TAG, "Data is ::" + line + "\n");
                if (line.contains("enabled")) {
                    String tmp[] = line.split(":", 2);
                    if (tmp.length == 2) {
                        if (tmp[0].trim().equals("enabled")) {
                            Log.d(TAG, "status is ::" + tmp[1].trim());
                            if (tmp[1].trim().equals("false")) {
                                SocketServer
                                .sendSocketData("BT Turned Off Successfully");
                            } else if (tmp[1].trim().equals("true")) {
                                SocketServer
                                .sendSocketData("BT Turned On Successfully");
                            }
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "There is an exception while running dumpsys");
            e.printStackTrace();
            SocketServer
            .sendSocketData("BT OFF->ON Test case failed. Please check logcat for more Info.");
        } catch (InterruptedException e) {
            Log.e(TAG, "There is an exception while running dumpsys");
            e.printStackTrace();
            SocketServer
            .sendSocketData("BT OFF->ON Test case failed. Please check logcat for more Info.");
        }
    }

    public void startDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "cancelDiscovery");
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    public void startReadyToAcceptConnection(IncomingConnection incomingConnection) {
        Log.d(TAG, "startReadyToAcceptConnection");
        mAppControlStateMachine = new AppControlStateMachine(AppControlService.this);
        Log.d(TAG, "Starting App State Machine for "+incomingConnection.uuid.toUpperCase());
        mAppControlStateMachine.start();
        String incoming_conn_direction = incomingConnection.uuid.toUpperCase()+ " "+ "SERVER";
        Utils.btAddrUUIDToStateMachineMap.put(incoming_conn_direction,mAppControlStateMachine);
        Message message = Message.obtain();
        message.what = Utils.StateMachineMessageConstants.STATE_READY_TO_ACCEPT_CONNECTION;
        mAppControlStateMachine.sendMessage(message);
        SocketServer.sendSocketData("Accepting Incoming Connection with UUID :: "+incomingConnection.uuid);
        readyToAcceptThread = new ReadyToAcceptThread(incomingConnection.uuid);
        readyToAcceptThread.start();
    }

    private class ReadyToAcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mUuid;

        public ReadyToAcceptThread(String uuid) {
           BluetoothServerSocket tmp = null;
           mUuid = uuid;
            try {
                Log.d(TAG,
                        "ReadyToAcceptThread: Setting up BluetoothServerSocket for listening to connection requests from client");
                tmp = bluetoothAdapter
                        .listenUsingInsecureRfcommWithServiceRecord(
                                "RFCOMM Test App",
                                UUID.fromString(mUuid));
                Message message = Message.obtain();
                message.what = Utils.StateMachineMessageConstants.STATE_READY_TO_ACCEPT_CONNECTION;
                if(Utils.btAddrUUIDToStateMachineMap.get(uuid) != null){
                    Utils.btAddrUUIDToStateMachineMap.get(uuid).sendMessage(message);
                }
            } catch (IOException e) {
                Log.e(TAG,
                        "ReadyToAcceptThread: Socket's listen() method failed",
                        e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "ReadyToAcceptThread run()");

            //Keep listening till exception occurs
            while (true) {
                try {
                    Log.d(TAG, "ReadyToAcceptThread: going to wait till connection is made");
                    if(mmServerSocket == null)
                    {
                        SocketServer.sendSocketData("Socket Listening failed... Please check if Bluetooth is turned ON or OFF");
                        SocketServer.processOutputState = SocketServer.INIT_MENU;
                        SocketServer.mainMenuState = SocketServer.INIT_MENU;
                        SocketServer.updateSocketClient();
                        return;
                    }
                    else{
                        mmSocket = mmServerSocket.accept();
                    }
                    Utils.btAddrUUIDToBTSocketMap.put(mmSocket.getRemoteDevice().getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+" "+"SERVER", mmSocket);
                    AppControlStateMachine appControlStateMachine = (AppControlStateMachine)Utils.btAddrUUIDToStateMachineMap.remove(mUuid.toUpperCase()+ " " +"SERVER");
                    Utils.btAddrUUIDToStateMachineMap.put(mmSocket.getRemoteDevice().getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+" "+"SERVER",appControlStateMachine);
                    Log.d(TAG, "ReadyToAcceptThread: Connection was made, BluetoothSocket returned");
                    if(Utils.isSppConnection){
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_SPP_CONNECTED;
                        message.obj = mmSocket.getRemoteDevice().getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "SERVER";
                        if(Utils.btAddrUUIDToStateMachineMap.get(mmSocket.getRemoteDevice().getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "SERVER") != null){
                                Utils.btAddrUUIDToStateMachineMap.get(mmSocket.getRemoteDevice().getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "SERVER").sendMessage(message);
                        }
                    }
                    else
                    {
                        Log.d(TAG,"Utils.isThroughputStateMachineUnderProcessing :: "+Utils.isThroughputStateMachineUnderProcessing);
                        if (Utils.isThroughputStateMachineUnderProcessing == true)
                        {
                            Message message = Message.obtain();
                            message.what = Utils.StateMachineMessageConstants.STATE_CONNECTED;
                            message.obj = mmSocket.getRemoteDevice().getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "SERVER";
                            if(Utils.btAddrUUIDToStateMachineMap.get(mmSocket.getRemoteDevice().getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "SERVER") != null){
                                Utils.btAddrUUIDToStateMachineMap.get(mmSocket.getRemoteDevice().getAddress().toString().toUpperCase()+" "+mUuid.toUpperCase()+ " "+ "SERVER").sendMessage(message);
                            }
                        }
                    }
                    try {
                        mmServerSocket.close();
                        break;
                    }catch (IOException e) {
                        Log.e(TAG, "AcceptThread: Socket's close() method failed", e);
                        break;
                    }


                }
                catch (IOException e)
                {
                    Log.e(TAG,"ReadyToAcceptThread: Socket's accept() method failed", e);
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
                if(mmSocket != null){
                    if(mmSocket.getInputStream() != null){
                        mmSocket.getInputStream().close();
                    }
                    if(mmSocket.getOutputStream() != null){
                        mmSocket.getOutputStream().close();
                    }
                    mmSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG,"ReadyToAcceptThread: Could not close the connect socket",e);
            }

        }
    }

    public void checkIsConnected(String bdAddress){
        BluetoothDevice btDevice = bluetoothAdapter.getRemoteDevice(bdAddress.toUpperCase());
        if(btDevice != null){
            Log.d(TAG,"btDevice is not null");
            boolean result = btDevice.isConnected();
            if(result == false){
                Log.d(TAG,"Device is not connected");
                SocketServer
                        .sendSocketData("Device "
                                + btDevice.getAddress()
                                + " is not connected");
            }else{
                Log.d(TAG,"Device is connected");
                SocketServer
                        .sendSocketData("Device "
                                + btDevice.getAddress()
                                + " is connected");
            }
        }else{
            Log.d(TAG,"btDevice is null");
        }
        SocketServer.processOutputState = SocketServer.CONNECTION_TEST_MENU;
        SocketServer.updateSocketClient();
    }

    public void startSendingFile(Utils.TxForSpp txParam){
        BluetoothSocket mSocket = txParam.socket;
        String[] tmp = txParam.bt_addr_uuid.split(" ",3);
        String mUuid = tmp[2];
        String fileName = txParam.fileName;
        OutputStream outputStream = null;

        Log.d(TAG, "Inside startSendingFile :: "+ txParam);
        File sdcard = Environment.getExternalStorageDirectory();
        File srcFile = new File(sdcard,fileName);
        File dstFile = new File(mContext.getFilesDir(), fileName);
        if(srcFile.exists() && !srcFile.isDirectory()){
            try{
                copyFile(srcFile,dstFile);
            }catch(IOException e){
                Log.e(TAG,"Caught IOException while copying file :: "+e);
            }
            if(dstFile.exists() && !dstFile.isDirectory()) {
                SocketServer.sendSocketData("File Exists...Transferring File\n");
                Runnable sendFileRunnable = new SendFileRunnable(fileName);
                try{
                    Thread sendFile = new Thread(sendFileRunnable);
                    sendFile.start();
                    sendFile.join();
                }catch(InterruptedException e){
                    Log.e(TAG,"Caught InterruptedException while starting thread :: "+e);
                }
            }else{
                SocketServer.sendSocketData("Something went wrong...Please try again\n");
            }
        }else{
            SocketServer.sendSocketData("Source File Doesn't exist\n");
        }

        SocketServer.sendSocketData("----------");
        SocketServer.sendSocketData("Device Address       : "+tmp[0]);
        SocketServer.sendSocketData("Connection UUID      : "+tmp[1]);
        SocketServer.sendSocketData("Connection Direction : "+tmp[2]);

        Message message = Message.obtain();
        message.what = Utils.StateMachineMessageConstants.STATE_END_SEND_FILE;
        mAppControlStateMachine.sendMessage(message);
    }

    public void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private class SendFileRunnable implements Runnable {

        String mFileName;

        public SendFileRunnable(String fileName) {
            mFileName = fileName;
        }

        public void run() {

            OutputStream outputStream = null;
            try {
                outputStream = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }
            File myFile = new File(mContext.getFilesDir(), mFileName);
            Log.d(TAG,"file created success!");

            byte[] mybytearray = new byte[(int)myFile.length()];
            Log.d(TAG,"file length() =" + (int)myFile.length());

            try{
                FileInputStream fis = new FileInputStream(myFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                byte[] buffer = new byte[1024];
                int len = -1;
                String start_send_file = "SPP_START_SENDING_FILE";
                byte[] start_send_file_bytes = start_send_file.getBytes();
                outputStream.write(start_send_file_bytes,0,start_send_file_bytes.length);
                while ((len = bis.read(buffer)) != -1) {
                    String strFileContents = new String(buffer, 0, len);
                    Log.d(TAG,"strFileContents is :: "+strFileContents);
                    outputStream.write(buffer,0,len);
                }
                String end_send_file = "SPP_END_SENDING_FILE";
                byte[] end_send_file_bytes = end_send_file.getBytes();
                outputStream.write(end_send_file_bytes,0,end_send_file_bytes.length);

                SocketServer.sendSocketData("File Transfer Complete");
            }catch(Exception e){
                Log.e(TAG,"Caught Error :: "+e);
            }
        }
    }

}
