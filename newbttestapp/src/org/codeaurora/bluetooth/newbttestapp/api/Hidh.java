/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidHost;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Objects;

final class Hidh extends BluetoothProfileBase
        implements BluetoothIntent.IBluetoothIntentFilter {

    private static final String TAG = "Hidh";

    private BluetoothHidHost mBluetoothHidHost;
    private BluetoothIntent mBluetoothIntent;
    private IBluetoothHidhIntent mIntentHandler;

    public interface IBluetoothHidhIntent {
        void handleActionConnectionStateChanged(BluetoothDevice device, int state);
    }

    Hidh(Context context, IBluetoothHidhIntent handler) {
        super(context, TAG, Adapter.getNewAdapter());
        mBluetoothIntent = new BluetoothIntent(context, this);
        mIntentHandler = Objects.requireNonNull(handler);
        getProfileProxy(BluetoothProfile.HID_HOST);
    }

    public boolean connect(BluetoothDevice device) {
        logd("connect device: " + device);
        return mBluetoothHidHost != null && mBluetoothHidHost.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
        return mBluetoothHidHost != null && mBluetoothHidHost.disconnect(device);
    }

    public int getConnectionState(BluetoothDevice device) {
        logd("getConnectionState device: " + device);
        return mBluetoothHidHost != null ?
                mBluetoothHidHost.getConnectionState(device) :
                BluetoothProfile.STATE_DISCONNECTED;
    }

    @Override
    protected void handleServiceConnected(int profile, BluetoothProfile proxy) {
        if (profile == BluetoothProfile.HID_HOST) {
            mBluetoothHidHost = (BluetoothHidHost) proxy;
        }
    }

    @Override
    protected void handleServiceDisconnected(int profile) {
        if (profile == BluetoothProfile.HID_HOST) {
            mBluetoothHidHost = null;
        }
    }

    @Override
    public void initIntentFilter(IntentFilter intentFilter) {
        intentFilter.addAction(BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED);
    }

    @Override
    public void handleIntent(Context context, Intent intent) {
        String action = intent.getAction();
        logd("handleIntent action: " + action);
        if (BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            logd("BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                    BluetoothProfile.STATE_DISCONNECTED);
            mIntentHandler.handleActionConnectionStateChanged(device, state);

        } else {
           loge("unknown action: " + action);
        }
    }
}
