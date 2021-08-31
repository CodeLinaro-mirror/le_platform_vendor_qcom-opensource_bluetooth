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

package org.codeaurora.bluetooth.wearos_bluetooth_rfcomm_testapp;

import static android.widget.Toast.makeText;

//import com.qualcomm.bluetooth_offload.client.*;

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

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
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
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.Notification;


public class AppControlService extends Service {
    private static final String TAG = "RFCOMMTestApp Service";
    private static BluetoothAdapter bluetoothAdapter = BluetoothAdapter
            .getDefaultAdapter();
    BluetoothDevice btDeviceToPair = null;
    private BluetoothSocket mmSocket;
    private Context mContext;
    ConfigFileParser parser;
    AppControlStateMachine mAppControlStateMachine;
    NotificationOffloadStateMachine mNotificationOffloadStateMachine;
    private ConnectThread connectThread;
    private ReadyToAcceptThread readyToAcceptThread;
    private Semaphore getSdpSearchSem = new Semaphore(0);
    public AcceptThread mAcceptThread = null;
    public ServerConnectedThread mServerConnectedThread = null;
    private static final String name = "BluetoothRFCommNotApp";
    private static boolean sdpRecordFound = false;
    private Looper mlooper;
    public static OffloadServiceMessageHandler msghandler = null;
    //public OffloadableAppAdapter mOfflodableAppAdapter;

    //offload callback status values
    public static final int BT_FAIL = 0;
    public static final int BT_OK = 1;
    public static final int BT_INVALID_STATE = 2;

    private static LinkedList<NotificationPacketInd> NotificationPacketList = new LinkedList<NotificationPacketInd>();

    public static class NotificationPacketInd {
        byte NotificationID;
        byte NotificationStatus;
        byte action = 0x00;
        byte getAttID;
        byte[] NotificationHandle = new byte[2];
        Semaphore sem = new Semaphore(0);
        Semaphore getInfoSem = new Semaphore(0);

        public NotificationPacketInd(byte[] value) {
            NotificationID = value[5];
            NotificationStatus = value[6];
            NotificationHandle = Arrays.copyOfRange(value, 3, 5);
        }
    }

