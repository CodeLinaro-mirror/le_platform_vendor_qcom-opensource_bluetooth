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
import android.bluetooth.BluetoothPbapClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;

import org.codeaurora.bluetooth.bttestapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import org.codeaurora.bluetooth.bttestapp.util.Logger;

public class PbapProfile {

    private final static String TAG = "PbapProfile";

    public static final String PBAP_CLIENT_ENABLE_PTS_PROPERTY =
        "bt.pbapclient.enable_pts";

    public static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;

    public static final String ROOT_PATH = "/root";
    public static final String PB_PATH = "telecom/pb";
    public static final String MCH_PATH = "telecom/mch";
    public static final String ICH_PATH = "telecom/ich";
    public static final String OCH_PATH = "telecom/och";
    public static final String CCH_PATH = "telecom/cch";

    public static final String SIM_PB_PATH = "SIM1/telecom/pb";
    public static final String SIM_MCH_PATH = "SIM1/telecom/mch";
    public static final String SIM_ICH_PATH = "SIM1/telecom/ich";
    public static final String SIM_OCH_PATH = "SIM1/telecom/och";
    public static final String SIM_CCH_PATH = "SIM1/telecom/cch";

    // vCard type
    public static final byte VCARD_TYPE_21 = 0;
    public static final byte VCARD_TYPE_30 = 1;

    public static final long PBAP_FILTER_VERSION = 1 << 0;
    public static final long PBAP_FILTER_FN = 1 << 1;
    public static final long PBAP_FILTER_N = 1 << 2;
    public static final long PBAP_FILTER_PHOTO = 1 << 3;
    public static final long PBAP_FILTER_ADR = 1 << 5;
    public static final long PBAP_FILTER_TEL = 1 << 7;
    public static final long PBAP_FILTER_EMAIL = 1 << 8;
    public static final long PBAP_FILTER_NICKNAME = 1 << 23;

    public static final long PBAP_REQUESTED_FIELDS = PBAP_FILTER_VERSION | PBAP_FILTER_FN
            | PBAP_FILTER_N | PBAP_FILTER_PHOTO | PBAP_FILTER_ADR | PBAP_FILTER_TEL
            | PBAP_FILTER_EMAIL | PBAP_FILTER_NICKNAME;

    /* PBAP listing order types */
    public static final byte ORDER_INDEXED = 0x00;  // increasing order of vCard handles
    public static final byte ORDER_ALPHABETICAL = 0x01; // alphabetical order of vCard Name property
    public static final byte ORDER_PHONETICAL = 0x02; // order of vCard Sound property

    /* PBAP search attribute types */
    public static final byte SEARCH_ATTR_NAME = 0x00;  // matching vCard Name property
    public static final byte SEARCH_ATTR_NUMBER = 0x01; // matching vCard Telephone Number property
    public static final byte SEARCH_ATTR_SOUND = 0x02; // matching vCard Sound property

    // +++ Custom action definition for PBAP client

    /**
     * Broadcast Action: Indicates custom action(request) from application.
     * This is mainly for PTS.
     *
     * <p>Always contains the extra field {@link #EXTRA_CUSTOM_ACTION}.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    public static final String ACTION_CUSTOM_ACTION =
        "android.bluetooth.pbapclient.action.CUSTOM_ACTION";

    public static final String EXTRA_CUSTOM_ACTION =
        "android.bluetooth.pbapclient.extra.CUSTOM_ACTION";

    public static final String KEY_COMMAND = "command";

    /**
     * Custom action to pull phonebook
     *
     * @param Bundle wrapped with
     *  {@link #KEY_COMMAND}
     *  {@link #BluetoothDevice.EXTRA_DEVICE}
     *  {@link #KEY_PB_NAME}
     *  {@link #KEY_FILTER}
     *  {@link #KEY_VCARD_TYPE}
     *  {@link #KEY_MAX_LIST_COUNT}
     *  {@link #KEY_LIST_START_OFFSET}
     */
    public static final String CUSTOM_ACTION_PULL_PHONEBOOK =
        "android.bluetooth.pbapclient.CUSTOM_ACTION_PULL_PHONEBOOK";
    public static final String KEY_PB_NAME = "pb_name";
    public static final String KEY_FILTER = "filter";
    public static final String KEY_VCARD_TYPE = "vcard_type";
    public static final String KEY_MAX_LIST_COUNT = "max_list_count";
    public static final String KEY_LIST_START_OFFSET = "list_start_offset";

