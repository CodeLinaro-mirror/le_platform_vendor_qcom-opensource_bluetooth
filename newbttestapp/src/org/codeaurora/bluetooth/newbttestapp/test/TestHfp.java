/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

class TestHfp extends TestBase
        implements Hfp.IBluetoothHeadsetIntent {

    private static final String TAG = "TestHfp";

    private final Hfp mHfp;

    TestHfp(Context context) {
        super(context, TAG, Adapter.getNewAdapter());
        mHfp = new Hfp(context, this);
    }

    public void connect(BluetoothDevice device) {
        logd("connect device: " + device);
        if (isConnected(device)) {
            // hfp already connected, return
            outputSuccess("connect");
            return;
        }

        mDevice = Objects.requireNonNull(device);
        if (!mHfp.connect(device)) {
            outputFail("connect");
            return;
        }
        lock();
        outputResult("connect", isConnected(device));
    }

    public void disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
        if (isDisconnected(device)) {
            // hfp already disconnected, return
            outputSuccess("disconnect");
            return;
        }

        mDevice = Objects.requireNonNull(device);
        if (!mHfp.disconnect(device)) {
            outputFail("disconnect");
            return;
        }
        lock();
        outputResult("disconnect", isDisconnected(device));
    }

    public void connectAudio(BluetoothDevice device) {
        logd("connectAudio device: " + device);
        if (isAudioConnected(device)) {
            // hfp audio already connected, return
            outputSuccess("connectAudio");
            return;
        }

        mDevice = Objects.requireNonNull(device);
        if (!mHfp.connectAudio(device)) {
            outputFail("connectAudio");
            return;
        }
        lock();
        outputResult("connectAudio", isAudioConnected(device));
    }

    public void disconnectAudio(BluetoothDevice device) {
        logd("disconnectAudio device: " + device);
        if (isAudioDisconnected(device)) {
            // hfp audio already disconnected, return
            outputSuccess("disconnectAudio");
            return;
        }

        mDevice = Objects.requireNonNull(device);
        if (!mHfp.disconnectAudio(device)) {
            outputFail("disconnectAudio");
            return;
        }
        lock();
        outputResult("disconnectAudio", isAudioDisconnected(device));
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
    public void handleActionAudioStateChanged(BluetoothDevice device, int state) {
        logd("handleActionAudioStateChanged device: " + device + ", state " + state);
        if (isSameDevice(device) &&
                (Hfp.isAudioConnected(state) ||
                Hfp.isAudioDisconnected(state))) {
            unlock();
        }
    }

    @Override
    public void handleActionActiveDeviceChanged(BluetoothDevice device) {
        logd("handleActionActiveDeviceChanged device: " + device);
    }

    private boolean isConnected(BluetoothDevice device) {
        return isConnected(mHfp.getConnectionState(device));
    }

    private boolean isDisconnected(BluetoothDevice device) {
        return isDisconnected(mHfp.getConnectionState(device));
    }

    private boolean isAudioConnected(BluetoothDevice device) {
        return Hfp.isAudioConnected(mHfp.getAudioState(device));
    }

    private boolean isAudioDisconnected(BluetoothDevice device) {
        return Hfp.isAudioDisconnected(mHfp.getAudioState(device));
    }
}
