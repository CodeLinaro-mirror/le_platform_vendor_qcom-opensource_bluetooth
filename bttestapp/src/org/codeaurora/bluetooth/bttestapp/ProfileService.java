/*
 * Copyright (c) 2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
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
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.SdpMasRecord;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothAudioConfig;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.os.Bundle;
import android.os.SystemProperties;

import com.android.vcard.VCardEntry;
// import android.bluetooth.client.map.BluetoothMapBmessage;
// import android.bluetooth.client.map.BluetoothMapEventReport;
// import android.bluetooth.client.map.BluetoothMapMessage;
// import android.bluetooth.client.map.BluetoothMasClient;
import org.codeaurora.bluetooth.bttestapp.R;
// import org.codeaurora.bluetooth.bttestapp.services.IMapServiceCallback;

import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.MediaDescription;
import android.media.session.MediaController;
import android.media.session.MediaController.TransportControls;
import android.media.session.MediaSession;
import android.media.session.MediaSession.QueueItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.lang.String;

import org.codeaurora.bluetooth.bttestapp.util.Logger;

public class ProfileService extends Service {

    private final static String TAG = "ProfileService";

    private static final int PBAP_AUTH_NOTIFICATION_ID = 10000;

    public static final String ACTION_HFP_CONNECTION_STATE = "org.codeaurora.bluetooth.action.HFP_CONNECTION_STATE";

    public static final String ACTION_AVRCP_CONNECTION_STATE = "org.codeaurora.bluetooth.action.AVRCP_CONNECTION_STATE";

    public static final String ACTION_PBAP_CONNECTION_STATE = "org.codeaurora.bluetooth.action.PBAP_CONNECTION_STATE";

    public static final String ACTION_MAP_CONNECTION_STATE = "org.codeaurora.bluetooth.action.MAP_CONNECTION_STATE";

    public static final String ACTION_MAP_NOTIFICATION_STATE = "org.codeaurora.bluetooth.action.MAP_NOTIFICATION_STATE";

    public static final String EXTRA_CONNECTED = "org.codeaurora.bluetooth.extra.CONNECTED";

    public static final String EXTRA_NOTIFICATION_STATE = "org.codeaurora.bluetooth.extra.NOTIFICATION_STATE";

    public static final String PBAP_AUTH_ACTION_REQUEST = "org.codeaurora.bluetooth.PBAP_AUTH_ACTION_REQUEST";

    public static final String PBAP_AUTH_ACTION_CANCEL = "org.codeaurora.bluetooth.PBAP_AUTH_ACTION_CANCEL";

    public static final String PBAP_AUTH_ACTION_RESPONSE = "org.codeaurora.bluetooth.PBAP_AUTH_ACTION_RESPONSE";

    public static final String PBAP_AUTH_ACTION_TIMEOUT = "org.codeaurora.bluetooth.PBAP_AUTH_ACTION_TIMEOUT";

    public static final String PBAP_AUTH_EXTRA_KEY = "org.codeaurora.bluetooth.PBAP_AUTH_EXTRA_KEY";

    public static final String ACTION_MAP_GET_MESSAGE = "org.codeaurora.bluetooth.action.MAP_GET_MESSAGE";

    public static final String EXTRA_MAP_INSTANCE_ID = "org.codeaurora.bluetooth.extra.MAP_INSTANCE_ID";

    public static final String EXTRA_MAP_MESSAGE_HANDLE = "org.codeaurora.bluetooth.extra.MAP_MESSAGE_HANDLE";


    // +++ Custom action definition

    /**
      * Broadcast Action: Indicates custom action(request) from application.
      *
      * <p>Always contains the extra field {@link #EXTRA_CUSTOM_ACTION}.
      *
      * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
      */
    public static final String ACTION_CUSTOM_ACTION =
        "android.bluetooth.adapter.action.CUSTOM_ACTION";

    public static final String EXTRA_CUSTOM_ACTION =
        "android.bluetooth.adapter.extra.CUSTOM_ACTION";

    public static final String KEY_COMMAND = "command";

    /**
      * Custom action to get link key
      *
      * @param Bundle wrapped with
      *  {@link #KEY_COMMAND}
      *  {@link #BluetoothDevice.EXTRA_DEVICE}
      */

    public static final String CUSTOM_ACTION_GET_LINK_KEY = "android.bluetooth.adapter.CUSTOM_ACTION_GET_LINK_KEY";

    /**
      * Custom action to add oob bond device
      *
      * @param Bundle wrapped with
      *  {@link #KEY_COMMAND}
      *  {@link #BluetoothDevice.EXTRA_DEVICE}
      */

    public static final String CUSTOM_ACTION_ADD_OOB_BOND_DEV =
        "android.bluetooth.adapter.CUSTOM_ACTION_ADD_OOB_BOND_DEV";

    public static final String KEY_LINK_KEY = "link_key";
    public static final String KEY_LINK_KEY_TYPE = "link_key_type";
    public static final String KEY_PIN_LEN = "pin_len";

    // + Response for custom action

    /**
     * Intent used to broadcast custom action result
     *
     * <p>This intent will have 2 extras at least:
     * <ul>
     *   <li> {@link #EXTRA_CUSTOM_ACTION} - custom action command. </li>
     *
     *   <li> {@link #EXTRA_CUSTOM_ACTION_RESULT} - custom action result. </li>
     *
     * </ul>
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission to
     * receive.
     */

    public static final String ACTION_CUSTOM_ACTION_RESULT =
        "android.bluetooth.adapter.action.CUSTOM_ACTION_RESULT";

    public static final String EXTRA_CUSTOM_ACTION_RESULT =
        "android.bluetooth.adapter.extra.CUSTOM_ACTION_RESULT";

    public static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;
    public static final String PTS_IOPT_PROPERTY = "vendor.qcom.bluetooth.pts.iopt";

    private BluetoothDevice mDevice = null;

    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    private AvrcpProfile mAvrcp;
    private HfpProfile mHfp;
    private PbapProfile mPbap;
    private MapProfile mMap;

    private boolean mIsBound = false;

    private final IBinder mBinder = new LocalBinder();

    private Context mContext;
    private static volatile BluetoothServerSocket mListenSocket = null;

    public class LocalBinder extends Binder {
        ProfileService getService() {
            return ProfileService.this;
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Logger.d(TAG, "received " + action);

            if (BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                Intent new_intent = new Intent(ACTION_HFP_CONNECTION_STATE);

                if (state == BluetoothProfile.STATE_CONNECTED) {
                    new_intent.putExtra(EXTRA_CONNECTED, true);
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    new_intent.putExtra(EXTRA_CONNECTED, false);
                } else {
                    return;
                }

                ProfileService.this.sendBroadcast(new_intent);
            } else if(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice == null || device == null) {
                    Logger.e(TAG, "Unexpected error!");
                    return;
                }
                Logger.d(TAG,"AVRCP connection state changed for: "+ device);
                Logger.d(TAG, "mDevice: " + mDevice.getAddress());
                if (mDevice.equals(device)) {
                    Intent new_intent = new Intent(ACTION_AVRCP_CONNECTION_STATE);
                    Logger.d(TAG, "state: " + state);
                    if (state == 1) {
                        new_intent.putExtra(EXTRA_CONNECTED, true);
                    } else {
                        new_intent.putExtra(EXTRA_CONNECTED, false);
                    }
                    ProfileService.this.sendBroadcast(new_intent);
                } else {
                    Logger.d(TAG,"AVRCP connection state change not updated");
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (dev.equals(mDevice)) {
                    checkAndStop(false, true);
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    checkAndStop(false, true);
                }
            }
        }
    };

    private void checkAndStop(boolean unbind, boolean disconnect) {
        boolean canStop = true;

        Logger.v(TAG, "checkAndStop(): unbind=" + unbind + " disconnect=" + disconnect);

        if (unbind) {
            if (mHfp != null &&
                mHfp.getHeadsetClient().getConnectionState(mDevice) != BluetoothProfile.STATE_DISCONNECTED) {
                canStop = false;
            }

            if (!canStop) {
                Logger.v(TAG, "clients are still connected, won't stop");
            }
        }

        if (disconnect && mIsBound) {
            canStop = false;
            Logger.v(TAG, "service is still bound, won't stop");
        }

        if (canStop) {
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        mIsBound = true;

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mIsBound = false;

        checkAndStop(true, false);
        return false;
    }

    @Override
    public void onCreate() {
        Logger.v(TAG, "onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(PBAP_AUTH_ACTION_RESPONSE);
        filter.addAction(PBAP_AUTH_ACTION_CANCEL);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        mContext = getApplicationContext();

        mAvrcp = new AvrcpProfile(mContext);
        mHfp = new HfpProfile(mContext);
        mPbap = new PbapProfile(mContext);
        mMap = new MapProfile(mContext);
        enablePts(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.v(TAG, "onStartCommand intent=" + intent + " flags=" + Integer.toHexString(flags)
                + " startId=" + startId);

        createPceService();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.v(TAG, "onDestroy");

        unregisterReceiver(mReceiver);
    }

    public void setDevice(BluetoothDevice device) {
        if (mDevice != null && mDevice.equals(device)) {
            return;
        }

        if (mHfp != null) {
            BluetoothHeadsetClient headsetClient = mHfp.getHeadsetClient();
            if (headsetClient != null) {
                headsetClient.disconnect(mDevice);
            }
        }

        mDevice = device;

        if (mDevice != null) {
            Logger.d(TAG,
                    "Current device: address=" + mDevice.getAddress() + " name="
                            + mDevice.getName());
        } else {
            Logger.e(TAG, "Current device: none");
        }
    }

    public AvrcpProfile getAvrcpProfile() {
        return mAvrcp;
    }

    public HfpProfile getHfpProfile() {
        return mHfp;
    }
    public PbapProfile getPbapProfile() {
        return mPbap;
    }

    public MapProfile getMapProfile() {
        return mMap;
    }

    public void createPceService() {
        if (/*SystemProperties.getBoolean(PTS_IOPT_PROPERTY, false)*/false) {
            Logger.d(TAG, "connectSocket: UUID: " + BluetoothUuid.PBAP_PCE.getUuid());
            try {
                mListenSocket = mAdapter.listenUsingRfcommWithServiceRecord(
                    "Phonebook Access - PCE",
                    BluetoothUuid.PBAP_PCE.getUuid());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Logger.d(TAG, "bt.pts.iopt is not set");
        }
    }


    public void addOutOfBandBondDevice(BluetoothDevice device, String linkKey, int linkKeyType, int pinLen) {
        Logger.v(TAG, "Current device: address = " + device.getAddress() + " linkKey = "
              + linkKey + " linkKeyType = " + linkKeyType + " pinLen = " + pinLen);

        Bundle extras = createBundle(CUSTOM_ACTION_ADD_OOB_BOND_DEV, device);
        extras.putString(KEY_LINK_KEY, linkKey);
        extras.putInt(KEY_LINK_KEY_TYPE, linkKeyType);
        extras.putInt(KEY_PIN_LEN, pinLen);
        sendCustomAction(extras);
    }

    public void getLinkKey(BluetoothDevice device) {
        Logger.v(TAG, "Current device: address= " + device.getAddress());

        Bundle extras = createBundle(CUSTOM_ACTION_GET_LINK_KEY, device);
        sendCustomAction(extras);
    }

    public void enablePts(boolean enable) {
        //SystemProperties.set(PTS_IOPT_PROPERTY, String.valueOf(enable));
    }

    private Bundle createBundle(String cmd, BluetoothDevice device) {
        Bundle extras = new Bundle();
        extras.putString(KEY_COMMAND, cmd);
        extras.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
        return extras;
    }
    private void sendCustomAction(Bundle extras) {
        Intent intent = new Intent(ACTION_CUSTOM_ACTION);
        intent.putExtra(EXTRA_CUSTOM_ACTION, extras);
        mContext.sendBroadcast(intent, BLUETOOTH_PERM);
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }
}
