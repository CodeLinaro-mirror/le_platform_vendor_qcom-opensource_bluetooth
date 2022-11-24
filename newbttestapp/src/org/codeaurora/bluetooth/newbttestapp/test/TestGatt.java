/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapterUtil;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

import java.util.Objects;

class TestGatt extends TestBase {

    private static final String TAG = "TestGatt";

    private static int mAdapterIndex;

    TestGatt(Context context) {
        this(context, ADAPTER_DEFAULT);
    }

    TestGatt(Context context, int adapter) {
        super(context, TAG, Adapter.getBluetoothAdapter(adapter));
        mAdapterIndex = adapter;
    }

    public void connect(BluetoothDevice device) {
        logd("connect device: " + device);
    }

    public void disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
    }
}
