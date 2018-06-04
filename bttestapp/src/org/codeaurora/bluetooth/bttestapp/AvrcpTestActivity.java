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
import android.widget.Button;
import android.widget.EditText;

public class AvrcpTestActivity extends MonkeyActivity implements OnClickListener {

    private final String TAG = "AvrcpTestActivity";
    private Button mBtnPlayPause;
    private Button mBtnFastforward;
    private Button mBtnRewind;
    private Button mBtnSearch;
    private EditText mEditTextSearch;

    private final String STATUS_PLAY = "play";
    private final String STATUS_PAUSE = "pause";
    private final String FASTFORWARD = "fastforward";
    private final String REWIND = "rewind";
    private final String SEARCH = "search";
    private ProfileService mProfileService = null;

    public static final String ACTION_TRACK_EVENT =
        "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT";

    public static final String EXTRA_PLAYBACK =
        "android.bluetooth.avrcp-controller.profile.extra.PLAYBACK";

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
        ActivityHelper.initialize(this, R.layout.layout_avrcp);
        mBtnPlayPause = (Button) findViewById(R.id.id_btn_play_pause);
        mBtnPlayPause.setText(STATUS_PLAY);
        mBtnPlayPause.setOnClickListener(this);

        mBtnFastforward = (Button) findViewById(R.id.id_btn_fast_forward);
        mBtnFastforward.setText(FASTFORWARD);
        mBtnFastforward.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
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

                return false;
            }
        });

        mBtnRewind = (Button) findViewById(R.id.id_btn_rewind);
        mBtnRewind.setText(REWIND);
        mBtnRewind.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
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

                return false;
            }
        });

        mEditTextSearch = (EditText) findViewById(R.id.id_edit_search);
        mEditTextSearch.setText("You");  // Sample search string for test
        mEditTextSearch.setVisibility(View.VISIBLE);

        mBtnSearch = (Button) findViewById(R.id.id_btn_search);
        mBtnSearch.setText(SEARCH);
        mBtnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchQuery = mEditTextSearch.getText().toString();
                if ((searchQuery != null) && !searchQuery.isEmpty()) {
                    Log.d(TAG, "BtnSearch clicked, search: " + searchQuery);
                    sendSearch(searchQuery);
                } else {
                    Log.w(TAG, "BtnSearch clicked, but search string empty");
                }
            }
        });

        // bind to app service
        Intent intent = new Intent(this, ProfileService.class);
        bindService(intent, mAvrcpConnection, BIND_AUTO_CREATE);

        // receive playback state change
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TRACK_EVENT);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mAvrcpConnection);
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onClick(View v) {
        sendCommand();
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
}
