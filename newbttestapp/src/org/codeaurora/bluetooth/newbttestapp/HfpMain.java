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
final class HfpMain implements Hfp.IBluetoothHeadsetIntent {

    private static final String TAG = "HfpMain";

    private Context mContext;
    private Hfp mHfp;

    HfpMain(Context context) {
        mContext = context;
        mHfp = new Hfp(context, this);
        init();
    }

    private void init() {
    }

    @Override
    public void handleActionConnectionStateChanged(BluetoothDevice device, int state) {
        logd("handleActionConnectionStateChanged device: " + device + ", state " +
                state + " (" + BluetoothProfile.getConnectionStateName(state) + ")");
    }

    @Override
    public void handleActionAudioStateChanged(BluetoothDevice device, int state) {
        logd("handleActionAudioStateChanged device: " + device + ", state " + state);
    }

    @Override
    public void handleActionActiveDeviceChanged(BluetoothDevice device) {
        logd("handleActionActiveDeviceChanged device: " + device);
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }

    private void loge(String msg) {
        Log.e(TAG, msg);
    }
}
