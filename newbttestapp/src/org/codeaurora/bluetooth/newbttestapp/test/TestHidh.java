/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidHost;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

import java.util.Objects;

class TestHidh extends TestBase
        implements Hidh.IBluetoothHidhIntent {

    private static final String TAG = "TestHidh";

    private final Hidh mHidh;

    TestHidh(Context context) {
        super(context, TAG, Adapter.getNewAdapter());
        mHidh = new Hidh(context, this);
    }

    public void connect(BluetoothDevice device) {
        logd("connect device: " + device);
        if (isConnected(device)) {
            // HIDH already connected, return
            outputSuccess("connect");
            return;
        }

        mDevice = Objects.requireNonNull(device);
        if (!mHidh.connect(device)) {
            outputFail("connect");
            return;
        }
        lock();
        outputResult("connect", isConnected(mHidh.getConnectionState(device)));
    }

    public void disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
        if (isDisconnected(device)) {
            // HIDH already disconnected, return
            outputSuccess("disconnect");
            return;
        }

        mDevice = Objects.requireNonNull(device);
        if (!mHidh.disconnect(device)) {
            outputFail("disconnect");
            return;
        }
        lock();
        outputResult("disconnect", isDisconnected(mHidh.getConnectionState(device)));
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

    private boolean isConnected(BluetoothDevice device) {
        return isConnected(mHidh.getConnectionState(device));
    }

    private boolean isDisconnected(BluetoothDevice device) {
        return isDisconnected(mHidh.getConnectionState(device));
    }
}
