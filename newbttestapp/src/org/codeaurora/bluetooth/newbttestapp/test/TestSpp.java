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
import android.bluetooth.BluetoothServerSocket;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

class TestSpp extends TestBase {

    private static final String TAG = "TestSpp";

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static HashMap<BluetoothDevice, HashMap<UUID, BluetoothSocket>> sSockets =
            new HashMap<BluetoothDevice, HashMap<UUID, BluetoothSocket>>();

    private static int BUFFER_SIZE = 1024;

    private int mAdapterIndex;

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
        BluetoothSocket socket = getSocket(device, uuid);
        if (socket != null) {
            if (socket.isConnected()) {
                logd("spp already connected");
                return;
            } else {
                removeSocket(device, uuid);
                socket = null;
            }
        }

        try {
            socket = mDevice.createRfcommSocketToServiceRecord(uuid, mAdapterIndex);
            socket.connect();
        } catch (IOException e) {
            loge("connect socket fail " + e);
            return;
        }

        addSocket(device, uuid, socket);
    }

    public void disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
        disconnect(device, SPP_UUID);
    }

    public void disconnect(BluetoothDevice device, UUID uuid) {
        logd("disconnect device: " + device + ", uuid: " + uuid);
        mDevice = Objects.requireNonNull(device);
        removeSocket(device, uuid);
    }

    public void read(BluetoothDevice device) {
        read(device, SPP_UUID);
    }

    public void read(BluetoothDevice device, UUID uuid) {
        logd("read device: " + device + ", uuid: " + uuid);
        byte[] buffer = new byte[BUFFER_SIZE];
        int len = 0;
        BluetoothSocket socket = getSocket(device, uuid);
        if (!socket.isConnected()) {
            loge("fail to read spp due to socket disconnected");
            return;
        }

        try {
            len = socket.getInputStream().read(buffer);
            String s = new String(buffer, 0, len);
            logd("read date len = " + len +" from device:" + device
                + "[" + s + "]");
        } catch (IOException e) {
            loge("fail to read spp " + e);
        }
    }

    public void write(BluetoothDevice device, String data) {
        write(device, SPP_UUID, data);
    }

    public void write(BluetoothDevice device, UUID uuid, String data) {
        logd("write device: " + device + ", uuid: " + uuid + ", data: " + data);
        byte[] buffer = data.getBytes();
        BluetoothSocket socket = getSocket(device, uuid);
        if (!socket.isConnected()) {
            loge("fail to write spp due to socket disconnected");
            return;
        }

        try {
            socket.getOutputStream().write(buffer, 0, buffer.length);
            logd("write date: [" + data + "] to socket");
        } catch (IOException e) {
            loge("fail to write spp " + e);
        }
    }

    public void accept() {
        logd("accept");
        accept(SPP_UUID);
    }

    public void accept(UUID uuid) {
        logd("accept connection device with uuid: " + uuid);
        BluetoothServerSocket serverSocket;
        BluetoothSocket socket;

        try {
            serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(TAG, uuid);
            socket = serverSocket.accept();
            logd("accept connection of remote device: " + socket.getRemoteDevice());
        } catch (IOException e) {
            loge("connect socket fail " + e);
            return;
        }

        addSocket(socket.getRemoteDevice(), uuid, socket);
    }

    private BluetoothSocket getSocket(BluetoothDevice device, UUID uuid) {
        if (!sSockets.containsKey(device))
            return null;

        HashMap<UUID, BluetoothSocket> sockets = sSockets.get(device);
        return sockets.containsKey(uuid) ? sockets.get(uuid) : null;
    }

    private void addSocket(BluetoothDevice device, UUID uuid, BluetoothSocket socket) {
        HashMap<UUID, BluetoothSocket> sockets;

        if (!sSockets.containsKey(device)) {
            sockets = new HashMap<UUID, BluetoothSocket>();
            sSockets.put(device, sockets);
        } else {
            sockets = sSockets.get(device);
        }

        sockets.put(uuid, socket);
    }

    private void removeSocket(BluetoothDevice device, UUID uuid) {
        if (!sSockets.containsKey(device))
            return;

        HashMap<UUID, BluetoothSocket> sockets = sSockets.get(device);
        if (!sockets.containsKey(uuid))
            return;

        BluetoothSocket socket = sockets.get(uuid);
        try {
            socket.close();
        } catch (IOException e) {
            loge("fail to close socket " + socket);
        }

        sockets.remove(uuid);
    }

    private void removeSocket(BluetoothDevice device) {
        if (!sSockets.containsKey(device))
            return;

        HashMap<UUID, BluetoothSocket> sockets = sSockets.get(device);
        sockets.clear();

        sSockets.remove(device);
    }
}
