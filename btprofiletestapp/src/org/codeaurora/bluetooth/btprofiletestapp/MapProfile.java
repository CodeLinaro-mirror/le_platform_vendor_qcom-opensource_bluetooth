/*
 * Copyright (c) 2019, The Linux Foundation. All rights reserved.
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

package org.codeaurora.bluetooth.btprofiletestapp;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMapClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.codeaurora.bluetooth.btprofiletestapp.util.Logger;

public class MapProfile {

    private final static String TAG = "MapProfile";

    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    private BluetoothDevice mDevice = null;

    private Context mContext;

    private BluetoothMapClient mMapClient = null;

    private final ServiceListener mMapClientServiceListener = new ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.MAP_CLIENT) {
                mMapClient = (BluetoothMapClient) proxy;
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.MAP_CLIENT) {
                mMapClient = null;
            }
        }
    };

    public MapProfile(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        Logger.d(TAG, "init");

        mAdapter.getProfileProxy(mContext, mMapClientServiceListener,
                BluetoothProfile.MAP_CLIENT);
    }

    public boolean isConnected(BluetoothDevice device) {
        Logger.d(TAG, "isConnected");
        if (mMapClient != null) {
            return mMapClient.isConnected(device);
        } else {
            return false;
        }
    }

    public boolean connect(BluetoothDevice device) {
        Logger.d(TAG, "connect");
        if (mMapClient != null) {
            return mMapClient.connect(device);
        } else {
            return false;
        }
    }

    public boolean disconnect(BluetoothDevice device) {
        Logger.d(TAG, "disconnect");
        if (mMapClient != null) {
            return mMapClient.disconnect(device);
        } else {
            return false;
        }
    }

    public boolean getUnreadMessages(BluetoothDevice device) {
        Logger.d(TAG, "getUnreadMessages");
        if (mMapClient != null) {
            return mMapClient.getUnreadMessages(device);
        } else {
            return false;
        }
    }

    public boolean abort(BluetoothDevice device) {
        Logger.d(TAG, "abort");

	return false;
    }

    public boolean sendMessage(BluetoothDevice device, Uri[] contacts, String message,
            PendingIntent sentIntent, PendingIntent deliveredIntent) {
        Logger.d(TAG, "sendMessage");
        if (mMapClient != null) {
            return mMapClient.sendMessage(device, contacts, message, sentIntent, deliveredIntent);
        } else {
            return false;
        }
    }

    public boolean setMessageStatus(BluetoothDevice device, String handle, int status) {
        Logger.d(TAG, "setMessageStatus");
        if (mMapClient != null) {
           Logger.d(TAG, "setMessageStatus map status");
           return mMapClient.setMessageStatus(device, handle, status);
        } else {
            return false;
        }
    }
}
