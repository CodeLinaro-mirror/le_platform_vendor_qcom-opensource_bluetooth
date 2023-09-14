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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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


import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class ScannerService extends Service {
    //Log Details
    private static final String TAG = "ScannerService";
    public static int LOG_LEVEL = 6;

    //States of the service
    public static boolean START_STATE = false; //Tracks if the service is started
    public static boolean mScanstatus = false; //Tracks if scan is in progress
    private Context mContext;

    //Actions
    public static final String START_SCAN = "START SCAN";
    public ScannerServiceMessageHandler mScannerHandler = null;
    private static final int MSG_START_BLE_SCAN = 0;
    private static final int MSG_STOP_BLE_SCAN = 1;
    private static final int MSG_SCAN_TIMEOUT = 2;
    private static final int MSG_SCAN_RESULT = 3;
    private static int count = 0;

    //Variables
    private ScanSettings mScanSettings;
    private ArrayList<ScanFilter> mScanFilters;
    private List<BluetoothDevice> mDeviceList;
    private List<ScanResult> mScanResult;
    private List<Integer> mRssiList;
    private BluetoothAdapter mBTAdapter = MainActivity.bleAdapter;
    private BluetoothLeScanner mBleScanner;


    public class LocalBinder extends Binder {
        ScannerService getService() {
            Log.d(TAG, "getService:scanner ");
            return ScannerService.this;
        }
    }

    //@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    //Service lifecycle methods
    @Override
    public void onCreate() {
        if(ScannerService.LOG_LEVEL >= 2)
            Log.d(TAG, "Scanner service is created.");

        mContext = this;
        //set default scan settings
        mScanSettings = null;
        mScanFilters = null;
        mBleScanner = mBTAdapter.getBluetoothLeScanner();
        mDeviceList = new ArrayList<BluetoothDevice>();
        mScanResult = new ArrayList<ScanResult>();
        mRssiList = new ArrayList<Integer>();
        HandlerThread thread = new HandlerThread("ScannerServiceHandler");
        thread.start();
        Looper looper = thread.getLooper();

        mScannerHandler = new ScannerServiceMessageHandler(this, looper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void startScan(ArrayList<ScanFilter> filters, ScanSettings settings) {
        mScanFilters = filters;
        mScanSettings = settings;
        if (mScanFilters != null) {
            Log.d(TAG, "Start Scan with filters");
        }
        Log.d(TAG, "Start Scan ");
        Message msg = mScannerHandler.obtainMessage(MSG_START_BLE_SCAN, null);
        mScannerHandler.sendMessage(msg);
    }

    public void stopScan() {
        Log.d(TAG, "Stop Scan ");
        mBTAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
    }

    private void resetScanParams() {
        mScanSettings = null;
        mScanFilters = null;
    }

    @Override
    public void onDestroy() {
        START_STATE = false;

        if(mScanstatus) {
            if(mBTAdapter.isEnabled())
                mBTAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
            mScanstatus = false;
        }
    }

    public ScanCallback mScanCallback = new ScanCallback(){
        @Override
        public void onScanResult(int CallbackType, ScanResult result){
            final BluetoothDevice bluetoothDevice = result.getDevice();
            ScanRecord scanRecord = result.getScanRecord();
            if(scanRecord == null) {
              Log.d(TAG,"scan record null");
              return;
            }
            final String devName = scanRecord.getDeviceName();
            final int i = result.getRssi();
            final ScanResult r = result;
            byte[] bytes = result.getScanRecord().getBytes();
            Log.d(TAG, "Device found, devName: "+devName);
            if(!mScanstatus)
                return;

            for(BluetoothDevice dev:mDeviceList){
                if(dev.getAddress().equals(bluetoothDevice.getAddress()))
                    return;
            }
            Log.d(TAG, "Device found with addr:" + bluetoothDevice.getAddress().toString());
                mDeviceList.add(bluetoothDevice);
                mScanResult.add(r);
                MainActivity.mStateMachine.sendMessage(MainActivity
                          .TA_SM_DEV_FOUND,bluetoothDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {

            int batchResultSize = results == null ? 0 : results.size();
            if(ScannerService.LOG_LEVEL >= 2) {
                Log.d(TAG, "current time stamp is " + SystemClock.elapsedRealtimeNanos());
                Log.d(TAG, "onBatchScanResults - size " + batchResultSize);
            }
        }
    };

    public class ScannerServiceMessageHandler extends Handler {
        Context mMsgContext;
        private static final String TAG = "ScannerServiceMessageHandler";

        public ScannerServiceMessageHandler(Context contxt, Looper looper) {
            super(looper);
            mMsgContext = contxt;
            if(ScannerService.LOG_LEVEL >= 2)
                Log.d(TAG, "ScannerServiceMessageHandler");
        }

        @Override
        public void handleMessage(Message msg) {
            if (ScannerService.LOG_LEVEL >= 2)
                Log.d(TAG, "Handler(): msg = " + msg.what);
            int status;
            switch (msg.what) {
                case MSG_START_BLE_SCAN:
                    scanLeDevice(true);
                    break;
                case MSG_STOP_BLE_SCAN:
                    resetScanParams();
                    scanLeDevice(false);
                    break;
                case MSG_SCAN_TIMEOUT:
                    Message mstop = obtainMessage(MSG_STOP_BLE_SCAN);
                    mstop.obj = null;
                    sendMessage(mstop);
                    break;
                case MSG_SCAN_RESULT:
                    //Need to decide if we can call directly
                    break;
                default:
            }
        }

        public synchronized boolean scanLeDevice(boolean action){
            Log.d(TAG, "scanLeDevice" + action);

            if(action && !mScanstatus){
                if(mScanFilters != null && mScanSettings !=null ){
                    Log.d(TAG, "set Scan with Settings and filters1");
                    mBTAdapter.getBluetoothLeScanner().startScan(mScanFilters,
                      mScanSettings, mScanCallback);
                }
                else if (mScanFilters != null){
                    Log.d(TAG, "set Scan with Settings and filters");
                    mBTAdapter.getBluetoothLeScanner().startScan(mScanFilters,
                      new ScanSettings.Builder().build(),  mScanCallback);
                }
                else { //do a regular scan
                    Log.d(TAG, "Do a regular scan");
                    mBTAdapter.getBluetoothLeScanner().startScan(mScanCallback);
                }
                mScanstatus = true;

                if(ScannerService.LOG_LEVEL >= 3)
                    Log.d(TAG, "scan started");
                Message msg = obtainMessage(MSG_SCAN_TIMEOUT);
                msg.obj = null;
                sendMessageDelayed(msg, ScanParams.ScanTO);
            }
            else if(!action && mScanstatus){
                if(mBTAdapter.isEnabled()) {
                    mBTAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                    mBleScanner.flushPendingScanResults(mScanCallback);
                }
                mDeviceList.clear();
                mScanResult.clear();
                mScanstatus = false;
                if(ScannerService.LOG_LEVEL >= 3)
                    Log.d(TAG, "scan stopped");
            }
            return true;
        }
    }
}


