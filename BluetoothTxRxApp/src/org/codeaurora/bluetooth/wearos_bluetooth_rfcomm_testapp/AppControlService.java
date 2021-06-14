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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.Notification;

public class AppControlService extends Service {
    private static final String TAG = "BluetoothTxRxApp Service";
    private static BluetoothAdapter bluetoothAdapter = BluetoothAdapter
            .getDefaultAdapter();
    BluetoothDevice btDeviceToPair = null;
    private BluetoothSocket mmSocket;
    private Context mContext;
    ConfigFileParser parser;
    AppControlStateMachine mAppControlStateMachine;
    private ConnectThread connectThread;

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
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
        mContext = null;
        unregisterReceiver(eventReceiver);
        if(mAppControlStateMachine != null){
            mAppControlStateMachine.cleanUp();
        }
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
        mContext.registerReceiver(eventReceiver, intentFilter);
    }

    private BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(Utils.bdAddressFromConfig != null && device != null){
                    Log.d(TAG,"Disconnected Device :: "+device.getAddress());
                    Log.d(TAG,"RFCOMM Test App Connected Device :: "+Utils.bdAddressFromConfig);
                    if(Utils.bdAddressFromConfig.equalsIgnoreCase(device.getAddress())){
                        Log.d(TAG, "Socket Disconnected");
                        SocketServer
                        .sendSocketData("Connection to Remote Device " +device.getAddress()+" is Terminated");
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_DISCONNECTED;
                        Utils.appControlStateMachine.sendMessage(message);
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
            }
        }
    };

    private void startStateMachine() {
        mAppControlStateMachine = new AppControlStateMachine(
                AppControlService.this);
        Log.d(TAG,"Starting App State Machine");
        mAppControlStateMachine.start();
        Utils.appControlStateMachine = mAppControlStateMachine;
    }

    protected void initializeTestSetup() {
        if (Utils.bdAddressFromConfig != null) {
            btDeviceToPair = bluetoothAdapter
                    .getRemoteDevice(Utils.bdAddressFromConfig);
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

    public void closeConnection(){
        Log.d(TAG,"closeConnection");
        connectThread.cancel();
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
                Log.d(TAG,
                        "Creating a BluetoothSocket to create a connection to a remote device");
                tmp = device
                        .createInsecureRfcommSocketToServiceRecord(Utils.UUIDConstants.APP_UUID);
            } catch (IOException e) {
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
                Message message = Message.obtain();
                message.what = Utils.StateMachineMessageConstants.STATE_CONNECTED;
                mAppControlStateMachine.sendMessage(message);

            } catch (IOException connectException) {
                Log.e(TAG, "Unable to connect; close the socket and return",
                        connectException);
                try {
                    mmSocket.close();
                    Message message = Message.obtain();
                    message.what = Utils.StateMachineMessageConstants.STATE_CONNECTION_FAILED;
                    mAppControlStateMachine.sendMessage(message);
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket",
                            closeException);
                }
                return;
            }
        }

        public void cancel() {
            try {
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
                        //break;
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

}
