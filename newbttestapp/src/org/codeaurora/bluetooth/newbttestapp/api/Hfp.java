/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Objects;

final class Hfp extends BluetoothProfileBase
        implements BluetoothIntent.IBluetoothIntentFilter {

    private static final String TAG = "Hfp";

    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothIntent mBluetoothIntent;
    private IBluetoothHeadsetIntent mIntentHandler;

    public interface IBluetoothHeadsetIntent {
        void handleActionConnectionStateChanged(BluetoothDevice device, int state);
        void handleActionAudioStateChanged(BluetoothDevice device, int state);
        void handleActionActiveDeviceChanged(BluetoothDevice device);
    }

    Hfp(Context context, IBluetoothHeadsetIntent handler) {
        super(context, TAG, Adapter.getNewAdapter());
        mBluetoothIntent = new BluetoothIntent(context, this);
        mIntentHandler = Objects.requireNonNull(handler);
        getProfileProxy(BluetoothProfile.HEADSET);
    }

    public boolean connect(BluetoothDevice device) {
        logd("connect device: " + device);
        return mBluetoothHeadset != null && mBluetoothHeadset.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
        return mBluetoothHeadset != null && mBluetoothHeadset.disconnect(device);
    }

    public int getConnectionState(BluetoothDevice device) {
        logd("getConnectionState device: " + device);
        return mBluetoothHeadset != null ?
                mBluetoothHeadset.getConnectionState(device) :
                BluetoothProfile.STATE_DISCONNECTED;
    }

    public int getAudioState(BluetoothDevice device) {
        logd("getAudioState device: " + device);
        return mBluetoothHeadset != null ?
                mBluetoothHeadset.getAudioState(device) :
                BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
    }

    public boolean connectAudio(BluetoothDevice device) {
        logd("connectAudio device: " + device);
        return mBluetoothHeadset != null && mBluetoothHeadset.connectAudio();
    }

    public boolean disconnectAudio(BluetoothDevice device) {
        logd("disconnectAudio device: " + device);
        return mBluetoothHeadset != null && mBluetoothHeadset.disconnectAudio();
    }

    public boolean setActiveDevice(BluetoothDevice device) {
        logd("setActiveDevice device: " + device);
        return mBluetoothHeadset != null && mBluetoothHeadset.setActiveDevice(device);
    }

    @Override
    protected void handleServiceConnected(int profile, BluetoothProfile proxy) {
        if (profile == BluetoothProfile.HEADSET) {
            mBluetoothHeadset = (BluetoothHeadset) proxy;
        }
    }

    @Override
    protected void handleServiceDisconnected(int profile) {
        if (profile == BluetoothProfile.HEADSET) {
            mBluetoothHeadset = null;
        }
    }

    @Override
    public void initIntentFilter(IntentFilter intentFilter) {
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_ACTIVE_DEVICE_CHANGED);
    }

    @Override
    public void handleIntent(Context context, Intent intent) {
        String action = intent.getAction();
        logd("handleIntent action: " + action);
        if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            logd("BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                    BluetoothProfile.STATE_DISCONNECTED);
            mIntentHandler.handleActionConnectionStateChanged(device, state);

        } else if (BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED.equals(action)) {
            logd("BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                    BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
            mIntentHandler.handleActionAudioStateChanged(device, state);

        } else if (BluetoothHeadset.ACTION_ACTIVE_DEVICE_CHANGED.equals(action)) {
            logd("BluetoothHeadset.ACTION_ACTIVE_DEVICE_CHANGED");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            mIntentHandler.handleActionActiveDeviceChanged(device);

        } else {
           loge("unknown action: " + action);
        }
    }

    public static boolean isAudioConnected(int state) {
        return state == BluetoothHeadset.STATE_AUDIO_CONNECTED;
    }

    public static boolean isAudioDisconnected(int state) {
        return state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
    }
}
