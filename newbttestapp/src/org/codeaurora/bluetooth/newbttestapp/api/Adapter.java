/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapterCommon;
import android.bluetooth.BluetoothAdapterExt;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.Objects;

/**
 * Bluetooth adapter API
 */
final class Adapter implements BluetoothIntent.IBluetoothIntentFilter {

    private static final String TAG = "Adapter";

    private static final int ADAPTER_DEFAULT = BluetoothAdapterCommon.ADAPTER_DEFAULT;
    private static final int ADAPTER_1 = BluetoothAdapterCommon.ADAPTER_1;

    private int mAdapterIndex;
    private IBluetoothAdapterIntent mIntentHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothIntent mBluetoothIntent;

    public static BluetoothAdapter getBluetoothAdapter(int adapterIndex) {
        if (!validAdapter(adapterIndex)) {
            throw new IllegalArgumentException("Invalid adapter index: " + adapterIndex);
        }
        return isAdapter1(adapterIndex) ? getNewAdapter() : getDefaultAdapter();
    }

    public static BluetoothAdapter getDefaultAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    public static BluetoothAdapter getNewAdapter() {
        BluetoothAdapterExt adapterExt = BluetoothAdapterExt.getDefaultAdapter();
        if (adapterExt != null) {
            return adapterExt.getBluetoothAdapter();
        } else {
            throw new IllegalArgumentException("Can't create BluetoothAdapterExt");
        }
    }

    public static BluetoothDevice getRemoteDevice(String address, int adapterIndex) {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter(adapterIndex);
        return bluetoothAdapter.getRemoteDevice(address);
    }

    public interface IBluetoothAdapterIntent {
        void handleActionStateChanged(int adapter, int state);
        void handleActionDiscoveryStarted(int adapter);
        void handleActionDiscoveryFinished(int adapter);
        void handleActionFound(int adapter, BluetoothDevice device,
                BluetoothClass bluetoothClass, short rssi, String name);
        void handleActionPairingRequest(int adapter, BluetoothDevice device,
                int pin, int type);
        void handleActionBondStateChanged(int adapter, BluetoothDevice device, int bondState);
        void handleActionLocalNameChanged(int adapter, String name);
        void handleActionScanModeChanged(int adapter, int mode);
        void handleActionAliasChanged(int adapter, BluetoothDevice device, String name);
        void handleActionRssi(int adapter, BluetoothDevice device, short rssi);
        void handleActionLinkkey(int adapter, BluetoothDevice device, String linkkey, int type);
    }

    Adapter(Context context, IBluetoothAdapterIntent handler) {
        this(context, ADAPTER_DEFAULT, handler);
    }

