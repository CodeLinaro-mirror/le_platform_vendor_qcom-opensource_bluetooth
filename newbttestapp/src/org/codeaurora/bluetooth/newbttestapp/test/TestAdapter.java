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

import com.android.internal.util.CollectionUtils;

import java.util.List;
import java.util.HashMap;
import java.util.Objects;

class TestAdapter extends TestBase
        implements Adapter.IBluetoothAdapterIntent {

    private static final String TAG = "TestAdapter";

    private final Adapter mAdapter;
    // true: accept pairing from remote device, false: reject
    private boolean mAcceptPair;
    private String mPasskey;
    private boolean mSetScanMode;

    TestAdapter(Context context) {
        this(context, ADAPTER_DEFAULT);
    }

    TestAdapter(Context context, int adapterIndex) {
        super(context, TAG, Adapter.getBluetoothAdapter(adapterIndex));
        mAdapter = new Adapter(context, adapterIndex, this);
    }

    public void enable() {
        logd("enable");
        if (isEnabled()) {
            // Already enabled, return
            outputSuccess("enable");
            return;
        }

        if (!mBluetoothAdapter.enable()) {
            outputFail("enable");
            return;
        }
        lock();

        outputResult("enable", isEnabled());
    }

    public void disable() {
        logd("disable");
        if (isDisabled()) {
            // Already disabled, return
            outputSuccess("disable");
            return;
        }

        if (!mBluetoothAdapter.disable()) {
            outputFail("disable");
            return;
        }
        lock();

        outputResult("disable", isDisabled());
    }

    public void search() {
        logd("search");
        if (!mBluetoothAdapter.startDiscovery()) {
            outputFail("search");
            return;
        }
        lock();
        outputSuccess("search");
    }

    public void cancelSearch() {
        logd("cancelSearch");
        if (!mBluetoothAdapter.cancelDiscovery()) {
            outputFail("cancelSearch");
            return;
        }
        outputSuccess("cancelSearch");
    }

    public void pair(BluetoothDevice device) {
        logd("pair device: " + device);
        mDevice = Objects.requireNonNull(device);
        mAcceptPair = true;
        if (!mDevice.createBond()) {
            outputFail("pair");
            return;
        }
        lock();
        outputResult("pair", isBonded(mDevice.getBondState()));
    }

    public void acceptPair(BluetoothDevice device) {
        logd("accept pairing from device: " + device);
        mDevice = Objects.requireNonNull(device);
        mAcceptPair = true;
        lock();
        outputResult("acceptPair", isBonded(mDevice.getBondState()));
    }

    public void rejectPair(BluetoothDevice device) {
        logd("reject pairing from device: " + device);
        mDevice = Objects.requireNonNull(device);
        mAcceptPair = false;
        lock();
        outputResult("rejectPair", isUnbonded(mDevice.getBondState()));
    }

    public void setPin(String passkey) {
        logd("set passkey:" + passkey);
        mPasskey = passkey;
        lock();
        outputResult("rejectPair", isBonded(mDevice.getBondState()));
    }

    public void unpair(BluetoothDevice device) {
        logd("unpair device: " + device);
        mDevice = Objects.requireNonNull(device);
        mDevice.removeBond();
        lock();
        outputResult("unpair", isUnbonded(mDevice.getBondState()));
    }

    public void getSupportedProfiles() {
        logd("getSupportedProfiles");
        List<Integer> supportedProfiles = mBluetoothAdapter.getSupportedProfiles();
        dumpSupportedProfile(supportedProfiles);
        outputResult("getSupportedProfiles", !CollectionUtils.isEmpty(supportedProfiles));
    }

    public void getLocalAddress() {
        logd("getLocalAddress");
        String address = mBluetoothAdapter.getAddress();
        logd("getLocalAddress: " + address);
        outputResult("getLocalAddress", address != null);
    }

    public void getLocalName() {
        logd("getLocalName");
        String name = mBluetoothAdapter.getName();
        logd("getLocalName: " + name);
        outputResult("getLocalName", name != null);
    }

    public void setLocalName(String name) {
        logd("setLocalName");
        mBluetoothAdapter.setName(name);
        lock();
        outputResult("setLocalName", name.equals(mBluetoothAdapter.getName()));
    }

    public void getScanMode() {
        logd("getScanMode");
        int mode = mBluetoothAdapter.getScanMode();
        logd("getScanMode: " + mode);
        outputResult("getScanMode", mode != 0);
    }

    public void setScanMode(int mode) {
        logd("setScanMode");
        mSetScanMode = true;
        mBluetoothAdapter.setScanMode(mode);
        lock();
        outputResult("setScanMode", mode == mBluetoothAdapter.getScanMode());
    }

    public void setDiscoverableTout(int timeout) {
        logd("setDiscoverableTout");
        mBluetoothAdapter.setDiscoverableTimeout(timeout);
        outputResult("setDiscoverableTout", timeout == mBluetoothAdapter.getDiscoverableTimeout());
    }

    public void getLocalCod() {
        logd("getLocalCod");
        BluetoothClass Cod = mBluetoothAdapter.getBluetoothClass();
        logd("getLocalCod: " + Cod);
        outputResult("getLocalCod", Cod != null);
    }

    public void getRemoteName(BluetoothDevice device) {
        logd("getRemoteName");
        mDevice = Objects.requireNonNull(device);
        String name = mDevice.getName();
        logd("getRemoteName: " + name);
        outputResult("getRemoteName", name != null);
    }

    public void getAliasName(BluetoothDevice device) {
        logd("getAliasName");
        mDevice = Objects.requireNonNull(device);
        String name = mDevice.getAlias();
        logd("getAliasName: " + name);
        outputResult("getAliasName", name != null);
    }

    public void setAliasName(BluetoothDevice device, String name) {
        logd("setAliasName device: " + device);
        mDevice = Objects.requireNonNull(device);
        mDevice.setAlias(name);
        lock();
        outputResult("setAliasName", name.equals(device.getAlias()));
    }

    public void getRemoteCod(BluetoothDevice device) {
        logd("getRemoteCod device: " + device);
        mDevice = Objects.requireNonNull(device);
        BluetoothClass Cod = mDevice.getBluetoothClass();
        logd("getRemoteCod: " + Cod);
        outputResult("getRemoteCod", Cod != null);
    }

    public void getRssi(BluetoothDevice device) {
        logd("getRssi device: " + device);
        mDevice = Objects.requireNonNull(device);
        mDevice.getRssi(BluetoothDevice.TRANSPORT_BREDR);
        lock();
        outputResult("getRssi", true);
    }

    public void getLinkKey(BluetoothDevice device, Context context) {
        logd("getLinkKey device: " + device);
        mDevice = Objects.requireNonNull(device);
        mDevice.getLinkKey(context);
        lock();
        outputResult("getLinkKey", true);
    }

    private void dumpSupportedProfile(@NonNull List<Integer> supportedProfiles) {
        logd("dumpSupportedProfile: " + supportedProfiles.size());
        for (Integer profile : supportedProfiles) {
            logd(BluetoothProfile.getProfileName(profile));
        }
    }

    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public boolean isDisabled() {
        return isDisabled(mBluetoothAdapter.getState());
    }

    @Override
    public void handleActionStateChanged(int adapter, int state) {
        logd("handleActionStateChanged adapter: " + adapter + ", state: " +
                state + " (" + BluetoothAdapter.nameForState(state) + ")");
        if (isEnabled(state) || isDisabled(state)) {
            unlock();
        }
    }

    @Override
    public void handleActionDiscoveryStarted(int adapter) {
        logd("handleActionDiscoveryStarted adapter: " + adapter);
    }

    @Override
    public void handleActionDiscoveryFinished(int adapter) {
        logd("handleActionDiscoveryFinished adapter: " + adapter);
        unlock();
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
                type + " (" + Adapter.pairingRequestType2String(type) + ")");
        if (isSameDevice(device)) {
            switch (type) {
                case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION: // fall through
                case BluetoothDevice.PAIRING_VARIANT_CONSENT: // for "Just Works" association model 
                    if (mAcceptPair) {
                        // Accept pairing from remote device
                        mDevice.setPairingConfirmation(true);
                    } else {
                        // Reject pairing from remote device
                        mDevice.setPairingConfirmation(false);
                    }
                    break;
                case BluetoothDevice.PAIRING_VARIANT_PIN:
                case BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS:
                    mDevice.setPin(mPasskey);
            }
        }
    }

    @Override
    public void handleActionBondStateChanged(int adapter, BluetoothDevice device, int bondState) {
        logd("handleActionBondStateChanged adatper: " + adapter +
                ", device: " + device + ", bondState: " + bondState +
                " (" + Adapter.bondState2String(bondState) + ")");
        if (isSameDevice(device) &&
                (isBonded(bondState) ||
                isUnbonded(bondState))) {
            if (isBonded(bondState)) {
                logd("handleActionBondStateChanged: allow PBAP connection");
                // Allow PBAP connection after pairing is completed.
                device.setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
            }
            unlock();
        }
    }

    @Override
    public void handleActionLocalNameChanged(int adapter, String name) {
        logd("handleActionLocalNameChanged adatper: " + adapter +
                ", name: " + name);
        unlock();
    }

    @Override
    public void handleActionScanModeChanged(int adapter, int mode) {
        logd("handleActionScanModeChanged adatper: " + adapter +
                ", mode: " + mode);
        // Only unlock when action is triggered by calling API setScanMode
        if (mSetScanMode) {
            unlock();
            mSetScanMode = false;
        }
    }

    @Override
    public void handleActionAliasChanged(int adapter, BluetoothDevice device, String name) {
        logd("handleActionAliasChanged adatper: " + adapter +
                ", device: " + device + ", name: " + name);
        unlock();
    }

    @Override
    public void handleActionRssi(int adapter, BluetoothDevice device, short rssi) {
        logd("handleActionRssi adatper: " + adapter +
                ", device: " + device + ", rssi: " + rssi);
        unlock();
    }

    @Override
    public void handleActionLinkkey(int adapter, BluetoothDevice device, String linkkey, int type) {
        logd("handleActionLinkkey adatper: " + adapter +
                ", device: " + device + ", linkkey: " + linkkey + ", type:" + type);
        unlock();
    }
}
