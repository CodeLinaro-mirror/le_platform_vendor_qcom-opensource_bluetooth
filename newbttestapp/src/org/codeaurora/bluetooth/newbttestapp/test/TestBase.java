/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapterCommon;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.Objects;

class TestBase {

    protected static final int ADAPTER_DEFAULT = BluetoothAdapterCommon.ADAPTER_DEFAULT;
    protected static final int ADAPTER_1 = BluetoothAdapterCommon.ADAPTER_1;

    private Object mLock = new Object();
    protected Context mContext;
    protected String mTag;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothDevice mDevice;

    TestBase(Context context, String tag, BluetoothAdapter bluetoothAdapter) {
        mContext = Objects.requireNonNull(context);
        mTag = tag;
        mBluetoothAdapter = Objects.requireNonNull(bluetoothAdapter);
    }

    public void lock() {
        synchronized (mLock) {
            try {
                mLock.wait();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void unlock() {
        synchronized (mLock) {
            mLock.notify();
        }
    }

    public static boolean isEnabled(int state) {
        return state == BluetoothAdapter.STATE_ON;
    }

    public static boolean isDisabled(int state) {
        return state == BluetoothAdapter.STATE_OFF;
    }

    public static boolean isBonded(int bondState) {
        return bondState == BluetoothDevice.BOND_BONDED;
    }

    public static boolean isUnbonded(int bondState) {
        return bondState == BluetoothDevice.BOND_NONE;
    }

    public static boolean isConnected(int state) {
        return BluetoothProfileBase.isConnected(state);
    }

    public static boolean isDisconnected(int state) {
        return BluetoothProfileBase.isDisconnected(state);
    }

    public boolean isSameDevice(BluetoothDevice device) {
        return BluetoothProfileBase.isSameDevice(device, mDevice);
    }

    public void outputSuccess(String test) {
        outputResult(test, true);
    }

    public void outputFail(String test) {
        outputResult(test, false);
    }

    public void outputResult(String test, boolean success) {
        if (success) {
            logd(test + " succeed");
        } else {
            loge(test + " fail");
        }
    }

    public void logd(String msg) {
        Log.d(mTag, msg);
    }

    public void loge(String msg) {
        Log.e(mTag, msg);
    }
}
