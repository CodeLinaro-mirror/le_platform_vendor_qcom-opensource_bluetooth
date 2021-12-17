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
import android.bluetooth.BluetoothDevice;
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

import org.codeaurora.bluetooth.bttestapp.util.Logger;

public class AvrcpCoverArtActivity extends Activity
        implements OnClickListener, OnItemSelectedListener {

    private final String TAG = "AvrcpCoverArtActivity";
    private final String ACTION_TRACK_EVENT =
            "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT";
    private final String EXTRA_METADATA =
            "android.bluetooth.avrcp-controller.profile.extra.METADATA";
    private ImageView mIvCoverArt, mIvThumbNail;
    private Spinner mSpImgType, mSpScheme, mSpImgEncode, mSpImgWidth, mSpImgSize, mSpImgheight;
    private Button mBtnConfig, mBtnConfigBase;

    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothAvrcpController mAvrcpController;

    // Image Download Schemes for cover art
    public static final String AVRCP_CONTROLLER_COVER_ART_SCHEME =
            "persist.bluetooth.avrcpcontroller.BIP_DOWNLOAD_SCHEME";
    public static final String AVRCP_CONTROLLER_COVER_ART_MIMETYPE =
            "persist.bluetooth.avrcpcontroller.BIP_DOWNLOAD_MIMETYPE";
    public static final String AVRCP_CONTROLLER_COVER_ART_IMGTYPE =
            "persist.bluetooth.avrcpcontroller.BIP_DOWNLOAD_TYPE";
    public static final String AVRCP_CONTROLLER_COVER_ART_IMGHEIGHT =
            "persist.bluetooth.avrcpcontroller.BIP_DOWNLOAD_HEIGHT";
    public static final String AVRCP_CONTROLLER_COVER_ART_IMGWIDTH =
            "persist.bluetooth.avrcpcontroller.BIP_DOWNLOAD_WIDTH";
    public static final String AVRCP_CONTROLLER_COVER_ART_IMGMAXSIZE =
            "persist.bluetooth.avrcpcontroller.BIP_DOWNLOAD_MAXSIZE";

    // Refer to class com.android.bluetooth.avrcpcontroller.BipEncoding
    // for supported mime types
    private static final String MIMETYPE_DEFAULT = "JPEG";
    // Image types defined by AVRCP spec are Image, Thumbnail, ThumbnailLinked.
    private static final String IMAGETYPE_DEFAULT = "Image";
    // Scheme defined by AVRCP spec are thumbnail and native
    private static final String SCHMEME_DEFAULT = "thumbnail";
    private static final int IMAGE_HEIGHT_DEFAULT = 500;
    private static final int IMAGE_WIDTH_DEFAULT = 500;
    private static final int THUMBNAIL_IMAGE_HEIGHT_DEFAULT = 200;
    private static final int THUMBNAIL_IMAGE_WIDTH_DEFAULT = 200;
    private static final int MAXSIZE_DEFAULT = 200000;

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
            Logger.v(TAG, "onServiceConnected() profile = " + profile);
            if (profile == BluetoothProfile.AVRCP_CONTROLLER) {
                mAvrcpController = (BluetoothAvrcpController) proxy;
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Logger.v(TAG, "onServiceDisconnected() profile = " + profile);
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
        mSpScheme = (Spinner) findViewById(R.id.id_sp_scheme);
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
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_IMGTYPE,
                    getValue(mSpImgType));
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_SCHEME,
                    getValue(mSpScheme));
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_MIMETYPE,
                    getValue(mSpImgEncode));
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_IMGWIDTH,
                    getValue(mSpImgWidth));
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_IMGHEIGHT,
                    getValue(mSpImgheight));
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_IMGMAXSIZE,
                    getValue(mSpImgSize));
        } else if (v == mBtnConfigBase) {
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_IMGTYPE, IMAGETYPE_DEFAULT);
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_SCHEME, SCHMEME_DEFAULT);
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_MIMETYPE, MIMETYPE_DEFAULT);
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_IMGWIDTH,
                IMAGE_WIDTH_DEFAULT + "");
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_IMGHEIGHT,
                IMAGE_HEIGHT_DEFAULT + "");
            SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_IMGMAXSIZE,
                MAXSIZE_DEFAULT + "");
            setSpinners();
        }
    }

    public void getCoveArtImage(View v) {
        Log.i(TAG, "Start Fetching Album art");
        String type = SystemProperties.get(AVRCP_CONTROLLER_COVER_ART_IMGTYPE,
            IMAGETYPE_DEFAULT);
        String scheme = SystemProperties.get(AVRCP_CONTROLLER_COVER_ART_SCHEME,
            SCHMEME_DEFAULT);
        String mimeType = SystemProperties.get(AVRCP_CONTROLLER_COVER_ART_MIMETYPE,
            MIMETYPE_DEFAULT);
        int height = SystemProperties.getInt(AVRCP_CONTROLLER_COVER_ART_IMGHEIGHT,
            IMAGE_HEIGHT_DEFAULT);
        int width = SystemProperties.getInt(AVRCP_CONTROLLER_COVER_ART_IMGWIDTH,
            IMAGE_WIDTH_DEFAULT);
        int maxSize = SystemProperties.getInt(AVRCP_CONTROLLER_COVER_ART_IMGMAXSIZE,
            MAXSIZE_DEFAULT);

        //Note, API shall be getActiveDevice when multiple AVRCP connections are enabled
        BluetoothDevice device = mAvrcpController.getConnectedDevices().get(0);
        mAvrcpController.startFetchingAlbumArt(device, type, scheme, mimeType, height, width, maxSize);
    }

    private void setSpinners() {
        String type = SystemProperties.get(AVRCP_CONTROLLER_COVER_ART_IMGTYPE);
        if (TextUtils.isEmpty(type) || type.equalsIgnoreCase(IMAGETYPE_DEFAULT)) {
            mSpImgType.setSelection(0);
        } else {
            mSpImgType.setSelection(1);
        }

        String scheme = SystemProperties.get(AVRCP_CONTROLLER_COVER_ART_SCHEME,
            SCHMEME_DEFAULT);
        if (TextUtils.isEmpty(type) || type.equalsIgnoreCase(SCHMEME_DEFAULT)) {
            mSpScheme.setSelection(0);
        } else {
            mSpScheme.setSelection(1);
        }

        String mime = SystemProperties.get(AVRCP_CONTROLLER_COVER_ART_MIMETYPE);
        if ("PNG".equalsIgnoreCase(mime)){
            mSpImgEncode.setSelection(1);
        } else if ("GIF".equalsIgnoreCase(mime)){
            mSpImgEncode.setSelection(2);
        } else {
            mSpImgEncode.setSelection(0);
        }

        int height = SystemProperties.getInt(AVRCP_CONTROLLER_COVER_ART_IMGHEIGHT,
            IMAGE_HEIGHT_DEFAULT);
        int width = SystemProperties.getInt(AVRCP_CONTROLLER_COVER_ART_IMGWIDTH,
            IMAGE_WIDTH_DEFAULT);
        int maxSize = SystemProperties.getInt(AVRCP_CONTROLLER_COVER_ART_IMGMAXSIZE,
            MAXSIZE_DEFAULT);
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
        if(mSpImgType.getSelectedItem().toString().equals(IMAGETYPE_DEFAULT)) {
            mSpScheme.setEnabled(true);
            mSpImgEncode.setEnabled(true);
            mSpImgheight.setEnabled(true);
            mSpImgSize.setEnabled(true);
            mSpImgWidth.setEnabled(true);
            Logger.v(TAG," Image");
        } else {
            Logger.v(TAG," Thumbnail");
            if(mSpImgType.getSelectedItem().toString().equals("ThumbnailImage")) {
                SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_MIMETYPE, MIMETYPE_DEFAULT);
                SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_IMGHEIGHT,
                    THUMBNAIL_IMAGE_HEIGHT_DEFAULT + "");
                SystemProperties.set(AVRCP_CONTROLLER_COVER_ART_IMGWIDTH,
                    THUMBNAIL_IMAGE_WIDTH_DEFAULT + "");
                mSpImgEncode.setSelection(getIndex(mSpImgEncode, MIMETYPE_DEFAULT));
                mSpImgWidth.setSelection(getIndex(mSpImgheight,
                    THUMBNAIL_IMAGE_HEIGHT_DEFAULT + ""));
                mSpImgheight.setSelection(getIndex(mSpImgWidth,
                    THUMBNAIL_IMAGE_WIDTH_DEFAULT + ""));
            }
            mSpScheme.setEnabled(false);
            mSpImgEncode.setEnabled(false);
            mSpImgheight.setEnabled(false);
            mSpImgSize.setEnabled(false);
            mSpImgWidth.setEnabled(false);
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
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

}
