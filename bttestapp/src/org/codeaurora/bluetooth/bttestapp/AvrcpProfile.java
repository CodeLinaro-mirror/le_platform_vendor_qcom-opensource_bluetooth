/*
 * Copyright (c) 2013-2016, 2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *           * Redistributions of source code must retain the above copyright
 *             notice, this list of conditions and the following disclaimer.
 *           * Redistributions in binary form must reproduce the above
 *           * copyright notice, this list of conditions and the following
 *             disclaimer in the documentation and/or other materials provided
 *             with the distribution.
 *           * Neither the name of The Linux Foundation nor the names of its
 *             contributors may be used to endorse or promote products derived
 *             from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codeaurora.bluetooth.bttestapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothAudioConfig;
import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;

import org.codeaurora.bluetooth.bttestapp.R;

import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.MediaDescription;
import android.media.session.MediaController;
import android.media.session.MediaController.TransportControls;
import android.media.session.MediaSession;
import android.media.session.MediaSession.QueueItem;
import android.media.MediaMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class AvrcpProfile {

    private final static String TAG = "AvrcpProfile";

    public static final String BLUETOOTH_PACKAGE = "com.android.bluetooth";

    public static final String A2DP_MEDIA_BROWSER_SERVICE =
        "com.android.bluetooth.a2dpsink.mbs.A2dpMediaBrowserService";

    public static final String ACTION_TRACK_EVENT =
        "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT";

    public static final String EXTRA_PLAYBACK =
        "android.bluetooth.avrcp-controller.profile.extra.PLAYBACK";

    public static final String EXTRA_METADATA =
        "android.bluetooth.avrcp-controller.profile.extra.METADATA";

    /**
     * Intent used to broadcast the change of folder list.
     *
     * <p>This intent will have the one extra:
     * <ul>
     *    <li> {@link #EXTRA_FOLDER_LIST} - array of {@link MediaBrowser#MediaItem}
     *    containing the folder listing of currently selected folder.
     * </ul>
     */
    public static final String ACTION_FOLDER_LIST =
        "android.bluetooth.avrcp-controller.profile.action.FOLDER_LIST";

    public static final String EXTRA_FOLDER_LIST =
        "android.bluetooth.avrcp-controller.profile.extra.FOLDER_LIST";

    public static final String EXTRA_FOLDER_ID =
        "com.android.bluetooth.avrcp.EXTRA_FOLDER_ID";

    // [TODO] Unify EXTRA_CODEC_TYPE into BluetoothA2dpSink
    /**
     * Extra for the {@link #ACTION_AUDIO_CONFIG_CHANGED} intent.
     *
     * This extra represents the current codec type of the A2DP source device.
     */
    public static final String EXTRA_CODEC_TYPE =
        "android.bluetooth.a2dp-sink.profile.extra.CODEC_TYPE";

    public static int UNKNOWN_CODEC_TYPE = -1;

    // AVRCP feature
    public static final int BTRC_FEAT_NONE = 0x00;
    public static final int BTRC_FEAT_METADATA = 0x01;
    public static final int BTRC_FEAT_ABSOLUTE_VOLUME = 0x02;
    public static final int BTRC_FEAT_BROWSE = 0x04;
    public static final int BTRC_FEAT_COVER_ART = 0x08;

    // Custom actions for PTS testing.
    private String CUSTOM_ACTION_VOL_UP = "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_VOL_UP";
    private String CUSTOM_ACTION_VOL_DN = "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_VOL_DN";
    private String CUSTOM_ACTION_GET_PLAY_STATUS_NATIVE =
        "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_GET_PLAY_STATUS_NATIVE";

    // [TODO] Move the common defintion for customer action into framework
    // +++ Custom action definition for AVRCP controller

    /**
     * Custom action to send pass through command (with key state).
     *
     * <p>This is called in {@link MediaController.TransportControls.sendCustomAction}
     *
     * <p>This is an asynchronous call: it will return immediately.
     *
     * @param Bundle wrapped with {@link #KEY_CMD}, {@link #KEY_STATE}
     *
     * @return void
     *
     * @See {@link android.media.session.MediaController}
     */
    public static final String CUSTOM_ACTION_SEND_PASS_THRU_CMD =
        "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_SEND_PASS_THRU_CMD";
    public static final String KEY_CMD = "cmd";
    public static final String KEY_STATE = "state";

    /**
     * Custom action to search.
     *
     * <p>This is called in {@link MediaController.TransportControls.sendCustomAction}
     *
     * <p>This is an asynchronous call: it will return immediately.
     *
     * <p>Intent {@link #ACTION_CUSTOM_ACTION_RESULT} will be broadcast to notify the result.
     * {@link AvrcpControllerService} will also receive search result.
     * Application can find search list when to browse AVRCP folder.
     *
     * @param Bundle wrapped with {@link #KEY_SEARCH}
     *
     * @return void
     *
     * @See {@link android.media.session.MediaController}
     *      {@link com.android.bluetooth.avrcpcontroller.AvrcpControllerService}
     */
    public static final String CUSTOM_ACTION_SEARCH =
        "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_SEARCH";
    public static final String KEY_SEARCH = "search";

    /**
     * Custom action to add item into NowPlaying.
     *
     * <p>This is called in {@link MediaController.TransportControls.sendCustomAction}
     *
     * <p>This is an asynchronous call: it will return immediately.
     *
     * <p>Intent {@link #ACTION_CUSTOM_ACTION_RESULT} will be broadcast to notify the result.
     * {@link AvrcpControllerService} will update NowPlaying list if succeed.
     *
     * @param Bundle wrapped with {@link #MediaMetadata.METADATA_KEY_MEDIA_ID}
     *
     * @return void
     *
     * @See {@link android.media.session.MediaController}
     *      {@link com.android.bluetooth.avrcpcontroller.AvrcpControllerService}
     */
    public static final String CUSTOM_ACTION_ADD_TO_NOW_PLAYING =
        "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_ADD_TO_NOW_PLAYING";

    /**
     * Custom action to get item attributes.
     *
     * <p>This is called in {@link MediaController.TransportControls.sendCustomAction}
     *
     * <p>This is an asynchronous call: it will return immediately.
     *
     * <p>Intent {@link AvrcpControllerService.ACTION_TRACK_EVENT} will be broadcast.
     * to notify the item attributes retrieved.
     *
     * @param Bundle wrapped with {@link MediaMetadata.METADATA_KEY_MEDIA_ID}
     *
     * @return void
     *
     * @See {@link android.media.session.MediaController}
     *      {@link android.media.MediaMetadata}
     *      {@link com.android.bluetooth.avrcpcontroller.AvrcpControllerService}
     */
    public static final String CUSTOM_ACTION_GET_ITEM_ATTR =
        "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_GET_ITEM_ATTR";

    /**
     * Custom action to get total number of items.
     *
     * <p>This is called in {@link MediaController.TransportControls.sendCustomAction}
     *
     * <p>This is an asynchronous call: it will return immediately.
     *
     * <p>Intent {@link #ACTION_CUSTOM_ACTION_RESULT} will be broadcast to notify the result.
     *
     * @param Bundle wrapped with {@link #KEY_BROWSE_SCOPE}
     *
     * @return void
     *
     * @See {@link android.media.session.MediaController}
     */
    public static final String CUSTOM_ACTION_GET_TOTAL_NUM_OF_ITEMS =
        "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_GET_TOTAL_NUM_OF_ITEMS";
    public static final String KEY_BROWSE_SCOPE = "scope";

    // + Response for custom action

    /**
     * Intent used to broadcast A2DP/AVRCP custom action result
     *
     * <p>This intent will have 2 extras at least:
     * <ul>
     *   <li> {@link #EXTRA_CUSTOM_ACTION} - custom action command. </li>
     *
     *   <li> {@link #EXTRA_CUSTOM_ACTION_RESULT} - custom action result. </li>
     *
     *   <li> {@link #EXTRA_NUM_OF_ITEMS} - Number of items.
     *         Valid for {@link #CUSTOM_ACTION_SEARCH},
     *         {@link #CUSTOM_ACTION_GET_TOTAL_NUM_OF_ITEMS} </li>
     *
     * </ul>
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission to
     * receive.
     */
    public static final String ACTION_CUSTOM_ACTION_RESULT =
        "com.android.bluetooth.a2dpsink.mbs.action.CUSTOM_ACTION_RESULT";

    public static final String EXTRA_CUSTOM_ACTION =
        "com.android.bluetooth.a2dpsink.mbs.extra.CUSTOM_ACTION";

    public static final String EXTRA_CUSTOM_ACTION_RESULT =
        "com.android.bluetooth.a2dpsink.mbs.extra.CUSTOM_ACTION_RESULT";

    public static final String EXTRA_NUM_OF_ITEMS =
        "com.android.bluetooth.a2dpsink.mbs.extra.NUM_OF_ITEMS";

    // Result code
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_ERROR = 1;
    public static final int RESULT_INVALID_PARAMETER = 2;
    public static final int RESULT_NOT_SUPPORTED = 3;
    public static final int RESULT_TIMEOUT = 4;

    // - Response for custom action

    // --- Custom action definition for AVRCP controller

    public static final int PASS_THRU_CMD_ID_FF = 0x49;
    public static final int PASS_THRU_CMD_ID_REWIND = 0x48;

    public static final int KEY_STATE_PRESSED = 0;
    public static final int KEY_STATE_RELEASED = 1;

    /* Folder/Media Item scopes.
     * Keep in sync with AVRCP 1.6 sec. 6.10.1
     */
    public static final int BROWSE_SCOPE_PLAYER_LIST = 0x00;
    public static final int BROWSE_SCOPE_VFS = 0x01;
    public static final int BROWSE_SCOPE_SEARCH = 0x02;
    public static final int BROWSE_SCOPE_NOW_PLAYING = 0x03;

    public static final String ROOT = "__ROOT__";
    public static final String NOW_PLAYING_PREFIX = "NOW_PLAYING";
    public static final String PLAYER_PREFIX = "PLAYER";
    public static final String SEARCH_PREFIX = "SEARCH";

    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    private BluetoothDevice mDevice = null;

    private BluetoothAvrcpController mAvrcpController = null;
    private BluetoothA2dpSink mA2dpSink = null;

    /* Object used to connect to MediaBrowseService of BT-AVRCP app */
    private MediaBrowser mMediaBrowser = null;
    private MediaController mMediaController = null;
    private List<MediaBrowser.MediaItem> mNowPlayingItems = new ArrayList<>();
    private List<MediaBrowser.MediaItem> mSearchItems = new ArrayList<>();

    private Context mContext;

    private final ServiceListener mAvrcpControllerServiceListener = new ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.AVRCP_CONTROLLER) {
                mAvrcpController = (BluetoothAvrcpController) proxy;
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.AVRCP_CONTROLLER) {
                mAvrcpController = null;
            }
        }
    };

    private final ServiceListener mA2dpSinkServiceListener = new ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP_SINK) {
                mA2dpSink = (BluetoothA2dpSink) proxy;
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP_SINK) {
                mA2dpSink = null;
            }
        }
    };

    /* Browse connection state callback handler */
    private MediaBrowser.ConnectionCallback mBrowseMediaConnectionCallback =
            new MediaBrowser.ConnectionCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "mediaBrowser CONNECTED");
            mMediaController = new MediaController(mContext, mMediaBrowser.getSessionToken());
        }

        @Override
        public void onConnectionFailed() {
            Log.e(TAG, "mediaBrowser Connection failed");
        }

        @Override
        public void onConnectionSuspended() {
            Log.e(TAG, "mediaBrowser SUSPENDED");
        }
    };

    public AvrcpProfile(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        Log.d(TAG, "init");

        mAdapter.getProfileProxy(mContext, mAvrcpControllerServiceListener,
                BluetoothProfile.AVRCP_CONTROLLER);

        mAdapter.getProfileProxy(mContext, mA2dpSinkServiceListener,
                BluetoothProfile.A2DP_SINK);

        mMediaBrowser = new MediaBrowser(mContext, new ComponentName(BLUETOOTH_PACKAGE,
                                         A2DP_MEDIA_BROWSER_SERVICE), mBrowseMediaConnectionCallback, null);

        mMediaBrowser.connect();
    }

    public void play() {
        Log.d(TAG, "play");
        if (mMediaController != null) {
            Log.d(TAG, "calling play()");
            mMediaController.getTransportControls().play();
        }
    }

    public void pause() {
        Log.d(TAG, "pause");
        if (mMediaController != null) {
            Log.d(TAG, "calling pause()");
            mMediaController.getTransportControls().pause();
        }
    }

    public void stop() {
        Log.d(TAG, "stop");
        if (mMediaController != null) {
            Log.d(TAG, "calling stop()");
            mMediaController.getTransportControls().stop();
        }
    }

    public void volumeUp() {
        Log.d(TAG, "volumeUp");
        Bundle extras = new Bundle();
        sendCustomAction(CUSTOM_ACTION_VOL_UP, extras);
    }

    public void volumeDown() {
        Log.d(TAG, "volumeDown");
        Bundle extras = new Bundle();
        sendCustomAction(CUSTOM_ACTION_VOL_DN, extras);
    }

    public void getPlayStatus() {
        Log.d(TAG, "getPlayStatus");
        Bundle extras = new Bundle();
        sendCustomAction(CUSTOM_ACTION_GET_PLAY_STATUS_NATIVE, extras);
    }

    public void sendPassThruCmd(int cmd, boolean pressed) {
        Log.d(TAG, "sendPassThruCmd, cmd: " + cmd + ", pressed: " + pressed);
        Bundle extras = new Bundle();
        extras.putInt(KEY_CMD, cmd);
        extras.putInt(KEY_STATE, pressed ? KEY_STATE_PRESSED : KEY_STATE_RELEASED);
        sendCustomAction(CUSTOM_ACTION_SEND_PASS_THRU_CMD, extras);
    }

    public void fastForward(boolean pressed) {
        Log.d(TAG, "fastForward, pressed: " + pressed);
        sendPassThruCmd(PASS_THRU_CMD_ID_FF, pressed);
    }

    public void rewind(boolean pressed) {
        Log.d(TAG, "rewind, pressed: " + pressed);
        sendPassThruCmd(PASS_THRU_CMD_ID_REWIND, pressed);
    }

    public void search(String query) {
        Log.d(TAG, "search " + query);
        Bundle extras = new Bundle();
        extras.putString(KEY_SEARCH, query);
        sendCustomAction(CUSTOM_ACTION_SEARCH, extras);
    }

    public void addToNowPlaying(int scope, String mediaId) {
        Log.d(TAG, "addToNowPlaying scope: " + scope + ", mediaId: " + mediaId);
        Bundle extras = new Bundle();
        extras.putInt(KEY_BROWSE_SCOPE, scope);
        extras.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId);
        sendCustomAction(CUSTOM_ACTION_ADD_TO_NOW_PLAYING, extras);
    }

    public void getItemAttributes(int scope, String mediaId) {
        Log.d(TAG, "getItemAttributes scope: " + scope + ", mediaId: " + mediaId);
        Bundle extras = new Bundle();
        extras.putInt(KEY_BROWSE_SCOPE, scope);
        extras.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId);
        sendCustomAction(CUSTOM_ACTION_GET_ITEM_ATTR, extras);
    }

    public void getTotalNumberOfItems(int scope) {
        Log.d(TAG, "getTotalNumberOfItems, scope: " + scope);
        Bundle extras = new Bundle();
        extras.putInt(KEY_BROWSE_SCOPE, scope);
        sendCustomAction(CUSTOM_ACTION_GET_TOTAL_NUM_OF_ITEMS, extras);
    }

    private void sendCustomAction(String action, Bundle extras) {
        if (mMediaController != null) {
            Log.d(TAG, "sendCustomAction, action: " + action + ", extras: " + extras);
            mMediaController.getTransportControls().sendCustomAction(action, extras);
        }
    }

    public BluetoothAudioConfig getAudioConfig(BluetoothDevice device) {
        Log.d(TAG, "getAudioConfig, device: " + device);
        if (mA2dpSink != null) {
            return mA2dpSink.getAudioConfig(device);
        } else {
            Log.e(TAG, "A2dpSink service null");
            return null;
        }
    }

    public int getSupportedFeatures(BluetoothDevice device) {
        Log.d(TAG, "getSupportedFeatures, device: " + device);
        // TODO
        return 0;
    }

    public static boolean isRoot(String parentId) {
        return parentId.startsWith(ROOT);
    }

    public static boolean isPlayer(String parentId) {
        return parentId.startsWith(PLAYER_PREFIX);
    }

    public static boolean isNowPlaying(String parentId) {
        return parentId.startsWith(NOW_PLAYING_PREFIX);
    }

    public static boolean isSearch(String parentId) {
        return parentId.startsWith(SEARCH_PREFIX);
    }

    private List<MediaBrowser.MediaItem> getItemList(String parentId) {
        if (isNowPlaying(parentId)) {
            return mNowPlayingItems;
        } else if (isSearch(parentId)) {
            return mSearchItems;
        } else {
            return null;
        }
    }

    public BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice device) {
        if (mAvrcpController != null) {
            return mAvrcpController.getPlayerSettings(device);
        } else {
            Log.e(TAG, "mAvrcpController null");
            return null;
        }
    }

    public boolean setPlayerApplicationSetting(BluetoothAvrcpPlayerSettings plAppSetting) {
        if (mAvrcpController != null) {
            return mAvrcpController.setPlayerApplicationSetting(plAppSetting);
        } else {
            Log.e(TAG, "mAvrcpController null");
            return false;
        }
    }

    public static String getCustomActionCmd(String cmd) {
        if (cmd == null) {
            return null;
        }

        if (cmd.equals(CUSTOM_ACTION_SEND_PASS_THRU_CMD)) {
            return "SendPassThruCmd";
        } else if (cmd.equals(CUSTOM_ACTION_SEARCH)) {
            return "Search";
        } else if (cmd.equals(CUSTOM_ACTION_ADD_TO_NOW_PLAYING)) {
            return "AddToNowPlaying";
        } else if (cmd.equals(CUSTOM_ACTION_GET_ITEM_ATTR)) {
           return "GetItemAttributes";
        } else if (cmd.equals(CUSTOM_ACTION_GET_TOTAL_NUM_OF_ITEMS)) {
           return "GetTotalNumberOfItems";
        } else {
            return "Unknown";
        }
    }

    public static String getCustomActionResult(int result) {
        switch (result) {
            case RESULT_SUCCESS:
                return "success";
            case RESULT_INVALID_PARAMETER:
                return "invalid parameter";
            case RESULT_NOT_SUPPORTED:
                return "not supported";
            case RESULT_TIMEOUT:
                return "timeout";
            default:
                return "error";
        }
    }
}
