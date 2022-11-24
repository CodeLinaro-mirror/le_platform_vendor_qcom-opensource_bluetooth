/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPbap;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Objects;

final class Pbap extends BluetoothProfileBase
        implements BluetoothIntent.IBluetoothIntentFilter {

    private static final String TAG = "Pbap";

    private BluetoothPbap mBluetoothPbap;
    private BluetoothIntent mBluetoothIntent;
    private IBluetoothPbapIntent mIntentHandler;

    public interface IBluetoothPbapIntent {
        void handleActionConnectionStateChanged(BluetoothDevice device, int state);
    }

    Pbap(Context context, IBluetoothPbapIntent handler) {
        super(context, TAG, Adapter.getNewAdapter());
        mBluetoothIntent = new BluetoothIntent(context, this);
        mIntentHandler = Objects.requireNonNull(handler);
        getProfileProxy(BluetoothProfile.PBAP);
    }

    public int getConnectionState(BluetoothDevice device) {
        return mBluetoothPbap != null ?
                mBluetoothPbap.getConnectionState(device) :
                BluetoothProfile.STATE_DISCONNECTED;
    }

    public boolean disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
        return mBluetoothPbap != null && mBluetoothPbap.disconnect(device);
    }

    @Override
    protected void handleServiceConnected(int profile, BluetoothProfile proxy) {
        if (profile == BluetoothProfile.PBAP) {
            mBluetoothPbap = (BluetoothPbap) proxy;
        }
    }

    @Override
    protected void handleServiceDisconnected(int profile) {
        if (profile == BluetoothProfile.PBAP) {
            mBluetoothPbap = null;
        }
    }

    @Override
    public void initIntentFilter(IntentFilter intentFilter) {
        intentFilter.addAction(BluetoothPbap.ACTION_CONNECTION_STATE_CHANGED);
    }

    @Override
    public void handleIntent(Context context, Intent intent) {
        String action = intent.getAction();
        logd("handleIntent action: " + action);
        if (BluetoothPbap.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            logd("BluetoothPbap.ACTION_CONNECTION_STATE_CHANGED");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                    BluetoothProfile.STATE_DISCONNECTED);
            mIntentHandler.handleActionConnectionStateChanged(device, state);

        } else {
           loge("unknown action: " + action);
        }
    }
}
