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

public class MessageFilterDialogFragment extends DialogFragment {

    private static final String TAG = "MessageFilterDialogFragment";
    private static final String MESSAGES_FILTER_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /* Properties for MAP filter */
    /* When set to "true", bluetooth process get the filter from the following properties */
    private final static String BLUETOOTH_MAP_FILTER_USE_PROPERTY = "vendor.bt.pts.mce.useproperty";
    private final static String BLUETOOTH_MAP_FILTER_MESSAGE_TYPE = "vendor.bt.pts.mce.messagetype";
    private final static String BLUETOOTH_MAP_FILTER_READ_STATUS = "vendor.bt.pts.mce.readstatus";
    private final static String BLUETOOTH_MAP_FILTER_PERIODBEGIN = "vendor.bt.pts.mce.periodbegin";
    private final static String BLUETOOTH_MAP_FILTER_PERIODEND = "vendor.bt.pts.mce.periodend";
    private final static String BLUETOOTH_MAP_FILTER_RECIPIENT = "vendor.bt.pts.mce.recipient";
    private final static String BLUETOOTH_MAP_FILTER_ORIGINATOR = "vendor.bt.pts.mce.originator";
    private final static String BLUETOOTH_MAP_FILTER_PRIORITY = "vendor.bt.pts.mce.priority";

    // MessagesFilter parameters
    private byte mMessageType = MessagesFilter.MESSAGE_TYPE_ALL;
    private Date mPeriodBegin = null;
    private Date mPeriodEnd = null;
    private byte mReadStatus = MessagesFilter.READ_STATUS_ANY;
    private String mRecipient = null;
    private String mOriginator = null;
    private byte mPriority = MessagesFilter.PRIORITY_ANY;
    private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat(MESSAGES_FILTER_DATE_FORMAT);

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View dialogView = getActivity().getLayoutInflater().inflate(
                R.layout.messages_filter, null);

        final MessageFilterHolder holder = new MessageFilterHolder(dialogView);
        getFilterFromProperties();
        holder.populate();

        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
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
                Logger.e(TAG, "Exception during parse begin period!");
            }

            try {
                mPeriodEnd = mSimpleDateFormat.parse(period_end.getText().toString());
            } catch (ParseException e) {
                mPeriodEnd = null;
                Logger.e(TAG, "Exception during parse end period!");
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
                    Logger.e(TAG, "Parse exception in DataTimePicker!");
                }
            }
        }

        public void pick() {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
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
        Logger.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_MESSAGE_TYPE + "  " + mMessageType);
        SystemProperties.set(BLUETOOTH_MAP_FILTER_MESSAGE_TYPE, mMessageType + "");

        Logger.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_READ_STATUS + "  " + mReadStatus);
        SystemProperties.set(BLUETOOTH_MAP_FILTER_READ_STATUS, mReadStatus + "");

        if (mPeriodBegin != null) {
            Logger.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_PERIODBEGIN + "  " + mSimpleDateFormat.format(mPeriodBegin));
            SystemProperties.set(BLUETOOTH_MAP_FILTER_PERIODBEGIN, mSimpleDateFormat.format(mPeriodBegin));
        } else {
            Logger.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_PERIODBEGIN + "  " );
            SystemProperties.set(BLUETOOTH_MAP_FILTER_PERIODBEGIN, "");
        }

        if (mPeriodEnd != null) {
            Logger.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_PERIODEND + "  " + mSimpleDateFormat.format(mPeriodEnd));
            SystemProperties.set(BLUETOOTH_MAP_FILTER_PERIODEND, mSimpleDateFormat.format(mPeriodEnd));
        } else {
            Logger.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_PERIODEND + "  ");
            SystemProperties.set(BLUETOOTH_MAP_FILTER_PERIODEND, "");
        }

        Logger.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_RECIPIENT + "  " + mRecipient);
        SystemProperties.set(BLUETOOTH_MAP_FILTER_RECIPIENT, mRecipient + "");

        Logger.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_ORIGINATOR + "  " + mOriginator);
        SystemProperties.set(BLUETOOTH_MAP_FILTER_ORIGINATOR, mOriginator + "");

        Logger.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_PRIORITY + "  " + mPriority);
        SystemProperties.set(BLUETOOTH_MAP_FILTER_PRIORITY, mPriority + "");

        Logger.d(TAG, "Set " + BLUETOOTH_MAP_FILTER_USE_PROPERTY + " true");
        SystemProperties.set(BLUETOOTH_MAP_FILTER_USE_PROPERTY, "true");
    }

    private void getFilterFromProperties() {
        mMessageType = ((byte)SystemProperties.getInt(
                BLUETOOTH_MAP_FILTER_MESSAGE_TYPE,
                MessagesFilter.MESSAGE_TYPE_ALL));
        mReadStatus = ((byte)SystemProperties.getInt(
                BLUETOOTH_MAP_FILTER_READ_STATUS,
                MessagesFilter.READ_STATUS_UNREAD));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(MESSAGES_FILTER_DATE_FORMAT);
        Date filterBegin, filterEnd;
        try {
            mPeriodBegin = simpleDateFormat.parse(SystemProperties.get(BLUETOOTH_MAP_FILTER_PERIODBEGIN));
        } catch (ParseException e) {
            mPeriodBegin = null;
            Logger.e(TAG, "Exception during parse begin period " + SystemProperties.get(BLUETOOTH_MAP_FILTER_PERIODBEGIN));
        }
        try {
            mPeriodEnd = simpleDateFormat.parse(SystemProperties.get(BLUETOOTH_MAP_FILTER_PERIODEND));
        } catch (ParseException e) {
            mPeriodEnd = null;
            Logger.e(TAG, "Exception during parse end period " + SystemProperties.get(BLUETOOTH_MAP_FILTER_PERIODEND));
        }

        mRecipient = (SystemProperties.get(BLUETOOTH_MAP_FILTER_RECIPIENT));
        mOriginator = (SystemProperties.get(BLUETOOTH_MAP_FILTER_ORIGINATOR));
        mPriority = ((byte)SystemProperties.getInt(
                BLUETOOTH_MAP_FILTER_PRIORITY,
                MessagesFilter.PRIORITY_ANY));
    }

}