    Adapter(Context context, int adapterIndex, IBluetoothAdapterIntent handler) {
        if (!validAdapter(adapterIndex)) {
            throw new IllegalArgumentException("Invalid adapter index: " + adapterIndex);
        }
        mAdapterIndex = adapterIndex;
        mIntentHandler = Objects.requireNonNull(handler);
        mBluetoothAdapter = isAdapter1() ? getNewAdapter() : getDefaultAdapter();
        mBluetoothIntent = new BluetoothIntent(context, this);
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    @Override
    public void initIntentFilter(IntentFilter intentFilter) {
        intentFilter.addAction(getAction(BluetoothAdapter.ACTION_STATE_CHANGED,
                BluetoothAdapterExt.ACTION_STATE_CHANGED));
        intentFilter.addAction(getAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED,
                BluetoothAdapterExt.ACTION_DISCOVERY_STARTED));
        intentFilter.addAction(getAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
                BluetoothAdapterExt.ACTION_DISCOVERY_FINISHED));

        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        intentFilter.addAction(getAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED,
                BluetoothAdapterExt.ACTION_LOCAL_NAME_CHANGED));
        intentFilter.addAction(getAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED,
                BluetoothAdapterExt.ACTION_SCAN_MODE_CHANGED));

        intentFilter.addAction(BluetoothDevice.ACTION_ALIAS_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_RSSI);
        intentFilter.addAction(BluetoothDevice.ACTION_LINKKEY);
    }

    @Override
    public void handleIntent(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            Log.w(TAG, "Received intent with null action");
            return;
        }
        logd("handleIntent action: " + action);
        switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED: //fall through
            case BluetoothAdapterExt.ACTION_STATE_CHANGED: {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                mIntentHandler.handleActionStateChanged(mAdapterIndex, state);
                break;
            }
            case BluetoothAdapter.ACTION_DISCOVERY_STARTED: //fall through
            case BluetoothAdapterExt.ACTION_DISCOVERY_STARTED: {
                mIntentHandler.handleActionDiscoveryStarted(mAdapterIndex);
                break;
            }
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: //fall through
            case BluetoothAdapterExt.ACTION_DISCOVERY_FINISHED: {
                mIntentHandler.handleActionDiscoveryFinished(mAdapterIndex);
                break;
            }
            case BluetoothDevice.ACTION_FOUND: {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothClass bluetoothClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                mIntentHandler.handleActionFound(mAdapterIndex, device, bluetoothClass, rssi, name);
                break;
            }
            case BluetoothDevice.ACTION_PAIRING_REQUEST: {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int pin = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY,
                        BluetoothDevice.ERROR);
                int variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.ERROR);
                mIntentHandler.handleActionPairingRequest(mAdapterIndex, device, pin, variant);
                break;
            }
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                mIntentHandler.handleActionBondStateChanged(mAdapterIndex, device, bondState);
                break;
            }
            case BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED: //fall through
            case BluetoothAdapterExt.ACTION_LOCAL_NAME_CHANGED: {
                String name = intent.getStringExtra(BluetoothAdapter.EXTRA_LOCAL_NAME);
                mIntentHandler.handleActionLocalNameChanged(mAdapterIndex, name);
                break;
            }
            case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED: //fall through
            case BluetoothAdapterExt.ACTION_SCAN_MODE_CHANGED: {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                mIntentHandler.handleActionScanModeChanged(mAdapterIndex, mode);
                break;
            }
            case BluetoothDevice.ACTION_ALIAS_CHANGED: {
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mIntentHandler.handleActionAliasChanged(mAdapterIndex, device, name);
                break;
            }
            case BluetoothDevice.ACTION_RSSI: {
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mIntentHandler.handleActionRssi(mAdapterIndex, device, rssi);
                break;
            }
            case BluetoothDevice.ACTION_LINKKEY: {
                String linkkey = intent.getStringExtra(BluetoothDevice.EXTRA_KEY_LINK_KEY);
                int type = intent.getIntExtra(BluetoothDevice.EXTRA_KEY_LINK_KEY_TYPE,
                           BluetoothAdapter.ERROR);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mIntentHandler.handleActionLinkkey(mAdapterIndex, device, linkkey, type);
                break;
            }
            default: {
               loge("unknown action: " + action);
               break;
            }
        }
    }

    private String getAction(String action, String newAction) {
        return isAdapter1() ? newAction : action;
    }

    private boolean isAdapter1() {
        return BluetoothAdapterCommon.isAdapter1(mAdapterIndex);
    }

    public static boolean isAdapter1(int adapterIndex) {
        return BluetoothAdapterCommon.isAdapter1(adapterIndex);
    }

    public static boolean validAdapter(int adapterIndex) {
        return BluetoothAdapterCommon.validAdapter(adapterIndex);
    }

    public static String pairingRequestType2String(int type) {
        switch (type) {
            case BluetoothDevice.PAIRING_VARIANT_PIN:
                return "PAIRING_VARIANT_PIN";
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                return "PAIRING_VARIANT_PASSKEY";
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                return "PAIRING_VARIANT_PASSKEY_CONFIRMATION";
            case BluetoothDevice.PAIRING_VARIANT_CONSENT:
                return "PAIRING_VARIANT_CONSENT";
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
                return "PAIRING_VARIANT_DISPLAY_PASSKEY";
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
                return "PAIRING_VARIANT_DISPLAY_PIN";
            case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
                return "PAIRING_VARIANT_OOB_CONSENT";
            case BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS:
                return "PAIRING_VARIANT_PIN_16_DIGITS";
            default:
                return "Unknown pairing type";
        }
    }

    public static String bondState2String(int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_NONE:
                return "UNBONDED";
            case BluetoothDevice.BOND_BONDING:
                return "BONDING";
            case BluetoothDevice.BOND_BONDED:
                return "BONDED";
            default:
                return "Unknown bond state";
        }
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }

    private void loge(String msg) {
        Log.e(TAG, msg);
    }
}
