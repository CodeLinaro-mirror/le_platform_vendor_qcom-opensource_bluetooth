/*
 * Copyright (c) 2017, The Linux Foundation. All rights reserved.
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

import org.codeaurora.bluetooth.bttestapp.util.Logger;

import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

public class AvrcpTestActivity extends MonkeyActivity implements
    OnClickListener, IBluetoothConnectionObserver, OnItemSelectedListener,
    OnTouchListener {

    private final String TAG = "AvrcpTestActivity";
    private Button mBtnPlayPause;
    private Button mBtnFastforward;
    private Button mBtnRewind;
    private Button mBtnGetCurrentPas;
    private Button mBtnSearch;
    private EditText mEditTextSearch;
    private Button mBtnGetSupportedFeatures;

    private final String STATUS_PLAY = "Play";
    private final String STATUS_PAUSE = "Pause";

    private ProfileService mProfileService = null;
    private BluetoothAvrcpPlayerSettings mPlayerAppSetting;
    private BluetoothDevice mDevice;
    private Spinner mSpEqualizer;
    private Spinner mSpRepeat;
    private Spinner mSpShuffle;
    private Spinner mSpScan;
    /*
     * Hash map. key: pas attribute value, value: pas attribute value in string
     */
    private Map<Integer, String> mPasText = new HashMap<Integer, String>();
    /*
     * Hash map. key: pas attribute value in string, value: pas attribute value
     */
    private Map<String, Integer> mPasValue = new HashMap<String, Integer>();

    public static final String ACTION_TRACK_EVENT =
        "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT";

    public static final String EXTRA_PLAYBACK =
        "android.bluetooth.avrcp-controller.profile.extra.PLAYBACK";

    public static final String ACTION_SUPPORTED_FEATURES =
        "android.bluetooth.avrcp-controller.profile.action.SUPPORTED_FEATURES";

    public static final String EXTRA_SUPPORTED_FEATURES =
        "android.bluetooth.avrcp-controller.profile.extra.SUPPORTED_FEATURES";

    public static final int BTRC_FEAT_NONE = 0x00;
    public static final int BTRC_FEAT_METADATA = 0x01;
    public static final int BTRC_FEAT_ABSOLUTE_VOLUME = 0x02;
    public static final int BTRC_FEAT_BROWSE = 0x04;
    public static final int BTRC_FEAT_COVER_ART = 0x08;

    private static final int FASTFORWARD_PRESSED = 0;
    private static final int REWIND_PRESSED = 1;

    private static final int TIMEOUT_IN_MS = 1000;

    private final ServiceConnection mAvrcpConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected()");
            mProfileService = null;
            if (mBtnPlayPause != null) mBtnPlayPause.setText(STATUS_PLAY);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected()");
            mProfileService = ((ProfileService.LocalBinder) service).getService();

        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "action " + action);
            if (action.equals(ACTION_TRACK_EVENT)) {
                PlaybackState ps = intent.getParcelableExtra(EXTRA_PLAYBACK);
                if (ps != null && mBtnPlayPause != null
                        && (ps.getState() == PlaybackState.STATE_PAUSED
                        || ps.getState() == PlaybackState.STATE_STOPPED)) {
                    mBtnPlayPause.setText(STATUS_PLAY);
                } else if (ps != null && mBtnPlayPause != null
                        && ps.getState() == PlaybackState.STATE_PLAYING) {
                    mBtnPlayPause.setText(STATUS_PAUSE);
                }
            } else if (action.equals(BluetoothAvrcpController.ACTION_PLAYER_SETTING)) {
                mPlayerAppSetting = intent.getParcelableExtra(BluetoothAvrcpController.EXTRA_PLAYER_SETTING);
                updatePlayerAppSettingUI(mPlayerAppSetting);
            } else if (action.equals(ACTION_SUPPORTED_FEATURES)) {
                handleActionSupportedFeatures(intent);
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == FASTFORWARD_PRESSED) {
                sendFastForward(true);
                sendEmptyMessageDelayed(FASTFORWARD_PRESSED, TIMEOUT_IN_MS);
            } else if (msg.what == REWIND_PRESSED) {
                sendRewind(true);
                sendEmptyMessageDelayed(REWIND_PRESSED, TIMEOUT_IN_MS);
            } else {
                Log.e(TAG, "Unknown msg: " + msg.what);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelper.initialize(this, R.layout.layout_avrcp);//use layout_avrcp.xml
        initPasMaps();
        mBtnPlayPause = (Button) findViewById(R.id.id_btn_play_pause);
        mBtnPlayPause.setText(STATUS_PLAY);
        mBtnPlayPause.setOnClickListener(this);
        mBtnGetCurrentPas = (Button) findViewById(R.id.id_btn_get_current_pas);
        mBtnGetCurrentPas.setOnClickListener(this);

        mSpEqualizer = (Spinner) findViewById(R.id.id_sp_equalizer);
        mSpEqualizer.setOnItemSelectedListener(this);

        mSpRepeat = (Spinner) findViewById(R.id.id_sp_repeat);
        mSpRepeat.setOnItemSelectedListener(this);

        mSpShuffle = (Spinner) findViewById(R.id.id_sp_shuffle);
        mSpShuffle.setOnItemSelectedListener(this);

        mSpScan = (Spinner) findViewById(R.id.id_sp_scan);
        mSpScan.setOnItemSelectedListener(this);

        BluetoothConnectionReceiver.registerObserver(this);

        mBtnFastforward = (Button) findViewById(R.id.id_btn_fast_forward);
        mBtnFastforward.setOnTouchListener(this);

        mBtnRewind = (Button) findViewById(R.id.id_btn_rewind);
        mBtnRewind.setOnTouchListener(this);

        mEditTextSearch = (EditText) findViewById(R.id.id_edit_search);
        mEditTextSearch.setText("You");  // Sample search string for test
        mEditTextSearch.setVisibility(View.VISIBLE);

        mBtnSearch = (Button) findViewById(R.id.id_btn_search);
        mBtnSearch.setOnClickListener(this);

        mBtnGetSupportedFeatures = (Button) findViewById(R.id.id_btn_get_supported_features);
        mBtnGetSupportedFeatures.setOnClickListener(this);

        // bind to app service
        Intent intent = new Intent(this, ProfileService.class);
        bindService(intent, mAvrcpConnection, BIND_AUTO_CREATE);

        // receive playback state change
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TRACK_EVENT);
        filter.addAction(BluetoothAvrcpController.ACTION_PLAYER_SETTING);
        filter.addAction(ACTION_SUPPORTED_FEATURES);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mAvrcpConnection);
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onDeviceChanged(BluetoothDevice device) {
        Logger.d(TAG, "onDeviceChanged()");

        mDevice = device;
    }

    @Override
    public void onDeviceDisconected() {
        Logger.v(TAG, "onDeviceDisconected");

        invalidateOptionsMenu();
    }

    @Override
    public void onClick(View v) {
        if (v == mBtnPlayPause) {
            Log.d(TAG, "onClick mBtnPlayPause");
            sendCommand();
        } else if (v == mBtnSearch) {
            Log.d(TAG, "onClick mBtnSearch");
            handleClickBtnSearch();
        } else if (v == mBtnGetSupportedFeatures) {
            Log.d(TAG, "onClick mBtnGetSupportedFeatures");
            handleClickBtnGetSupportedFeatures();
        } else if (v == mBtnGetCurrentPas) {
            Log.d(TAG, "onClick mBtnGetCurrentPas");
            handleClickBtnGetCurrentPas();
        }
    }

    private void handleClickBtnSearch() {
        String searchQuery = mEditTextSearch.getText().toString();
        if ((searchQuery != null) && !searchQuery.isEmpty()) {
            Log.d(TAG, "BtnSearch clicked, search: " + searchQuery);
            sendSearch(searchQuery);
        } else {
            Log.w(TAG, "BtnSearch clicked, but search string empty");
        }
    }

    private void handleClickBtnGetSupportedFeatures() {
        sendGetSupportedFeatures(mDevice);
    }

    private void handleClickBtnGetCurrentPas() {
        if (mDevice == null || mProfileService == null ||
            mProfileService.getAvrcpController() == null ||
            mProfileService.getAvrcpController().getPlayerSettings(mDevice) == null) {
            Log.e(TAG, "mDevice " + mDevice + " mProfileService " + mProfileService
                + " getAvrcpController() " + mProfileService.getAvrcpController()
                + " getPlayerSettings" + mProfileService.getAvrcpController().getPlayerSettings(mDevice));
        } else {
            mPlayerAppSetting = mProfileService.getAvrcpController().getPlayerSettings(mDevice);
            updatePlayerAppSettingUI(mPlayerAppSetting);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == mBtnFastforward) {
            handleTouchBtnFastforward(event);
        } else if (v == mBtnRewind) {
            handleTouchBtnRewind(event);
        }
        return false;
    }

    private void handleTouchBtnFastforward(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "BtnFastforward, key pressed");
                sendFastForward(true);
                mHandler.sendEmptyMessageDelayed(FASTFORWARD_PRESSED, TIMEOUT_IN_MS);
                break;

            case MotionEvent.ACTION_UP:
                Log.d(TAG, "BtnFastforward, key released");
                mHandler.removeMessages(FASTFORWARD_PRESSED);
                sendFastForward(false);
                break;

            default:
                break;
        }
    }

    private void handleTouchBtnRewind(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "BtnRewind, key pressed");
                sendRewind(true);
                mHandler.sendEmptyMessageDelayed(REWIND_PRESSED, TIMEOUT_IN_MS);
                break;

            case MotionEvent.ACTION_UP:
                Log.d(TAG, "BtnRewind, key released");
                mHandler.removeMessages(REWIND_PRESSED);
                sendRewind(false);
                break;

            default:
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "parent " + parent + " view " + view + " position " + position + " id " + id + mSpEqualizer.getSelectedItem().toString());
        if (mDevice == null || mProfileService == null ||
            mProfileService.getAvrcpController() == null ||
            mProfileService.getAvrcpController().getPlayerSettings(mDevice) == null ||
            mPlayerAppSetting == null) {
            Log.e(TAG, "mDevice " + mDevice + " mProfileService " + mProfileService + " mPlayerAppSetting" + mPlayerAppSetting);
            return;
        }

        boolean result = true;
        if(parent == mSpEqualizer) {
            Log.d(TAG, "mSpEqualizer, set to " + mSpEqualizer.getSelectedItem().toString() +
                " " + mPasValue.get(mSpEqualizer.getSelectedItem().toString()));
            if (mPlayerAppSetting != null) {
                mPlayerAppSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER,
                    mPasValue.get(mSpEqualizer.getSelectedItem().toString()));
            }

        } else if (parent == mSpRepeat) {
            Log.d(TAG, "mSpRepeat, set to " + mSpRepeat.getSelectedItem().toString() +
                " " + mPasValue.get(mSpRepeat.getSelectedItem().toString()));
            if (mPlayerAppSetting != null) {
                mPlayerAppSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_REPEAT,
                    mPasValue.get(mSpRepeat.getSelectedItem().toString()));
            }

        } else if (parent == mSpShuffle) {
            Log.d(TAG, "mSpShuffle, set to " + mSpShuffle.getSelectedItem().toString() +
                mPasValue.get(mSpShuffle.getSelectedItem().toString()));
            if (mPlayerAppSetting != null) {
                mPlayerAppSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE,
                    mPasValue.get(mSpShuffle.getSelectedItem().toString()));
            }

        } else if (parent == mSpScan) {
            Log.d(TAG, "mSpScan, set to " + mSpScan.getSelectedItem().toString() +
                " " + mPasValue.get(mSpScan.getSelectedItem().toString()));
            if (mPlayerAppSetting != null) {
                mPlayerAppSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SCAN,
                    mPasValue.get(mSpScan.getSelectedItem().toString()));
            }

        } else {
            Log.e(TAG, "Unknown item");
            result = false;
        }
        if (result) {
            Log.d(TAG, "setPlayerApplicationSetting");
            mProfileService.getAvrcpController().setPlayerApplicationSetting(mPlayerAppSetting);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void initPasMaps() {
        mPasText.put(BluetoothAvrcpPlayerSettings.STATE_OFF, "Off");
        mPasText.put(BluetoothAvrcpPlayerSettings.STATE_ON, "On");
        mPasText.put(BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK, "Single");
        mPasText.put(BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK, "All");
        mPasText.put(BluetoothAvrcpPlayerSettings.STATE_GROUP, "Group");

        mPasValue.put("Off", BluetoothAvrcpPlayerSettings.STATE_OFF);
        mPasValue.put("On", BluetoothAvrcpPlayerSettings.STATE_ON);
        mPasValue.put("Single", BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK);
        mPasValue.put("All", BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK);
        mPasValue.put("Group", BluetoothAvrcpPlayerSettings.STATE_GROUP);
    }

    private void setSelectionByString(Spinner spinner, String setting) {
        Log.d(TAG," set " + setting);
        SpinnerAdapter adapter = spinner.getAdapter();
        int count= adapter.getCount();
        for(int i = 0; i < count; i++) {
            if(setting.equals(adapter.getItem(i).toString())) {
                Log.d(TAG," set " + i);
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void updatePlayerAppSettingUI(BluetoothAvrcpPlayerSettings mPlayerAppSetting) {
        String text;
        if (mPlayerAppSetting == null) {
            Log.e(TAG,"mPlayerAppSetting is null");
            return;
        }
        int supportedSetting = mPlayerAppSetting.getSettings();
        Log.d(TAG," setting: " + supportedSetting);
        mSpEqualizer.setEnabled(false);
        mSpRepeat.setEnabled(false);
        mSpShuffle.setEnabled(false);
        mSpScan.setEnabled(false);
        if ((supportedSetting & BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER) != 0) {
            TextView avrcp_equalizer_value = (TextView) findViewById(R.id.id_avrcp_equalizer_value);
            text = mPasText.get(mPlayerAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.
                                                             SETTING_EQUALIZER));
            avrcp_equalizer_value.setText(text);
            mSpEqualizer.setEnabled(true);
        }
        if ((supportedSetting & BluetoothAvrcpPlayerSettings.SETTING_REPEAT) != 0) {
            TextView avrcp_repeat_value = (TextView) findViewById(R.id.id_avrcp_repeat_value);
            text = mPasText.get(mPlayerAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.
                                                             SETTING_REPEAT));
            avrcp_repeat_value.setText(text);
            mSpRepeat.setEnabled(true);
        }
        if ((supportedSetting & BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE) != 0) {
            TextView avrcp_shuffle_value = (TextView) findViewById(R.id.id_avrcp_shuffle_value);
            text = mPasText.get(mPlayerAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.
                                                             SETTING_SHUFFLE));
            avrcp_shuffle_value.setText(text);
            mSpShuffle.setEnabled(true);
        }
        if ((supportedSetting & BluetoothAvrcpPlayerSettings.SETTING_SCAN) != 0) {
            TextView avrcp_scan_value = (TextView) findViewById(R.id.id_avrcp_scan_value);
            text = mPasText.get(mPlayerAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.
                                                             SETTING_SCAN));
            avrcp_scan_value.setText(text);
            mSpScan.setEnabled(true);
        }

    }

    private void sendCommand() {
        if (mProfileService == null) {
            Log.d(TAG, " Service not connected ");
            return;
        }
        String status = mBtnPlayPause.getText().toString().trim();
        try {
            if (status.equals(STATUS_PLAY)) {

                mProfileService.sendPlay();
                mBtnPlayPause.setText(STATUS_PAUSE);

            } else if (status.equals(STATUS_PAUSE)) {

                mProfileService.sendPause();
                mBtnPlayPause.setText(STATUS_PLAY);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void sendFastForward(boolean pressed) {
        if (mProfileService == null) {
            Log.e(TAG, " Service not connected ");
            return;
        }

        try {
            mProfileService.sendFastForward(pressed);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void sendRewind(boolean pressed) {
        if (mProfileService == null) {
            Log.e(TAG, " Service not connected ");
            return;
        }

        try {
            mProfileService.sendRewind(pressed);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void sendSearch(String searchQuery) {
        if (mProfileService == null) {
            Log.e(TAG, " Service not connected ");
            return;
        }

        try {
            mProfileService.sendSearch(searchQuery);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void sendGetSupportedFeatures(BluetoothDevice device) {
        if (mProfileService == null) {
            Log.e(TAG, " Service not connected ");
            return;
        }

        try {
            mProfileService.sendGetSupportedFeatures(device);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void handleActionSupportedFeatures(Intent intent) {
        BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        int features = (int) intent.getExtra(EXTRA_SUPPORTED_FEATURES);
        Log.i(TAG, "Device: " + device + ", AVRCP supported features: " + features);

        String val = "AVRCP Features: " + features + " (";

        if (features != 0) {
            if ((features & BTRC_FEAT_METADATA) != 0) {
                val += " metadata, ";
            }

            if ((features & BTRC_FEAT_ABSOLUTE_VOLUME) != 0) {
                val += " absolute_volume, ";
            }

            if ((features & BTRC_FEAT_BROWSE) != 0) {
                val += " browse, ";
            }

            if ((features & BTRC_FEAT_COVER_ART) != 0) {
                val += " cover_art, ";
            }
        } else {
            val += "none";
        }

        val += ")";

        TextView avrcpSupportedFeatures = (TextView) findViewById(R.id.id_supported_features);
        avrcpSupportedFeatures.setText(val);
    }
}
