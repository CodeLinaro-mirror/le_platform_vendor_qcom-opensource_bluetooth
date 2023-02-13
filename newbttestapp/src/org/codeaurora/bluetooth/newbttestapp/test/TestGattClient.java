/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

import java.util.HashMap;
import java.util.List;

class TestGattClient extends TestBase {

    private static final String TAG = "TestGattClient";

    private BluetoothLeScanner mScanner;
    private ClientCallback mClientCallback = new ClientCallback();

    private static HashMap<BluetoothDevice, BluetoothGatt> sGattClient =
            new HashMap<BluetoothDevice, BluetoothGatt>();

    private static final boolean IS_AUTO_CONN = false; // TODO: Support auto connection

    TestGattClient(Context context) {
        this(context, ADAPTER_DEFAULT);
    }

    TestGattClient(Context context, int adapterIndex) {
        super(context, TAG, Adapter.getBluetoothAdapter(adapterIndex));
        mScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    public void startScanning() {
        mScanner.startScan(mScanCallback);
        lock();
        outputSuccess("startScanning");
    }

    public void stopScanning() {
        mScanner.stopScan(mScanCallback);
        outputSuccess("stopScanning");
    }

    public BluetoothGatt connect(BluetoothDevice device) {
        BluetoothGatt gattClient = device.connectGatt(mContext, IS_AUTO_CONN, mClientCallback, BluetoothDevice.TRANSPORT_LE);
        lock();
        outputSuccess("connectServer");
        sGattClient.put(device, gattClient);
        return gattClient;
    }

    public void discoverService(BluetoothDevice device) {
        BluetoothGatt gattClient = sGattClient.get(device);
        if (gattClient == null) {
            outputFail("discoverService");
            return;
        }
        gattClient.discoverServices();
        lock();
    }

    public void disconnect(BluetoothDevice device) {
        BluetoothGatt gattClient = sGattClient.get(device);
        if (gattClient == null) {
            outputFail("disconnectServer");
        }
        gattClient.disconnect();
        lock();
        outputSuccess("disconnectServer");
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int CallbackType, ScanResult result) {
            final BluetoothDevice device = result.getDevice();
            final int i = result.getRssi();
            final ScanResult r = result;
            logd("onScanResult, device: " + device + ", name:" + device.getName());
            logd("onScanResult, ScanResult: " + r);
            unlock();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            int batchResultSize = results == null ? 0 : results.size();
            Log.d(TAG, "onBatchScanResults - size " + batchResultSize);
            for (ScanResult scanResult : results) {
                final BluetoothDevice device = scanResult.getDevice();
                final int i = scanResult.getRssi();
                final ScanResult r = scanResult;
                logd("onScanResult, device: " + device + ", name:" + device.getName());
                logd("onScanResult, ScanResult: " + r);
                unlock();
            }
        }
    };

    private class ClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            logd("GATTClient:onConnectionStateChange, device " + gatt.getDevice() + ", status: " + status
                    + ", new state = " + newState);
            unlock();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            logd("GATTClient:onServicesDiscovered: " + gatt.getDevice());
            unlock();
        }
    }
}
