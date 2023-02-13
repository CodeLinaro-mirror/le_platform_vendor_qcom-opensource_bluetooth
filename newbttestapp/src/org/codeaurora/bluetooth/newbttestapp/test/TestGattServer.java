/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;

import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;

class TestGattServer extends TestBase {

    private static final String TAG = "TestGattServer";

    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattServer;
    private BluetoothLeAdvertiser mAdvertiser;
    private AdvCallback mAdvCallback = new AdvCallback();
    private ServerCallback mServerCallback = new ServerCallback();

    private static LinkedHashSet<BluetoothGattService> sGattService =
            new LinkedHashSet<BluetoothGattService>();

    private static final boolean IS_AUTO_CONN = false; // TODO: Support auto connection

    private static final int ADV_DURATION = 0;
    private static final int MAX_EXT_ADV_EVENTS = 0;

    private static final AdvertiseSettings ADV_SETTINGS = null;
    private static final AdvertisingSetParameters ADV_SET_PARAM = new AdvertisingSetParameters.Builder()
               .setConnectable(true)
               .setScannable(true)
               .setLegacyMode(true)
               .setAnonymous(false)
               .setIncludeTxPower(false)
               .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
               .setSecondaryPhy(BluetoothDevice.PHY_LE_1M)
               .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
               .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
               .build();
    private static final PeriodicAdvertisingParameters PERIODIC_ADV_PARAM = null; // TODO: Support periodic advertising
    private static final AdvertiseData ADV_DATA_ADV = new AdvertiseData.Builder()
               .setIncludeDeviceName(true)
               .setIncludeTxPowerLevel(false)
               .build();
    private static final AdvertiseData ADV_DATA_SCAN_RESPONSE = null;
    private static final AdvertiseData ADV_DATA_PERIODIC = null;
    private static final UUID IMMEDIATE_ALERT_UUID = UUID.fromString("00001200-0000-1000-8000-00805f9b34fb");

    TestGattServer(Context context) {
        this(context, ADAPTER_DEFAULT);
    }

    TestGattServer(Context context, int adapterIndex) {
        super(context, TAG, Adapter.getBluetoothAdapter(adapterIndex));
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mGattServer = mBluetoothManager.openGattServer(adapterIndex, context, mServerCallback);
        mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    }

    public void startAdvertisingSet() {
        mAdvertiser.startAdvertisingSet(ADV_SET_PARAM,
            ADV_DATA_ADV, ADV_DATA_SCAN_RESPONSE,
            PERIODIC_ADV_PARAM,
            ADV_DATA_PERIODIC, ADV_DURATION,
            MAX_EXT_ADV_EVENTS,
            mAdvCallback);
        lock();
    }

    public void stopAdvertisingSet() {
        startAdvertisingSet();
        loge("stopAdvertisingSet");
        mAdvertiser.stopAdvertisingSet(mAdvCallback);
        lock();
    }

    public void addService() {
        BluetoothGattService service = new BluetoothGattService(IMMEDIATE_ALERT_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mGattServer.addService(service);
        lock();
        outputSuccess("addService");
    }

    public void removeService() {
        if (sGattService.size() == 0) {
            loge("No service to remove");
            outputFail("removeService");
        }
        BluetoothGattService service = sGattService.stream().findFirst().get();
        mGattServer.removeService(service);
        sGattService.remove(service);
        outputSuccess("removeService");
    }

    public void connect(BluetoothDevice device) {
        mGattServer.connect(device, IS_AUTO_CONN);
        outputSuccess("connectClient");
    }

    public void disconnect(BluetoothDevice device) {
        mGattServer.cancelConnection(device);
        outputSuccess("disconnectClient");
    }

    public void close() {
        mGattServer.close();
        outputSuccess("close");
    }

    private class AdvCallback extends AdvertisingSetCallback {
        @Override
        public void onAdvertisingSetStarted(AdvertisingSet advSet, int txPower, int status) {
            logd("onAdvertisingSetStarted");
            if (status != 0) {
                unlock();
                outputFail("startAdvertisingSet");
                return;
            }
            advSet.enableAdvertising(true, ADV_DURATION, MAX_EXT_ADV_EVENTS);
        }

        @Override
        public void onAdvertisingSetStopped(AdvertisingSet advSet) {
            logd("onAdvertisingSetStopped, SID: " + advSet.getAdvertiserId());
            unlock();
            outputSuccess("stopAdvertisingSet");
        }

        @Override
        public void onAdvertisingEnabled(AdvertisingSet advertisingSet, boolean enable, int status) {
            unlock();
            outputSuccess("startAdvertisingSet");
        }
    }

    private class ServerCallback extends BluetoothGattServerCallback {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange: state = " + newState + " device:" + device.getAddress());
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
                device.connectGatt(mContext, false, new ClientCallback(), BluetoothDevice.TRANSPORT_LE);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            sGattService.add(service);
            logd("ServerCallback:onServiceAdded, status: " + status);
            unlock();
        }
    }

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
