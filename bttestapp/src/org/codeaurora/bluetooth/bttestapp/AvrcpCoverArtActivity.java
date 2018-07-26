/*
 * Copyright (c) 2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *           * Redistributions of source code must retain the above copyright
 *             notice, this list of conditions and the following disclaimer.
 *           * Redistributions in binary form must reproduce the above
 *           * copyright notice, this list of conditions and the following
 *             disclaimer in the documentation and/or other materials provided
 *             with the distribution.
 *           * Neither the name of The Linux Foundation nor the names of its
 *             contributors may be used to endorse or promote products derived
 *             from this software without specific prior written permission.
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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

public class AvrcpCoverArtActivity extends Activity
        implements OnClickListener, OnItemSelectedListener {

    private final String TAG = "AvrcpCoverArtActivity";
    private final String ACTION_TRACK_EVENT =
            "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT";
    private final String EXTRA_METADATA =
            "android.bluetooth.avrcp-controller.profile.extra.METADATA";
    private ImageView mIvCoverArt, mIvThumbNail;
    private Spinner mSpImgType, mSpImgEncode, mSpImgWidth, mSpImgSize, mSpImgheight;
    private Button mBtnConfig, mBtnConfigBase;

    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothAvrcpController mAvrcpController;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "action " + action);
            if (action.equals(ACTION_TRACK_EVENT)) {
                MediaMetadata metaData = intent.getParcelableExtra(EXTRA_METADATA);
                if (metaData != null) {
                    setCoverArt(metaData);
                }
            }
        }
    };

    private final ServiceListener mAvrcpControllerServiceListener = new ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.v(TAG, "onServiceConnected() profile = " + profile);
            if (profile == BluetoothProfile.AVRCP_CONTROLLER) {
                mAvrcpController = (BluetoothAvrcpController) proxy;
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.v(TAG, "onServiceDisconnected() profile = " + profile);
            if (profile == BluetoothProfile.AVRCP_CONTROLLER) {
                mAvrcpController = null;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_avrcp_coverart);
        /* Add back button */
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mIvCoverArt = (ImageView) findViewById(R.id.id_iv_fullimage);
        mIvThumbNail = (ImageView) findViewById(R.id.id_iv_thumbnail);
        mIvCoverArt.setImageResource(R.drawable.ic_bt_connected);
        mBtnConfig = (Button) findViewById(R.id.id_btn_config);
        mBtnConfigBase = (Button) findViewById(R.id.id_btn_config_reset);
        mSpImgType = (Spinner) findViewById(R.id.id_sp_img_type);
        mSpImgType.setOnItemSelectedListener(this);
        mSpImgheight = (Spinner) findViewById(R.id.id_sp_img_height);
        mSpImgEncode = (Spinner) findViewById(R.id.id_sp_img_encode);
        mSpImgWidth = (Spinner) findViewById(R.id.id_sp_img_width);
        mSpImgSize = (Spinner) findViewById(R.id.id_sp_img_maxsize);
        mBtnConfig.setOnClickListener(this);
        mBtnConfigBase.setOnClickListener(this);
        mIvCoverArt.setVisibility(ImageView.GONE);
        mIvThumbNail.setVisibility(ImageView.GONE);
        setSpinners();
        mBluetoothAdapter.getProfileProxy(this, mAvrcpControllerServiceListener,
                BluetoothProfile.AVRCP_CONTROLLER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TRACK_EVENT);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    private void setCoverArt(MediaMetadata metaData) {
        String path = metaData.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
        Bitmap bitMapThumbNail = metaData
                .getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        Log.i(TAG, "bitMapThumbNail :" + bitMapThumbNail + " Cover art path :" + path);

        if (bitMapThumbNail != null) {
            mIvThumbNail.setVisibility(ImageView.VISIBLE);
            mIvCoverArt.setVisibility(ImageView.GONE);
            mIvThumbNail.setImageBitmap(bitMapThumbNail);
            return;
        }
        if (path == null || path.trim().length() == 0) {
            return;
        }

        mIvThumbNail.setVisibility(ImageView.GONE);
        mIvCoverArt.setVisibility(ImageView.VISIBLE);
        Bitmap bitMap = BitmapFactory.decodeFile(path);
        Log.i(TAG, "setCoverArt path :" + path + " bitMap :" + bitMap);
        mIvCoverArt.setImageBitmap(bitMap);
    }

    @Override
    public void onClick(View v) {
        if (v == mBtnConfig) {
            SystemProperties.set("persist.service.bt.avrcpct.imgtype",
                    getValue(mSpImgType));
            SystemProperties.set("persist.service.bt.avrcpct.imgencode",
                    getValue(mSpImgEncode));
            SystemProperties.set("persist.service.bt.avrcpct.imgwidth",
                    getValue(mSpImgWidth));
            SystemProperties.set("persist.service.bt.avrcpct.imgheight",
                    getValue(mSpImgheight));
            SystemProperties.set("persist.service.bt.avrcpct.imgsize",
                    getValue(mSpImgSize));
        } else if (v == mBtnConfigBase) {
            SystemProperties.set("persist.service.bt.avrcpct.imgtype", "Image");
            SystemProperties.set("persist.service.bt.avrcpct.imgencode", "JPEG");
            SystemProperties.set("persist.service.bt.avrcpct.imgwidth", "500");
            SystemProperties.set("persist.service.bt.avrcpct.imgheight", "500");
            SystemProperties.set("persist.service.bt.avrcpct.imgsize", "200000");
            setSpinners();
        }
    }

    public void getCoveArtImage(View v) {
        Log.i(TAG, "Start Fetching Album art");
        mAvrcpController.startFetchingAlbumArt("JPEG", 500, 500, 2000000);
    }

    private void setSpinners() {
        String type = SystemProperties.get("persist.service.bt.avrcpct.imgtype");
        if (TextUtils.isEmpty(type) || type.equalsIgnoreCase("Image")) {
            mSpImgType.setSelection(0);
        } else {
            mSpImgType.setSelection(1);
        }
        String mime = SystemProperties.get("persist.service.bt.avrcpct.imgencode");
        if (TextUtils.isEmpty(mime) || mime.equalsIgnoreCase("JPEG")) {
            mSpImgEncode.setSelection(0);
        } else {
            mSpImgEncode.setSelection(1);
        }

        int height = SystemProperties.getInt("persist.service.bt.avrcpct.imgheight", 500);
        int width = SystemProperties.getInt("persist.service.bt.avrcpct.imgwidth", 500);
        int maxSize = SystemProperties.getInt("persist.service.bt.avrcpct.imgsize",
                200000);
        Log.i(TAG, " Type :" + type + " Mime :" + mime + " Height:" + height + ": width :"
                + width + " Max size:" + maxSize);
        mSpImgWidth.setSelection(getIndex(mSpImgWidth, width + ""));
        mSpImgheight.setSelection(getIndex(mSpImgheight, height + ""));
        mSpImgSize.setSelection(getIndex(mSpImgSize, maxSize + ""));
    }

    private int getIndex(Spinner spinner, String myString) {
        int index = 0;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).equals(myString)) {
                index = i;
            }
        }
        return index;
    }

    private String getValue(Spinner spinner) {
        return spinner.getSelectedItem().toString();
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        if(mSpImgType.getSelectedItem().toString().equals("Image")) {
            mSpImgEncode.setEnabled(true);
            mSpImgheight.setEnabled(true);
            mSpImgSize.setEnabled(true);
            mSpImgWidth.setEnabled(true);
            Log.v(TAG," Image");
        }else {
            mSpImgEncode.setEnabled(false);
            mSpImgheight.setEnabled(false);
            mSpImgSize.setEnabled(false);
            mSpImgWidth.setEnabled(false);
            Log.v(TAG," Not image");
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
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

}
