/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

/**
 * Bluetooth adapter main
 */
final class SppMain {

    private static final String TAG = "SppMain";

    private Context mContext;
    private Spp mSpp;
    private Spp mSpp1;

    SppMain(Context context) {
        mContext = context;
        mSpp = new Spp(context);
        init();
    }

    private void init() {
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }

    private void loge(String msg) {
        Log.e(TAG, msg);
    }
}
