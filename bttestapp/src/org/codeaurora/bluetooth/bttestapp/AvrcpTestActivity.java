/*
 * Copyright (c) 2013-2015, The Linux Foundation. All rights reserved.
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

import android.app.ActionBar;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.view.View;
import android.view.View.OnTouchListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.content.ContentResolver;
import android.os.Handler;
import android.os.Process;
import android.widget.TextView;
import android.app.Activity;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import java.util.concurrent.TimeUnit;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.codeaurora.bluetooth.bttestapp.R;
import org.codeaurora.bluetooth.bttestapp.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AvrcpTestActivity extends MonkeyActivity implements IBluetoothConnectionObserver {

    private final String TAG = "AvrcpTestActivity";
    private boolean ffPressed = false;

    private ActionBar mActionBar = null;
    private PressandHoldHandler mPressandHoldHandler;
    private View appView;
    private Activity mLocalActivity;
    private final ReentrantLock mLock = new ReentrantLock();
    BluetoothAvrcpController mAvrcpController;
    ProfileService mProfileService = null;
    BluetoothDevice mDevice;
    private class PlayerSettings
    {
        public byte attr_Id;
        public byte attr_val;
        public byte [] supported_values; // app shld check these values before Setting Player Attributes.
    };

    public static final int SEND_PASS_THROUGH_CMD = 1;
    public static final int FETCH_CURRENT_INFO = 2;
    public static final int PTS_GET_ELEMENT_ATTRIBUTE_ID = 0x71;
    public static final int PTS_GET_PLAY_STATUS_ID       = 0x72;

    private TextView mRepeatStatus;
    private TextView mShuffleStatus;
    private TextView mGenreStatus;
    private TextView mArtistName;
    private TextView mAlbumName;
    private TextView mPlayTime;
    private TextView mScanStatus;
    private TextView mPlayStatus;
    private TextView mEqualizerStatus;
    private TextView mTrackNumber;
    private TextView mTitleName;
    private Button ffButton;
    private Button rwButton;
    private ImageView mCoverArtImageView;

    private String repeatText;
    private String shuffleText;
    private String genreText;
    private String artistText;
    private String albumText;
    private String playText;
    private String scanText;
    private String playStatusText;
    private String equalizerText;
    private String trackNumText;
    private String titleNameText;
    private long   trackLen;
    private Bitmap coverArtBitmap;

    private final BroadcastReceiver mAvrcpControllerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED)) {
                BluetoothDevice device = (BluetoothDevice)
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                onReceiveActionConnectionStateChanged(device, prevState, state, intent.getExtras());
            }
            if (action.equals(BluetoothAvrcpController.ACTION_TRACK_EVENT)) {
                MediaMetadata mMetaData = (MediaMetadata)
                        intent.getParcelableExtra(BluetoothAvrcpController.EXTRA_METADATA);

                PlaybackState mState = (PlaybackState)
                        intent.getParcelableExtra(BluetoothAvrcpController.EXTRA_PLAYBACK);
                if(mMetaData != null)
                    onMetaDataChanged(mMetaData);
                if(mState != null)
                    onPlaybackStateChanged(mState);
            }
            if (action.equals(BluetoothAvrcpController.ACTION_PLAYER_SETTING)) {
                BluetoothAvrcpPlayerSettings plAppSett = (BluetoothAvrcpPlayerSettings)
                        intent.getParcelableExtra(BluetoothAvrcpController.EXTRA_PLAYER_SETTING);
                if(plAppSett != null)
                    onPlayerAppSettingChanged(plAppSett);
            }
        }
        private void onMetaDataChanged(MediaMetadata mMetaData) {
            parseMetaData(mMetaData);
            Log.d(TAG," onMetaDataChanged Title " + titleNameText + " Artist " + artistText +
                  " genre "+ genreText + " album " + albumText + " TrackNum " + trackNumText);
            displayMetaData();
        }
        private void onPlaybackStateChanged(PlaybackState mState) {
            parsePlaybackState(mState);
            Log.d(TAG," onPlaybackStateCHanged playstatus" + playStatusText + " playTime "
                                                + playText);
            displayPlayState();
        }
        private void onPlayerAppSettingChanged(BluetoothAvrcpPlayerSettings mPlAppSett) {
            Log.d(TAG," onPlayerAppSettingChanged ");
            parsePlayerAppSetting(mPlAppSett);
            displayPlayeAppSetting();
        }
        private void onReceiveActionConnectionStateChanged(BluetoothDevice device,
                int prevState, int state, Bundle features) {
            Logger.v(TAG, "onReceiveActionConnectionStateChanged: AVRCP: " +
                    device.getAddress() + " (" +
                    String.valueOf(prevState) + " -> " +
                    String.valueOf(state) + ")");
            if (state ==  BluetoothProfile.STATE_DISCONNECTED) {
                if (device.equals(mDevice))
                    mDevice = null;
                resetDisplay();
                Toast.makeText(mLocalActivity, "Device " + device + " AVRCP Disconnected", Toast.LENGTH_SHORT).show();
            }
            else if(state == BluetoothProfile.STATE_CONNECTED) {
                mDevice = device;
                if(mAvrcpController!= null) {
                    parseMetaData(mAvrcpController.getMetadata(mDevice));
                    displayMetaData();
                    parsePlaybackState(mAvrcpController.getPlaybackState(mDevice));
                    displayPlayState();
                    parsePlayerAppSetting(mAvrcpController.getPlayerSettings(mDevice));
                    displayPlayeAppSetting();
                    int pixel = SystemProperties.getInt("persist.bt.avrcp.ca.pixel", 500);
                    mAvrcpController.startFetchingAlbumArt("JPEG", pixel, pixel, 2000000);
                }
                Toast.makeText(mLocalActivity, "Device " + device + " AVRCP Connected", Toast.LENGTH_SHORT).show();
            }
        }
    };
    private View.OnTouchListener onTouchListenerRW = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent me) {
            v.onTouchEvent(me);
            Log.d(TAG," onTouch for RW " + me.getAction());
            if (me.getAction() == MotionEvent.ACTION_UP){
                if ((mAvrcpController != null) && mDevice != null &&
                        BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
                        //mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_REWIND, KEY_STATE_RELEASED);
                    if (mPressandHoldHandler != null)
                        mPressandHoldHandler.sendMessageAtFrontOfQueue(mPressandHoldHandler.
                         obtainMessage(SEND_PASS_THROUGH_CMD,BluetoothAvrcpController.
                         PASS_THRU_CMD_ID_REWIND,BluetoothAvrcpController.KEY_STATE_RELEASED));
                    } else {
                        Logger.e(TAG, "passthru command not sent, connection unavailable");
                    }
            }
            else if (me.getAction() == MotionEvent.ACTION_DOWN) {
                if ((mAvrcpController != null) && mDevice != null &&
                        BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
                        //mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_REWIND, KEY_STATE_PRESSED);
                	if ((mPressandHoldHandler != null)&&(!mPressandHoldHandler.hasMessages(SEND_PASS_THROUGH_CMD)))
                        mPressandHoldHandler.sendMessage(mPressandHoldHandler.
                         obtainMessage(SEND_PASS_THROUGH_CMD,BluetoothAvrcpController.
                          PASS_THRU_CMD_ID_REWIND,BluetoothAvrcpController.KEY_STATE_PRESSED));
                    } else {
                        Logger.e(TAG, "passthru command not sent, connection unavailable");
                    }
            }
            return true;
        }
    };
    private View.OnTouchListener onTouchListenerFF = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent me) {
            v.onTouchEvent(me);
            Log.d(TAG," onTouch for FF " + me.getAction());
            if (me.getAction() == MotionEvent.ACTION_UP){
                if ((mAvrcpController != null) && mDevice != null &&
                        BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
                        //mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_FF, KEY_STATE_RELEASED);
                        if (mPressandHoldHandler != null)
                            mPressandHoldHandler.sendMessageAtFrontOfQueue(mPressandHoldHandler.
                             obtainMessage(SEND_PASS_THROUGH_CMD,BluetoothAvrcpController.
                                PASS_THRU_CMD_ID_FF,BluetoothAvrcpController.KEY_STATE_RELEASED));
                    } else {
                        Logger.e(TAG, "passthru command not sent, connection unavailable");
                    }
            }
            else if (me.getAction() == MotionEvent.ACTION_DOWN) {
                if ((mAvrcpController != null) && mDevice != null &&
                        BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
                        if ((mPressandHoldHandler != null)&&(!mPressandHoldHandler.hasMessages(SEND_PASS_THROUGH_CMD)))
                            mPressandHoldHandler.sendMessage(mPressandHoldHandler.
                                 obtainMessage(SEND_PASS_THROUGH_CMD,BluetoothAvrcpController.
                                    PASS_THRU_CMD_ID_FF,BluetoothAvrcpController.KEY_STATE_PRESSED));
                        //mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_FF, KEY_STATE_PRESSED);
                    } else {
                        Logger.e(TAG, "passthru command not sent, connection unavailable");
                    }
            }
            return true;
        }
    };
    private final class PressandHoldHandler extends Handler {
        private PressandHoldHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG," Recvd Msg " + msg.what);
            int keyState = msg.arg2;
            int keyCode = msg.arg1;
            switch(msg.what) {
            case SEND_PASS_THROUGH_CMD:
                if (keyState == BluetoothAvrcpController.KEY_STATE_PRESSED) {
                    Message msgsend = mPressandHoldHandler.obtainMessage(SEND_PASS_THROUGH_CMD, keyCode, keyState);
                    mPressandHoldHandler.sendMessageDelayed(msgsend, 1000);
                    if ((mAvrcpController != null) && mDevice != null &&
                            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
                            mAvrcpController.sendPassThroughCmd(mDevice, keyCode, keyState);
                    }
                }
                else if(keyState == BluetoothAvrcpController.KEY_STATE_RELEASED) {
                    if (mPressandHoldHandler.hasMessages(SEND_PASS_THROUGH_CMD))
                        mPressandHoldHandler.removeMessages(SEND_PASS_THROUGH_CMD);
                    if ((mAvrcpController != null) && mDevice != null &&
                            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
                            mAvrcpController.sendPassThroughCmd(mDevice, keyCode, keyState);
                    }
                }
                break;
            case FETCH_CURRENT_INFO:
                if(mAvrcpController != null) {
                    int pixel = SystemProperties.getInt("persist.bt.avrcp.ca.pixel", 500);
                    mAvrcpController.startFetchingAlbumArt("JPEG", pixel, pixel, 2000000);
                    List<BluetoothDevice> deviceList = mAvrcpController.getConnectedDevices();
                    if(deviceList.isEmpty()) break;
                    /* Right now we support only one connection */
                    mDevice = deviceList.get(0);
                    parseMetaData(mAvrcpController.getMetadata(mDevice));
                    displayMetaData();
                    parsePlaybackState(mAvrcpController.getPlaybackState(mDevice));
                    displayPlayState();
                    parsePlayerAppSetting(mAvrcpController.getPlayerSettings(mDevice));
                    displayPlayeAppSetting();
                }
                break;
            }
        }
    }

    private final ServiceConnection mAvrcpControllerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.v(TAG, "onServiceConnected()");
            mProfileService = ((ProfileService.LocalBinder) service).getService();
            mAvrcpController = mProfileService.getAvrcpController();
            mPressandHoldHandler.sendEmptyMessageDelayed(FETCH_CURRENT_INFO, 200);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.v(TAG, "onServiceDisconnected()");
            mProfileService = null;
            mAvrcpController = null;
            mDevice = null;
            resetDisplay();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.v(TAG, "onCreate()");

        ActivityHelper.initialize(this, R.layout.activity_avrcp_test);
        BluetoothConnectionReceiver.registerObserver(this);
        ActivityHelper.setActionBarTitle(this, R.string.title_avrcp_test);
        mLocalActivity = this;
        initializeViewFragments();
        // bind to app service
        HandlerThread thread = new HandlerThread("BT-TestAppPressandHoldHandler");
        thread.start();
        Looper looper = thread.getLooper();
        mPressandHoldHandler = new PressandHoldHandler(looper);
        Intent intent = new Intent(this, ProfileService.class);
        bindService(intent, mAvrcpControllerServiceConnection, BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAvrcpController.ACTION_TRACK_EVENT);
        filter.addAction(BluetoothAvrcpController.ACTION_PLAYER_SETTING);
        registerReceiver(mAvrcpControllerReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        Logger.v(TAG, "onDestroy");
        mDevice = null;
        unregisterReceiver(mAvrcpControllerReceiver);
        unbindService(mAvrcpControllerServiceConnection);
        BluetoothConnectionReceiver.removeObserver(this);
        mPressandHoldHandler.removeCallbacksAndMessages(null);
        Looper looper = mPressandHoldHandler.getLooper();
        if (looper != null) {
            looper.quit();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Logger.v(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Logger.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mActionBarMenu = menu;
        return true;
    }

    @Override
    public void onDeviceChanged(BluetoothDevice device) {
        Logger.v(TAG, "onDeviceChanged() BD = "+ device.getAddress());
        mDevice = device;
    }

    @Override
    public void onDeviceDisconected() {
        Logger.v(TAG, "onDeviceDisconected");
        mDevice = null;
    }

    private void prepareActionBar() {
        Logger.v(TAG, "prepareActionBar()");

        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setTitle(R.string.title_avrcp_test);
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
    }
    public boolean isDeviceConnected() {
        return ((mAvrcpController != null) && mDevice != null &&
                BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice)));
    }

    public void onClickPassthruPlay(View v) {
        Logger.v(TAG, "onClickPassthruPlay()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice,BluetoothAvrcpController.
                          PASS_THRU_CMD_ID_PLAY, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                          PASS_THRU_CMD_ID_PLAY, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }
    }
    public void onClickNextGroup(View v) {
        Logger.v(TAG, "onClickNextGroup()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendGroupNavigationCmd(mDevice, BluetoothAvrcpController.
                   PASS_THRU_CMD_ID_NEXT_GRP, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendGroupNavigationCmd(mDevice, BluetoothAvrcpController.
                   PASS_THRU_CMD_ID_NEXT_GRP, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "grp nav command not sent, connection unavailable");
        }
    }
    public void onClickPrevGroup(View v) {
        Logger.v(TAG, "onClickPrevGroup()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendGroupNavigationCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_PREV_GRP, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendGroupNavigationCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_PREV_GRP, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "grp nav command not sent, connection unavailable");
        }
    }
    public void onClickPassthruGetElementAttributes(View v) {
        Logger.v(TAG, "onClickPassthruGetElementAttributes()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, PTS_GET_ELEMENT_ATTRIBUTE_ID,
                BluetoothAvrcpController.KEY_STATE_PRESSED);
        } else {
            Logger.e(TAG, "onClickPassthruGetElementAttributes not sent, connection unavailable");
        }
    }
    public void onClickPassthruGetPlayStatus(View v) {
        Logger.v(TAG, "onClickPassthruGetPlayStatus()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, PTS_GET_PLAY_STATUS_ID,
                BluetoothAvrcpController.KEY_STATE_PRESSED);
        } else {
            Logger.e(TAG, "onClickPassthruGetElementAttributes not sent, connection unavailable");
        }
    }
    public void onClickPassthruForward(View v) {
        Logger.v(TAG, "onClickPassthruForward()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_FORWARD, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_FORWARD, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }
    }
    public void onClickPassthruBackward(View v) {
        Logger.v(TAG, "onClickPassthruBackward()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_BACKWARD, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_BACKWARD, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }
    }
    public void onClickToggleRepeat(View v) {
        Logger.v(TAG, "onClickToggleRepeat()");
        if ((mAvrcpController == null) ||
            (mDevice == null)||
            (BluetoothProfile.STATE_CONNECTED != (mAvrcpController.getConnectionState(mDevice)))) {
               return;
        }
        BluetoothAvrcpPlayerSettings mPlAppSetting = mAvrcpController.
                                                                   getPlayerSettings(mDevice);
        int setting = mPlAppSetting.getSettings();
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_REPEAT) == 0) {
            Log.e(TAG,"onClickToggleRepeat repeat not supported");
            return;
        }
        int value = mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_REPEAT);
        int nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
        boolean supported = false;
        do {
            switch(value) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_GROUP;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
                break;
            }
            BluetoothAvrcpPlayerSettings mNewRepeatSetting = new
                    BluetoothAvrcpPlayerSettings(BluetoothAvrcpPlayerSettings.SETTING_REPEAT);
            mNewRepeatSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_REPEAT,
                                                                                   nextVal);
            supported = mAvrcpController.setPlayerApplicationSetting(mNewRepeatSetting);
            value = nextVal;
        }while(!supported);
    }
    public void onClickToggleEq(View v) {
        Logger.v(TAG, "onClickToggleEq()");
        if ((mAvrcpController == null) ||
            (mDevice == null)||
            (BluetoothProfile.STATE_CONNECTED != (mAvrcpController.getConnectionState(mDevice)))) {
               return;
        }
        BluetoothAvrcpPlayerSettings mPlAppSetting = mAvrcpController.
                                            getPlayerSettings(mDevice);
        int setting = mPlAppSetting.getSettings();
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER) == 0) {
            Log.e(TAG,"onClickToggleEq equalizer not supported");
            return;
        }
        int value = mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER);
        int nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
        boolean supported = false;
        do {
            switch(value) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_ON;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ON:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
                break;
            }
            BluetoothAvrcpPlayerSettings mNewRepeatSetting = new
                    BluetoothAvrcpPlayerSettings(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER);
            mNewRepeatSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER,
                            nextVal);
            supported = mAvrcpController.setPlayerApplicationSetting(mNewRepeatSetting);
            value = nextVal;
        }while(!supported);
    }
    public void onClickToggleScan(View v) {
        Logger.v(TAG, "onClickToggleScan()");
        if ((mAvrcpController == null) ||
            (mDevice == null)||
            (BluetoothProfile.STATE_CONNECTED != (mAvrcpController.getConnectionState(mDevice)))) {
               return;
        }
        BluetoothAvrcpPlayerSettings mPlAppSetting = mAvrcpController.
                getPlayerSettings(mDevice);
        int setting = mPlAppSetting.getSettings();
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_SCAN) == 0) {
            Log.e(TAG,"onClickToggleScan Scan not supported");
            return;
        }
        int value = mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SCAN);
        int nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
        boolean supported = false;
        do {
            switch(value) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_GROUP;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
                break;
            }
            BluetoothAvrcpPlayerSettings mNewRepeatSetting = new
                    BluetoothAvrcpPlayerSettings(BluetoothAvrcpPlayerSettings.SETTING_SCAN);
            mNewRepeatSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SCAN,
                                        nextVal);
            supported = mAvrcpController.setPlayerApplicationSetting(mNewRepeatSetting);
            value = nextVal;
        }while(!supported);
    }
    public void onClickToggleShuffle(View v) {
        Logger.v(TAG, "onClickToggleShuffle()");
        if ((mAvrcpController == null) ||
            (mDevice == null)||
            (BluetoothProfile.STATE_CONNECTED != (mAvrcpController.getConnectionState(mDevice)))) {
               return;
        }
        BluetoothAvrcpPlayerSettings mPlAppSetting = mAvrcpController.
                getPlayerSettings(mDevice);
        int setting = mPlAppSetting.getSettings();
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE) == 0) {
            Log.e(TAG,"onClickToggleShuffle Shuffle not supported");
            return;
        }
        int value = mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE);
        int nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
        boolean supported = false;
        do {
            switch(value) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_GROUP;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
                break;
            }
            BluetoothAvrcpPlayerSettings mNewRepeatSetting = new
                    BluetoothAvrcpPlayerSettings(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE);
            mNewRepeatSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE,
                                        nextVal);
            supported = mAvrcpController.setPlayerApplicationSetting(mNewRepeatSetting);
            value = nextVal;
        }while(!supported);
    }

    public void onClickPassthruPause(View v) {
        Logger.v(TAG, "onClickPassthruPause()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_PAUSE, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_PAUSE, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }
    }

    public void onClickPassthruStop(View v) {
        Logger.v(TAG, "onClickPassthruStop()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_STOP, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_STOP, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }

    }

    private void parseMetaData(MediaMetadata mMetaData) {
        if(mMetaData == null) return;
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_ARTIST))
            artistText = mMetaData.getString(MediaMetadata.METADATA_KEY_ARTIST);
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_TITLE))
            titleNameText = mMetaData.getString(MediaMetadata.METADATA_KEY_TITLE);
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_ALBUM))
            albumText = mMetaData.getString(MediaMetadata.METADATA_KEY_ALBUM);
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_GENRE))
            genreText = mMetaData.getString(MediaMetadata.METADATA_KEY_GENRE);

        StringBuffer trackNumBuffer = new StringBuffer();
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_TRACK_NUMBER))
            trackNumBuffer.append(String.valueOf(mMetaData.getLong(MediaMetadata.
                                                                METADATA_KEY_TRACK_NUMBER)));
        trackNumBuffer.append(" | ");
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_NUM_TRACKS))
            trackNumBuffer.append(String.valueOf(mMetaData.getLong(MediaMetadata.
                                                               METADATA_KEY_NUM_TRACKS)));
        trackNumText = trackNumBuffer.toString();

        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_DURATION))
            trackLen = mMetaData.getLong(MediaMetadata.METADATA_KEY_DURATION);
        /*
         * Image is given preference over thumbnail
         */
        if (mMetaData.containsKey(MediaMetadata.METADATA_KEY_DISPLAY_ICON)) {
            Log.d(TAG," ParseMetaData, update Thumbnail");
            coverArtBitmap = mMetaData.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        }
        else if (mMetaData.containsKey(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)) {
            Log.d(TAG," ParseMetaData, update IMAGE");
            String mImageLocation = mMetaData.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
            coverArtBitmap = BitmapFactory.decodeFile(mImageLocation);
        }
        else
            coverArtBitmap = null;
    }
    private void parsePlaybackState(PlaybackState mState) {
        if(mState == null) return;
        playStatusText = "NONE";
        switch(mState.getState()) {
        case PlaybackState.STATE_STOPPED:
            playStatusText = "STOPPED";
            break;
        case PlaybackState.STATE_PLAYING:
            playStatusText = "PLAYING";
            break;
        case PlaybackState.STATE_PAUSED:
            playStatusText = "PAUSED";
            break;
        case PlaybackState.STATE_FAST_FORWARDING:
            playStatusText = "FORWARDING";
            break;
        case PlaybackState.STATE_REWINDING:
            playStatusText = "REWINDING";
            break;
        }

        long playing_time = mState.getPosition();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(playing_time);
        playing_time = playing_time - (60*minutes*1000);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(playing_time);
        StringBuffer strPlayTime = new StringBuffer();
        strPlayTime.append(String.valueOf(minutes));
        strPlayTime.append(":");
        strPlayTime.append(String.valueOf(seconds));
        strPlayTime.append(" | ");
        long totalTrackTime = trackLen;
        minutes = TimeUnit.MILLISECONDS.toMinutes(totalTrackTime);
        totalTrackTime = totalTrackTime - (60*minutes*1000);
        seconds = TimeUnit.MILLISECONDS.toSeconds(totalTrackTime);
        strPlayTime.append(String.valueOf(minutes));
        strPlayTime.append(":");
        strPlayTime.append(String.valueOf(seconds));
        playText = strPlayTime.toString();

    }
    private void parsePlayerAppSetting(BluetoothAvrcpPlayerSettings mPlAppSetting) {
        if(mPlAppSetting == null) return;
        int setting = mPlAppSetting.getSettings();
        Log.d(TAG," parsePlayerAppSetting Sett" + setting);
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER) != 0) {
            Log.d(TAG," parsePlayerAppSetting value eq:" + mPlAppSetting.
                 getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER));
            switch(mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER)) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                equalizerText = "EQ_OFF";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ON:
                equalizerText = "EQ_ON";
                break;
            }
        }
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_REPEAT) != 0) {
            Log.d(TAG," parsePlayerAppSetting value rep:" + mPlAppSetting.getSettingValue
                    (BluetoothAvrcpPlayerSettings.SETTING_REPEAT));
            switch(mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_REPEAT)) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                repeatText = "REP_OFF";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK:
                repeatText = "REP_SINGLE_TRACK";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                repeatText = "REP_ALL_TRACK";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                repeatText = "REP_GRP";
                break;
            }
        }
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE) != 0) {
            Log.d(TAG," parsePlayerAppSetting value shuffle:" + mPlAppSetting.getSettingValue
                   (BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE));
            switch(mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE)) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                shuffleText = "SHUFFLE_OFF";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                shuffleText = "SHUFFLE_ALL";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                shuffleText = "SHUFFLE_GRP";
                break;
            }
        }
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_SCAN) != 0) {
            Log.d(TAG," parsePlayerAppSetting value scan:" + mPlAppSetting.getSettingValue
                    (BluetoothAvrcpPlayerSettings.SETTING_SCAN));
            switch(mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SCAN)) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                scanText = "SCAN_OFF";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                scanText = "SCAN_ALL";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                scanText = "SCAN_GRP";
                break;
            }
        }
    }
    private void displayMetaData() {
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    mTrackNumber.setText(trackNumText);
                    mTitleName.setText(titleNameText);
                    mArtistName.setText(artistText);
                    mGenreStatus.setText(genreText);
                    mAlbumName.setText(albumText);
                    if (coverArtBitmap != null) {
                        mCoverArtImageView.setImageBitmap(coverArtBitmap);
                    }
                    else {
                        Log.d(TAG," displayMetaData: bitmap is null, invalidate it ");
                        mCoverArtImageView.setImageBitmap(null);
                        mCoverArtImageView.invalidate();
                    }
                }
                finally {
                    mLock.unlock();
                }
            }
        });

    }
    private void displayPlayState() {
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    mPlayStatus.setText(playStatusText);
                    mPlayTime.setText(playText);
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }
    private void displayPlayeAppSetting() {
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    mEqualizerStatus.setText(equalizerText);
                    mScanStatus.setText(scanText);
                    mShuffleStatus.setText(shuffleText);
                    mRepeatStatus.setText(repeatText);
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }
    private void resetDisplay() {
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                trackNumText = "NOT_SUPP";
                titleNameText = "NOT_SUPP";
                equalizerText = "NOT_SUPP";
                artistText = "NOT_SUPP";
                scanText = "NOT_SUPP";
                shuffleText = "NOT_SUPP";
                repeatText = "NOT_SUPP";
                playStatusText = "NOT_SUPP";
                genreText = "NOT_SUPP";
                playText = "NOT_SUPP";
                albumText = "NOT_SUPP";
                trackLen = 0;
                coverArtBitmap = null;
                try {
                    mTrackNumber.setText(trackNumText);
                    mTitleName.setText(titleNameText);
                    mEqualizerStatus.setText(equalizerText);
                    mArtistName.setText(artistText);
                    mScanStatus.setText(scanText);
                    mShuffleStatus.setText(shuffleText);
                    mRepeatStatus.setText(repeatText);
                    mPlayStatus.setText(playStatusText);
                    mGenreStatus.setText(genreText);
                    mPlayTime.setText(playText);
                    mAlbumName.setText(albumText);
                    mCoverArtImageView.setImageBitmap(null);
                    mCoverArtImageView.invalidate();
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }
    private void initializeViewFragments() {
         Log.v(TAG,"initializeViewFragments");
         mShuffleStatus = (TextView) findViewById(R.id.shuffle_status);
         mRepeatStatus = (TextView) findViewById(R.id.repeat_status);
         mGenreStatus = (TextView) findViewById(R.id.genre_name);
         mArtistName = (TextView) findViewById(R.id.artist_name);
         mAlbumName = (TextView) findViewById(R.id.album_name);
         mPlayTime = (TextView) findViewById(R.id.playing_time);
         mScanStatus = (TextView) findViewById(R.id.scan_status);
         mPlayStatus = (TextView) findViewById(R.id.play_status);
         mEqualizerStatus = (TextView) findViewById(R.id.equalizer_status);
         mTrackNumber = (TextView) findViewById(R.id.track_number);
         mTitleName = (TextView) findViewById(R.id.title_name);
         ffButton = (Button) findViewById(R.id.onClickPassthruFF);
         rwButton = (Button) findViewById(R.id.onClickPassthruRewind);
         mCoverArtImageView = (ImageView) findViewById(R.id.cover_art_image_view);
         Log.d(TAG," call setImageBitmap ");
         mCoverArtImageView.setImageBitmap(null);
         ffButton.setOnTouchListener(onTouchListenerFF);
         rwButton.setOnTouchListener(onTouchListenerRW);
    }
}
