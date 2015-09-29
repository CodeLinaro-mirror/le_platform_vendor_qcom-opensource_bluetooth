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
import android.bluetooth.BluetoothAvrcpInfo;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.view.View;
import android.view.View.OnTouchListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.content.ContentResolver;
import android.os.Handler;
import android.os.Process;
import android.net.Uri;
import android.widget.TextView;
import android.app.Activity;
import java.util.concurrent.TimeUnit;

import org.codeaurora.bluetooth.bttestapp.R;
import org.codeaurora.bluetooth.bttestapp.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteException;

public class AvrcpTestActivity extends MonkeyActivity implements IBluetoothConnectionObserver {

    private final String TAG = "AvrcpTestActivity";
    private boolean ffPressed = false;

    private ActionBar mActionBar = null;
    private UpdateThread mUpdateThread;
    private PressandHoldHandler mPressandHoldHandler;
    private View appView;
    private Activity mLocalActivity;
    private final ReentrantLock mLock = new ReentrantLock();
    BluetoothAvrcpController mAvrcpController;
    ProfileService mProfileService = null;
    BluetoothDevice mDevice;
    BluetoothAvrcpShareContentObserver mAvrcpDataObserver = null;
    private class PlayerSettings
    {
        public byte attr_Id;
        public byte attr_val;
        public byte [] supported_values; // app shld check these values before Setting Player Attributes.
    };
    ArrayList<PlayerSettings> plSetting= null;
    int remoteSupportedFeatures = 0;
    public static final int KEY_STATE_PRESSED = BluetoothAvrcpController.KEY_STATE_PRESSED;
    public static final int KEY_STATE_RELEASED = BluetoothAvrcpController.KEY_STATE_RELEASED;
    public static final int AVRC_ID_PLAY = BluetoothAvrcpController.AVRC_ID_PLAY;
    public static final int AVRC_ID_PAUSE = BluetoothAvrcpController.AVRC_ID_PAUSE;
    public static final int AVRC_ID_VOL_UP = BluetoothAvrcpController.AVRC_ID_VOL_UP;
    public static final int AVRC_ID_VOL_DOWN = BluetoothAvrcpController.AVRC_ID_VOL_DOWN;
    public static final int AVRC_ID_STOP = BluetoothAvrcpController.AVRC_ID_STOP;
    public static final int AVRC_ID_FF = BluetoothAvrcpController.AVRC_ID_FF;
    public static final int AVRC_ID_REWIND = BluetoothAvrcpController.AVRC_ID_REWIND;
    public static final int AVRC_ID_FORWARD = BluetoothAvrcpController.AVRC_ID_FORWARD;
    public static final int AVRC_ID_BACKWARD = BluetoothAvrcpController.AVRC_ID_BACKWARD;
    public static final int AVRC_ID_NEXT_GRP = BluetoothAvrcpController.AVRC_ID_NEXT_GRP;
    public static final int AVRC_ID_PREV_GRP = BluetoothAvrcpController.AVRC_ID_PREV_GRP;

    public static final int SEND_PASS_THROUGH_CMD = 1;

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
    private ToggleButton mCTStartButton;
    private Button ffButton;
    private Button rwButton;
    private TextView mBatteryStatus;
    private TextView mSystemStatus;

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
    private String batteryStatusText;
    private String systemStatusText;

