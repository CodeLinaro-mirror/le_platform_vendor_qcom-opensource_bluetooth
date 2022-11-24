/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

final class Spp implements BluetoothIntent.IBluetoothIntentFilter {

    private static final String TAG = "Spp";

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Context mContext;
    private final HashMap<BluetoothDevice, ArrayList<BluetoothSocket>> mSockets;
    private final Object mObject = new Object();

    Spp(Context context) {
        mContext = context;
        mSockets = new HashMap<BluetoothDevice, ArrayList<BluetoothSocket>>();
    }

    public void connect(BluetoothDevice device) {
        logd("connect device: " + device);
        connect(device, SPP_UUID);
    }

    public void connect(BluetoothDevice device, UUID uuid) {
        logd("connect device: " + device + ", uuid: " + uuid);
        verifyDevice(device);
        synchronized (mObject) {
            try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
                socket.connect();
                putSocket(device, socket);
            } catch (IOException e) {
                Log.e(TAG, "connect socket fail ", e);
            }
        }
    }

    public void disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
        verifyDevice(device);
        ArrayList<BluetoothSocket> sockets = mSockets.get(device);
        if (sockets != null) {
            for (BluetoothSocket socket : sockets) {
                disconnect(device, socket);
            }
        } else {
            loge("none spp is connected in device: " + device);
        }
    }

    public void disconnect(BluetoothDevice device, BluetoothSocket socket) {
        logd("disconnect device: " + device + ", socket: " + socket);
        verifySocket(device, socket);
        synchronized (mObject) {
            try {
                socket.close();
                removeSocket(device, socket);
            } catch (IOException e) {
                Log.e(TAG, "close socket fail", e);
            }
        }
    }

    private void putSocket(BluetoothDevice device, BluetoothSocket socket) {
        ArrayList<BluetoothSocket> sockets = mSockets.get(device);
        if (sockets == null) {
            sockets = new ArrayList<BluetoothSocket>();
            mSockets.put(device, sockets);
        }
        sockets.add(socket);
    }

    private void removeSocket(BluetoothDevice device, BluetoothSocket socket) {
        ArrayList<BluetoothSocket> sockets = mSockets.get(device);
        if (sockets != null) {
            sockets.remove(socket);
            if (sockets.size() == 0) {
                mSockets.remove(device);
            }
        }
    }

    private void verifyDevice(BluetoothDevice device) {
        if (device == null ||
                !Adapter.validAdapter(device.getAdapterIndex())) {
            throw new IllegalArgumentException("Invalid BluetoothDevice: " + device);
        }
    }

    private void verifySocket(BluetoothDevice device, BluetoothSocket socket) {
        verifyDevice(device);
        if (!existSocket(device, socket)) {
            throw new IllegalArgumentException("Invalid BluetoothSocket: " + socket);
        }
    }

    private boolean existSocket(BluetoothDevice device, BluetoothSocket socket) {
        ArrayList<BluetoothSocket> sockets = mSockets.get(device);
        return sockets != null && sockets.contains(socket);
    }

    @Override
    public void initIntentFilter(IntentFilter intentFilter) {
    }

    @Override
    public void handleIntent(Context context, Intent intent) {
    }

    private static void logd(String msg) {
        Log.d(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
