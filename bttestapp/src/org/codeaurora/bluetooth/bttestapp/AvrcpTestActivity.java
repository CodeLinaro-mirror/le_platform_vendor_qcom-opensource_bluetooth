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
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AvrcpTestActivity extends MonkeyActivity implements OnClickListener {

    private final String TAG = "AvrcpTestActivity";
    private Button mBtnPlayPause;
    private final String STATUS_PLAY = "play";
    private final String STATUS_PAUSE = "pause";
    private ProfileService mProfileService = null;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelper.initialize(this, R.layout.layout_avrcp);
        mBtnPlayPause = (Button) findViewById(R.id.id_btn_play_pause);
        mBtnPlayPause.setText(STATUS_PLAY);
        mBtnPlayPause.setOnClickListener(this);

        // bind to app service
        Intent intent = new Intent(this, ProfileService.class);
        bindService(intent, mAvrcpConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mAvrcpConnection);
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

}
