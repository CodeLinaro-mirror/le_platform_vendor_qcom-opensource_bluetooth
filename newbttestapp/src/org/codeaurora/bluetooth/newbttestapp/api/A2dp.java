/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Objects;

final class A2dp extends BluetoothProfileBase
        implements BluetoothIntent.IBluetoothIntentFilter {

    private static final String TAG = "A2dp";

    private BluetoothA2dp mBluetoothA2dp;
    private BluetoothIntent mBluetoothIntent;
    private IBluetoothA2dpIntent mIntentHandler;

    public interface IBluetoothA2dpIntent {
        void handleActionConnectionStateChanged(BluetoothDevice device, int state);
        void handleActionPlayingStateChanged(BluetoothDevice device, int state);
    }

    A2dp(Context context, IBluetoothA2dpIntent handler) {
        super(context, TAG, Adapter.getNewAdapter());
        mBluetoothIntent = new BluetoothIntent(context, this);
        mIntentHandler = Objects.requireNonNull(handler);
        getProfileProxy(BluetoothProfile.A2DP);
    }

    public boolean connect(BluetoothDevice device) {
        logd("connect device: " + device);
        return mBluetoothA2dp != null && mBluetoothA2dp.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
        return mBluetoothA2dp != null && mBluetoothA2dp.disconnect(device);
    }

    public int getConnectionState(BluetoothDevice device) {
        logd("getConnectionState device: " + device);
        return mBluetoothA2dp != null ?
                mBluetoothA2dp.getConnectionState(device) :
                BluetoothProfile.STATE_DISCONNECTED;
    }

    public String getMediaPlayer(BluetoothDevice device) {
        logd("getMediaPlayer device: " + device);
        return mBluetoothA2dp != null ?
                mBluetoothA2dp.getMediaPlayer(device) :
                "";
    }

    public boolean setMediaPlayer(BluetoothDevice device, String mediaPlayer) {
        logd("setMediaPlayer device: " + device + ", media player: " + mediaPlayer);
        return mBluetoothA2dp != null ?
                mBluetoothA2dp.setMediaPlayer(device, mediaPlayer) :
                false;
    }

    @Override
    protected void handleServiceConnected(int profile, BluetoothProfile proxy) {
        if (profile == BluetoothProfile.A2DP) {
            mBluetoothA2dp = (BluetoothA2dp) proxy;
        }
    }

    @Override
    protected void handleServiceDisconnected(int profile) {
        if (profile == BluetoothProfile.A2DP) {
            mBluetoothA2dp = null;
        }
    }

    @Override
    public void initIntentFilter(IntentFilter intentFilter) {
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        intentFilter.addAction(BluetoothA2dp.ACTION_AVRCP_CONNECTION_STATE_CHANGED);
    }

    @Override
    public void handleIntent(Context context, Intent intent) {
        String action = intent.getAction();
        logd("handleIntent action: " + action);
        if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            logd("BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                    BluetoothProfile.STATE_DISCONNECTED);
            mIntentHandler.handleActionConnectionStateChanged(device, state);

        } else if (BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED.equals(action)) {
            logd("BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                    BluetoothA2dp.STATE_NOT_PLAYING);
            mIntentHandler.handleActionPlayingStateChanged(device, state);

        } else {
           loge("unknown action: " + action);
        }
    }

    public static boolean isPlaying(int state) {
        return state == BluetoothA2dp.STATE_PLAYING;
    }
}
