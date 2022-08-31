/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.Objects;

public abstract class BluetoothProfileBase {

    protected Context mContext;
    protected String mTag;
    protected BluetoothAdapter mAdapter;
    protected final BluetoothProfile.ServiceListener mProfileListener =
            new BluetoothProfile.ServiceListener() {
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    handleServiceConnected(profile, proxy);
                }

                public void onServiceDisconnected(int profile) {
                    handleServiceDisconnected(profile);
                }
            };

    BluetoothProfileBase(Context context, String tag) {
        this(context, tag, Adapter.getDefaultAdapter());
    }

    BluetoothProfileBase(Context context, String tag, BluetoothAdapter adapter) {
        mContext = Objects.requireNonNull(context);
        mTag = tag;
        mAdapter = Objects.requireNonNull(adapter);
    }

    public boolean getProfileProxy(int profile) {
        return mAdapter.getProfileProxy(mContext, mProfileListener, profile);
    }

    protected abstract void handleServiceConnected(int profile, BluetoothProfile proxy);
    protected abstract void handleServiceDisconnected(int profile);

    public static boolean isSameDevice(BluetoothDevice device1, BluetoothDevice device2) {
        return device1 != null && device2 != null
                && device1.equals(device2);
    }

    public static boolean isConnected(int state) {
        return state == BluetoothProfile.STATE_CONNECTED;
    }

    public static boolean isDisconnected(int state) {
        return state == BluetoothProfile.STATE_DISCONNECTED;
    }

    protected void logd(String msg) {
        Log.d(mTag, msg);
    }

    protected void loge(String msg) {
        Log.e(mTag, msg);
    }
}
