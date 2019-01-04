/*
 * Copyright (c) 2018-2019, The Linux Foundation. All rights reserved.
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

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMapClient;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class MapTestActivity extends MonkeyActivity implements OnClickListener,
        IBluetoothConnectionObserver {
    private static final String TAG = "MapTestActivity";
    private static final String EXTRA_BD_ADDRESS = "org.codeaurora.bluetooth.extra.bdaddress";
    private static final int MAX_MESSAGES = 20;
    private static final String MESSAGES_FILTER_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /* Default content len to push message */
    private static final int CONTENT_LEN = 256;

    /* Properties for MAP filter */
    /* When set to "true", bluetooth process get the filter from the following properties */
    private final static String BLUETOOTH_MAP_FILTER_USE_PROPERTY = "vendor.bt.mce.useproperty";
    private final static String BLUETOOTH_MAP_FILTER_MESSAGE_TYPE = "vendor.bt.mce.messagetype";
    private final static String BLUETOOTH_MAP_FILTER_READ_STATUS = "vendor.bt.mce.readstatus";
    private final static String BLUETOOTH_MAP_FILTER_PERIODBEGIN = "vendor.bt.mce.periodbegin";
    private final static String BLUETOOTH_MAP_FILTER_PERIODEND = "vendor.bt.mce.periodend";
    private final static String BLUETOOTH_MAP_FILTER_RECIPIENT = "vendor.bt.mce.recipient";
    private final static String BLUETOOTH_MAP_FILTER_ORIGINATOR = "vendor.bt.mce.originator";
    private final static String BLUETOOTH_MAP_FILTER_PRIORITY = "vendor.bt.mce.priority";

    private final String TAB_BROWSE = "Browse";
    private final String TAB_PUSH = "Push";
    private final String RECIPIENT_URI = "tel:1234567";

    private String mCurrentTab = TAB_BROWSE;

    private final String[] mActionBarTabsNames = {
            TAB_BROWSE, TAB_PUSH
    };

    private BluetoothDevice mDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private MapProfile mMap;
    private boolean mConnected = false;
    private ActionBar mActionBar;
    private ViewFlipper mViewFlipper = null;
    private Button mBtnBack, mBtnGetUnreadMessages, mBtnConnect, mBtnAbort, mBtnFilter, mBtnPushPrefill, mBtnPushMessage;
    private ListView mListViewMessages;

    /* For push message */
    private EditText mEditRecipient, mEditPrefillLen, mEditContent;
    private HashMap<String, BluetoothMapMessage> mMessagesMap = new HashMap<>(MAX_MESSAGES);
    private List<BluetoothMapMessage> mModelMessages = null;
    private BluetoothMapMessageAdapter mAdapterMessages = null;
    private PendingIntent mSentIntent;
    private PendingIntent mDeliveredIntent;

    // MessagesFilter parameters
    private byte mMessageType = MessagesFilter.MESSAGE_TYPE_ALL;
    private Date mPeriodBegin = null;
    private Date mPeriodEnd = null;
    private byte mReadStatus = MessagesFilter.READ_STATUS_ANY;
    private String mRecipient = null;
    private String mOriginator = null;
    private byte mPriority = MessagesFilter.PRIORITY_ANY;
    private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat(MESSAGES_FILTER_DATE_FORMAT);

    Object mLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "OnCreate");

        setContentView(R.layout.activity_map_test);
        ActionBar mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        } else {
            Log.w(TAG, "getActionBar() null");
        }
        mViewFlipper = (ViewFlipper) findViewById(R.id.maptest_viewflipper);
        BluetoothConnectionReceiver.registerObserver(this);
        mBtnConnect = (Button) findViewById(R.id.connect);
        mBtnConnect.setOnClickListener(this);
        mBtnGetUnreadMessages = (Button) findViewById(R.id.get_unread_messages);
        mBtnGetUnreadMessages.setOnClickListener(this);
        mBtnAbort = (Button) findViewById(R.id.abort);
        mBtnAbort.setOnClickListener(this);
        mBtnFilter = (Button) findViewById(R.id.filter);
        mBtnFilter.setOnClickListener(this);
        mBtnPushPrefill = (Button) findViewById(R.id.map_push_prefill);
        mBtnPushPrefill.setOnClickListener(this);
        mBtnPushMessage = (Button) findViewById(R.id.map_push);
        mBtnPushMessage.setOnClickListener(this);
        mEditRecipient = (EditText) findViewById(R.id.map_push_rcpt_edit);
        mEditRecipient.setText(RECIPIENT_URI);
        mEditPrefillLen = (EditText) findViewById(R.id.map_push_prefill_len_edit);
        mEditPrefillLen.setText(Integer.toString(CONTENT_LEN));
        mEditContent = (EditText) findViewById(R.id.map_push_content_edit);

        // Create list view for messages
        mListViewMessages = (ListView) findViewById(R.id.msglist_lv);
        mListViewMessages.setEmptyView(findViewById(R.id.msglist_empty));
        mModelMessages = new ArrayList<BluetoothMapMessage>();
        mAdapterMessages = new BluetoothMapMessageAdapter();
        mListViewMessages.setAdapter(mAdapterMessages);
        mListViewMessages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String handle = mModelMessages.get(position).getHandle();
                Log.d(TAG, "Select handle " + handle);
            }
        });
        registerForContextMenu(mListViewMessages);
        updateListEmptyView(false);

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            }

            @Override
            public void onTabSelected(Tab tab, FragmentTransaction ft) {
                mCurrentTab = tab.getText().toString();

                if (mCurrentTab.equals(TAB_BROWSE)) {
                    mViewFlipper.setDisplayedChild(0);
                } else if (mCurrentTab.equals(TAB_PUSH)) {
                    mViewFlipper.setDisplayedChild(1);
                }
                invalidateOptionsMenu();
            }

            @Override
            public void onTabReselected(Tab tab, FragmentTransaction ft) {
            }
        };

        for (String tab : mActionBarTabsNames) {
            mActionBar.addTab(
                mActionBar.newTab()
                    .setText(tab)
                    .setTabListener(tabListener));
        }

        // bind to app service
        Intent intent = new Intent(this, ProfileService.class);
        bindService(intent, mMapConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothMapClient.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothMapClient.ACTION_MESSAGE_SENT_SUCCESSFULLY);
        filter.addAction(BluetoothMapClient.ACTION_MESSAGE_DELIVERED_SUCCESSFULLY);
        filter.addAction(BluetoothMapClient.ACTION_MESSAGE_RECEIVED);
        filter.addAction(BluetoothMapClient.ACTION_EXT_MESSAGE_DELETED_STATUS_CHANGED);
        filter.addAction(BluetoothMapClient.ACTION_MESSAGE_READ_STATUS_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.v(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.v(TAG, "onDestroy");
        BluetoothConnectionReceiver.removeObserver(this);
        unbindService(mMapConnection);
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onDeviceChanged(BluetoothDevice device) {
        Log.v(TAG, "onDeviceChanged() device " + device);
        mDevice = device;
    }

    @Override
    public void onDeviceDisconected() {
        Log.v(TAG, "onDeviceDisconected");
        setButtons(false);
    }

    private final ServiceConnection mMapConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected()");
            mMap = null;
            setButtons(false);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected()");
            ProfileService profileService = ((ProfileService.LocalBinder) service).getService();
            mMap = profileService.getMapProfile();
            mConnected = false;
            if (mMap.isConnected(mDevice)) {
                mConnected = true;
            }
            setButtons(mConnected);
        }
    };

    @Override
    public void onClick(View v) {
        if (v == mBtnBack) {
            Log.d(TAG, "Back home");
            finish();
            return;
        } else if (v == mBtnConnect) {
            if (mConnected) {
                disconnect();
            } else {
                connect();
            }
        } else if (v == mBtnGetUnreadMessages) {
            getUnreadMessages();
        } else if (v == mBtnAbort) {
            abort();
        } else if (v == mBtnFilter) {
            setFilter();
        } else if (v == mBtnPushPrefill) {
            prefillMessage();
        } else if (v == mBtnPushMessage) {
            pushMessage();
        }
    }

    private void connect() {
        Log.d(TAG, "connect");
        synchronized (mLock) {
            if (mMap == null) {
                Log.e(TAG, "mMap is null");
                return;
            }
            if (!mMap.connect(mDevice)) {
                Log.e(TAG, "connect failed");
            }
        }
    }

    private void disconnect() {
        Log.d(TAG, "disconnect");
        synchronized (mLock) {
            if (mMap == null) {
                Log.e(TAG, "mMap is null");
                return;
            }
            if (!mMap.disconnect(mDevice)) {
                Log.e(TAG, "disconnect failed");
            }
        }
    }

    private void getUnreadMessages() {
        Log.d(TAG, "getUnreadMessages");
        synchronized (mLock) {
            clearMessages();
            if (mMap == null) {
                Log.e(TAG, "mMap is null");
                return;
            }
            if (!mMap.getUnreadMessages(mDevice)) {
                Log.e(TAG, "getUnreadMessages failed");
            }
        }
    }

    private void abort() {
        Log.d(TAG, "abort");
        synchronized (mLock) {
            if (mMap == null) {
                Log.e(TAG, "mMap is null");
                return;
            }
            if (!mMap.abort(mDevice)) {
                Log.e(TAG, "abort failed");
            }
        }
    }

    private void setFilter() {
        Log.d(TAG, "setFilter");
        synchronized (mLock) {
            new MessageFilterDialogFragment().show(getFragmentManager(), "msg_filter");
        }
    }

    private void prefillMessage() {
        Log.d(TAG, "prefillMessage");
        synchronized (mLock) {
            int len;
            String text = mEditPrefillLen.getText().toString();
            if (text == null || text.isEmpty()) {
                len = CONTENT_LEN;
            } else {
                len = Integer.parseInt(text);
            }
            Log.d(TAG, "len " + len);
            String content = "";
            for (int i = 0; i < len; i++) {
                content = content + String.valueOf(i);
            }
            Log.d(TAG, "setText " + content);
            mEditContent.setText(content);
        }
    }

    private void pushMessage() {
        Log.d(TAG, "pushMessage");
        synchronized (mLock) {
            if (mEditContent.getText().length() == 0) {
                prefillMessage();
            }

            Log.d(TAG, "Recipient :" + mEditRecipient.getText().toString());
            mSentIntent = PendingIntent.getBroadcast(this, 0, new Intent(BluetoothMapClient.ACTION_MESSAGE_SENT_SUCCESSFULLY),
                    PendingIntent.FLAG_ONE_SHOT);
            mDeliveredIntent = PendingIntent.getBroadcast(this, 0, new Intent(BluetoothMapClient.ACTION_MESSAGE_DELIVERED_SUCCESSFULLY),
                    PendingIntent.FLAG_ONE_SHOT);

            Uri[] recipients = new Uri[]{Uri.parse(mEditRecipient.getText().toString())};
            if (recipients == null) {
                Log.e(TAG, "recipients is null");
                return;
            }
            Log.e(TAG, "Uri recipients " + recipients);
            mMap.sendMessage(mDevice, recipients,
                    mEditContent.getText().toString(), mSentIntent, mDeliveredIntent);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TAG, "mReceiver got " + action);
            synchronized (mLock) {
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (dev.equals(mDevice)) {
                        mConnected = false;
                        setButtons(mConnected);
                    }
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                        mConnected = false;
                        setButtons(mConnected);
                    }
                } else if (action.equals(BluetoothMapClient.ACTION_CONNECTION_STATE_CHANGED)) {
                    BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (!device.equals(mDevice)) {
                        Log.d(TAG, "device " + device + " connected");
                        return;
                    }
                    if (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
                            == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, mDevice + " connected");
                        mConnected = true;
                        setButtons(mConnected);
                    } else if (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
                                == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, mDevice + " disconnected");
                        mConnected = false;
                        setButtons(mConnected);
                    }
                } else if (action.equals(BluetoothMapClient.ACTION_MESSAGE_SENT_SUCCESSFULLY)) {
                    Log.d(TAG, mDevice + " ");
                } else if (action.equals(
                        BluetoothMapClient.ACTION_MESSAGE_DELIVERED_SUCCESSFULLY)) {
                    Log.d(TAG, mDevice + " ");
                } else if (action.equals(BluetoothMapClient.ACTION_MESSAGE_RECEIVED)) {
                    HashMap<String, String> attrs = new HashMap<String, String>();
                    attrs.put("handle", intent.getStringExtra(BluetoothMapClient.EXTRA_MESSAGE_HANDLE));
                    attrs.put("body_content", intent.getStringExtra(android.content.Intent.EXTRA_TEXT));
                    attrs.put("sender_phone_number", intent.getStringExtra(BluetoothMapClient.EXTRA_SENDER_CONTACT_URI));
                    attrs.put("sender_name", intent.getStringExtra(BluetoothMapClient.EXTRA_SENDER_CONTACT_NAME));
                    attrs.put("type", intent.getStringExtra(BluetoothMapClient.EXTRA_TYPE));
                    attrs.put("read_status", intent.getStringExtra(BluetoothMapClient.EXTRA_READ_STATUS));
                    BluetoothMapMessage message = new BluetoothMapMessage(attrs);
                    Log.d(TAG, mDevice + " received message " + message);
                    onGetMessage(message);
                } else if (action.equals(BluetoothMapClient.ACTION_EXT_MESSAGE_DELETED_STATUS_CHANGED)) {
                    Log.d(TAG, mDevice + " Set delete staus successfully");
                    onRemoveMessage(intent.getStringExtra(BluetoothMapClient.EXTRA_MESSAGE_HANDLE));
                } else if (action.equals(BluetoothMapClient.ACTION_MESSAGE_READ_STATUS_CHANGED)) {
                    /* Cannot know message is set to "read" or "unread" in Event Report v1.1 */
                    Log.d(TAG, "Read status changed for handle " + intent.getStringExtra(BluetoothMapClient.EXTRA_MESSAGE_HANDLE)
                            + " in folder " + intent.getStringExtra(BluetoothMapClient.EXTRA_FOLDER));
                    onMessageRead(intent.getStringExtra(BluetoothMapClient.EXTRA_MESSAGE_HANDLE),
                            intent.getStringExtra(BluetoothMapClient.EXTRA_READ_STATUS));
                }
            }
        }
    };

    /* set buttons status according to connected status */
    private void setButtons(boolean connected) {
        Log.v(TAG, connected ? "enable" : "disable" + " buttons");

        if (connected) {
            mBtnConnect.setText(R.string.map_disconnect);
        } else {
            mBtnConnect.setText(R.string.map_connect);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Log.d(TAG, "Go back");
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        mActionBarMenu = menu;
        getMenuInflater().inflate(R.menu.menu_map_test, menu);
        return true;
    }

    public void updateListEmptyView(boolean working) {
        View progressBar = findViewById(R.id.msglist_progressbar);
        View textView = findViewById(R.id.msglist_empty);
        Log.d(TAG, "updateListEmptyView working status " + working);
        if (working) {
            textView.setVisibility(View.GONE);
            mListViewMessages.setEmptyView(progressBar);
        } else {
            progressBar.setVisibility(View.GONE);
            mListViewMessages.setEmptyView(textView);
        }
    }

    public enum Type {
        EMAIL, SMS_GSM, SMS_CDMA, MMS
    }

    public enum Status {
        READ, UNREAD
    }

    public static class BluetoothMapMessage {
        private String mHandle;
        private String mBodyContent;
        private String mSenderPhoneNumber;
        private String mSenderName;
        private String mType;
        private String mReadStatus;

        BluetoothMapMessage(HashMap<String, String> attrs) throws IllegalArgumentException {
            int size;

            try {
                /* just to validate */
                new BigInteger(attrs.get("handle"), 16);

                mHandle = attrs.get("handle");
            } catch (NumberFormatException e) {
                /*
                 * handle MUST have proper value, if it does not then throw
                 * something here
                 */
                throw new IllegalArgumentException(e);
            }

            mBodyContent = attrs.get("body_content");
            mSenderPhoneNumber = attrs.get("sender_phone_number");
            mSenderName = attrs.get("sender_name");
            mReadStatus = attrs.get("read_status");
            mType = attrs.get("type");
        }

        /**
         * @return value corresponding to <code>handle</code> parameter in MAP
         *         specification
         */
        public String getHandle() {
            return mHandle;
        }

        /**
         * @return value corresponding to <code>bmessage-body-content</code> parameter in MAP
         *         specification
         */
        public String getBodyContent() {
            return mBodyContent;
        }

        /**
         * @return value corresponding to TEL of <code>VCARD</code> parameter in MAP
         *         specification
         */
        public String getSenderPhoneNumber() {
            return mSenderPhoneNumber;
        }

        /**
         * @return value corresponding to Name of <code>VCARD</code> parameter in MAP
         *         specification
         */
        public String getSenderName() {
            return mSenderName;
        }

        /**
         * @return value corresponding to <code>readstatus</code> parameter in MAP
         *         specification
         */
        public String getReadStatus() {
            return mReadStatus;
        }

        /**
         * @return value corresponding to <code>readstatus</code> parameter in MAP
         *         specification
         */
        public void setReadStatus(String status) {
            mReadStatus = status;
        }

        /**
         * @return true when message has been read, otherwise return false
         *
         */
        public boolean isRead() {
            if (mReadStatus != null) {
                return mReadStatus.equalsIgnoreCase("READ");
            } else {
                return false;
            }
        }

        public String getType() {
            return mType;
        }

        public static String type2String(Type type) {
            switch (type) {
                case EMAIL:
                    return "EMAIL";
                case SMS_GSM:
                    return "SMS_GSM";
                case SMS_CDMA:
                    return "SMS_CDMA";
                case MMS:
                    return "MMS";
                default:
                    Log.e(TAG, "Unknown type " + type);
                    return "";
            }
        }

        @Override
        public String toString() {
            JSONObject json = new JSONObject();

            try {
                json.put("handle", mHandle);
                json.put("BodyContent", mBodyContent);
                json.put("SenderPhoneNumber", mSenderPhoneNumber);
                json.put("sender name", mSenderName);
                json.put("read status", mReadStatus);
            } catch (JSONException e) {
                // do nothing
            }
            return json.toString();
        }
    }

    class BluetoothMapMessageAdapter extends ArrayAdapter<BluetoothMapMessage> {
        BluetoothMapMessageAdapter() {
            super(MapTestActivity.this, android.R.layout.simple_list_item_1, mModelMessages);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(TAG, "getView, position " + position);
            View v = convertView;
            if (v == null) {
                v = getLayoutInflater().inflate(R.layout.message_row, parent, false);
            }

            BluetoothMapMessage msg = mModelMessages.get(position);

            ((TextView) v.findViewById(R.id.message_row_sender)).setText(msg.getSenderName());
            ((TextView) v.findViewById(R.id.message_row_phone_number)).setText(msg.getSenderPhoneNumber());
            ((TextView) v.findViewById(R.id.message_row_type)).setText(msg.getType());
            ((TextView) v.findViewById(R.id.message_row_flag_read_status)).setText(msg.getReadStatus());
            ((TextView) v.findViewById(R.id.message_row_flag_read_status))
                    .setTextColor(msg.isRead() ? Color.DKGRAY : Color.YELLOW);
            ((TextView) v.findViewById(R.id.message_row_handle)).setText(msg.getHandle());
            ((TextView) v.findViewById(R.id.message_row_body)).setText(msg.getBodyContent());
            Button mBtnRead = (Button) v.findViewById(R.id.message_row_btn_read);
            mBtnRead.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMap.setMessageStatus(mDevice, msg.getHandle(), BluetoothMapClient.READ);
                    mBtnRead.setFocusable(false);
                }
            });

            Button mBtnDel = (Button) v.findViewById(R.id.message_row_btn_delete);
            mBtnDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMap.setMessageStatus(mDevice, msg.getHandle(), BluetoothMapClient.DELETED);
                    mBtnDel.setFocusable(false);
                }
            });

            return v;
        }
    }

    private void onGetMessage(BluetoothMapMessage message) {
        Log.d(TAG, "onGetMessage " + message);
        mMessagesMap.put(message.getHandle(), message);
        mAdapterMessages.add(message);
//        updateListEmptyView(false);
    }

    private void onRemoveMessage(String handle) {
        Log.d(TAG, "removeMessage " + handle);
        mAdapterMessages.remove(mMessagesMap.remove(handle));
    }

    private void onMessageRead(String handle, String read) {
        Log.d(TAG, "onMessageRead " + handle);
        BluetoothMapMessage message = mMessagesMap.get(handle);
        if (message != null) {
            if (read != null && !read.isEmpty()) {
                message.setReadStatus(read);
            } else {
                message.setReadStatus("READ");
            }
            mAdapterMessages.notifyDataSetChanged();
        }
    }

    private void clearMessages() {
        Log.d(TAG, "clearMessages");
        mAdapterMessages.clear();
//        updateListEmptyView(true);
    }

   public class MessageFilterDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View dialogView = MapTestActivity.this.getLayoutInflater().inflate(
                    R.layout.messages_filter, null);

            final MessageFilterHolder holder = new MessageFilterHolder(dialogView);
            holder.populate();

            final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MapTestActivity.this);
            alertBuilder.setView(dialogView);
            alertBuilder
                    .setTitle(getResources().getString(R.string.dialog_title_create_msg_filter));
            alertBuilder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            holder.save();
                        }
                    });
            alertBuilder.setNegativeButton(android.R.string.cancel, null);
            alertBuilder.setNeutralButton(getResources().getString(R.string.clear), null);

            // After click on button with assigned listener in function
            // setNeutralButton, the dialog automatically will close. To avoid
            // it,
            // onClickListener for this button have to be assigned manually.
            final AlertDialog alertDialog = alertBuilder.create();
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    Button clearButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                    clearButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            holder.clear();
                        }
                    });
                }
            });

            return alertDialog;
        }
    }

    public void onClickGetMessagesFilter(View view) {
        new MessageFilterDialogFragment().show(getFragmentManager(), "msg_filter");
    }

    class MessageFilterHolder {
        CheckBox type_sms_gsm = null;
        CheckBox type_sms_cdma = null;
        CheckBox type_email = null;
        CheckBox type_mms = null;
        RadioButton status_read_all = null;
        RadioButton status_unread = null;
        RadioButton status_read = null;
        RadioButton priority_all = null;
        RadioButton priority_high = null;
        RadioButton priority_non_high = null;
        EditText period_begin = null;
        EditText period_end = null;
        EditText recipient = null;
        EditText originator = null;
        ImageButton pickPeriodBegin = null;
        ImageButton pickPeriodEnd = null;

        MessageFilterHolder(View row) {
            type_sms_gsm = (CheckBox) row.findViewById(R.id.map_filter_type_sms_gsm);
            type_sms_cdma = (CheckBox) row.findViewById(R.id.map_filter_type_sms_cdma);
            type_email = (CheckBox) row.findViewById(R.id.map_filter_type_email);
            type_mms = (CheckBox) row.findViewById(R.id.map_filter_type_mms);
            status_read_all = (RadioButton) row.findViewById(R.id.map_filter_read_status_all);
            status_unread = (RadioButton) row.findViewById(R.id.map_filter_read_status_unread);
            status_read = (RadioButton) row.findViewById(R.id.map_filter_read_status_read);
            priority_all = (RadioButton) row.findViewById(R.id.map_filter_priority_all);
            priority_high = (RadioButton) row.findViewById(R.id.map_filter_priority_high);
            priority_non_high = (RadioButton) row.findViewById(R.id.map_filter_status_non_high);
            period_begin = (EditText) row.findViewById(R.id.map_filter_period_begin);
            period_end = (EditText) row.findViewById(R.id.map_filter_period_end);
            recipient = (EditText) row.findViewById(R.id.map_filter_recipient);
            originator = (EditText) row.findViewById(R.id.map_filter_originator);
            pickPeriodBegin = (ImageButton) row.findViewById(R.id.map_pick_data_period_begin);
            pickPeriodBegin.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    new DateTimePicker(period_begin).pick();
                }
            });
            pickPeriodEnd = (ImageButton) row.findViewById(R.id.map_pick_data_period_end);
            pickPeriodEnd.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    new DateTimePicker(period_end).pick();
                }
            });
        }

        void populate() {
            type_sms_gsm.setChecked((mMessageType & MessagesFilter.MESSAGE_TYPE_NO_SMS_GSM) != 0);
            type_sms_cdma.setChecked((mMessageType & MessagesFilter.MESSAGE_TYPE_NO_SMS_CDMA) != 0);
            type_email.setChecked((mMessageType & MessagesFilter.MESSAGE_TYPE_NO_EMAIL) != 0);
            type_mms.setChecked((mMessageType & MessagesFilter.MESSAGE_TYPE_NO_MMS) != 0);
            status_read_all.setChecked(mReadStatus == MessagesFilter.READ_STATUS_ANY);
            status_read.setChecked(mReadStatus == MessagesFilter.READ_STATUS_READ);
            status_unread.setChecked(mReadStatus == MessagesFilter.READ_STATUS_UNREAD);
            priority_all.setChecked(mPriority == MessagesFilter.PRIORITY_ANY);
            priority_high.setChecked(mPriority == MessagesFilter.PRIORITY_HIGH);
            priority_non_high.setChecked(mPriority == MessagesFilter.PRIORITY_NON_HIGH);

            if (mPeriodBegin != null) {
                period_begin.setText(mSimpleDateFormat.format(mPeriodBegin));
            } else {
                period_begin.setText(null);
            }

            if (mPeriodEnd != null) {
                period_end.setText(mSimpleDateFormat.format(mPeriodEnd));
            } else {
                period_end.setText(null);
            }

            recipient.setText(mRecipient);
            originator.setText(mOriginator);
        }

        void clear() {
            mMessageType = MessagesFilter.MESSAGE_TYPE_ALL;
            mPeriodBegin = null;
            mPeriodEnd = null;
            mReadStatus = MessagesFilter.READ_STATUS_ANY;
            mRecipient = null;
            mOriginator = null;
            mPriority = MessagesFilter.PRIORITY_ANY;

            populate();
        }

        void save() {
            mMessageType = MessagesFilter.MESSAGE_TYPE_ALL;

            if (type_sms_gsm.isChecked()) {
                mMessageType |= MessagesFilter.MESSAGE_TYPE_NO_SMS_GSM;
            }

            if (type_sms_cdma.isChecked()) {
                mMessageType |= MessagesFilter.MESSAGE_TYPE_NO_SMS_CDMA;
            }

            if (type_email.isChecked()) {
                mMessageType |= MessagesFilter.MESSAGE_TYPE_NO_EMAIL;
            }

            if (type_mms.isChecked()) {
                mMessageType |= MessagesFilter.MESSAGE_TYPE_NO_MMS;
            }

            if (status_read.isChecked()) {
                mReadStatus = MessagesFilter.READ_STATUS_READ;
            } else if (status_unread.isChecked()) {
                mReadStatus = MessagesFilter.READ_STATUS_UNREAD;
            } else {
                mReadStatus = MessagesFilter.READ_STATUS_ANY;
            }

            if (priority_high.isChecked()) {
                mPriority = MessagesFilter.PRIORITY_HIGH;
            } else if (priority_non_high.isChecked()) {
                mPriority = MessagesFilter.PRIORITY_NON_HIGH;
            } else {
                mPriority = MessagesFilter.PRIORITY_ANY;
            }

            try {
                mPeriodBegin = mSimpleDateFormat.parse(period_begin.getText().toString());
            } catch (ParseException e) {
                mPeriodBegin = null;
                Log.e(TAG, "Exception during parse begin period!");
            }

            try {
                mPeriodEnd = mSimpleDateFormat.parse(period_end.getText().toString());
            } catch (ParseException e) {
                mPeriodEnd = null;
                Log.e(TAG, "Exception during parse end period!");
            }

            mRecipient = recipient.getText().toString();
            mOriginator = originator.getText().toString();
            save2Properties();
        }
    }

    /**
    * Object representation of filters to be applied on message listing
    *
    * @see MSG_GET_MESSAGE_LISTING in MceStateMachine.java
    */
    public static final class MessagesFilter {
        public final static byte MESSAGE_TYPE_ALL = 0x00;
        public final static byte MESSAGE_TYPE_NO_SMS_GSM = 0x01;
        public final static byte MESSAGE_TYPE_NO_SMS_CDMA = 0x02;
        public final static byte MESSAGE_TYPE_NO_EMAIL = 0x04;
        public final static byte MESSAGE_TYPE_NO_MMS = 0x08;

        public final static byte READ_STATUS_ANY = 0x00;
        public final static byte READ_STATUS_UNREAD = 0x01;
        public final static byte READ_STATUS_READ = 0x02;

        public final static byte PRIORITY_ANY = 0x00;
        public final static byte PRIORITY_HIGH = 0x01;
        public final static byte PRIORITY_NON_HIGH = 0x02;
    }

    class DateTimePicker {
        View view = null;
        EditText editText = null;
        DatePicker dataPicker = null;
        TimePicker timePicker = null;

        DateTimePicker(EditText ref) {
            editText = ref;

            view = getLayoutInflater().inflate(R.layout.date_time_picker, null);

            dataPicker = (DatePicker) view.findViewById(R.id.date_picker);
            timePicker = (TimePicker) view.findViewById(R.id.time_picker);
            timePicker.setIs24HourView(true);

            String oldValue = editText.getText().toString();

            if (oldValue != null && !oldValue.isEmpty() && !oldValue.equals("")) {
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(mSimpleDateFormat.parse(oldValue));
                    dataPicker.updateDate(cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                    timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
                    timePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
                } catch (ParseException e) {
                    Log.e(TAG, "Parse exception in DataTimePicker!");
                }
            }
        }

        public void pick() {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MapTestActivity.this);
            alertBuilder.setView(view);
            alertBuilder.setTitle(getResources().getString(R.string.dialog_title_pick_date_time));
            alertBuilder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int year = dataPicker.getYear();
                            int month = dataPicker.getMonth();
                            int day = dataPicker.getDayOfMonth();
                            int hour = timePicker.getCurrentHour();
                            int minute = timePicker.getCurrentMinute();

                            editText.setText(mSimpleDateFormat.format(new GregorianCalendar(
                                    year, month, day, hour, minute, 0).getTime()));
                        }
                    });
            alertBuilder.setNegativeButton(android.R.string.cancel, null);
            alertBuilder.show();
        }
    }

    private void save2Properties() {
        Log.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_MESSAGE_TYPE + "  " + mMessageType);
        SystemProperties.set(BLUETOOTH_MAP_FILTER_MESSAGE_TYPE, mMessageType + "");

        Log.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_READ_STATUS + "  " + mReadStatus);
        SystemProperties.set(BLUETOOTH_MAP_FILTER_READ_STATUS, mReadStatus + "");

        if (mPeriodBegin != null) {
            Log.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_PERIODBEGIN + "  " + mSimpleDateFormat.format(mPeriodBegin));
            SystemProperties.set(BLUETOOTH_MAP_FILTER_PERIODBEGIN, mSimpleDateFormat.format(mPeriodBegin));
        } else {
            Log.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_PERIODBEGIN + "  " );
            SystemProperties.set(BLUETOOTH_MAP_FILTER_PERIODBEGIN, "");
        }

        if (mPeriodEnd != null) {
            Log.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_PERIODEND + "  " + mSimpleDateFormat.format(mPeriodEnd));
            SystemProperties.set(BLUETOOTH_MAP_FILTER_PERIODEND, mSimpleDateFormat.format(mPeriodEnd));
        } else {
            Log.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_PERIODEND + "  ");
            SystemProperties.set(BLUETOOTH_MAP_FILTER_PERIODEND, "");
        }

        Log.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_RECIPIENT + "  " + mRecipient);
        SystemProperties.set(BLUETOOTH_MAP_FILTER_RECIPIENT, mRecipient + "");

        Log.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_ORIGINATOR + "  " + mOriginator);
        SystemProperties.set(BLUETOOTH_MAP_FILTER_ORIGINATOR, mOriginator + "");

        Log.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_PRIORITY + "  " + mPriority);
        SystemProperties.set(BLUETOOTH_MAP_FILTER_PRIORITY, mPriority + "");

        Log.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_USE_PROPERTY + " true");
        SystemProperties.set(BLUETOOTH_MAP_FILTER_USE_PROPERTY, "true");
    }
}