    private final BroadcastReceiver mAvrcpControllerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = (BluetoothDevice)
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (action.equals(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED)) {
                int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                onReceiveActionConnectionStateChanged(device, prevState, state, intent.getExtras());
            }
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
                if (plSetting != null)
                    plSetting.clear();
                remoteSupportedFeatures = 0;
                unregisterMetaDataObserver();
                resetDisplay();
                mCTStartButton.setChecked(false);
                Toast.makeText(mLocalActivity, "Device " + device + " AVRCP Disconnected", Toast.LENGTH_SHORT).show();
            }
            else if(state == BluetoothProfile.STATE_CONNECTED) {
                mDevice = device;
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
                        mPressandHoldHandler.sendMessageAtFrontOfQueue(mPressandHoldHandler.obtainMessage(SEND_PASS_THROUGH_CMD,AVRC_ID_REWIND,KEY_STATE_RELEASED));
                    } else {
                        Logger.e(TAG, "passthru command not sent, connection unavailable");
                    }
            }
            else if (me.getAction() == MotionEvent.ACTION_DOWN) {
                if ((mAvrcpController != null) && mDevice != null &&
                        BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
                    if ((mPressandHoldHandler != null)&&(!mPressandHoldHandler.hasMessages(SEND_PASS_THROUGH_CMD)))
                        mPressandHoldHandler.sendMessage(mPressandHoldHandler.obtainMessage(SEND_PASS_THROUGH_CMD,AVRC_ID_REWIND,KEY_STATE_PRESSED));
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
                            mPressandHoldHandler.sendMessageAtFrontOfQueue(mPressandHoldHandler.obtainMessage(SEND_PASS_THROUGH_CMD,AVRC_ID_FF,KEY_STATE_RELEASED));
                    } else {
                        Logger.e(TAG, "passthru command not sent, connection unavailable");
                    }
            }
            else if (me.getAction() == MotionEvent.ACTION_DOWN) {
                if ((mAvrcpController != null) && mDevice != null &&
                        BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
                        if ((mPressandHoldHandler != null)&&(!mPressandHoldHandler.hasMessages(SEND_PASS_THROUGH_CMD)))
                            mPressandHoldHandler.sendMessage(mPressandHoldHandler.obtainMessage(SEND_PASS_THROUGH_CMD,AVRC_ID_FF,KEY_STATE_PRESSED));
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
                if (keyState == KEY_STATE_PRESSED) {
                    Message msgsend = mPressandHoldHandler.obtainMessage(SEND_PASS_THROUGH_CMD, keyCode, keyState);
                    mPressandHoldHandler.sendMessageDelayed(msgsend, 1000);
                    if ((mAvrcpController != null) && mDevice != null &&
                            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
                            mAvrcpController.sendPassThroughCmd(mDevice, keyCode, keyState);
                    }
                }
                else if(keyState == KEY_STATE_RELEASED) {
                    if (mPressandHoldHandler.hasMessages(SEND_PASS_THROUGH_CMD))
                        mPressandHoldHandler.removeMessages(SEND_PASS_THROUGH_CMD);
                    if ((mAvrcpController != null) && mDevice != null &&
                            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
                            mAvrcpController.sendPassThroughCmd(mDevice, keyCode, keyState);
                    }
                }
                break;
            }
        }
    }
    private class BluetoothAvrcpShareContentObserver extends ContentObserver {
        public BluetoothAvrcpShareContentObserver() {
            super(new Handler());
        }
        @Override
        public void onChange(boolean selfChange) {
            updateFromAvrcpContentProvider();
        }
    };
    private void updateFromAvrcpContentProvider() {
        Logger.e(TAG," AVRCP DB updated");
        if (mUpdateThread == null)
        {
            Logger.e(TAG, "Starting a new Thread ");
            mUpdateThread = new UpdateThread();
            mUpdateThread.start();
        }
    }
    private class UpdateThread extends Thread {
        public UpdateThread() {
            super(" BT_TESTAPP AVRCP UpdateThread");
        }
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Uri avrcpDataUri = BluetoothAvrcpInfo.CONTENT_URI;
            String[] mSelectionArgs = {""}; mSelectionArgs[0] = "";
            Cursor cursor = getContentResolver().query(avrcpDataUri, null, null, null,
                    BluetoothAvrcpInfo._ID);

            if (cursor != null) {
                int num_rows = cursor.getCount();
                int index;
                cursor.moveToFirst();
                int num_colums = cursor.getColumnCount();
                Logger.e(TAG," number of rows " + num_rows + " num Col " + num_colums);
                mLock.lock();
                try {
                while(num_colums > 0){
                    switch(num_colums) {
                    case 1: // Track Num
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.TRACK_NUM);
                        if (index == -1)
                            break;
                        int track_num = cursor.getInt(index);
                        if (track_num == BluetoothAvrcpInfo.TRACK_NUM_INVALID) {
                            trackNumText = "NOT_SUP";
                            break;
                        }
                        StringBuffer str = new StringBuffer();
                        str.append(String.valueOf(track_num));
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.TOTAL_TRACKS);
                        if (index > 0) {
                            str.append(" | ");
                            str.append(String.valueOf(cursor.getInt(index)));
                        }
                        trackNumText = str.toString();
                        Logger.e(TAG, " Number of Tracks " + trackNumText);
                        break;
                    case 2: // TRACK Title
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.TITLE);
                        if (index == -1)
                            break;
                        titleNameText = cursor.getString(index);
                        Logger.e(TAG, " Track Title " + titleNameText);
                        break;
                    case 3: // Artist Name
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.ARTIST_NAME);
                        if (index == -1)
                            break;
                        artistText = cursor.getString(index);
                        Logger.e(TAG, " Artist Name " + artistText);
                        break;
                    case 4: // Album Name
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.ALBUM_NAME);
                        if (index == -1)
                            break;
                        albumText = cursor.getString(index);
                        Logger.e(TAG, " album_name " + albumText);
                        break;
                    case 5: // play time
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.PLAYING_TIME);
                        if (index == -1)
                            break;
                        long playing_time = cursor.getLong(index);
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(playing_time);
                        playing_time = playing_time - (60*minutes*1000);
                        long seconds = TimeUnit.MILLISECONDS.toSeconds(playing_time);
                        StringBuffer strPlayTime = new StringBuffer();
                        strPlayTime.append(String.valueOf(minutes));
                        strPlayTime.append(":");
                        strPlayTime.append(String.valueOf(seconds));
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.TOTAL_TRACK_TIME);
                        if (index > 0) {
                            strPlayTime.append(" | ");
                            long totalTrackTime = cursor.getLong(index);
                            minutes = TimeUnit.MILLISECONDS.toMinutes(totalTrackTime);
                            totalTrackTime = totalTrackTime - (60*minutes*1000);
                            seconds = TimeUnit.MILLISECONDS.toSeconds(totalTrackTime);
                            strPlayTime.append(String.valueOf(minutes));
                            strPlayTime.append(":");
                            strPlayTime.append(String.valueOf(seconds));
                        }
                        playText = strPlayTime.toString();
                        Logger.e(TAG, " playing_time " + playText);
                        break;
                    case 6: //Genre
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.GENRE);
                        if (index == -1)
                            break;
                        genreText = cursor.getString(index);
                        Logger.e(TAG, " genre  " + genreText);
                        break;
                    case 7:// play Status
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.PLAY_STATUS);
                        if (index == -1)
                            break;
                        playStatusText = cursor.getString(index);
                        Logger.e(TAG, " playStatus  " + playStatusText);
                        break;
                    case 8:// Repeat Status
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.REPEAT_STATUS);
                        if (index == -1)
                            break;
                        repeatText = cursor.getString(index);
                        Logger.e(TAG, " repetStatus  " + repeatText);
                        break;
                    case 9:// ShuffleStatus
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.SHUFFLE_STATUS);
                        if (index == -1)
                            break;
                        shuffleText = cursor.getString(index);
                        Logger.e(TAG, " ShuffleStatus  " + shuffleText);
                        break;
                    case 10:// Scan Status
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.SCAN_STATUS);
                        if (index == -1)
                            break;
                        scanText = cursor.getString(index);
                        Logger.e(TAG, " Scan Status  " + scanText);
                        break;
                    case 11:// Eq Status
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.EQUALIZER_STATUS);
                        if (index == -1)
                            break;
                        equalizerText = cursor.getString(index);
                        Logger.e(TAG, " Equalizer Status  " + equalizerText);
                        break;
                    case 12: //Battery Status
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.BATTERY_STATUS);
                        if (index == -1)
                            break;
                        batteryStatusText = cursor.getString(index);
                        Logger.e(TAG, " batteryStatus  " + batteryStatusText);
                        break;
                    case 13: //System Status
                        index = cursor.getColumnIndex(BluetoothAvrcpInfo.SYSETEM_STATUS);
                        if (index == -1)
                            break;
                        systemStatusText = cursor.getString(index);
                        Logger.e(TAG, " systemStatus  " + systemStatusText);
                        break;
                    }
                    num_colums --;
                }
                } // try end
                catch(CursorIndexOutOfBoundsException e) {
                    Log.d(TAG," CursorIndexOutOfBoundsException happended");
                }
                catch(SQLiteCantOpenDatabaseException e) {
                    Log.d(TAG," SQLiteCantOpenDatabaseException happended");
                }
                catch(SQLiteException e) {
                    Log.d(TAG," SQLiteException happended");
                }
                finally {
                    if(cursor != null)
                        cursor.close();
                    mLock.unlock();
                }
            }
            else {
                Logger.v(TAG," Cursor is NULL");
            }
            mLocalActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLock.lock();
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
                        mBatteryStatus.setText(batteryStatusText);
                        mSystemStatus.setText(systemStatusText);
                    }
                    finally {
                        mLock.unlock();
                    }
                }
            });
            mUpdateThread =  null;
        }
    }
    private final ServiceConnection mAvrcpControllerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.v(TAG, "onServiceConnected()");
            mProfileService = ((ProfileService.LocalBinder) service).getService();
            mAvrcpController = mProfileService.getAvrcpController();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.v(TAG, "onServiceDisconnected()");
            mProfileService = null;
            mAvrcpController = null;
            mDevice = null;
            remoteSupportedFeatures = 0;
            if (plSetting != null)
                plSetting.clear();
            unregisterMetaDataObserver();
            resetDisplay();
            mCTStartButton.setChecked(false);
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
        registerReceiver(mAvrcpControllerReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        Logger.v(TAG, "onDestroy");
        mDevice = null;
        remoteSupportedFeatures = 0;
        if (plSetting != null)
            plSetting.clear();
        unregisterMetaDataObserver();
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
        remoteSupportedFeatures = 0;
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
    public void registerMetaDataObserver() {
       if(!isDeviceConnected()) {
           Logger.e(TAG," Device not Connected");
           return;
       }
       if ((remoteSupportedFeatures & BluetoothAvrcpInfo.BTRC_FEAT_METADATA) == 0) {
           Logger.e(TAG," Device does not support MetaData");
           return;
       }
       /* check if DataObserver is already registered */
       if (mAvrcpDataObserver != null)
           return;
       Uri avrcpDataUri = BluetoothAvrcpInfo.CONTENT_URI;
       mAvrcpDataObserver = new BluetoothAvrcpShareContentObserver();
       getContentResolver().registerContentObserver(avrcpDataUri, true, mAvrcpDataObserver);
       Logger.v(TAG," Registered Content Observer");
    }
    public void unregisterMetaDataObserver() {
        Logger.d(TAG," unregisterMetaDataObserver");
        if (mAvrcpDataObserver == null)
            return;
        getContentResolver().unregisterContentObserver(mAvrcpDataObserver);
        mAvrcpDataObserver = null;
    }
    public void onClickPrevGroup(View v) {
        Logger.v(TAG, "onClickPrevGroup()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendGroupNavigationCmd(mDevice, AVRC_ID_PREV_GRP, KEY_STATE_PRESSED);
            mAvrcpController.sendGroupNavigationCmd(mDevice, AVRC_ID_PREV_GRP, KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "GRP Navigation not sent, connection unavailable");
        }
    }
    public void onClickNextGroup(View v) {
        Logger.v(TAG, "onClickNextGroup()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendGroupNavigationCmd(mDevice, AVRC_ID_NEXT_GRP, KEY_STATE_PRESSED);
            mAvrcpController.sendGroupNavigationCmd(mDevice, AVRC_ID_NEXT_GRP, KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "GRP Navigation not sent, connection unavailable");
        }
    }
    public void onClickPassthruPlay(View v) {
        Logger.v(TAG, "onClickPassthruPlay()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_PLAY, KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_PLAY, KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }
    }
    public void onClickPassthruForward(View v) {
        Logger.v(TAG, "onClickPassthruForward()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_FORWARD, KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_FORWARD, KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }
    }
    public void onClickPassthruBackward(View v) {
        Logger.v(TAG, "onClickPassthruBackward()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_BACKWARD, KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_BACKWARD, KEY_STATE_RELEASED);
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
        if ((remoteSupportedFeatures & BluetoothAvrcpInfo.BTRC_FEAT_METADATA)==0) {
            return;
        }
        BluetoothAvrcpInfo mMetaData = mAvrcpController.getSupportedPlayerAppSetting(mDevice);
        if ((plSetting == null)||(plSetting.isEmpty())) {
            updatePlayerSettings(mMetaData);
        }

        if ((plSetting == null)||(plSetting.isEmpty())||(repeatText == null)) {
            Log.w(TAG," not supported, return");
            return;
        }
        for (PlayerSettings sett: plSetting) {
            Log.d(TAG," finding the current value " + sett.attr_Id);
            if (sett.attr_Id == BluetoothAvrcpInfo.ATTRIB_REPEAT_STATUS) {
                sett.attr_val = mMetaData.getCurrentPlayerAttributeVal(sett.attr_Id);
                int repeat_status = sett.attr_val;
                Log.d(TAG," shuffle_status " + repeat_status);
                for (int zz = 0; zz < sett.supported_values.length; zz++) {
                    if (repeat_status == sett.supported_values[zz]) {
                        repeat_status = sett.supported_values[(zz + 1)%sett.supported_values.length];
                        break;
                    }
                }
                mAvrcpController.setPlayerApplicationSetting(sett.attr_Id, repeat_status);
                break;
            }
        }
    }
    public void onClickToggleEq(View v) {
        Logger.v(TAG, "onClickToggleEq()");
        if ((mAvrcpController == null) ||
            (mDevice == null)||
            (BluetoothProfile.STATE_CONNECTED != (mAvrcpController.getConnectionState(mDevice)))) {
               return;
        }
        if ((remoteSupportedFeatures & BluetoothAvrcpInfo.BTRC_FEAT_METADATA)==0) {
            return;
        }
        BluetoothAvrcpInfo mMetaData = mAvrcpController.getSupportedPlayerAppSetting(mDevice);
        if ((plSetting == null)||(plSetting.isEmpty())) {
            updatePlayerSettings(mMetaData);
        }
        if ((plSetting == null)||(plSetting.isEmpty())||(equalizerText == null)) {
            Log.w(TAG," not supported, return");
            return;
        }
        for (PlayerSettings sett: plSetting) {
            if (sett.attr_Id == BluetoothAvrcpInfo.ATTRIB_EQUALIZER_STATUS) {
                sett.attr_val = mMetaData.getCurrentPlayerAttributeVal(sett.attr_Id);
                int eq_status = sett.attr_val;
                Log.d(TAG," equalizer_status " + eq_status);
                for (int zz = 0; zz < sett.supported_values.length; zz ++) {
                    if (eq_status == sett.supported_values[zz]) {
                        eq_status = sett.supported_values[(zz + 1)%sett.supported_values.length];
                        break;
                    }
                }
                mAvrcpController.setPlayerApplicationSetting(sett.attr_Id, eq_status);
                break;
            }
        }
    }
    public void onClickToggleScan(View v) {
        Logger.v(TAG, "onClickToggleScan()");
        if ((mAvrcpController == null) ||
            (mDevice == null)||
            (BluetoothProfile.STATE_CONNECTED != (mAvrcpController.getConnectionState(mDevice)))) {
               return;
        }
        if ((remoteSupportedFeatures & BluetoothAvrcpInfo.BTRC_FEAT_METADATA)==0) {
            return;
        }
        BluetoothAvrcpInfo mMetaData = mAvrcpController.getSupportedPlayerAppSetting(mDevice);
        if ((plSetting == null)||(plSetting.isEmpty())) {
            updatePlayerSettings(mMetaData);
        }
        if ((plSetting == null)||(plSetting.isEmpty())||(scanText == null)) {
            Log.w(TAG," not supported, return");
            return;
        }
        for (PlayerSettings sett: plSetting) {
            if (sett.attr_Id == BluetoothAvrcpInfo.ATTRIB_SCAN_STATUS) {
                sett.attr_val = mMetaData.getCurrentPlayerAttributeVal(sett.attr_Id);
                int scan_status = sett.attr_val;
                Log.d(TAG," scan_status " + scan_status);
                for (int zz = 0; zz < sett.supported_values.length; zz ++) {
                    if (scan_status == sett.supported_values[zz]) {
                        scan_status = sett.supported_values[(zz + 1)%sett.supported_values.length];
                        break;
                    }
                }
                mAvrcpController.setPlayerApplicationSetting(sett.attr_Id, scan_status);
                break;
            }
        }
    }
    public void onClickToggleShuffle(View v) {
        Logger.v(TAG, "onClickToggleShuffle()");
        if ((mAvrcpController == null) ||
            (mDevice == null)||
            (BluetoothProfile.STATE_CONNECTED != (mAvrcpController.getConnectionState(mDevice)))) {
               return;
        }
        if ((remoteSupportedFeatures & BluetoothAvrcpInfo.BTRC_FEAT_METADATA)==0) {
            return;
        }
        BluetoothAvrcpInfo mMetaData = mAvrcpController.getSupportedPlayerAppSetting(mDevice);
        if ((plSetting == null)||(plSetting.isEmpty())) {
            updatePlayerSettings(mMetaData);
        }
        if ((plSetting == null)||(plSetting.isEmpty())||(shuffleText == null)) {
            Log.w(TAG," not supported, return");
            return;
        }
        for (PlayerSettings sett: plSetting) {
            if (sett.attr_Id == BluetoothAvrcpInfo.ATTRIB_SHUFFLE_STATUS) {
                sett.attr_val = mMetaData.getCurrentPlayerAttributeVal(sett.attr_Id);
                int shuffle_status = sett.attr_val;
                Log.d(TAG," shuffle_status " + shuffle_status);
                for (int zz = 0; zz < sett.supported_values.length; zz ++) {
                    if (shuffle_status == sett.supported_values[zz]) {
                        shuffle_status = sett.supported_values[(zz + 1)%sett.supported_values.length];
                        break;
                    }
                }
                mAvrcpController.setPlayerApplicationSetting(sett.attr_Id, shuffle_status);
                break;
            }
        }
    }

    public void onClickPassthruPause(View v) {
        Logger.v(TAG, "onClickPassthruPause()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_PAUSE, KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_PAUSE, KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }
    }

    public void onClickPassthruStop(View v) {
        Logger.v(TAG, "onClickPassthruStop()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_STOP, KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, AVRC_ID_STOP, KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }

    }

    private void updatePlayerSettings(BluetoothAvrcpInfo mData) {
        if (plSetting != null) {
            plSetting.clear();
        }
        plSetting = new ArrayList<PlayerSettings>();
        if (mData == null)
            return;
        byte[] plAttributes = mData.getSupportedPlayerAttributes();
        for (int zz = 0; zz < plAttributes.length; zz++) {
            PlayerSettings playerSetting = new PlayerSettings();
            playerSetting.attr_Id = plAttributes[zz];
            playerSetting.supported_values = new byte[mData.getNumSupportedPlayerAttributeVal(playerSetting.attr_Id)];
            byte[] plAttribSupportedValues = mData.getSupportedPlayerAttributeVlaues(playerSetting.attr_Id);
            for (int xx = 0; xx < playerSetting.supported_values.length; xx++) {
                playerSetting.supported_values[xx] = plAttribSupportedValues[xx];
            }
            plSetting.add(playerSetting);
        }
    }
    private void resetDisplay() {
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                trackNumText = "NOT_SUPP";
                titleNameText = BluetoothAvrcpInfo.TITLE_INVALID;
                equalizerText = BluetoothAvrcpInfo.EQUALIZER_STATUS_INVALID;
                artistText = BluetoothAvrcpInfo.ARTIST_NAME_INVALID;
                scanText = BluetoothAvrcpInfo.SCAN_STATUS_INVALID;
                shuffleText = BluetoothAvrcpInfo.SHUFFLE_STATUS_INVALID;
                repeatText = BluetoothAvrcpInfo.REPEAT_STATUS_INVALID;
                playStatusText = BluetoothAvrcpInfo.PLAY_STATUS_INVALID;
                genreText = BluetoothAvrcpInfo.GENRE_INVALID;
                playText = BluetoothAvrcpInfo.PLAY_STATUS_INVALID;
                albumText = BluetoothAvrcpInfo.ALBUM_NAME_INVALID;
                batteryStatusText = BluetoothAvrcpInfo.BATTERY_STATUS_INVALID;
                systemStatusText = BluetoothAvrcpInfo.SYSTEM_STATUS_INVALID;
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
                    mBatteryStatus.setText(batteryStatusText);
                    mSystemStatus.setText(systemStatusText);
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
         mCTStartButton = (ToggleButton) findViewById(R.id.toggleButton1);
         ffButton = (Button) findViewById(R.id.onClickPassthruFF);
         rwButton = (Button) findViewById(R.id.onClickPassthruRewind);
         ffButton.setOnTouchListener(onTouchListenerFF);
         rwButton.setOnTouchListener(onTouchListenerRW);
         mBatteryStatus = (TextView) findViewById(R.id.battery_status);
         mSystemStatus = (TextView) findViewById(R.id.system_status);
    }
    public void onCTStartToggleClicked(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();
        Log.v(TAG, "onCTStartToggleClicked is_on: " + on);
        if ((mAvrcpController != null)&&(on)) {
            List<BluetoothDevice> deviceList = mAvrcpController.getConnectedDevices();
            if (deviceList.size() > 0) {
                mDevice = deviceList.get(0);
                if (mAvrcpController.getConnectionState(mDevice) != BluetoothProfile.STATE_CONNECTED) {
                    mCTStartButton.setChecked(false);
                    Toast.makeText(mLocalActivity, "Device Not Connected", Toast.LENGTH_SHORT).show();
                    return;
                }
                remoteSupportedFeatures = mAvrcpController.getSupportedFeatures(mDevice);
                Log.d(TAG," getSupportedFeatures " + remoteSupportedFeatures);
                if ((remoteSupportedFeatures & BluetoothAvrcpInfo.BTRC_FEAT_METADATA)!=0) {
                    BluetoothAvrcpInfo mMetaData = mAvrcpController.getSupportedPlayerAppSetting(mDevice);
                    updatePlayerSettings(mMetaData);
                    registerMetaDataObserver();
                    int[] elementAttribute = new int[1];
                    elementAttribute[0] = BluetoothAvrcpInfo.MEDIA_ATTRIBUTE_ALL;
                    mAvrcpController.getMetaData(elementAttribute);
                }
                else {
                    unregisterMetaDataObserver();
                    resetDisplay();
                    mCTStartButton.setChecked(false);
                    Toast.makeText(mLocalActivity, "Device Don't Support MetaData", Toast.LENGTH_SHORT).show();
                }
            }
            else {  // no device connected
               mCTStartButton.setChecked(false);
               Toast.makeText(mLocalActivity, "Device Not Connected", Toast.LENGTH_SHORT).show();
            }
        } else if(mAvrcpController != null){
              unregisterMetaDataObserver();
              resetDisplay();
        }
    }
}