    public AppControlService() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(TAG, "Service onCreate");
        Intent notificationIntent = new Intent(this, AppControlActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
        .setContentTitle("RFCOMM Test App")
        .setContentText("Running...!!!")
        .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);
        parser = new ConfigFileParser();
        if (!initAdapter()) {
            Log.d(TAG, "Unexpected error: Turning off BT");
        } else {
            startStateMachine();
        }
        registerReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO do something useful
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "Service onStartCommand");
        HandlerThread Thread = new HandlerThread("OffloadServiceHandler");
        Thread.start();
        mlooper = Thread.getLooper();
        msghandler = new OffloadServiceMessageHandler(mContext, mlooper);
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
        /* Stop Offload Service msg hdlr looper*/
        mlooper.quitSafely();
        SocketServer.cleanUp();
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
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_SDP_RECORD);
        mContext.registerReceiver(eventReceiver, intentFilter);
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
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (Utils.bdAddressFromConfig != null && device != null) {
                    Log.d(TAG, "Disconnected Device :: " + device.getAddress());
                    Log.d(TAG, "RFCOMM Test App Connected Device :: "
                            + Utils.bdAddressFromConfig);
                    if (Utils.bdAddressFromConfig.equalsIgnoreCase(device
                            .getAddress())) {
                        Log.d(TAG, "Socket Disconnected");
                        SocketServer
                        .sendSocketData("Connection to Remote Device "
                                + device.getAddress()
                                + " is Terminated");
                        Log.d(TAG,"Utils.isThroughputStateMachineUnderProcessing :: "+Utils.isThroughputStateMachineUnderProcessing);
                        Log.d(TAG,"Utils.isOffloadStateMachineUnderProcessing :: "+Utils.isOffloadStateMachineUnderProcessing);
                        if (Utils.isThroughputStateMachineUnderProcessing == true) {
                            Message message = Message.obtain();
                            message.what = Utils.StateMachineMessageConstants.STATE_DISCONNECTED;
                            Utils.appControlStateMachine.sendMessage(message);
                        }
                        if (Utils.isOffloadStateMachineUnderProcessing == true) {
                            Message message = Message.obtain();
                            message.what = Utils.NotificationOffloadStateMachineMessageConstants.STATE_DISCONNECTED;
                            Utils.notificationOffloadStateMachine
                            .sendMessage(message);
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                SocketServer.sendSocketData("Discovery Started");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                    .equals(action)) {
                SocketServer.sendSocketData("Discovery Ended");
                Message message = Message.obtain();
                message.what = Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_END_DISCOVERY;
                mAppControlStateMachine.sendMessage(message);
            } else if (BluetoothDevice.ACTION_SDP_RECORD.equals(action)) {
                Log.d(TAG, "Received ACTION_SDP_RECORD intent with status "+intent.getParcelableExtra(BluetoothDevice.EXTRA_SDP_SEARCH_STATUS)+
                        " for uuid " +intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID)+ " Extra sdp record "+ intent.getParcelableExtra(BluetoothDevice.EXTRA_SDP_RECORD));
                if (intent.getParcelableExtra(BluetoothDevice.EXTRA_SDP_RECORD) == null) {
                    Log.e(TAG, "SDP Record not found for UUID "+intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID));
                } else {
                    Log.d(TAG, "SDP Record found for UUID "+intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID));
                    sdpRecordFound = true;
                }
                getSdpSearchSem.release();
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

    private void startStateMachine() {
        mAppControlStateMachine = new AppControlStateMachine(
                AppControlService.this);
        Log.d(TAG, "Starting App State Machine");
        mAppControlStateMachine.start();
        Utils.appControlStateMachine = mAppControlStateMachine;

        mNotificationOffloadStateMachine = new NotificationOffloadStateMachine(
                AppControlService.this);
        Log.d(TAG, "Starting Notification Offload State Machine");
        mNotificationOffloadStateMachine.start();
        Utils.notificationOffloadStateMachine = mNotificationOffloadStateMachine;
    }

    protected void initializeTestSetup() {
        if (Utils.bdAddressFromConfig != null) {
            btDeviceToPair = bluetoothAdapter
                    .getRemoteDevice(Utils.bdAddressFromConfig.toUpperCase());
        } else {
            SocketServer
                .sendSocketData("Unable to Process BT Address...Please Restart the Apps");
            return;
        }
        startConnectionProcess();
    }

    public void startConnectionProcess() {
        Log.d(TAG, "startConnectionProcess");
        connectThread = new ConnectThread(btDeviceToPair);
        connectThread.start();
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

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;

            if (device == null) {
                Log.d(TAG, "ConnectThread: Remote Device is null");
                SocketServer
                .sendSocketData("Remote Device is Null... Please close and restart the Process");
                return;
            } else {
                Log.d(TAG, "ConnectThread: Device is not null");
            }

            try {
                Log.d(TAG, "Creating a BluetoothSocket to create a connection to a remote device");
                device.sdpSearch(ParcelUuid.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66"));
                getSdpSearchSem.acquire();
                if (sdpRecordFound) {
                    SocketServer.sendSocketData("SDP Record found and Creating RFComm Socket");
                    tmp = device.createInsecureRfcommSocketToServiceRecord(Utils.UUIDConstants.APP_UUID);
                    sdpRecordFound = false;
                } else {
                    SocketServer.sendSocketData("SDP Record not found for this UUID on Remote Device");
                    Log.e(TAG, "SDP Record not found for this UUID");
                }
                getSdpSearchSem.release();
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            } catch (Exception e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }

            if (tmp == null) {
                Log.d(TAG, "Socket is null");
                SocketServer
                    .sendSocketData("Communication Socket is not created... Please close and restart the Process");
                return;
            } else {
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
                if (mmSocket != null) {
                    mmSocket.connect();
                } else {
                    Log.d(TAG, "Socket is null");
                    SocketServer
                    .sendSocketData("Communication Socket is not created... Please close and restart the Process");
                    return;
                }
                Log.d(TAG, "Socket Connected");
                Log.d(TAG,"Utils.isThroughputStateMachineUnderProcessing :: "+Utils.isThroughputStateMachineUnderProcessing);
                Log.d(TAG,"Utils.isOffloadStateMachineUnderProcessing :: "+Utils.isOffloadStateMachineUnderProcessing);
                if (Utils.isThroughputStateMachineUnderProcessing == true) {
                    Message message = Message.obtain();
                    message.what = Utils.StateMachineMessageConstants.STATE_CONNECTED;
                    message.obj = mmSocket.getRemoteDevice();
                    mAppControlStateMachine.sendMessage(message);
                }

                if (Utils.isOffloadStateMachineUnderProcessing == true) {
                    Message message = Message.obtain();
                    message.what = Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONNECTED;
                    message.obj = mmSocket.getRemoteDevice();
                    mNotificationOffloadStateMachine.sendMessage(message);
                }

            } catch (IOException connectException) {
                Log.e(TAG, "Unable to connect; close the socket and return",
                        connectException);
                try {
                    mmSocket.close();
                    Log.d(TAG,"Utils.isThroughputStateMachineUnderProcessing :: "+Utils.isThroughputStateMachineUnderProcessing);
                    Log.d(TAG,"Utils.isOffloadStateMachineUnderProcessing :: "+Utils.isOffloadStateMachineUnderProcessing);
                    if (Utils.isThroughputStateMachineUnderProcessing == true) {
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_CONNECTION_FAILED;
                        mAppControlStateMachine.sendMessage(message);
                    }
                    if (Utils.isOffloadStateMachineUnderProcessing == true) {
                        Message message = Message.obtain();
                        message.what = Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONNECTION_FAILED;
                        mNotificationOffloadStateMachine.sendMessage(message);
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

    public void startTxOperation(int chunkSize) {
        Runnable txOperationRunnable = new TxOperationRunnable(chunkSize);
        new Thread(txOperationRunnable).start();
    }

    private class TxOperationRunnable implements Runnable {

        int mChunkSize;

        public TxOperationRunnable(int chunkSize) {
            mChunkSize = chunkSize;
        }

        public void run() {

            OutputStream outputStream = null;
            long tx_start_time, tx_end_time;
            try {
                outputStream = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }
            int chunksize = mChunkSize;
            chunksize = chunksize * 1024;
            Log.d(TAG, "chunkSize is :: " + chunksize);
            StringBuilder sb = new StringBuilder(chunksize);
            for (int i = 0; i < chunksize; i++) {
                sb.append('a');
            }
            String senttext = sb.toString();
            String start = "Start";
            String end = "End";
            bluetoothAdapter.cancelDiscovery();
            tx_start_time = SystemClock.elapsedRealtime();
            Log.d(TAG, "start time: " + tx_start_time);
            Log.d(TAG, "senttext.length() :: " + senttext.length());
            try {
                outputStream.write(start.getBytes());
                outputStream.write(senttext.getBytes());
                outputStream.write(end.getBytes());
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
                Message message = Message.obtain();
                message.what = Utils.StateMachineMessageConstants.STATE_DATA_TX_FAILED;
                mAppControlStateMachine.sendMessage(message);
            }
            tx_end_time = SystemClock.elapsedRealtime();
            Log.d(TAG, "end time: " + tx_end_time);
            Log.d(TAG, "end_time - start_time: "
                    + (tx_end_time - tx_start_time));

            // throughput calculations
            float TxTput = ((float) senttext.length() * 8 * 1000)
                    / (tx_end_time - tx_start_time);
            float TxTputk = TxTput / 1000;
            Log.d(TAG, "write: Through put (send) is (in kbps): " + TxTputk);
            SocketServer.sendSocketData("Throughput (send) is (in kbps): "
                    + TxTputk);
            Message message = Message.obtain();
            message.what = Utils.StateMachineMessageConstants.STATE_END_DATA_TX;
            mAppControlStateMachine.sendMessage(message);

        }
    }

    Runnable txOperationRunnable = new Runnable() {
        public void run() {
        }
    };

    public void startRxOperation() {

        Thread rxOperation = new Thread(rxOperationRunnable);
        rxOperation.start();
    }

    Runnable rxOperationRunnable = new Runnable() {
        public void run() {
            InputStream inputStream = null;
            long rx_start_time = 0, rx_end_time = 0;
            byte[] mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()
            int totalBytes = 0; // totalBytes received
            try {
                inputStream = mmSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            while (true) {
                try {
                    numBytes = inputStream.read(mmBuffer);
                    totalBytes = totalBytes + numBytes;
                    String incomingMsg = new String(mmBuffer, 0, numBytes);

                    if (incomingMsg.contains("Start")) {
                        rx_start_time = SystemClock.elapsedRealtime();
                        Log.d(TAG, "start_time: " + rx_start_time);
                    }

                    if (incomingMsg.contains("end")) {
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
                        SocketServer
                        .sendSocketData("Throughput (receive) is (in kbps): "
                                + RxTputk);
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_END_DATA_RX;
                        mAppControlStateMachine.sendMessage(message);
                        totalBytes = 0;
                        // break;
                    }

                    if (incomingMsg.contains("WAKEABLE_NOTIFICATION_END")) {
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_END_DATA_TX_WAKEABLE;
                        mAppControlStateMachine.sendMessage(message);
                        break;
                    }

                    if (incomingMsg.contains("ACTIONABLE_NOTIFICATION_END")) {
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_END_DATA_TX_ACTIONABLE;
                        mAppControlStateMachine.sendMessage(message);
                        break;
                    }

                    if (incomingMsg.contains("CACHEABLE_NOTIFICATION_END")) {
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_END_DATA_TX_CACHEABLE;
                        mAppControlStateMachine.sendMessage(message);
                        break;
                    }

                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    Message message = Message.obtain();
                    message.what = Utils.StateMachineMessageConstants.STATE_DATA_RX_FAILED;
                    mAppControlStateMachine.sendMessage(message);
                    break;
                }
            }
        }
    };

    public void startWakeableNotificationOperation(int timer) {
        OutputStream outputStream = null;
        try {
            outputStream = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }
        Log.d(TAG, "timer is :: " + timer);
        String start = "WAKEABLE_NOTIFICATION_START";
        String timerStart = "WAKEABLE_NOTIFICATION_TIMER_START";
        String timerEnd = "WAKEABLE_NOTIFICATION_TIMER_END";
        try {
            outputStream.write(start.getBytes());
            outputStream.write(timerStart.getBytes());
            outputStream.write(Integer.toString(timer).getBytes());
            outputStream.write(timerEnd.getBytes());
            startRxOperation();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);
            Message message = Message.obtain();
            message.what = Utils.StateMachineMessageConstants.STATE_DATA_TX_FAILED;
            mAppControlStateMachine.sendMessage(message);
        }
    }

    public void startActionableNotificationOperation(int timer) {
        OutputStream outputStream = null;
        try {
            outputStream = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }
        Log.d(TAG, "timer is :: " + timer);
        String start = "ACTIONABLE_NOTIFICATION_START";
        String timerStart = "ACTIONABLE_NOTIFICATION_TIMER_START";
        String timerEnd = "ACTIONABLE_NOTIFICATION_TIMER_END";
        try {
            outputStream.write(start.getBytes());
            outputStream.write(timerStart.getBytes());
            outputStream.write(Integer.toString(timer).getBytes());
            outputStream.write(timerEnd.getBytes());
            startRxOperation();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);
            Message message = Message.obtain();
            message.what = Utils.StateMachineMessageConstants.STATE_DATA_TX_FAILED;
            mAppControlStateMachine.sendMessage(message);
        }
    }

    public void startCacheableNotificationOperation(int timer) {
        OutputStream outputStream = null;
        try {
            outputStream = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }
        Log.d(TAG, "timer is :: " + timer);
        String start = "CACHEABLE_NOTIFICATION_START";
        String timerStart = "CACHEABLE_NOTIFICATION_TIMER_START";
        String timerEnd = "CACHEABLE_NOTIFICATION_TIMER_END";
        try {
            outputStream.write(start.getBytes());
            outputStream.write(timerStart.getBytes());
            outputStream.write(Integer.toString(timer).getBytes());
            outputStream.write(timerEnd.getBytes());
            startRxOperation();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);
            Message message = Message.obtain();
            message.what = Utils.StateMachineMessageConstants.STATE_DATA_TX_FAILED;
            mAppControlStateMachine.sendMessage(message);
        }
    }

    public void setScanMode(int scanMode) {
        boolean result = false;
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
        if (true == result) {
            SocketServer.sendSocketData("Set Scan Mode Successful");
        } else {
            SocketServer
            .sendSocketData("Set Scan Mode Failed...Please try again");
        }
        Message message = Message.obtain();
        message.what = Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_END_SCAN_MODE;
        mAppControlStateMachine.sendMessage(message);
        SocketServer.processOutputState = SocketServer.CONNECTION_TEST_MENU;
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
        mAppControlStateMachine.sendMessage(message);
        SocketServer.processOutputState = SocketServer.CONNECTION_TEST_MENU;
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
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    public void startReadyToAcceptConnection() {
        Log.d(TAG, "startReadyToAcceptConnect");
        readyToAcceptThread = new ReadyToAcceptThread();
        readyToAcceptThread.start();
    }

    private class ReadyToAcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public ReadyToAcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                Log.d(TAG,
                        "ReadyToAcceptThread: Setting up BluetoothServerSocket for listening to connection requests from client");
                tmp = bluetoothAdapter
                        .listenUsingInsecureRfcommWithServiceRecord(
                                "RFCOMM Test App",
                                Utils.UUIDConstants.INCOMING_CONNECTION_UUID);
            } catch (IOException e) {
                Log.e(TAG,
                        "ReadyToAcceptThread: Socket's listen() method failed",
                        e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "AcceptThread Running run()");

            // Keep listening till exception occurs
            while (true) {
                try {
                    Log.d(TAG,
                            "ReadyToAcceptThread: going to wait till connection is made");
                    mmSocket = mmServerSocket.accept();
                    Log.d(TAG,
                            "ReadyToAcceptThread: Connection was made, BluetoothSocket returned");
                    Log.d(TAG,"Utils.isThroughputStateMachineUnderProcessing :: "+Utils.isThroughputStateMachineUnderProcessing);
                    Log.d(TAG,"Utils.isOffloadStateMachineUnderProcessing :: "+Utils.isOffloadStateMachineUnderProcessing);
                    if (Utils.isThroughputStateMachineUnderProcessing == true) {
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_CONNECTED;
                        message.obj = mmSocket.getRemoteDevice();
                        mAppControlStateMachine.sendMessage(message);
                    }

                    if (Utils.isOffloadStateMachineUnderProcessing) {
                        Message message = Message.obtain();
                        message.what = Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONNECTED;
                        message.obj = mmSocket.getRemoteDevice();
                        mNotificationOffloadStateMachine.sendMessage(message);
                    }

                    break;
                } catch (IOException e) {
                    Log.e(TAG,
                            "ReadyToAcceptThread: Socket's accept() method failed",
                            e);
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

    public void startNotRcvOperation() {
        Thread notRcvOperation = new Thread(notRcvOperationRunnable);
        notRcvOperation.start();
    }

    Runnable notRcvOperationRunnable = new Runnable() {
        public void run() {
            InputStream inputStream = null;
            byte[] mBuffer = new byte[1024];
            int numBytes; // bytes returned from read()
            int packet_code;
            try {
                inputStream = mmSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            while (true) {
                try {
                    numBytes = inputStream.read(mBuffer);
                    if (numBytes > 0) {
                        Log.d(TAG, "Bytes received " + numBytes);
                        packet_code = mBuffer[2] & 0xFF;
                        if (packet_code == 0xE1) {
                            Log.d(TAG, "New Notification received");
                            notificationReceived(mBuffer);
                        } else if (packet_code == 0xE3) {
                            Log.d(TAG, "Response received");
                            notificationInfoRespoceReceived(mBuffer);
                        } else {
                            Log.e(TAG, "Invalid packet code");
                        }
                    } else {
                        Log.e(TAG, "Error occurred. Read returned 0 bytes");
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }
    };

    public void startNotCTLPTOperation() {
        Thread prNotRunnable = new Thread(processNotificationRunnable);
        prNotRunnable.start();
    }

    Runnable processNotificationRunnable = new Runnable() {
        public void run() {
            while (NotificationPacketList.size() > 0) {
                processNotification(NotificationPacketList.getFirst());
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    Log.e(TAG, "Error occurred when creating output stream", e);
                }
            }
            Message message = Message.obtain();
            message.what = Utils.NotificationOffloadStateMachineMessageConstants.STATE_NOT_PROCESS_END;
            mNotificationOffloadStateMachine.sendMessage(message);
        }
    };

    private void processNotification(NotificationPacketInd obj) {
        StringBuilder sendStr = new StringBuilder();
        OutputStream outputStream = null;
        ByteBuffer wrapped = ByteBuffer.wrap(obj.NotificationHandle);
        byte action = 0;
        byte getInfoId = 0;
        short num = wrapped.getShort();
        try {
            outputStream = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }
        Log.d(TAG, "Notification received with notification handle " + num);
        sendStr.append("\n*********Received notification with below details*********** \n\n");
        switch (obj.NotificationID) {
        case 0x01:
            sendStr.append("Notification ID     : 0x01[Incoming call]\n");
            break;
        case 0x02:
            sendStr.append("Notification ID     : 0x02[Missed call]\n");
            break;
        case 0x03:
            sendStr.append("Notification ID     : 0x03[Email]\n");
            break;
        default:
            Log.e(TAG, "Invalid notification id\t");
            break;
        }
        switch (obj.NotificationStatus) {
        case 0x01:
            sendStr.append("Notification Status : 0x01[Notification added]\n");
            break;
        case 0x02:
            sendStr.append("Notification Status : 0x02[Notification cleared]\n");
            break;
        case 0x03:
            sendStr.append("Notification Status : 0x03[Notification modified]\n");
            break;
        default:
            Log.e(TAG, "Invalid Notification Status\n");
            break;
        }
        if (obj.NotificationID == 0x01) {
            while (true) {
                sendStr.append("\n*********Please select getInfo to get more info about Notification and Do select the action to be performed***********");
                sendStr.append("\nGet Info                [exp: getInfo 1]\n");
                sendStr.append("Do Action               [exp: action 1]\n");
                if (obj.NotificationID == 0x01 || obj.NotificationID == 0x02) {
                    sendStr.append("getInfo Values = 1[Caller Phone Number]  2[Caller Name]\n");
                } else {
                    sendStr.append("getInfo Values = 3[Sender email address] 4[Email Subject] 5[Email Body]\n");
                }
                sendStr.append("Action values   = 1[Dismiss]               2[Attend]        3[Ignore]\n");
                sendStr.append("\n**************************************************************\n");
                SocketServer.sendSocketData(sendStr.toString());
                sendStr.delete(0, sendStr.length());
                try {
                    obj.sem.acquire();
                } catch (Exception e) {
                    Log.e(TAG, "There is an exception when acquiring semaphore");
                    e.printStackTrace();
                }
                if (obj.action > 0x00) {
                    switch (obj.action) {
                        case 0x01:
                            Log.d(TAG, "Action Dismiss Notification");
                            action = 0x01;
                            break;
                        case 0x02:
                            Log.d(TAG, "Action Attend Notification");
                            action = 0x02;
                            break;
                        case 0x03:
                            Log.d(TAG, "Action Ignore Notification");
                            action = 0x03;
                            break;
                        default:
                            Log.d(TAG, "Invalid Action");
                            break;
                    }
                    try {
                        outputStream.write(parser.getActionPacket(action, getInfoId));
                    } catch (IOException e) {
                        Log.e(TAG, "Error occurred when sending data", e);
                    } catch (Exception e) {
                        Log.e(TAG, "Error occurred when sending data", e);
                    }
                    break;
                } else {
                    switch (obj.getAttID) {
                        case 0x01:
                            Log.d(TAG, "Get Caller Phone Number");
                            getInfoId = 0x01;
                            break;
                        case 0x02:
                            Log.d(TAG, "Get Caller Name");
                            getInfoId = 0x02;
                            break;
                        case 0x03:
                            Log.d(TAG, "Get Sender's email address");
                            getInfoId = 0x03;
                            break;
                        case 0x04:
                            Log.d(TAG, "Get Email Subject");
                            getInfoId = 0x04;
                            break;
                        case 0x05:
                            Log.d(TAG, "Get Email Body Snippet");
                            getInfoId = 0x05;
                            break;
                        default:
                            Log.e(TAG, "Invalid get Attribute ID");
                            break;
                    }
                    try {
                        outputStream.write(parser.getReadInfoPacket(getInfoId));
                    } catch (IOException e) {
                        Log.e(TAG, "Error occurred when sending data", e);
                    }
                    try {
                        if (obj.getInfoSem.tryAcquire(2, TimeUnit.SECONDS)) {
                        } else {
                            Log.e(TAG, "Error while getting response for readInfo");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "There is an exception when acquiring semaphore");
                        e.printStackTrace();
                    }
                }
            }
        } else {
            sendStr.append("\n**************************************************************\n");
            SocketServer.sendSocketData(sendStr.toString());
            if (obj.NotificationID == 0x02) {
                try {
                    Log.d(TAG, "Get Caller Name");
                    getInfoId = 0x02;
                    outputStream.write(parser.getReadInfoPacket(getInfoId));
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when sending data", e);
                }
                try {
                    if (obj.getInfoSem.tryAcquire(2, TimeUnit.SECONDS)) {
                    } else {
                        Log.e(TAG, "Error while getting response for readInfo[CallerName]");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "There is an exception when acquiring semaphore");
                    e.printStackTrace();
                }
                try {
                    Log.d(TAG, "Get Caller Phone Number");
                    getInfoId = 0x01;
                    outputStream.write(parser.getReadInfoPacket(getInfoId));
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when sending data", e);
                }
                try {
                    if (obj.getInfoSem.tryAcquire(2, TimeUnit.SECONDS)) {
                    } else {
                        Log.e(TAG, "Error while getting response for readInfo[Caller Number]");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "There is an exception when acquiring semaphore");
                    e.printStackTrace();
                }
            } else {
                try {
                    Log.d(TAG, "Get Sender's email address");
                    getInfoId = 0x03;
                    outputStream.write(parser.getReadInfoPacket(getInfoId));
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when sending data", e);
                }
                try {
                    if (obj.getInfoSem.tryAcquire(2, TimeUnit.SECONDS)) {
                    } else {
                        Log.e(TAG, "Error while getting response for readInfo[EmailAddress]");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "There is an exception when acquiring semaphore");
                    e.printStackTrace();
                }
                try {
                    Log.d(TAG, "Get Email Subject");
                    getInfoId = 0x04;
                    outputStream.write(parser.getReadInfoPacket(getInfoId));
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when sending data", e);
                }
                try {
                    if (obj.getInfoSem.tryAcquire(2, TimeUnit.SECONDS)) {
                    } else {
                        Log.e(TAG, "Error while getting response for readInfo[Email Subject]");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "There is an exception when acquiring semaphore");
                    e.printStackTrace();
                }
                try {
                    Log.d(TAG, "Get Email Body Snippet");
                    getInfoId = 0x05;
                    outputStream.write(parser.getReadInfoPacket(getInfoId));
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when sending data", e);
                }
                try {
                    if (obj.getInfoSem.tryAcquire(2, TimeUnit.SECONDS)) {
                    } else {
                        Log.e(TAG, "Error while getting response for readInfo[Email body]");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "There is an exception when acquiring semaphore");
                    e.printStackTrace();
                }
            }
        }
        NotificationPacketList.remove();
    }

    public void notificationReceived(byte[] bytes) {
        NotificationPacketInd notification = new NotificationPacketInd(bytes);
        NotificationPacketList.add(notification);
        if (mServerConnectedThread != null) {
            mServerConnectedThread.write("Notification delivered".getBytes());
        }
        Message message = Message.obtain();
        message.what = Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONTROL_POINT;
        mNotificationOffloadStateMachine.sendMessage(message);
    }

    public void notificationInfoRespoceReceived(byte[] bytes) {
        Log.d(TAG, "Received notification info respose");
        SocketServer.sendSocketData((parser.notificationInfoParse(bytes))
                .toString());
        NotificationPacketList.getFirst().getInfoSem.release();
    }

    public void responseFromCLI(String responce) {
        String[] tmpStr;
        Log.d(TAG, "responceFromCLI");
        tmpStr = responce.split(" ", 2);
        if (tmpStr[0].equals("getInfo")) {
            Log.d(TAG, "responceFromCLI getInfo");
            try {
                NotificationPacketList.getFirst().getAttID = Byte
                        .valueOf(tmpStr[1]);
            } catch (Exception e) {
                //
            }
            NotificationPacketList.getFirst().sem.release();
        } else if (tmpStr[0].equals("action")) {
            Log.d(TAG, "responceFromCLI action");
            NotificationPacketList.getFirst().action = Byte.valueOf(tmpStr[1]);
            NotificationPacketList.getFirst().sem.release();
        }
    }

    /* Offload Service Message Handler */
    public class OffloadServiceMessageHandler extends Handler {
        Context mMsgContext;
        private static final String TAG = "OffloadServiceMessageHandler";

        public OffloadServiceMessageHandler(Context contxt, Looper looper) {
            super(looper);
            mMsgContext = contxt;
        }

        @Override
        public void handleMessage(Message message) {
            Log.d(TAG, "Handler(): msg = " + message.what);

            switch (message.what) {
                case Utils.MSG_AS_REGISTER_OFFLODABLE_ADAPTER:
                    processRegisterOfflodableAdapter();
                    break;
                case Utils.MSG_AS_DREGISTER_OFFLODABLE_ADAPTER:
                    processDRegisterOfflodableAdapter();
                    break;
                default:
                    Log.e(TAG, "Unknown Operation");
                    break;
            }
        }
    }

/*
    private final OffloadableAppCallback mofflodableappcallback = new OffloadableAppCallback(){
        @Override
        public void notifyStartDone(int status) {
            if(status == BT_OK) {
                Log.d(TAG, "Offload Register Done");
            } else {
                Log.d(TAG, "offload Registartion failed Status: " + status);
            }
        }

        @Override
        public void notifyStopDone(int status) {
            if(status == BT_OK) {
                Log.d(TAG, "Offload Deregister Done");
            } else {
                Log.d(TAG, "offload Deregistration failed Status: " + status);
            }
        }

        @Override
        public int notifyOffloadEnable(int mode) {
            Log.d(TAG, "notifyOffloadEnable Mode: " + mode);
            mNotificationOffloadStateMachine.ncPreviousState = mNotificationOffloadStateMachine.getCurrentState();
            Log.d(TAG, "CurrentState: " + mNotificationOffloadStateMachine.ncPreviousState.getName());
            Message message = Message.obtain();
            message.what = Utils.MSG_NC_SM_OFFLOADED;
            mNotificationOffloadStateMachine.sendMessage(message);
            return 0;
        }

        @Override
        public int notifyOffloadDisable(ArrayList<Byte> blob) {
            Log.d(TAG, "notifyOffloadDisable blob len: " + blob.size() + " Blob " + blob);
            //mOfflodableAppAdapter.disableOffloadDone(BT_OK);
            Message message = Message.obtain();
            message.what = Utils.MSG_NC_SM_ACTIVE;
            mNotificationOffloadStateMachine.sendMessage(message);
            return 0;
        }

        @Override
        public void notifyAsyncErr(int status) {
            Log.i(TAG, "notifyAsyncErr status: " + status);
        }
    };
*/

    private void processRegisterOfflodableAdapter() {
        Log.d(TAG, "processGRegisterOfflodableAdapter()");
        //mOfflodableAppAdapter = new OffloadableAppAdapter(mContext, mofflodableappcallback);
        //mOfflodableAppAdapter.start();
        SocketServer.sendSocketData("OffloadableApp Registered");
    }

    private void processDRegisterOfflodableAdapter() {
        Log.d(TAG, "processDRegisterOfflodableAdapter()");
        //mOfflodableAppAdapter.stop();
        SocketServer.sendSocketData("OffloadableService Deregistered");
    }

}
