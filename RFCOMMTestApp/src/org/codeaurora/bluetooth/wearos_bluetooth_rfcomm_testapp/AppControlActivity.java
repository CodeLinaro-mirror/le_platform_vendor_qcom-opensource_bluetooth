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

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import android.provider.Settings;
import android.net.Uri;

public class AppControlActivity extends Activity {

    private static final String TAG = "BluetoothTxRxApp AppControlActivity";

    private static SocketServer socServer;
    private static Context mContext;

    AppControlService appControlService;
    boolean isBound = false;
    private static final int REQUEST_EXTERNAL_STORAGE = 100;
    String[] permissions = new String[]{"android.permission.BLUETOOTH_CONNECT"};
    int Build_Version_S = 31;
    int Build_Version_O = 26;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        if(Build.VERSION.SDK_INT >= Build_Version_S)
        {
            requestPermissions(permissions, 2);
        }
        if (savedInstanceState != null) {
            // Restore value of members from saved state
        } else {
        }
        mContext = getApplicationContext();
        setContentView(R.layout.activity_main);
        requestStoragePermissions();
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(on_offBroadcastReceiver, filter1);
        socServer = SocketServer.getInstance();
        Intent intent = new Intent(this, AppControlService.class);
        if (Build.VERSION.SDK_INT >= Build_Version_O) {
            this.startForegroundService(intent);
        }else{
            this.startService(intent);
        }
    }

    private void requestStoragePermissions(){
        requestPermissions(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
        }, REQUEST_EXTERNAL_STORAGE);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState called");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (isBound) {
            // unbindService(appControlServiceConnection);
            isBound = false;
        }
        Intent intent = new Intent(this, AppControlService.class);
        this.stopService(intent);
        unregisterReceiver(on_offBroadcastReceiver);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private ServiceConnection appControlServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            AppControlService.MyBinder binder = (AppControlService.MyBinder) service;
            appControlService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            isBound = false;
            appControlService = null;
        }
    };

   private final BroadcastReceiver on_offBroadcastReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch(state) {
                case BluetoothAdapter.STATE_OFF:
                     socServer.sendSocketData("BT is turned off !!\n");
                     Log.d("BroadcastActions", "BT is turned off !!");
                     break;
                case BluetoothAdapter.STATE_ON:
                     socServer.sendSocketData("BT is turned on !!\n");
                     Log.d("BroadcastActions", "BT is turned on !!");
                     break;
            }
        }
    }
  };
}
