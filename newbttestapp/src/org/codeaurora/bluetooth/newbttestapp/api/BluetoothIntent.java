/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Objects;

public class BluetoothIntent {

    private static final String TAG = "BluetoothIntent";

    private Context mContext;
    private IBluetoothIntentFilter mHandler;
    private IntentFilter mIntentFilter = new IntentFilter();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.handleIntent(context, intent);
        }
    };

    public interface IBluetoothIntentFilter {
        void initIntentFilter(IntentFilter intentFilter);
        void handleIntent(Context context, Intent intent);
    }

    BluetoothIntent(Context context, IBluetoothIntentFilter handler) {
        mContext = Objects.requireNonNull(context);
        mHandler = Objects.requireNonNull(handler);
        registerReceiver();
    }

    private void registerReceiver() {
        mHandler.initIntentFilter(mIntentFilter);
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    public void unregisterReceiver() {
        mContext.unregisterReceiver(mReceiver);
    }
}
