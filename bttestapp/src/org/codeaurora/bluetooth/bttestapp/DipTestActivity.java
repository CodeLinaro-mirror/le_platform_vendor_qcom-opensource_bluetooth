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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.SdpDipRecord;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import org.codeaurora.bluetooth.bttestapp.util.Logger;

public class DipTestActivity extends Activity
        implements OnClickListener, IBluetoothConnectionObserver {

    private final String TAG = "DipTestActivity";
    private static final boolean DBG = true;
    private Button mBtnDipTest, mBtnDipClear;

    BluetoothDevice mDevice;
    SdpDipRecord mRecord;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Logger.i(TAG, "action " + action);
            if (action.equals(BluetoothDevice.ACTION_SDP_RECORD)) {
                ParcelUuid uuid = intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID);

                if (DBG) {
                    Logger.d(TAG, "Received UUID: " + uuid.toString());
                    Logger.d(TAG, "expected UUID: " + BluetoothUuid.ObexObjectPush.toString());
                }
                if (uuid.equals(BluetoothUuid.DIP)) {
                    int status = intent.getIntExtra(BluetoothDevice.EXTRA_SDP_SEARCH_STATUS, -1);
                    Logger.d(TAG, " -> status: " + status);
                    BluetoothDevice device =
                         intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (DBG) {
                        Logger.d(TAG, "Dip from " + device);
                    }
                    mRecord =
                         intent.getParcelableExtra(BluetoothDevice.EXTRA_SDP_RECORD);
                    if (mRecord == null) {
                        Logger.w(TAG, " Invalid SDP , ignoring !!");
                        return;
                    }
                    if (DBG) {
                        Logger.d(TAG, "SpecificationId " + mRecord.getSpecificationId());
                        Logger.d(TAG, "VendorId "        + mRecord.getVendorId());
                        Logger.d(TAG, "VendorIdSource " + mRecord.getVendorIdSource());
                        Logger.d(TAG, "ProductId " + mRecord.getProductId());
                        Logger.d(TAG, "Version " + mRecord.getVersion());
                        Logger.d(TAG, "PrimaryRecord " + mRecord.getPrimaryRecord());
                        Logger.d(TAG, "ClientExecutableUrl " + mRecord.getClientExecutableUrl());
                        Logger.d(TAG, "ServiceDescription " + mRecord.getServiceDescription());
                        Logger.d(TAG, "DocumentationUrl " + mRecord.getDocumentationUrl());
                    }
                    setDipInfor();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dip_test);
        /* Add back button */
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mBtnDipTest = (Button) findViewById(R.id.id_dip_test);
        mBtnDipTest.setOnClickListener(this);
        mBtnDipClear = (Button) findViewById(R.id.id_dip_clear);
        mBtnDipClear.setOnClickListener(this);
        BluetoothConnectionReceiver.registerObserver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_SDP_RECORD);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        Logger.v(TAG, "onDestroy");

        super.onDestroy();

        BluetoothConnectionReceiver.removeObserver(this);
    }

    @Override
    public void onClick(View v) {
        if(v == mBtnDipTest) {
            Logger.d(TAG, "sdpSearch(BluetoothUuid.DIP)");
            clearDipInfor();
            mDevice.sdpSearch(BluetoothUuid.DIP);
        } else if (v == mBtnDipClear) {
            Logger.d(TAG, "Clear dip information");
            clearDipInfor();
        }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Logger.d(TAG, "Go back");
                finish();
                return true;
        }
        return false;
    }

    private void clearDipInfor() {
        TextView specificationId = (TextView) findViewById(R.id.id_dip_specification_id_value);
        specificationId.setText(R.string.blank);

        TextView vendorId = (TextView) findViewById(R.id.id_dip_vendor_id_value);
        vendorId.setText(R.string.blank);

        TextView vendorIdSource = (TextView) findViewById(R.id.id_dip_vendor_id_source_value);
        vendorIdSource.setText(R.string.blank);

        TextView productId = (TextView) findViewById(R.id.id_dip_product_id_value);
        productId.setText(R.string.blank);

        TextView version = (TextView) findViewById(R.id.id_dip_version_value);
        version.setText(R.string.blank);

        TextView primaryRecord = (TextView) findViewById(R.id.id_dip_primary_record_value);
        primaryRecord.setText(R.string.blank);

        TextView clientExecutableUrl = (TextView) findViewById(R.id.id_dip_client_executable_url_value);
        clientExecutableUrl.setText(R.string.blank);

        TextView serviceDescription = (TextView) findViewById(R.id.id_dip_service_description_value);
        serviceDescription.setText(R.string.blank);

        TextView documentationUrl = (TextView) findViewById(R.id.id_dip_documentation_url_value);
        documentationUrl.setText(R.string.blank);
    }

    private void setDipInfor() {
        TextView specificationId = (TextView) findViewById(R.id.id_dip_specification_id_value);
        specificationId.setText(Integer.toHexString(mRecord.getSpecificationId()));

        TextView vendorId = (TextView) findViewById(R.id.id_dip_vendor_id_value);
        vendorId.setText(Integer.toHexString(mRecord.getVendorId()));

        TextView vendorIdSource = (TextView) findViewById(R.id.id_dip_vendor_id_source_value);
        vendorIdSource.setText(Integer.toHexString(mRecord.getVendorIdSource()));

        TextView productId = (TextView) findViewById(R.id.id_dip_product_id_value);
        productId.setText(Integer.toHexString(mRecord.getProductId()));

        TextView version = (TextView) findViewById(R.id.id_dip_version_value);
        version.setText(Integer.toHexString(mRecord.getVersion()));

        TextView primaryRecord = (TextView) findViewById(R.id.id_dip_primary_record_value);
        primaryRecord.setText(String.valueOf(mRecord.getPrimaryRecord()));

        if (null != mRecord.getClientExecutableUrl()) {
            TextView clientExecutableUrl = (TextView) findViewById(R.id.id_dip_client_executable_url_value);
            clientExecutableUrl.setText(String.valueOf(mRecord.getClientExecutableUrl()));
        }

        if (null != mRecord.getServiceDescription()) {
            TextView serviceDescription = (TextView) findViewById(R.id.id_dip_service_description_value);
            serviceDescription.setText(String.valueOf(mRecord.getServiceDescription()));
        }

        if (null != mRecord.getDocumentationUrl()) {
            TextView documentationUrl = (TextView) findViewById(R.id.id_dip_documentation_url_value);
            documentationUrl.setText(String.valueOf(mRecord.getDocumentationUrl()));
        }
    }
}
