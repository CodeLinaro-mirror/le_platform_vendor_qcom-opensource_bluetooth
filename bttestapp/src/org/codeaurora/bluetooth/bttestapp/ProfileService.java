/*
 * Copyright (c) 2013-2016, 2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *            notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *            notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *        * Neither the name of The Linux Foundation nor
 *            the names of its contributors may be used to endorse or promote
 *            products derived from this software without specific prior written
 *            permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;

import com.android.vcard.VCardEntry;
import android.bluetooth.client.map.BluetoothMapBmessage;
import android.bluetooth.client.map.BluetoothMapEventReport;
import android.bluetooth.client.map.BluetoothMapMessage;
import android.bluetooth.client.map.BluetoothMasClient;
import org.codeaurora.bluetooth.bttestapp.R;
import org.codeaurora.bluetooth.bttestapp.services.IMapServiceCallback;

import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.MediaDescription;
import android.media.session.MediaController;
import android.media.session.MediaController.TransportControls;
import android.media.session.MediaSession;
import android.media.session.MediaSession.QueueItem;

import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;

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

    public static final String PTS_IOPT_PROPERTY = "bt.pts.iopt";

    private BluetoothDevice mDevice = null;

    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    private AvrcpProfile mAvrcp;
    private HfpProfile mHfp;
    private PbapProfile mPbap;

    private HashMap<Integer, BluetoothMasClient> mMapClients = null;

    private HashMap<Integer, IMapServiceCallback> mMapCallbacks = null;

    private HashMap<Integer, MapSessionData> mMapSessionData = null;

    private MapNotificationSender mMapNotificationSender = null;

    private boolean mIsBound = false;

    private final IBinder mBinder = new LocalBinder();

    private Context mContext;

    private static volatile BluetoothServerSocket mListenSocket = null;

    class MapSessionData {
        ArrayList<String> getFolderListing;
        ArrayList<BluetoothMapMessage> getMessagesListing;
        BluetoothMapBmessage getMessage;
    }

    public class LocalBinder extends Binder {
        ProfileService getService() {
            return ProfileService.this;
        }
    }

    private final Handler mMapHandler = new Handler() {

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            IMapServiceCallback cb = mMapCallbacks.get(msg.arg2);

            Log.v(TAG, "mMapHandler::handleMessage msg=" + msg.what + " status=" + msg.arg1
                    + " instanceid=" + msg.arg2);

            boolean success = (msg.arg1 == BluetoothMasClient.STATUS_OK);

            if (msg.what == BluetoothMasClient.EVENT_CONNECT) {
                Intent intent = new Intent(ACTION_MAP_CONNECTION_STATE);
                intent.putExtra(BluetoothDevice.EXTRA_SDP_RECORD, new SdpMasRecord(
                        msg.arg2, 0, 0, 0, 0, 0, null));
                intent.putExtra(EXTRA_CONNECTED, success);

                ProfileService.this.sendBroadcast(intent);
            } else if (msg.what == BluetoothMasClient.EVENT_SET_NOTIFICATION_REGISTRATION) {
                Intent intent = new Intent(ACTION_MAP_NOTIFICATION_STATE);
                intent.putExtra(BluetoothDevice.EXTRA_SDP_RECORD, new SdpMasRecord(
                    msg.arg2, 0, 0, 0, 0, 0, null));
                intent.putExtra(EXTRA_NOTIFICATION_STATE, ((Integer) msg.obj).intValue() != 0);

                ProfileService.this.sendBroadcast(intent);
            } else if (msg.what == BluetoothMasClient.EVENT_EVENT_REPORT) {
                BluetoothMapEventReport evt = (BluetoothMapEventReport) msg.obj;
                mMapNotificationSender.notify(msg.arg2, evt);
            }

            MapSessionData sessionData = mMapSessionData.get(msg.arg2);
            if (sessionData == null) {
                sessionData = new MapSessionData();
                mMapSessionData.put(msg.arg2, sessionData);
            }

            if (success) {
                switch (msg.what) {
                    case BluetoothMasClient.EVENT_GET_FOLDER_LISTING:
                        sessionData.getFolderListing = (ArrayList<String>) msg.obj;
                        break;
                    case BluetoothMasClient.EVENT_GET_MESSAGES_LISTING:
                        sessionData.getMessagesListing = (ArrayList<BluetoothMapMessage>) msg.obj;
                        break;
                    case BluetoothMasClient.EVENT_GET_MESSAGE:
                        sessionData.getMessage = (BluetoothMapBmessage) msg.obj;
                        break;
                }
            }

            if (cb == null) {
                Log.w(TAG, "No callback registered for MAS instance " + msg.arg2);
                return;
            }

            switch (msg.what) {
                case BluetoothMasClient.EVENT_CONNECT:
                    if (success) {
                        cb.onConnect();
                    } else {
                        cb.onConnectError();
                    }
                    break;
                case BluetoothMasClient.EVENT_UPDATE_INBOX:
                    if (success) {
                        cb.onUpdateInbox();
                    } else {
                        cb.onUpdateInboxError();
                    }
                    break;
                case BluetoothMasClient.EVENT_SET_PATH:
                    if (success) {
                        cb.onSetPath((String) msg.obj);
                    } else {
                        cb.onSetPathError((String) msg.obj);
                    }
                    break;
                case BluetoothMasClient.EVENT_GET_FOLDER_LISTING:
                    if (success && msg.obj instanceof ArrayList<?>) {
                        cb.onGetFolderListing(sessionData.getFolderListing);
                    } else {
                        cb.onGetFolderListingError();
                    }
                    break;
                case BluetoothMasClient.EVENT_GET_FOLDER_LISTING_SIZE:
                    if (success && msg.obj instanceof Integer) {
                        cb.onGetFolderListingSize((Integer) msg.obj);
                    } else {
                        cb.onGetFolderListingSizeError();
                    }
                    break;
                case BluetoothMasClient.EVENT_GET_MESSAGES_LISTING:
                    if (success && msg.obj instanceof ArrayList<?>) {
                        cb.onGetMessagesListing(sessionData.getMessagesListing);
                    } else {
                        cb.onGetMessagesListingError();
                    }
                    break;
                case BluetoothMasClient.EVENT_GET_MESSAGE:
                    if (success && msg.obj instanceof BluetoothMapBmessage) {
                        cb.onGetMessage(sessionData.getMessage);
                    } else {
                        cb.onGetMessageError();
                    }
                    break;
                case BluetoothMasClient.EVENT_SET_MESSAGE_STATUS:
                    if (success) {
                        cb.onSetMessageStatus();
                    } else {
                        cb.onSetMessageStatusError();
                    }
                    break;
                case BluetoothMasClient.EVENT_PUSH_MESSAGE:
                    if (success && msg.obj instanceof String) {
                        cb.onPushMessage((String) msg.obj);
                    } else {
                        cb.onPushMessageError();
                    }
                    break;
                case BluetoothMasClient.EVENT_GET_MESSAGES_LISTING_SIZE:
                    if (success && msg.obj instanceof Integer) {
                        cb.onGetMessagesListingSize((Integer) msg.obj);
                    } else {
                        cb.onGetMessagesListingSizeError();
                    }
                    break;
                case BluetoothMasClient.EVENT_EVENT_REPORT:
                    BluetoothMapEventReport evt = (BluetoothMapEventReport) msg.obj;
                    cb.onEventReport(evt);
                    break;
                case BluetoothMasClient.EVENT_ABORT:
                    if (success) {
                        cb.onAbort();
                    } else {
                        cb.onAbortError();
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown message in MAP: " + msg.what);
            }

        }
    };

    public class MapNotificationSender {

        public final int NEW_MESSAGE_NOTIFICATION_ID = 20000;

        public final int DELIVERY_SUCCESS_NOTIFICATION_ID = 20001;

        public final int SENDING_SUCCESS_NOTIFICATION_ID = 20002;

        public final int DELIVERY_FAILURE_NOTIFICATION_ID = 20003;

        public final int SENDING_FAILURE_NOTIFICATION_ID = 20004;

        public final int MEMORY_FULL_NOTIFICATION_ID = 20005;

        public final int MEMORY_AVAILABLE_NOTIFICATION_ID = 20006;

        public final int MESSAGE_DELETED_NOTIFICATION_ID = 20007;

        public final int MESSAGE_SHIFT_NOTIFICATION_ID = 20008;

        public final int READ_STATUS_CHANGED_NOTIFICATION_ID = 20009;

        private final NotificationManager mNotificationManager;

        MapNotificationSender() {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        public void notify(int instanceId, BluetoothMapEventReport eventReport) {
            switch (eventReport.getType()) {
                case NEW_MESSAGE:
                    notifyNewMessage(instanceId, eventReport);
                    break;
                case DELIVERY_SUCCESS:
                    notifyDeliverySuccess(eventReport);
                    break;
                case SENDING_SUCCESS:
                    notifySendingSuccess(eventReport);
                    break;
                case DELIVERY_FAILURE:
                    notifyDeliveryFailure(eventReport);
                    break;
                case SENDING_FAILURE:
                    notifySendingFailure(eventReport);
                    break;
                case MEMORY_AVAILABLE:
                    notifyMemoryAvailable(eventReport);
                    break;
                case MEMORY_FULL:
                    notifyMemoryFull(eventReport);
                    break;
                case MESSAGE_DELETED:
                    notifyMessageDeleted(eventReport);
                    break;
                case MESSAGE_SHIFT:
                    notifyMessageShift(eventReport);
                    break;
                case READ_STATUS_CHANGED:
                    notifyReadStatusChanged(eventReport);
                    break;
                default:
                    Log.e(TAG, "Unknown MAP report type (" + eventReport.getType().toString()
                            + ")!");
                    break;
            }
        }

        private void send(int id, String title, String text) {
            send(id, title, text, null);
        }

        private void send(int id, String title, String text, Intent click) {
            Notification.Builder builder = new Notification.Builder(getApplicationContext())
                    .setContentTitle(title)
                    .setContentText(text)
                    .setTicker(getString(R.string.map_report_notif_ticker))
                    .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true);

            if (click != null) {
                builder.setContentIntent(PendingIntent.getActivity(ProfileService.this, 0, click,
                        PendingIntent.FLAG_UPDATE_CURRENT));
            }

            mNotificationManager.notify(id, builder.build());
        }

        private void notifyNewMessage(int instanceId, BluetoothMapEventReport eventReport) {
            Intent click = new Intent(ProfileService.this, MapTestActivity.class);
            click.setAction(ACTION_MAP_GET_MESSAGE);
            click.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            click.putExtra(EXTRA_MAP_INSTANCE_ID, instanceId);
            click.putExtra(EXTRA_MAP_MESSAGE_HANDLE, eventReport.getHandle());

            send(NEW_MESSAGE_NOTIFICATION_ID,
                    String.format(getString(R.string.map_report_notif_received, eventReport
                            .getMsgType().toString())),
                    String.format(getString(R.string.map_report_notif_handle,
                            eventReport.getHandle())),
                    click);
        }

        private void notifyDeliverySuccess(BluetoothMapEventReport eventReport) {
            send(DELIVERY_SUCCESS_NOTIFICATION_ID,
                    getString(R.string.map_report_notif_title_delivery_success),
                    String.format(getString(R.string.map_report_notif_handle,
                            eventReport.getHandle())));
        }

        private void notifySendingSuccess(BluetoothMapEventReport eventReport) {
            send(SENDING_SUCCESS_NOTIFICATION_ID,
                    getString(R.string.map_report_notif_title_sending_success),
                    String.format(getString(R.string.map_report_notif_handle,
                            eventReport.getHandle())));
        }

        private void notifyDeliveryFailure(BluetoothMapEventReport eventReport) {
            send(DELIVERY_FAILURE_NOTIFICATION_ID,
                    getString(R.string.map_report_notif_title_delivery_failure),
                    String.format(getString(R.string.map_report_notif_handle,
                            eventReport.getHandle())));
        }

        private void notifySendingFailure(BluetoothMapEventReport eventReport) {
            send(SENDING_FAILURE_NOTIFICATION_ID,
                    getString(R.string.map_report_notif_title_sending_failure),
                    String.format(getString(R.string.map_report_notif_handle,
                            eventReport.getHandle())));
        }

        private void notifyMemoryFull(BluetoothMapEventReport eventReport) {
            send(MEMORY_FULL_NOTIFICATION_ID,
                    getString(R.string.map_report_notif_title_memory_full),
                    getString(R.string.blank));
        }

        private void notifyMemoryAvailable(BluetoothMapEventReport eventReport) {
            send(MEMORY_AVAILABLE_NOTIFICATION_ID,
                    getString(R.string.map_report_notif_title_memory_available),
                    getString(R.string.blank));
        }

        private void notifyMessageDeleted(BluetoothMapEventReport eventReport) {
            send(MESSAGE_DELETED_NOTIFICATION_ID,
                    getString(R.string.map_report_notif_title_message_deleted),
                    String.format(getString(R.string.map_report_notif_handle,
                            eventReport.getHandle())));
        }

        private void notifyMessageShift(BluetoothMapEventReport eventReport) {
            send(MESSAGE_SHIFT_NOTIFICATION_ID,
                    String.format(getString(R.string.map_report_notif_shifted,
                            eventReport.getHandle())),
                    String.format(getString(R.string.map_report_notif_fromto,
                            eventReport.getOldFolder(), eventReport.getFolder())));
        }

        private void notifyReadStatusChanged(BluetoothMapEventReport eventReport) {
            send(READ_STATUS_CHANGED_NOTIFICATION_ID,
                    getString(R.string.map_report_notif_title_read_status_changed),
                    String.format(getString(R.string.map_report_notif_handle,
                            eventReport.getHandle())));
        }

    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "received " + action);

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
                    Log.e(TAG, "Unexpected error!");
                    return;
                }
                Log.d(TAG,"AVRCP connection state changed for: "+ device);
                Log.d(TAG, "mDevice: " + mDevice.getAddress());
                if (mDevice.equals(device)) {
                    Intent new_intent = new Intent(ACTION_AVRCP_CONNECTION_STATE);
                    Log.d(TAG, "state: " + state);
                    if (state == 1) {
                        new_intent.putExtra(EXTRA_CONNECTED, true);
                    } else {
                        new_intent.putExtra(EXTRA_CONNECTED, false);
                    }
                    ProfileService.this.sendBroadcast(new_intent);
                } else {
                    Log.d(TAG,"AVRCP connection state change not updated");
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (dev.equals(mDevice)) {
                    for (BluetoothMasClient cli : mMapClients.values()) {
                        cli.disconnect();
                    }

                    checkAndStop(false, true);
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    for (BluetoothMasClient cli : mMapClients.values()) {
                        cli.disconnect();
                    }

                    checkAndStop(false, true);
                }
            }
        }
    };

    private void checkAndStop(boolean unbind, boolean disconnect) {
        boolean canStop = true;

        Log.v(TAG, "checkAndStop(): unbind=" + unbind + " disconnect=" + disconnect);

        if (unbind) {
            if (mHfp != null &&
                mHfp.getHeadsetClient().getConnectionState(mDevice) != BluetoothProfile.STATE_DISCONNECTED) {
                canStop = false;
            }

            for (BluetoothMasClient cli : mMapClients.values()) {
                if (cli.getState() != BluetoothMasClient.ConnectionState.DISCONNECTED) {
                    canStop = false;
                }
            }

            if (!canStop) {
                Log.v(TAG, "clients are still connected, won't stop");
            }
        }

        if (disconnect && mIsBound) {
            canStop = false;
            Log.v(TAG, "service is still bound, won't stop");
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
        Log.v(TAG, "onCreate");

        mMapClients = new HashMap<Integer, BluetoothMasClient>();
        mMapCallbacks = new HashMap<Integer, IMapServiceCallback>();
        mMapSessionData = new HashMap<Integer, MapSessionData>();
        mMapNotificationSender = new MapNotificationSender();

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
        enablePts(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand intent=" + intent + " flags=" + Integer.toHexString(flags)
                + " startId=" + startId);

        createPceService();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");

        unregisterReceiver(mReceiver);

        for (BluetoothMasClient cli : mMapClients.values()) {
            cli.disconnect();
        }
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

        if (mMapClients != null) {
            for (BluetoothMasClient cli : mMapClients.values()) {
                cli.disconnect();
            }
        }

        mDevice = device;

        if (mDevice != null) {
            Log.v(TAG,
                    "Current device: address=" + mDevice.getAddress() + " name="
                            + mDevice.getName());
        } else {
            Log.v(TAG, "Current device: none");
        }

        mMapClients = new HashMap<Integer, BluetoothMasClient>();
        mMapSessionData = new HashMap<Integer, MapSessionData>();
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

    public void setMasInstances(SdpMasRecord masrec) {
        // no need to recreate already existing MAS client
        if (mMapClients.containsKey(masrec.getMasInstanceId())) {
            /* Update mas sdp record */
            getMapClient(masrec.getMasInstanceId()).setInstanceData(masrec);
            return;
        }

        BluetoothMasClient client = new BluetoothMasClient(mDevice, masrec, mMapHandler);
        mMapClients.put(masrec.getMasInstanceId(), client);
    }

    public BluetoothMasClient getMapClient(int id) {
        return mMapClients.get(id);
    }

    public void setMapCallback(int id, IMapServiceCallback callback) {
        mMapCallbacks.put(id, callback);
    }

    public MapSessionData getMapSessionData(int id) {
        return mMapSessionData.get(id);
    }

    public void createPceService() {
        if (SystemProperties.getBoolean(PTS_IOPT_PROPERTY, false)) {
            Log.d(TAG, "connectSocket: UUID: " + BluetoothUuid.PBAP_PCE.getUuid());
            try {
                mListenSocket = mAdapter.listenUsingRfcommWithServiceRecord(
                    "Phonebook Access - PCE",
                    BluetoothUuid.PBAP_PCE.getUuid());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "bt.pts.iopt is not set");
        }
    }

    public void enablePts(boolean enable) {
        SystemProperties.set(PTS_IOPT_PROPERTY, String.valueOf(enable));
    }
}
