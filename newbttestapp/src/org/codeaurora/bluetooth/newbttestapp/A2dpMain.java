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
final class A2dpMain implements A2dp.IBluetoothA2dpIntent {

    private static final String TAG = "A2dpMain";

    private Context mContext;
    private A2dp mA2dp;

    A2dpMain(Context context) {
        mContext = context;
        mA2dp = new A2dp(context, this);
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
    public void handleActionPlayingStateChanged(BluetoothDevice device, int state) {
        logd("handleActionAudioStateChanged device: " + device + ", state " + state +
                " (" + (A2dp.isPlaying(state) ? "playing" : "not playing") + ")");
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }

    private void loge(String msg) {
        Log.e(TAG, msg);
    }
}
