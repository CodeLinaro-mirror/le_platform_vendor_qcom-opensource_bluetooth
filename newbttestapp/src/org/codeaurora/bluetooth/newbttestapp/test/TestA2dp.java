/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class TestA2dp extends TestBase
        implements A2dp.IBluetoothA2dpIntent {

    private static final String TAG = "TestA2dp";

    private final A2dp mA2dp;
    private final MediaPlayer mMediaPlayer;
    private List<ApplicationInfo> mApplicationList = null;

    TestA2dp(Context context) {
        super(context, TAG, Adapter.getNewAdapter());
        mA2dp = new A2dp(context, this);
        mMediaPlayer = new MediaPlayer(context);
    }

    public void connect(BluetoothDevice device) {
        logd("connect device: " + device);
        if (isConnected(device)) {
            // A2DP already connected, return
            outputSuccess("connect");
            return;
        }

        mDevice = Objects.requireNonNull(device);
        if (!mA2dp.connect(device)) {
            outputFail("connect");
            return;
        }
        lock();
        outputResult("connect", isConnected(mA2dp.getConnectionState(device)));
    }

    public void disconnect(BluetoothDevice device) {
        logd("disconnect device: " + device);
        if (isDisconnected(device)) {
            // A2DP already disconnected, return
            outputSuccess("disconnect");
            return;
        }

        mDevice = Objects.requireNonNull(device);
        if (!mA2dp.disconnect(device)) {
            outputFail("disconnect");
            return;
        }
        lock();
        outputResult("disconnect", isDisconnected(mA2dp.getConnectionState(device)));
    }

    public void getMediaPlayer(BluetoothDevice device) {
        logd("getMediaPlayer device: " + device);
        String mediaPlayer = mA2dp.getMediaPlayer(device);
        if (mediaPlayer.isEmpty()) {
            logd("media player is empty");
        } else {
            logd("media player: " + mediaPlayer);
        }
    }

    public void setMediaPlayer(BluetoothDevice device, String mediaPlayer) {
        outputResult("setMediaPlayer", mA2dp.setMediaPlayer(device, mediaPlayer));
    }

    public void clearMediaPlayer(BluetoothDevice device) {
        outputResult("clearMediaPlayer", mA2dp.setMediaPlayer(device, ""));
    }

    public void startMediaPlayer(String mediaPlayer) {
        String mediaPlayerLaunch = mMediaPlayer.getMediaPlayerLaunch(mediaPlayer);
        Intent intent = mContext.getPackageManager().
                getLaunchIntentForPackage(mediaPlayerLaunch);
        if (intent == null) {
            loge("not found media player: " + mediaPlayerLaunch);
            return;
        }
        mContext.startActivity(intent);
        outputResult("startMediaPlayer", true);
    }

    public void dumpMediaPlayerList() {
        logd("dumpMediaPlayerList");
        List<ApplicationInfo> mediaPlayerList = getMediaPlayerList();
        for (ApplicationInfo appInfo : mediaPlayerList) {
            logd("  media player: " + appInfo.packageName +
                    ", app name: " + getAppName(appInfo));
        }
        ApplicationInfo defaultMediaPlayer = getDefaultApplication();
        logd("  default media player: " + defaultMediaPlayer.packageName +
                ", app name: " + getAppName(defaultMediaPlayer));
    }

    @Override
    public void handleActionConnectionStateChanged(BluetoothDevice device, int state) {
        logd("handleActionConnectionStateChanged device: " + device + ", state " +
                state + " (" + BluetoothProfile.getConnectionStateName(state) + ")");
        if (isConnected(state) || isDisconnected(state)) {
            if (isSameDevice(device)) {
                unlock();
            }
        }
    }

    @Override
    public void handleActionPlayingStateChanged(BluetoothDevice device, int state) {
        logd("handleActionAudioStateChanged device: " + device + ", state " + state +
                " (" + (isPlaying(state) ? "playing" : "not playing") + ")");
        if (isSameDevice(device)) {
            unlock();
        }
    }

    private boolean isConnected(BluetoothDevice device) {
        return isConnected(mA2dp.getConnectionState(device));
    }

    private boolean isDisconnected(BluetoothDevice device) {
        return isDisconnected(mA2dp.getConnectionState(device));
    }

    private static boolean isPlaying(int state) {
        return state == BluetoothA2dp.STATE_PLAYING;
    }

    private String getAppName(ApplicationInfo appInfo) {
        return (String) mContext.getPackageManager().getApplicationLabel(appInfo);
    }

    private List<ApplicationInfo> getMediaPlayerList() {
        if (mApplicationList == null) {
            mApplicationList = mMediaPlayer.getApplicationInstalled();
        }
        return mApplicationList;
    }

    private ApplicationInfo getDefaultApplication() {
        return mMediaPlayer.getDefaultApplication(getMediaPlayerList());
    }

    private class MediaPlayer {
        private final String[] MEDIA_PLAYER_KEY_WORD = {
                "media", "player", "radio", "music", "youtube"
        };

        private static final String CAR_MEDIA = "com.android.car.media";
        private final String LOCAL_MEDIA_PLAYER = CAR_MEDIA + ".localmediaplayer";

        private final List<String> APPLICATION_FLITERED = new ArrayList<String>(Arrays.asList(
                /* Car Media Center */
                CAR_MEDIA,
                /* Radio */
                "com.android.car.radio",
                /* Android Open Source Music Player */
                "com.android.music",
                /* MusicFX */
                "com.android.musicfx",
                /* Multi-zone Audio Manager */
                "com.media.multizoneaudiomanager"));

        private final String PROVIDERS_PREFIX = "com.android.providers";

        private Context mContext;

        MediaPlayer(Context context) {
            mContext = context;
        }

        List<ApplicationInfo> getApplicationInstalled() {
            List<ApplicationInfo> mediaPlayers = new ArrayList<ApplicationInfo>();
            List<ApplicationInfo> apps = mContext.getPackageManager().
                    getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo appInfo : apps) {
                String packageName = appInfo.packageName;
                if (filterApplication(packageName)) {
                    // Ignore
                    continue;
                }
                for (String keyword : MEDIA_PLAYER_KEY_WORD) {
                    if (packageName.contains(keyword)) {
                        mediaPlayers.add(appInfo);
                        break;
                    }
                }
            }
            return mediaPlayers;
        }

        private boolean filterApplication(String packageName) {
            return packageName.startsWith(PROVIDERS_PREFIX) ||
                    APPLICATION_FLITERED.contains(packageName);
        }

        ApplicationInfo getDefaultApplication(List<ApplicationInfo> mediaPlayers) {
            for (ApplicationInfo appInfo : mediaPlayers) {
                String packageName = appInfo.packageName;
                if (packageName.equals(LOCAL_MEDIA_PLAYER)) {
                    return appInfo;
                }
            }
            return null;
        }

        String getMediaPlayerLaunch(String mediaPlayer) {
            return mediaPlayer.equals(LOCAL_MEDIA_PLAYER) ?
                    CAR_MEDIA :
                    mediaPlayer;
        }
    }
}
