/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

class TestA2dp extends TestBase
        implements A2dp.IBluetoothA2dpIntent {

    private static final String TAG = "TestA2dp";

    private final A2dp mA2dp;

    TestA2dp(Context context) {
        super(context, TAG, Adapter.getNewAdapter());
        mA2dp = new A2dp(context, this);
    }

    public void connect(BluetoothDevice device) {
        logd("connect device: " + device);
        if (isConnected(device)) {
            // A2DP already connected, return
            outputSuccess("connect");
            return;
        }

        mDevice = Objects.requireNonNull(device);
        if (!mA2dp.connect(device)) {
            outputFail("connect");
            return;
        }
        lock();
        outputResult("connect", isConnected(mA2dp.getConnectionState(device)));
    }

    public void disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
        if (isDisconnected(device)) {
            // A2DP already disconnected, return
            outputSuccess("disconnect");
            return;
        }

        mDevice = Objects.requireNonNull(device);
        if (!mA2dp.disconnect(device)) {
            outputFail("disconnect");
            return;
        }
        lock();
        outputResult("disconnect", isDisconnected(mA2dp.getConnectionState(device)));
    }

    @Override
    public void handleActionConnectionStateChanged(BluetoothDevice device, int state) {
        logd("handleActionConnectionStateChanged device: " + device + ", state " +
                state + " (" + BluetoothProfile.getConnectionStateName(state) + ")");
        if (isSameDevice(device) &&
                (isConnected(state) ||
                isDisconnected(state))) {
            unlock();
        }
    }

    @Override
    public void handleActionPlayingStateChanged(BluetoothDevice device, int state) {
        logd("handleActionAudioStateChanged device: " + device + ", state " + state +
                " (" + (isPlaying(state) ? "playing" : "not playing") + ")");
        if (isSameDevice(device)) {
            unlock();
        }
    }

    private boolean isConnected(BluetoothDevice device) {
        return isConnected(mA2dp.getConnectionState(device));
    }

    private boolean isDisconnected(BluetoothDevice device) {
        return isDisconnected(mA2dp.getConnectionState(device));
    }

    private static boolean isPlaying(int state) {
        return state == BluetoothA2dp.STATE_PLAYING;
    }
}
