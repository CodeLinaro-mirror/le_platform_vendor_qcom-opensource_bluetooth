/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.annotation.NonNull;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapterCommon;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;
import java.util.HashMap;

/**
 * Bluetooth adapter main
 */
final class AdapterMain implements Adapter.IBluetoothAdapterIntent {

    private static final String TAG = "AdapterMain";

    private static final int ADAPTER_DEFAULT = BluetoothAdapterCommon.ADAPTER_DEFAULT;
    private static final int ADAPTER_1 = BluetoothAdapterCommon.ADAPTER_1;
    private static final int ADAPTER_NUMBER = BluetoothAdapterCommon.ADAPTER_NUMBER;

    private Context mContext;
    private int mAdapterIndex = ADAPTER_1;
    private boolean mIsDualMode = false;
    private HashMap<Integer, Adapter> mAdapters;
    private final Object mObject = new Object();

    AdapterMain(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        initAdapter();
        dumpSupportedProfile();
        // [TODO] Init adapter UI
    }

    private void initAdapter() {
        mAdapters = new HashMap<Integer, Adapter>(ADAPTER_NUMBER);
        mAdapters.put(ADAPTER_DEFAULT, new Adapter(mContext, ADAPTER_DEFAULT, this));
        mAdapters.put(ADAPTER_1, new Adapter(mContext, ADAPTER_1, this));
    }

    public void setActiveAdapter(int adapter) {
        verifyAdapterIndex(adapter);
        synchronized (mObject) {
            mAdapterIndex = adapter;
            mIsDualMode = false;
            disableAll();
        }
    }

    public void setDualMode() {
        synchronized (mObject) {
            mIsDualMode = true;
            disableAll();
        }
    }

    private int getAdapterIndex() {
        synchronized (mObject) {
            return mAdapterIndex;
        }
    }

    private BluetoothAdapter getBluetoothAdapter(int adapter) {
        return mAdapters.get(adapter).getBluetoothAdapter();
    }

    public void enable() {
        synchronized (mObject) {
            if (mIsDualMode) {
                enableAll();
            } else {
                enable(mAdapterIndex);
            }
        }
    }

    private boolean enable(int adapter) {
        return getBluetoothAdapter(adapter).enable();
    }

    private void enableAll() {
        enable(ADAPTER_DEFAULT);
        enable(ADAPTER_1);
    }

    public void disable() {
        synchronized (mObject) {
            if (mIsDualMode) {
                disableAll();
            } else {
                disable(mAdapterIndex);
            }
        }
    }

    private boolean disable(int adapter) {
        return getBluetoothAdapter(adapter).disable();
    }

    private void disableAll() {
        disable(ADAPTER_DEFAULT);
        disable(ADAPTER_1);
    }

    private List<Integer> getSupportedProfiles(int adapter) {
        return getBluetoothAdapter(adapter).getSupportedProfiles();
    }

    public boolean search() {
        return getBluetoothAdapter(getAdapterIndex()).startDiscovery();
    }

    public boolean cancelSearch() {
        return getBluetoothAdapter(getAdapterIndex()).cancelDiscovery();
    }

    public boolean pair(@NonNull BluetoothDevice device) {
        return device.createBond();
    }

    private void dumpSupportedProfile() {
        dumpSupportedProfile(ADAPTER_DEFAULT, getSupportedProfiles(ADAPTER_DEFAULT));
        dumpSupportedProfile(ADAPTER_1, getSupportedProfiles(ADAPTER_1));
    }

    private void dumpSupportedProfile(int adapter, @NonNull List<Integer> supportedProfiles) {
        logd("dumpSupportedProfile: " + supportedProfiles.size() + " in adapter" + adapter);
        for (Integer profile : supportedProfiles) {
            logd(BluetoothProfile.getProfileName(profile));
        }
    }

    @Override
    public void handleActionStateChanged(int adapter, int state) {
        logd("handleActionStateChanged adapter: " + adapter + ", state: " +
                state + " (" + BluetoothAdapter.nameForState(state) + ")");
    }

    @Override
    public void handleActionDiscoveryStarted(int adapter) {
        logd("handleActionDiscoveryStarted adapter: " + adapter);
    }

    @Override
    public void handleActionDiscoveryFinished(int adapter) {
        logd("handleActionDiscoveryFinished adapter: " + adapter);
    }

    @Override
    public void handleActionFound(int adapter, BluetoothDevice device,
            BluetoothClass bluetoothClass, short rssi, String name) {
        logd("handleActionFound adapter: " + adapter + ", device: " + device +
                ", bluetoothClass: " + bluetoothClass + ", rssi " + rssi + ", name " + name);
    }

    @Override
    public void handleActionPairingRequest(int adapter, BluetoothDevice device,
            int pin, int type) {
        logd("handleActionPairingRequest adapter: " + adapter +
                ", device: " + device + ", pin: " + pin + ", type " +
                type + "(" + Adapter.pairingRequestType2String(type) + ")");
    }

    @Override
    public void handleActionBondStateChanged(int adapter, BluetoothDevice device, int bondState) {
        logd("handleActionBondStateChanged adatper: " + adapter +
                ", device: " + device + ", bondState: " + bondState);
    }

    @Override
    public void handleActionLocalNameChanged(int adapter, String name) {
        logd("handleActionLocalNameChanged adatper: " + adapter +
                ", name: " + name);
    }

    @Override
    public void handleActionScanModeChanged(int adapter, int mode) {
        logd("handleActionScanModeChanged adatper: " + adapter +
                ", mode: " + mode);
    }

    @Override
    public void handleActionAliasChanged(int adapter, BluetoothDevice device, String name) {
        logd("handleActionAliasChanged adatper: " + adapter +
                ", device: " + device + ", name: " + name);
    }

    @Override
    public void handleActionRssi(int adapter, BluetoothDevice device, short rssi) {
        logd("handleActionRssi adatper: " + adapter +
                ", device: " + device + ", rssi: " + rssi);
    }

    @Override
    public void handleActionLinkkey(int adapter, BluetoothDevice device, String linkkey, int type) {
        logd("handleActionLinkkey adatper: " + adapter +
                ", device: " + device + ", linkkey: " + linkkey + ", type:" + type);
    }

    private void verifyAdapterIndex(int adapter) {
        if (!Adapter.validAdapter(adapter)) {
            throw new IllegalArgumentException("Invalid adapter index: " + adapter);
        }
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }

    private void loge(String msg) {
        Log.e(TAG, msg);
    }
}
