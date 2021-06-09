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

import libcore.io.IoUtils;
import android.app.Service;
import android.app.IntentService;
import android.app.PendingIntent;

import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.util.Arrays;

public class AncsService extends Service {
    private static final String TAG = "AncsService";

    private static BluetoothAdapter bleAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mDevice = null;

    private Context mAppContext;

    public static final int TRANSPORT_LE = 2;

    public static NCStateMachine mStateMachine;
    public static boolean stateMachineStarted = false;
    private static boolean mReceiverRegistered = false;
    public StringBuilder printStr = new StringBuilder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mAppContext = this;

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "Service onStartCommand");

        startNCStateMachine();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothAdapter bluetoothAdapter = MainActivity.mBluetoothManager.getAdapter();
        Log.i(TAG, "OnDestroy");
        if (bluetoothAdapter.isEnabled()) {
            stopAdvertising();
        }
        /* Stopping notification consumer state machine */
        if (mStateMachine != null) {
            mStateMachine.doQuit();
        }
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

    /*
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private void startAdvertising() {
        mBluetoothLeAdvertiser = bleAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setConnectable(true)
                .setTimeout(0)
                .build();
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

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

    public class NCStateMachine extends StateMachine {
        public static final int MSG_NC_SM_START_ADV = 1;
        public static final int MSG_NC_SM_STOP_ADV = 2;
        private NCIdle mNCIdle;
        private NCPending mNCPending;
        private NCPaired mNCPaired;
        private NCNotificationReceived mNCNotificationReceived;
        private NCControlPoint mNCControlPoint;
        private NCDisconnect mNCDisconnect;
        private NCOffloaded mNCOffloaded;

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
                        Log.i(TAG, "wakelock acquired");
                        break;
                    case MSG_NC_SM_STOP_ADV:
                        stopAdvertising();
                        printStr.setLength(0);
                        printStr.append("Advertising stopped!");
                        SocketServer.sendSocketData(printStr.toString());
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }
        }

        private class NCPending extends State {
            private static final String TAG = "NCPending";

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
                return retValue;
            }
        }

        private class NCPaired extends State {
            private static final String TAG = "NCPaired";

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
                return retValue;
            }
        }

        private class NCNotificationReceived extends State {
            private static final String TAG = "NCNotificationReceived";

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

                return retValue;
            }
        }

        private class NCControlPoint extends State {
            private static final String TAG = "NCControlPoint";

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

                return retValue;
            }
        }

        private class NCOffloaded extends State {
            private static final String TAG = "NCOffloaded";

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

                }
                return retValue;
            }
        }

        private class NCDisconnect extends State {
            private static final String TAG = "NCDisconnect";

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

                return retValue;
            }
        }
    }
}
