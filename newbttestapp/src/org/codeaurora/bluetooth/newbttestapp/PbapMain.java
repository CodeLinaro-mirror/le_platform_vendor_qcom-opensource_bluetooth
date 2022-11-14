/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

/**
 * Bluetooth adapter main
 */
final class PbapMain implements Pbap.IBluetoothPbapIntent {

    private static final String TAG = "PbapMain";

    private Context mContext;
    private Pbap mPbap;

    PbapMain(Context context) {
        mContext = context;
        mPbap = new Pbap(context, this);
        init();
    }

    private void init() {
    }

    @Override
    public void handleActionConnectionStateChanged(BluetoothDevice device, int state) {
        logd("handleActionConnectionStateChanged device: " + device + ", state " +
                state + " (" + BluetoothProfile.getConnectionStateName(state) + ")");
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }

    private void loge(String msg) {
        Log.e(TAG, msg);
    }
}
