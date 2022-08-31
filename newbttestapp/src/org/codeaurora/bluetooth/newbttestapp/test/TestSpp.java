/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapterExt;
import android.bluetooth.BluetoothAdapterUtil;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.UUID;

class TestSpp extends TestBase {

    private static final String TAG = "TestSpp";

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothSocket mBluetoothSocket;

    private static int mAdapterIndex;

    TestSpp(Context context) {
        this(context, ADAPTER_DEFAULT);
    }

    TestSpp(Context context, int adapter) {
        super(context, TAG, Adapter.getBluetoothAdapter(adapter));
        mAdapterIndex = adapter;
    }

    public void connect(BluetoothDevice device) {
        logd("connect device: " + device);
        connect(device, SPP_UUID);
    }

    public void connect(BluetoothDevice device, UUID uuid) {
        logd("connect device: " + device + ", uuid: " + uuid);
        mDevice = Objects.requireNonNull(device);
        try {
            mBluetoothSocket = mDevice.createRfcommSocketToServiceRecord(uuid, mAdapterIndex);
            mBluetoothSocket.connect();
        } catch (IOException e) {
            Log.e(TAG, "connect socket fail", e);
        }
    }

    public void disconnect(BluetoothDevice device) {
        logd("connect device: " + device);
        disconnect(device, SPP_UUID);
    }

    public void disconnect(BluetoothDevice device, UUID uuid) {
        logd("connect device: " + device + ", uuid: " + uuid);
        mDevice = Objects.requireNonNull(device);
        try {
            mBluetoothSocket = mDevice.createRfcommSocketToServiceRecord(uuid, mAdapterIndex);
            mBluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close socket fail", e);
        }
    }
}
