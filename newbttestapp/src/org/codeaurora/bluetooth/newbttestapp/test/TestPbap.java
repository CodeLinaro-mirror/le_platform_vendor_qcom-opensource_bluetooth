/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPbap;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

class TestPbap extends TestBase
    implements Pbap.IBluetoothPbapIntent {

    private static final String TAG = "TestPbap";

    private final Pbap mPbap;
    private boolean mIsDisconnecting = false;
    private boolean mIsSettingPermission = false;

    TestPbap(Context context) {
        super(context, TAG, Adapter.getNewAdapter());
        mPbap = new Pbap(context, this);
    }

    public void disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
        if (isDisconnected(device)) {
            // PBAP already disconnected, return
            outputSuccess("disconnect");
            return;
        }

        mDevice = Objects.requireNonNull(device);
        if (!mPbap.disconnect(device)) {
            outputFail("disconnect");
            return;
        }
        mIsDisconnecting = true;
        lock();
        outputResult("disconnect", isDisconnected(mPbap.getConnectionState(device)));
    }

    public void allowConnection(BluetoothDevice device) {
        logd("allowConnection from device: " + device);
        mDevice = Objects.requireNonNull(device);
        // Set PBAP permission before remote device initiates PBAP connection
        if (!mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED)) {
            outputFail("allowConnection");
            return;
        }
        mIsSettingPermission = true;
        lock();
        outputResult("allowConnection", isConnected(mPbap.getConnectionState(device)));
    }

    public void rejectConnection(BluetoothDevice device) {
        logd("rejectConnection from device: " + device);
        mDevice = Objects.requireNonNull(device);
        // Set PBAP permission before remote device initiates PBAP connection
        if (!mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED)) {
            outputFail("rejectConnection");
            return;
        }
        mIsSettingPermission = true;
        lock();
        outputResult("rejectConnection", isDisconnected(mPbap.getConnectionState(device)));
    }

    @Override
    public void handleActionConnectionStateChanged(BluetoothDevice device, int state) {
        logd("handleActionConnectionStateChanged device: " + device + ", state " +
                state + " (" + BluetoothProfile.getConnectionStateName(state) + ")");
        if (isSameDevice(device) &&
                (isConnected(state) ||
                isDisconnected(state))) {
            if (mIsDisconnecting) {
                if (isDisconnected(state)) {
                    mIsDisconnecting = false;
                    unlock();
                }
            } else if (mIsSettingPermission) {
                mIsSettingPermission = false;
                unlock();
            }
        }
    }

    private boolean isDisconnected(BluetoothDevice device) {
        return isDisconnected(mPbap.getConnectionState(device));
    }
}
