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
import org.codeaurora.bluetooth.bttestapp.AvrcpProfile;

import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothAudioConfig;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.session.PlaybackState;
import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.AudioFormat;
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
import android.widget.RadioGroup;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class AvrcpTestActivity extends MonkeyActivity implements
    OnClickListener, IBluetoothConnectionObserver, OnItemSelectedListener,
    OnTouchListener {

    private final String TAG = "AvrcpTestActivity";
    private Button mBtnPlayPause;
    private Button mBtnFastforward;
    private Button mBtnRewind;

    private EditText mEditTextQuery;

    private Button mBtnTestCmd;
    private Spinner mSpTestCmd;
    private EditText mEditTestResp;
    private RadioGroup mFolders;
    private EditText mEditItemPosition;

    private Button mBtnGetCurrentPas;
    private Spinner mSpEqualizer;
    private Spinner mSpRepeat;
    private Spinner mSpShuffle;
    private Spinner mSpScan;

    private final String STATUS_PLAY = "Play";
    private final String STATUS_PAUSE = "Pause";

    private AvrcpProfile mAvrcp;
    private BluetoothAvrcpPlayerSettings mPlayerAppSetting;
    private BluetoothDevice mDevice;

    // Hash for storing A2DP codec type
    private HashMap<BluetoothDevice, Integer> mA2dpCodecType = new HashMap<BluetoothDevice, Integer>();
    private boolean mGetItemAttr = false;
    private List<MediaBrowser.MediaItem> mNowPlayingItems = new ArrayList<>();
    private List<MediaBrowser.MediaItem> mSearchItems = new ArrayList<>();

    /*
     * Hash map. key: pas attribute value, value: pas attribute value in string
     */
    private Map<Integer, String> mPasText = new HashMap<Integer, String>();
    /*
     * Hash map. key: pas attribute value in string, value: pas attribute value
     */
    private Map<String, Integer> mPasValue = new HashMap<String, Integer>();

    private static final int FASTFORWARD_PRESSED = 0;
    private static final int REWIND_PRESSED = 1;

    private static final int TIMEOUT_IN_MS = 1000;

    private final ServiceConnection mAvrcpConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected()");
            mAvrcp = null;
            if (mBtnPlayPause != null) mBtnPlayPause.setText(STATUS_PLAY);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected()");
            ProfileService profileService = ((ProfileService.LocalBinder) service).getService();
            mAvrcp = profileService.getAvrcpProfile();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "action " + action);
            if (action.equals(AvrcpProfile.ACTION_TRACK_EVENT)) {
                handleActionTrackEvent(intent);
            } else if (action.equals(AvrcpProfile.ACTION_FOLDER_LIST)) {
                handleActionFolderList(intent);
            } else if (action.equals(AvrcpProfile.ACTION_NUM_OF_ITEMS)) {
                handleActionGetTotalNumOfItems(intent);
            } else if (action.equals(BluetoothAvrcpController.ACTION_PLAYER_SETTING)) {
                handleActionPlayerSetting(intent);
            } else if (action.equals(AvrcpProfile.ACTION_SUPPORTED_FEATURES)) {
                handleActionSupportedFeatures(intent);
            } else if (action.equals(BluetoothA2dpSink.ACTION_AUDIO_CONFIG_CHANGED)) {
                handleActionAudioConfigChanged(intent);
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == FASTFORWARD_PRESSED) {
                fastForward(true);
                sendEmptyMessageDelayed(FASTFORWARD_PRESSED, TIMEOUT_IN_MS);
            } else if (msg.what == REWIND_PRESSED) {
                rewind(true);
                sendEmptyMessageDelayed(REWIND_PRESSED, TIMEOUT_IN_MS);
            } else {
                Log.e(TAG, "Unknown msg: " + msg.what);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        ActivityHelper.initialize(this, R.layout.layout_avrcp);//use layout_avrcp.xml

        initPasMaps();

        mBtnPlayPause = initButton(R.id.id_btn_play_pause);
        mBtnFastforward = initButton(R.id.id_btn_fast_forward, null, this);
        mBtnRewind = initButton(R.id.id_btn_rewind, null, this);

        mBtnTestCmd = initButton(R.id.id_btn_test_cmd);
        mSpTestCmd = initSpinner(R.id.id_sp_test_cmd, 1);   // Default "GetItemAttributes"
        mEditTestResp = initEditText(R.id.id_test_resp, "");

        mFolders = (RadioGroup) findViewById(R.id.id_folders);
        mEditItemPosition = (EditText) initEditText(R.id.id_edit_item_position, "0"); // The 1st item
        mEditTextQuery = initEditText(R.id.id_edit_query, "You"); // Sample search/query string

        mBtnGetCurrentPas = initButton(R.id.id_btn_get_current_pas);
        mSpEqualizer = initSpinner(R.id.id_sp_equalizer);
        mSpRepeat = initSpinner(R.id.id_sp_repeat);
        mSpShuffle = initSpinner(R.id.id_sp_shuffle);
        mSpScan = initSpinner(R.id.id_sp_scan);

        BluetoothConnectionReceiver.registerObserver(this);

        // bind to app service
        Intent intent = new Intent(this, ProfileService.class);
        bindService(intent, mAvrcpConnection, BIND_AUTO_CREATE);

        initIntentFilter();

        Log.d(TAG, "onCreate leave");
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

        // Initialize A2DP codec type
        mA2dpCodecType.put(device, AvrcpProfile.UNKNOWN_CODEC_TYPE);
    }

    @Override
    public void onDeviceDisconected() {
        Logger.v(TAG, "onDeviceDisconected");

        invalidateOptionsMenu();
    }

    private void initIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AvrcpProfile.ACTION_TRACK_EVENT);
        filter.addAction(AvrcpProfile.ACTION_FOLDER_LIST);
        filter.addAction(AvrcpProfile.ACTION_NUM_OF_ITEMS);
        filter.addAction(BluetoothAvrcpController.ACTION_PLAYER_SETTING);
        filter.addAction(BluetoothA2dpSink.ACTION_AUDIO_CONFIG_CHANGED);
        filter.addAction(AvrcpProfile.ACTION_SUPPORTED_FEATURES);
        registerReceiver(mReceiver, filter);
    }

    private Button initButton(int id) {
        return initButton(id, this, null);
    }

    private Button initButton(int id, OnClickListener clickListener,
                              OnTouchListener touchListener) {
        Button btn = (Button) findViewById(id);
        if (btn != null) {
            if (clickListener != null) {
                btn.setOnClickListener(clickListener);
            }
            if (touchListener != null) {
                btn.setOnTouchListener(touchListener);
            }
            return btn;
        } else {
            Log.w(TAG, "initButton can't find id " + id);
            return null;
        }
    }

    private EditText initEditText(int id, String str) {
        EditText editText = (EditText) findViewById(id);
        if (editText != null) {
            if (str != null) {
                editText.setText(str);
            }
            editText.setVisibility(View.VISIBLE);
            return editText;
        } else {
            Log.w(TAG, "initEditText can't find id " + id);
            return null;
        }
    }

    private Spinner initSpinner(int id) {
        return initSpinner(id, 0);
    }

    private Spinner initSpinner(int id, int pos) {
        Spinner spinner = (Spinner) findViewById(id);
        if (spinner != null) {
            spinner.setOnItemSelectedListener(this);
            spinner.setSelection(pos, true);
            spinner.setEnabled(true);
            return spinner;
        } else {
            Log.w(TAG, "initSpinner can't find id " + id);
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mBtnPlayPause) {
            Log.d(TAG, "onClick mBtnPlayPause");
            sendCommand();
        } else if (v == mBtnTestCmd) {
            Log.d(TAG, "onClick mBtnTestCmd");
            handleClickBtnTestCmd();
        } else if (v == mBtnGetCurrentPas) {
            Log.d(TAG, "onClick mBtnGetCurrentPas");
            handleClickBtnGetCurrentPas();
        }
    }

    private void handleClickBtnTestCmd() {
        String testCmd = mSpTestCmd.getSelectedItem().toString();
        Log.d(TAG, "handleClickBtnTestCmd " + testCmd);

        if (testCmd.equals(this.getString(R.string.avrcp_add_to_now_playing))) {
            handleAddToNowPlaying();
        } else if (testCmd.equals(this.getString(R.string.avrcp_get_item_attr))) {
            handleGetItemAttributes();
        } else if (testCmd.equals(this.getString(R.string.avrcp_get_total_num))) {
            handleGetTotalNumOfItems();
        } else if (testCmd.equals(this.getString(R.string.avrcp_search))) {
            handleSearch();
        } else if (testCmd.equals(this.getString(R.string.avrcp_get_audio_config))) {
            handleGetAudioConfig();
        } else if (testCmd.equals(this.getString(R.string.avrcp_get_supported_features))) {
            handleGetSupportedFeatures();
        } else {
            Log.w(TAG, "unknown test cmd: " + testCmd);
        }
    }

    private void handleAddToNowPlaying() {
        int scope = getScope();
        int position = getItemPosition();
        Log.d(TAG, "handleGetItemAttributes, scope: " + scope + ", item position: " + position);

        // Clear edit text
        mEditTestResp.setText("");

        switch (scope) {
            case AvrcpProfile.BROWSE_SCOPE_VFS:
                Log.d(TAG, "Add item in vfs");
                // TODO
                break;
            case AvrcpProfile.BROWSE_SCOPE_SEARCH:
                Log.d(TAG, "Add item in search folder");
                // TODO
                break;

            case AvrcpProfile.BROWSE_SCOPE_NOW_PLAYING:
                Log.d(TAG, "Add item in now playing");
                // TODO
                break;

            default:
                Log.w(TAG, "Ignore to add item in scope: " + scope);
                break;
        }
    }

    private void handleGetItemAttributes() {
        int scope = getScope();
        int position = getItemPosition();
        Log.d(TAG, "handleGetItemAttributes, scope: " + scope + ", item position: " + position);

        // Clear edit text
        mEditTestResp.setText("");

        switch (scope) {
            case AvrcpProfile.BROWSE_SCOPE_SEARCH:
                Log.d(TAG, "Get item attributes in search folder");
                mGetItemAttr = true;
                getItemAttributes(mSearchItems, position);
                break;

            case AvrcpProfile.BROWSE_SCOPE_NOW_PLAYING:
                Log.d(TAG, "Get item attributes in now playing");
                mGetItemAttr = true;
                getItemAttributes(mNowPlayingItems, position);
                break;

            default:
                Log.w(TAG, "Ignore to get item attributes in scope: " + scope);
                break;
        }
    }

    private void handleGetTotalNumOfItems() {
        int scope = getScope();
        Log.d(TAG, "handleGetTotalNumOfItems scope: " + scope);

        // Clear edit text
        mEditTestResp.setText("");

        getTotalNumberOfItems(scope);
    }

    private void handleSearch() {
        // Clear edit text
        mEditTestResp.setText("");

        String query = mEditTextQuery.getText().toString();
        Log.d(TAG, "handleSearch, query: " + query);
        if ((query != null) && !query.isEmpty()) {
            search(query);
        } else {
            Log.w(TAG, "handleSearch, but query string empty");
        }

        mEditTestResp.setText("Find search result in Android Bluetooth Audio app");
    }

    private void handleGetAudioConfig() {
        Log.d(TAG, "handleGetAudioConfig device: " + mDevice);
        getAudioConfig(mDevice);
    }

    private void handleGetSupportedFeatures() {
        Log.d(TAG, "handleGetSupportedFeatures device: " + mDevice);
        getSupportedFeatures(mDevice);
    }

    private void handleClickBtnGetCurrentPas() {
        Log.d(TAG, "handleClickBtnGetCurrentPas ");

        mPlayerAppSetting = getPlayerSettings(mDevice);
        if (mPlayerAppSetting == null) {
            Log.e(TAG, "handleClickBtnGetCurrentPas player app setting null ");
            return;
        }

        Log.d(TAG, "handleClickBtnGetCurrentPas update player app setting");
        updatePlayerAppSettingUI(mPlayerAppSetting);
    }

    private int getScope() {
        int scope = 0;
        int btnId = mFolders.getCheckedRadioButtonId();

        switch (btnId) {
            case R.id.id_rb_player:
                scope = AvrcpProfile.BROWSE_SCOPE_PLAYER_LIST;
                break;

            case R.id.id_rb_vfs:
                scope = AvrcpProfile.BROWSE_SCOPE_VFS;
                break;

            case R.id.id_rb_search:
                scope = AvrcpProfile.BROWSE_SCOPE_SEARCH;
                break;

            case R.id.id_rb_now_playing:
                scope = AvrcpProfile.BROWSE_SCOPE_NOW_PLAYING;
                break;

            default:
                Log.w(TAG, "Unknown btnId: " + btnId);
                break;
        }

        Log.d(TAG,"getScope " + scope);
        return scope;
    }

    private int getItemPosition() {
        String str = mEditItemPosition.getText().toString();
        int position = 0;

        if ((str != null) && !str.isEmpty()) {
            position = Integer.parseInt(str);
        }

        return position;
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
                fastForward(true);
                mHandler.sendEmptyMessageDelayed(FASTFORWARD_PRESSED, TIMEOUT_IN_MS);
                break;

            case MotionEvent.ACTION_UP:
                Log.d(TAG, "BtnFastforward, key released");
                mHandler.removeMessages(FASTFORWARD_PRESSED);
                fastForward(false);
                break;

            default:
                break;
        }
    }

    private void handleTouchBtnRewind(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "BtnRewind, key pressed");
                rewind(true);
                mHandler.sendEmptyMessageDelayed(REWIND_PRESSED, TIMEOUT_IN_MS);
                break;

            case MotionEvent.ACTION_UP:
                Log.d(TAG, "BtnRewind, key released");
                mHandler.removeMessages(REWIND_PRESSED);
                rewind(false);
                break;

            default:
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "parent " + parent + " view " + view + " position " + position + " id " + id);

        // PAS
        mPlayerAppSetting = getPlayerSettings(mDevice);
        if (mPlayerAppSetting == null) {
            Log.e(TAG, "player app setting null ");
            return;
        }

        boolean result = true;
        if (parent == mSpEqualizer) {
            String equalizer = mSpEqualizer.getSelectedItem().toString(); 
            Log.d(TAG, "Set equalizer " + equalizer);
            addPasValue(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER, equalizer, mPlayerAppSetting);
        } else if (parent == mSpRepeat) {
            String repeat = mSpRepeat.getSelectedItem().toString();
            Log.d(TAG, "Set repeat " + repeat);
            addPasValue(BluetoothAvrcpPlayerSettings.SETTING_REPEAT, repeat, mPlayerAppSetting);
        } else if (parent == mSpShuffle) {
            String shuffle = mSpShuffle.getSelectedItem().toString();
            Log.d(TAG, "Set shuffle " + shuffle);
            addPasValue(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE, shuffle, mPlayerAppSetting);
        } else if (parent == mSpScan) {
            String scan = mSpScan.getSelectedItem().toString();
            Log.d(TAG, "Set scan " + scan);
            addPasValue(BluetoothAvrcpPlayerSettings.SETTING_SCAN, scan, mPlayerAppSetting);
        } else {
            Log.e(TAG, "Unknown item");
            result = false;
        }

        if (result) {
            Log.d(TAG, "setPlayerApplicationSetting");
            mAvrcp.setPlayerApplicationSetting(mPlayerAppSetting);
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
        for (int i = 0; i < count; i++) {
            if (setting.equals(adapter.getItem(i).toString())) {
                Log.d(TAG," set " + i);
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void updatePlayerAppSettingUI(BluetoothAvrcpPlayerSettings pas) {
        int supportedSetting = getSupportedSetting(pas);
        if (supportedSetting == 0) {
            Log.e(TAG,"updatePlayerAppSettingUI none supported setting");
            return;
        }

        Log.d(TAG,"updatePlayerAppSettingUI supported setting: " + supportedSetting);

        // Disable spinner
        mSpEqualizer.setEnabled(false);
        mSpRepeat.setEnabled(false);
        mSpShuffle.setEnabled(false);
        mSpScan.setEnabled(false);

        // Enable equalizer (if available)
        enableSpinner(mSpEqualizer, R.id.id_avrcp_equalizer_value,
                      BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER,
                      supportedSetting, pas);

        // Enable repeat (if available)
        enableSpinner(mSpRepeat, R.id.id_avrcp_repeat_value,
                      BluetoothAvrcpPlayerSettings.SETTING_REPEAT,
                      supportedSetting, pas);

        // Enable shuffle (if available)
        enableSpinner(mSpShuffle, R.id.id_avrcp_shuffle_value,
                      BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE,
                      supportedSetting, pas);

        // Enable scan (if available)
        enableSpinner(mSpScan, R.id.id_avrcp_scan_value,
                      BluetoothAvrcpPlayerSettings.SETTING_SCAN,
                      supportedSetting, pas);
    }

    private void sendCommand() {
        if (mAvrcp == null) {
            Log.d(TAG, " Service not connected ");
            return;
        }

        String status = mBtnPlayPause.getText().toString().trim();

        try {
            if (status.equals(STATUS_PLAY)) {
                mAvrcp.play();
                mBtnPlayPause.setText(STATUS_PAUSE);
            } else if (status.equals(STATUS_PAUSE)) {
                mAvrcp.pause();
                mBtnPlayPause.setText(STATUS_PLAY);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void fastForward(boolean pressed) {
        if (mAvrcp == null) {
            Log.e(TAG, " Service not connected ");
            return;
        }

        try {
            mAvrcp.fastForward(pressed);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void rewind(boolean pressed) {
        if (mAvrcp == null) {
            Log.e(TAG, " Service not connected ");
            return;
        }

        try {
            mAvrcp.rewind(pressed);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void search(String query) {
        if (mAvrcp == null) {
            Log.e(TAG, " Service not connected ");
            return;
        }

        try {
            mAvrcp.search(query);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void getSupportedFeatures(BluetoothDevice device) {
        if (mAvrcp == null) {
            Log.e(TAG, " Service not connected ");
            return;
        }

        try {
            mAvrcp.getSupportedFeatures(device);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private MediaBrowser.MediaItem getMediaItem(List<MediaBrowser.MediaItem> list, int position) {
        Log.d(TAG, "getMediaItem position: " + position);
        if ((list == null) || (position >= list.size())) {
            Log.w(TAG, "getMediaItem exceed max size " + list.size());
            return null;
        }

        return list.get(position);
    }

    private void getItemAttributes(List<MediaBrowser.MediaItem> list, int position) {
        Log.d(TAG, "getItemAttributes position: " + position);
        MediaBrowser.MediaItem item = getMediaItem(list, position);
        if (item == null) {
            Log.e(TAG, "getItemAttributes, MediaItem null");
            return;
        }

        getItemAttributes(item.getMediaId());
    }

    private void getItemAttributes(String mediaId) {
        if (mAvrcp == null) {
            Log.e(TAG, " Service not connected ");
            return;
        }

        try {
            mAvrcp.getItemAttributes(mediaId);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void getTotalNumberOfItems(int scope) {
        if (mAvrcp == null) {
            Log.e(TAG, " Service not connected ");
            return;
        }

        try {
            mAvrcp.getTotalNumberOfItems(scope);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void getAudioConfig(BluetoothDevice device) {
        if (mAvrcp == null) {
            Log.e(TAG, " Service not connected ");
            return;
        }

        try {
            mAvrcp.getAudioConfig(device);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void handleActionTrackEvent(Intent intent) {
        PlaybackState ps = intent.getParcelableExtra(AvrcpProfile.EXTRA_PLAYBACK);
        if ((ps != null) && (mBtnPlayPause != null)) {
            int state = ps.getState();
            Log.d(TAG, "handleActionTrackEvent state: " + state);
            if (state == PlaybackState.STATE_PAUSED ||
                state == PlaybackState.STATE_STOPPED) {
                mBtnPlayPause.setText(STATUS_PLAY);
            } else if (state == PlaybackState.STATE_PLAYING) {
                mBtnPlayPause.setText(STATUS_PAUSE);
            }
        }

        if (mGetItemAttr) {
            MediaMetadata mmd = intent.getParcelableExtra(AvrcpProfile.EXTRA_METADATA);
            showMediaMetadata(mmd);
            mGetItemAttr = false;
        }
    }

    private void handleActionFolderList(Intent intent) {
        String id = intent.getStringExtra(AvrcpProfile.EXTRA_FOLDER_ID);
        ArrayList<MediaItem> items = intent.getParcelableArrayListExtra(AvrcpProfile.EXTRA_FOLDER_LIST);
        Log.d(TAG, "handleActionFolderList folder " + id);

        if (AvrcpProfile.isPlayer(id)) {
            Log.d(TAG, "handleActionFolderList player items " + items);
            // TODO
        } else if (AvrcpProfile.isNowPlaying(id)) {
            Log.d(TAG, "handleActionFolderList now playing items " + items);
            storeMediaItems(mNowPlayingItems, items);
        } else if (AvrcpProfile.isSearch(id)) {
            Log.d(TAG, "handleActionFolderList search items " + items);
            storeMediaItems(mSearchItems, items);
        } else {
            // Ignore
        }
    }

    private void handleActionGetTotalNumOfItems(Intent intent) {
        int items = intent.getIntExtra(AvrcpProfile.EXTRA_NUM_OF_ITEMS, 0);
        Log.d(TAG, "handleActionGetTotalNumOfItems " + items);

        mEditTestResp.setText(Integer.toString(items));
    }

    private void handleActionPlayerSetting(Intent intent) {
        mPlayerAppSetting = intent.getParcelableExtra(BluetoothAvrcpController.EXTRA_PLAYER_SETTING);
        Log.d(TAG, "handleActionPlayerSetting mPlayerAppSetting: " + mPlayerAppSetting);
        updatePlayerAppSettingUI(mPlayerAppSetting);
    }

    private void handleActionSupportedFeatures(Intent intent) {
        BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        int features = (int) intent.getExtra(AvrcpProfile.EXTRA_SUPPORTED_FEATURES);
        Log.i(TAG, "Device: " + device + ", AVRCP supported features: " + features);

        String val = "AVRCP Features: " + features + " (";

        if (features != 0) {
            if ((features & AvrcpProfile.BTRC_FEAT_METADATA) != 0) {
                val += " metadata, ";
            }

            if ((features & AvrcpProfile.BTRC_FEAT_ABSOLUTE_VOLUME) != 0) {
                val += " absolute_volume, ";
            }

            if ((features & AvrcpProfile.BTRC_FEAT_BROWSE) != 0) {
                val += " browse, ";
            }

            if ((features & AvrcpProfile.BTRC_FEAT_COVER_ART) != 0) {
                val += " cover_art, ";
            }
        } else {
            val += "none";
        }

        val += ")";

        mEditTestResp.setText(val);
    }

    private void handleActionAudioConfigChanged(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        BluetoothAudioConfig audioConfig = intent.getParcelableExtra(BluetoothA2dpSink.EXTRA_AUDIO_CONFIG);
        int codecType = intent.getIntExtra(AvrcpProfile.EXTRA_CODEC_TYPE, BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC);

        Log.d(TAG, "handleActionAudioConfigChanged device: " + device + ", audioConfig: " +
            audioConfig + ", codecType: " + codecType);

        mA2dpCodecType.put(device, codecType);

        showA2dpCodec(audioConfig);
    }

    private int getSupportedSetting(BluetoothAvrcpPlayerSettings pas) {
        return pas != null ? pas.getSettings() : 0;
    }

    private BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice device) {
        return mAvrcp != null ? mAvrcp.getPlayerSettings(device) : null;
    }

    private void addPasValue(int setting, String valueString, BluetoothAvrcpPlayerSettings pas) {
        if (valueString != null) {
            int value = mPasValue.get(valueString);
            Log.d(TAG, "addPasValue setting: " + setting +", value: " + value + "(" + valueString + ")");

            int supportedSetting = getSupportedSetting(pas);

            if ((setting & supportedSetting) != 0) {
                pas.addSettingValue(setting, value);
            } else {
                Log.w(TAG, "addPasValue setting " + setting + "not in supported list " + supportedSetting);
            }
        } else {
            Log.w(TAG, "addPasValue value null");
        }
    }

    private void enableSpinner(Spinner spinner, int textViewId, int setting, int supportedSetting,
                               BluetoothAvrcpPlayerSettings pas) {
        if ((setting & supportedSetting) != 0) {
            TextView textView = (TextView) findViewById(textViewId);

            int value = pas.getSettingValue(setting);
            String text = mPasText.get(value);
            textView.setText(text);

            spinner.setEnabled(true);
        } else {
            Log.w(TAG, "enableSpinner setting " + setting + "not in supported list " + supportedSetting);
        }
    }

    private void storeMediaItems(List<MediaBrowser.MediaItem> dstList,
                                 List<MediaBrowser.MediaItem> srcList) {
        if ((dstList == null) || (srcList == null)) {
            return;
        }
        dstList.clear();
        dstList.addAll(srcList);
    }

    private void showA2dpCodec(BluetoothAudioConfig audioConfig) {
        if (audioConfig == null) {
            Log.e(TAG, "audioConfig null");
            return;
        }

        int sampleRate = audioConfig.getSampleRate();
        int channelConfig = audioConfig.getChannelConfig();
        int audioFormat = audioConfig.getAudioFormat();
        int codecType = AvrcpProfile.UNKNOWN_CODEC_TYPE;

        codecType = mA2dpCodecType.containsKey(mDevice) ?
                    mA2dpCodecType.get(mDevice) : AvrcpProfile.UNKNOWN_CODEC_TYPE;
        Log.d(TAG, "BluetoothAudioConfig codecType: " + codecType);

        String codecTypeString = "";
        switch (codecType) {
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC:
                codecTypeString = "sbc";
                break;
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC:
                codecTypeString = "aac";
                break;
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX:
                codecTypeString = "aptx";
                break;
            default:
                codecTypeString = "unknown";
                break;
        }

        String channelConfigString = "";
        switch (channelConfig) {
            case AudioFormat.CHANNEL_IN_STEREO:
                channelConfigString = "stereo";
                break;
            case AudioFormat.CHANNEL_IN_MONO:
                channelConfigString = "mono";
                break;
            default:
                channelConfigString = "unknown";
                break;
        }

        String audioFormatString = "";
        switch (audioFormat) {
            case AudioFormat.ENCODING_PCM_16BIT:
                audioFormatString = "pcm 16bit";
                break;
            case AudioFormat.ENCODING_PCM_8BIT:
                audioFormatString = "pcm 8bit";
                break;
            default:
                audioFormatString = "unknown";
                break;
        }

        String str = "A2DP Codec: type =  " + codecType +
                     " (" + codecTypeString + ")" +
                     ", sample_rate = " + sampleRate +
                     ", channel_config = " + channelConfig +
                     " (" + channelConfigString + ")" +
                     ", audio_format = " + audioFormat +
                     " (" + audioFormatString + ")";

        mEditTestResp.setText(str);
    }

    private void showMediaMetadata(MediaMetadata mmd) {
        if (mmd == null) {
            return;
        }

        Log.d(TAG, "showMediaMetadata " + mmd);

        String str = mmd.getString(MediaMetadata.METADATA_KEY_TITLE) + "  " +
                     mmd.getString(MediaMetadata.METADATA_KEY_ARTIST) + "  " +
                     mmd.getString(MediaMetadata.METADATA_KEY_ALBUM);
        mEditTestResp.setText(str);
    }
}