    /**
     * Custom action to pull vCard listing
     *
     * @param Bundle wrapped with
     *  {@link #KEY_COMMAND}
     *  {@link #BluetoothDevice.EXTRA_DEVICE}
     *  {@link #KEY_PB_NAME}
     *  {@link #KEY_ORDER}
     *  {@link #KEY_SEARCH_PROP}
     *  {@link #KEY_SEARCH_VALUE}
     *  {@link #KEY_MAX_LIST_COUNT}
     *  {@link #KEY_LIST_START_OFFSET}
     */
    public static final String CUSTOM_ACTION_PULL_VCARD_LISTING =
        "android.bluetooth.pbapclient.CUSTOM_ACTION_PULL_VCARD_LISTING";
    public static final String KEY_ORDER = "order";
    public static final String KEY_SEARCH_PROP = "search_property";
    public static final String KEY_SEARCH_VALUE = "search_value";

    /**
     * Custom action to pull vCard entry
     *
     * @param Bundle wrapped with
     *  {@link #KEY_COMMAND}
     *  {@link #BluetoothDevice.EXTRA_DEVICE}
     *  {@link #KEY_VCARD_HANDLE}
     *  {@link #KEY_FILTER}
     *  {@link #KEY_VCARD_TYPE}
     */
    public static final String CUSTOM_ACTION_PULL_VCARD_ENTRY =
        "android.bluetooth.pbapclient.CUSTOM_ACTION_PULL_VCARD_ENTRY";
    public static final String KEY_VCARD_HANDLE = "vcard_handle";

    /**
     * Custom action to set phonebook folder path
     *
     * @param Bundle wrapped with
     *  {@link #KEY_COMMAND}
     *  {@link #BluetoothDevice.EXTRA_DEVICE}
     *  {@link #KEY_PB_NAME}
     */
    public static final String CUSTOM_ACTION_SET_PHONEBOOK =
        "android.bluetooth.pbapclient.CUSTOM_ACTION_SET_PHONEBOOK";

    /**
     * Custom action to abort
     *
     * @param Bundle wrapped with
     *  {@link #KEY_COMMAND}
     *  {@link #BluetoothDevice.EXTRA_DEVICE}
     */
    public static final String CUSTOM_ACTION_ABORT =
        "android.bluetooth.pbapclient.CUSTOM_ACTION_ABORT";

    // + Response for custom action

    /**
     * Intent used to broadcast PBAP client custom action result
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
        "android.bluetooth.pbapclient.action.CUSTOM_ACTION_RESULT";

    public static final String EXTRA_CUSTOM_ACTION_RESULT =
        "android.bluetooth.pbapclient.extra.CUSTOM_ACTION_RESULT";

    public static final String KEY_RESULT = "result";

    public static final String KEY_PHONEBOOK_SIZE = "phonebook_size";
    public static final String KEY_NEW_MISSED_CALLS = "new_missed_calls";

    public static final String KEY_VCARD_LISTING = "vcard_listing";

    public static final String KEY_VCARD_ENTRY = "vcard_entry";

    // - Response for custom action

    // --- Custom action definition for PBAP client

    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice mDevice = null;
    private BluetoothPbapClient mPbapClient = null;
    private Context mContext;

    private final ServiceListener mPbapClientServiceListener = new ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.PBAP_CLIENT) {
                mPbapClient = (BluetoothPbapClient) proxy;
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.PBAP_CLIENT) {
                mPbapClient = null;
            }
        }
    };

    public PbapProfile(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        Logger.d(TAG, "init");
        mAdapter.getProfileProxy(mContext, mPbapClientServiceListener,
                BluetoothProfile.PBAP_CLIENT);
    }

    public boolean connect(BluetoothDevice device) {
        if (mPbapClient != null) {
            return mPbapClient.connect(device);
        } else {
            return false;
        }
    }

    public boolean disconnect(BluetoothDevice device) {
        if (mPbapClient != null) {
            return mPbapClient.disconnect(device);
        } else {
            return false;
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        if (mPbapClient != null) {
            return mPbapClient.getConnectionState(device);
        } else {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
    }

    public boolean pullPhoneBook(BluetoothDevice device, String pbName,
            long filter, int listStartOffset, int maxListCount) {
        if (mPbapClient != null) {
            return mPbapClient.pullPhonebook(device, pbName,
                    filter, listStartOffset, maxListCount);
        } else {
            return false;
        }
    }

    public boolean pullVcardListing(BluetoothDevice device, String path, byte order, byte searchProp,
            String searchValue, int maxListCount, int listStartOffset) {
        // TODO
        return false;
    }

    public void pullVcardEntry(BluetoothDevice device, String vcardHandle,
            long filter, byte format) {
        // TODO
    }

    public void setPhoneBook(BluetoothDevice device, String folder) {
        // TODO
    }

    public void abort(BluetoothDevice device) {
        // TODO
    }

    public static boolean isSuccess(int result) {
        return (result == BluetoothPbapClient.RESULT_SUCCESS) ? true : false;
    }
}
