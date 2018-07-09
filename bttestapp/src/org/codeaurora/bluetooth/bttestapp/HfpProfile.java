/*
 Copyright (c) 2018, The Linux Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are
 met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above
       copyright notice, this list of conditions and the following
       disclaimer in the documentation and/or other materials provided
       with the distribution.
     * Neither the name of The Linux Foundation nor the names of its
       contributors may be used to endorse or promote products derived
       from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codeaurora.bluetooth.bttestapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;

import org.codeaurora.bluetooth.bttestapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class HfpProfile {

    private final static String TAG = "HfpProfile";

    public static final String HEADSET_CLIENT_ENABLE_PTS_PROPERTY =
        "persist.bt.headsetclient.enable_pts";

    public static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;

    // +++ Custom action definition for headset client

    /**
     * Broadcast Action: Indicates custom action(request) from application.
     *
     * <p>Always contains the extra field {@link #EXTRA_CUSTOM_ACTION}.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    public static final String ACTION_CUSTOM_ACTION =
        "android.bluetooth.headsetclient.action.CUSTOM_ACTION";

    public static final String EXTRA_CUSTOM_ACTION =
        "android.bluetooth.headsetclient.extra.CUSTOM_ACTION";

    public static final String KEY_COMMAND = "command";

    /**
     * Custom action to memory dial
     *
     * @param Bundle wrapped with
     *  {@link #KEY_COMMAND}
     *  {@link #BluetoothDevice.EXTRA_DEVICE}
     *  {@link #KEY_LOCATION}
     */
    public static final String CUSTOM_ACTION_MEM_DIAL =
        "android.bluetooth.headsetclient.CUSTOM_ACTION_MEM_DIAL";
    public static final String KEY_LOCATION = "location";

    // + Response for custom action

    /**
     * Intent used to broadcast headset client custom action result
     *
     * <p>This intent will have 1 extras at least:
     * <ul>
     *   <li> {@link #EXTRA_CUSTOM_ACTION_RESULT} - custom action result. </li>
     *
     * </ul>
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission to
     * receive.
     */
    public static final String ACTION_CUSTOM_ACTION_RESULT =
        "android.bluetooth.headsetclient.action.CUSTOM_ACTION_RESULT";

    public static final String EXTRA_CUSTOM_ACTION_RESULT =
        "android.bluetooth.headsetclient.extra.CUSTOM_ACTION_RESULT";

    public static final String KEY_RESULT = "result";

    // - Response for custom action

    // --- Custom action definition for headset client

    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice mDevice = null;
    private BluetoothHeadsetClient mHeadsetClient = null;
    private Context mContext;

    private final ServiceListener mHeadsetClientServiceListener = new ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HEADSET_CLIENT) {
                mHeadsetClient = (BluetoothHeadsetClient) proxy;
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HEADSET_CLIENT) {
                mHeadsetClient = null;
            }
        }
    };

    public HfpProfile(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        Log.d(TAG, "init");
        mAdapter.getProfileProxy(mContext, mHeadsetClientServiceListener,
            BluetoothProfile.HEADSET_CLIENT);
    }

    public BluetoothHeadsetClient getHeadsetClient() {
        return mHeadsetClient;
    }

    public void memDial(BluetoothDevice device, int location) {
        Bundle extras = createBundle(CUSTOM_ACTION_MEM_DIAL, device);
        extras.putInt(KEY_LOCATION, location);
        sendCustomAction(extras);
    }

    private void sendCustomAction(Bundle extras) {
        Intent intent = new Intent(ACTION_CUSTOM_ACTION);
        intent.putExtra(EXTRA_CUSTOM_ACTION, extras);
        mContext.sendBroadcast(intent, BLUETOOTH_PERM);
    }

    private Bundle createBundle(String cmd, BluetoothDevice device) {
        Bundle extras = new Bundle();
        extras.putString(KEY_COMMAND, cmd);
        extras.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
        return extras;
    }

    public void enablePts(boolean enable) {
        SystemProperties.set(HEADSET_CLIENT_ENABLE_PTS_PROPERTY, String.valueOf(enable));
    }

    public static boolean isSuccess(int result) {
        return (result == BluetoothHeadsetClient.ACTION_RESULT_OK) ? true : false;
    }
}
