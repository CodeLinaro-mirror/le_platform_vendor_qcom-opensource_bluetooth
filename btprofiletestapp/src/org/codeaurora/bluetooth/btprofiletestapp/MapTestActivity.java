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

package org.codeaurora.bluetooth.btprofiletestapp;

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
import android.widget.Spinner;
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

import org.codeaurora.bluetooth.btprofiletestapp.util.Logger;

public class MapTestActivity extends MonkeyActivity implements OnClickListener,
        IBluetoothConnectionObserver {
    private static final String TAG = "MapTestActivity";
    private static final int MAX_MESSAGES = 20;
    /* Default content len to push message */
    private static final int CONTENT_LEN = 256;

    private final String TAB_BROWSE = "Browse";
    private final String TAB_PUSH = "Push";
    private final String RECIPIENT_URI = "tel:1234567";

    private String mCurrentTab = TAB_BROWSE;

    private final String[] mActionBarTabsNames = {
            TAB_BROWSE, TAB_PUSH
    };

    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBluetoothAdapter;
    private MapProfile mMap;
    private boolean mConnected = false;
    private ActionBar mActionBar;
    private ViewFlipper mViewFlipper = null;
    private Button mBtnBack, mBtnGetUnreadMessages, mBtnConnect, mBtnAbort, mBtnFilter, mBtnPushPrefill, mBtnPushMessage;
    private ListView mListViewMessages;
    private Spinner mTabsSpinner = null;

    /* For push message */
    private EditText mEditRecipient, mEditPrefillLen, mEditContent;
    private HashMap<String, BluetoothMapMessage> mMessagesMap = new HashMap<>(MAX_MESSAGES);
    private List<BluetoothMapMessage> mModelMessages = null;
    private BluetoothMapMessageAdapter mAdapterMessages = null;
    private PendingIntent mSentIntent;
    private PendingIntent mDeliveredIntent;

    Object mLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.v(TAG, "OnCreate");

        setContentView(R.layout.activity_map_test);
        ActionBar mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        } else {
            Logger.w(TAG, "getActionBar() null");
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
                Logger.d(TAG, "Select handle " + handle);
            }
        });
        registerForContextMenu(mListViewMessages);
        updateListEmptyView(false);

        mTabsSpinner = (Spinner) findViewById(R.id.map_tab_spinner);
        mTabsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // An item was selected. You can retrieve the selected item using
                String itemSelected = parent.getItemAtPosition(pos).toString();
                Log.d(TAG,"Item Selected is ::"+itemSelected);

                if(itemSelected.equalsIgnoreCase("browse")) {
                    mViewFlipper.setDisplayedChild(0);
                } else if(itemSelected.equalsIgnoreCase("push")) {
                    mViewFlipper.setDisplayedChild(1);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // can leave this empty
            }
        });
        //mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        /*ActionBar.TabListener tabListener = new ActionBar.TabListener() {
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

        /*for (String tab : mActionBarTabsNames) {
            mActionBar.addTab( mActionBar.newTab().setText(tab).setTabListener(tabListener));
        }*/

        // bind to app service
        Intent intent = new Intent(this, ProfileService.class);
        bindService(intent, mMapConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Logger.v(TAG, "onStart");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothMapClient.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothMapClient.ACTION_MESSAGE_SENT_SUCCESSFULLY);
        filter.addAction(BluetoothMapClient.ACTION_MESSAGE_DELIVERED_SUCCESSFULLY);
        filter.addAction(BluetoothMapClient.ACTION_MESSAGE_RECEIVED);
        filter.addAction(BluetoothMapClient.ACTION_MESSAGE_READ_STATUS_CHANGED);
        filter.addAction(BluetoothMapClient.ACTION_MESSAGE_DELETED_STATUS_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.v(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.v(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Logger.v(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Logger.v(TAG, "onDestroy");
        BluetoothConnectionReceiver.removeObserver(this);
        unbindService(mMapConnection);
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onDeviceChanged(BluetoothDevice device) {
        Logger.v(TAG, "onDeviceChanged() device " + device);
        mDevice = device;
    }

    @Override
    public void onDeviceDisconected() {
        Logger.v(TAG, "onDeviceDisconected");
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
            Logger.d(TAG, "mDevice " + mDevice);
            if (mMap != null && mDevice != null) {
                if (mMap.isConnected(mDevice)) {
                    mConnected = true;
                }
            }
            setButtons(mConnected);
        }
    };

    @Override
    public void onClick(View v) {
        if (v == mBtnBack) {
            Logger.d(TAG, "Back home");
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
        Logger.d(TAG, "connect");
        synchronized (mLock) {
            if (mMap == null) {
                Logger.e(TAG, "mMap is null");
                return;
            }
            if (!mMap.connect(mDevice)) {
                Logger.e(TAG, "connect failed");
            }
        }
    }

    private void disconnect() {
        Logger.d(TAG, "disconnect");
        synchronized (mLock) {
            if (mMap == null) {
                Logger.e(TAG, "mMap is null");
                return;
            }
            if (!mMap.disconnect(mDevice)) {
                Logger.e(TAG, "disconnect failed");
            }
        }
    }

    private void getUnreadMessages() {
        Logger.d(TAG, "getUnreadMessages");
        synchronized (mLock) {
            clearMessages();
            if (mMap == null) {
                Logger.e(TAG, "mMap is null");
                return;
            }
            if (!mMap.getUnreadMessages(mDevice)) {
                Logger.e(TAG, "getUnreadMessages failed");
            }
        }
    }

    private void abort() {
        Logger.d(TAG, "abort");
        synchronized (mLock) {
            if (mMap == null) {
                Logger.e(TAG, "mMap is null");
                return;
            }
            if (!mMap.abort(mDevice)) {
                Logger.e(TAG, "abort failed");
            }
        }
    }

    private void setFilter() {
        Logger.d(TAG, "setFilter");
        synchronized (mLock) {
            new MessageFilterDialogFragment().show(getFragmentManager(), "msg_filter");
        }
    }

    private void prefillMessage() {
        Logger.d(TAG, "prefillMessage");
        synchronized (mLock) {
            int len;
            String text = mEditPrefillLen.getText().toString();
            if (text == null || text.isEmpty()) {
                len = CONTENT_LEN;
            } else {
                len = Integer.parseInt(text);
            }
            Logger.d(TAG, "len " + len);
            String content = "";
            for (int i = 0; i < len; i++) {
                content = content + String.valueOf(i);
            }
            Logger.d(TAG, "setText " + content);
            mEditContent.setText(content);
        }
    }

    private void pushMessage() {
        return;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Logger.v(TAG, "mReceiver got " + action);
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
                        Logger.d(TAG, "device " + device + " connected");
                        return;
                    }
                    if (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
                            == BluetoothProfile.STATE_CONNECTED) {
                        Logger.d(TAG, mDevice + " connected");
                        mConnected = true;
                        setButtons(mConnected);
                    } else if (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
                                == BluetoothProfile.STATE_DISCONNECTED) {
                        Logger.d(TAG, mDevice + " disconnected");
                        mConnected = false;
                        setButtons(mConnected);
                    }
                } else if (action.equals(BluetoothMapClient.ACTION_MESSAGE_SENT_SUCCESSFULLY)) {
                    Logger.d(TAG, mDevice + " ");
                } else if (action.equals(
                    BluetoothMapClient.ACTION_MESSAGE_DELIVERED_SUCCESSFULLY)) {
                    Logger.d(TAG, mDevice + " ");
                } else if (action.equals(BluetoothMapClient.ACTION_MESSAGE_RECEIVED)) {
                    HashMap<String, String> attrs = new HashMap<String, String>();
                    attrs.put("handle", intent.getStringExtra(BluetoothMapClient.EXTRA_MESSAGE_HANDLE));
                    attrs.put("body_content", intent.getStringExtra(android.content.Intent.EXTRA_TEXT));
                    attrs.put("sender_phone_number", intent.getStringExtra(BluetoothMapClient.EXTRA_SENDER_CONTACT_URI));
                    attrs.put("sender_name", intent.getStringExtra(BluetoothMapClient.EXTRA_SENDER_CONTACT_NAME));
                    attrs.put("read_status", intent.getBooleanExtra(BluetoothMapClient.EXTRA_MESSAGE_READ_STATUS, false) ? "READ":"UNREAD");
                    BluetoothMapMessage message = new BluetoothMapMessage(attrs);
                    Logger.d(TAG, mDevice + " received message " + message);
                    onGetMessage(message);
                } else if (action.equals(BluetoothMapClient.ACTION_MESSAGE_DELETED_STATUS_CHANGED)) {
                    Logger.d(TAG, "Status changed for handle " + intent.getStringExtra(BluetoothMapClient.EXTRA_MESSAGE_HANDLE)
                            + " result " + intent.getIntExtra(BluetoothMapClient.EXTRA_RESULT_CODE, BluetoothMapClient.RESULT_FAILURE)
                            + " deleted status " + intent.getBooleanExtra(BluetoothMapClient.EXTRA_MESSAGE_DELETED_STATUS, false));
                    if (intent.getIntExtra(BluetoothMapClient.EXTRA_RESULT_CODE, BluetoothMapClient.RESULT_FAILURE) == BluetoothMapClient.RESULT_SUCCESS) {
                         onRemoveMessage(intent.getStringExtra(BluetoothMapClient.EXTRA_MESSAGE_HANDLE),
                            intent.getBooleanExtra(BluetoothMapClient.EXTRA_MESSAGE_DELETED_STATUS, false));
                    }
                } else if (action.equals(BluetoothMapClient.ACTION_MESSAGE_READ_STATUS_CHANGED)) {
                    Logger.d(TAG, "Status changed for handle " + intent.getStringExtra(BluetoothMapClient.EXTRA_MESSAGE_HANDLE)
                            + " result " + intent.getIntExtra(BluetoothMapClient.EXTRA_RESULT_CODE, BluetoothMapClient.RESULT_FAILURE)
                            + " read status " + intent.getBooleanExtra(BluetoothMapClient.EXTRA_MESSAGE_READ_STATUS, false));
                    if (intent.getIntExtra(BluetoothMapClient.EXTRA_RESULT_CODE, BluetoothMapClient.RESULT_FAILURE) == BluetoothMapClient.RESULT_SUCCESS) {
                         onMessageRead(intent.getStringExtra(BluetoothMapClient.EXTRA_MESSAGE_HANDLE),
                            intent.getBooleanExtra(BluetoothMapClient.EXTRA_MESSAGE_READ_STATUS, false));
                    }
                }
            }
        }
    };

    /* set buttons status according to connected status */
    private void setButtons(boolean connected) {
        Logger.v(TAG, connected ? "enable" : "disable" + " buttons");

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
                Logger.d(TAG, "Go back");
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Logger.d(TAG, "onCreateOptionsMenu");
        mActionBarMenu = menu;
        getMenuInflater().inflate(R.menu.menu_map_test, menu);
        return true;
    }

    public void updateListEmptyView(boolean working) {
        View progressBar = findViewById(R.id.msglist_progressbar);
        View textView = findViewById(R.id.msglist_empty);
        Logger.d(TAG, "updateListEmptyView working status " + working);
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
                    Logger.e(TAG, "Unknown type " + type);
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
            Logger.d(TAG, "getView, position " + position);
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
        Logger.d(TAG, "onGetMessage " + message);
        mMessagesMap.put(message.getHandle(), message);
        mAdapterMessages.add(message);
        updateListEmptyView(true);
    }

    private void onRemoveMessage(String handle, boolean deleted) {
        Logger.d(TAG, "removeMessage " + handle);
        if (deleted) {
            mAdapterMessages.remove(mMessagesMap.remove(handle));
        }
    }

    private void onMessageRead(String handle, boolean read) {
        Logger.d(TAG, "onMessageRead " + handle);
        BluetoothMapMessage message = mMessagesMap.get(handle);
        if (message != null) {
            if (read) {
                message.setReadStatus("READ");
            } else {
                message.setReadStatus("UNREAD");
            }
            mAdapterMessages.notifyDataSetChanged();
        }
    }

    private void clearMessages() {
        Logger.d(TAG, "clearMessages");
        mAdapterMessages.clear();
//        updateListEmptyView(true);
    }
}
