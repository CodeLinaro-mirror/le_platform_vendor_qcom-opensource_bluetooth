/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
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

package org.codeaurora.bluetooth.wearos_ble_testapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.os.Message;


import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.util.UUID;

public class AdvertiserEntity {

    private String TAG = "ADV_ENTITY";
    public static final int ADV_STARTED = 0x00;
    public static final int ADV_STOPPED = 0x01;
    public static final int ADV_FAILED = 0x02;
    private String invalidParam = "XX";

    private byte[] manuData;
    private BluetoothAdapter mBTAdapter = MainActivity.bleAdapter;

    Adv adv_info;

    private BluetoothLeAdvertiser mAdvertiser;
    private AdvertiserService.AdvServiceCallback  mAdvServiceCb;
    //legacy
    private AdvertiseCallback mAdvCallback;
    private AdvertiseSettings mAdvSettings;
    private AdvertiseData mAdvData;

    //non-legacy
    private AdvertisingSetParameters mAdvParams;
    private AdvertisingSetCallback mAdvSetCallback;
    private PeriodicAdvertisingParameters mPeriodicAdvParams;
    private AdvertisingSet mAdvertisingSet;
    private AdvertiseData mScanResponseData;
    private AdvertiseData mPeriodicData;

    private boolean inuse;
    public int adv_id;
    public int adv_status;

    class AdvSetCallback extends AdvertisingSetCallback {
        Message msg;
        @Override
        public void onAdvertisingSetStarted(AdvertisingSet advertisingSet,
                                            int txPower, int status) {
            super.onAdvertisingSetStarted(advertisingSet, txPower, status);
            Log.d(TAG,"onAdvertisingSetStarted status : "+status);
            
            if (status == ADVERTISE_SUCCESS){
                adv_status = ADV_STARTED;
                msg = MainActivity.msghandler.obtainMessage(
                      MainActivity.MSG_MA_ADV_STARTED, Integer.toString(adv_id));
                MainActivity.msghandler.sendMessage(msg);
            } else {
                StringBuilder PrintStr = new StringBuilder();

                PrintStr.setLength(0);
                PrintStr.append("Failed to start advertising with error: ");
                PrintStr.append(status);
                SocketServer.sendSocketData(PrintStr.toString());
            }
        }

        @Override
        public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
            //super.onAdvertisingSetStopped(advertisingSet);
            Log.d(TAG,"onAdvertisingSetStopped");
            adv_status = ADV_STOPPED;
            msg = MainActivity.msghandler.obtainMessage(
                      MainActivity.MSG_MA_ADV_STOPPED, Integer.toString(adv_id));
            MainActivity.msghandler.sendMessage(msg);
        }

        @Override
        public void onAdvertisingEnabled(AdvertisingSet advertisingSet,
                                         boolean enable, int status) {
            //super.onAdvertisingEnabled(advertisingSet, enable, status);
            Log.d(TAG,"onAdvertisingEnabled");
        }

        @Override
        public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {
            //super.onAdvertisingDataSet(advertisingSet, status);
            Log.d(TAG,"onAdvertisingDataSet");
        }

        @Override
        public void onScanResponseDataSet(AdvertisingSet advertisingSet, int status) {
            //super.onScanResponseDataSet(advertisingSet, status);
            Log.d(TAG,"onScanResponseDataSet");
        }

        @Override
        public void onAdvertisingParametersUpdated(AdvertisingSet advertisingSet, int txPower,
                            int status) {
            //super.onAdvertisingParametersUpdated(advertisingSet, txPower, status);
            Log.d(TAG,"onAdvertisingParametersUpdated");
        }

        @Override
        public void onPeriodicAdvertisingParametersUpdated(AdvertisingSet advertisingSet,
                                                               int status) {
            //super.onPeriodicAdvertisingParametersUpdated(advertisingSet, status);
            Log.d(TAG,"onPeriodicAdvertisingParametersUpdated");
        }

        @Override
        public void onPeriodicAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {
            //super.onPeriodicAdvertisingDataSet(advertisingSet, status);
        }

        @Override
        public void onPeriodicAdvertisingEnabled(AdvertisingSet advertisingSet,
                                                     boolean enable, int status) {
            //super.onPeriodicAdvertisingEnabled(advertisingSet, enable, status);
            Log.d(TAG,"onPeriodicAdvertisingEnabled");
        }
    };

    class AdvCallback extends AdvertiseCallback {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "Advertisemnt sent");
            adv_status = ADV_STARTED;
            Message msg;
            msg = MainActivity.msghandler.obtainMessage(
                      MainActivity.MSG_MA_ADV_STARTED, Integer.toString(adv_id));
            MainActivity.msghandler.sendMessage(msg);
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "Advertisement failed, errorcode -> "+errorCode);
            adv_status = ADV_FAILED;
            super.onStartFailure(errorCode);

            StringBuilder PrintStr = new StringBuilder();

            PrintStr.setLength(0);
            PrintStr.append("Failed to start advertising with error: ");
            PrintStr.append(errorCode);
            SocketServer.sendSocketData(PrintStr.toString());
        }
    };


    public AdvertiserEntity(Adv adv_info, int adv_id,
                            AdvertiserService.AdvServiceCallback mAdvServiceCb) {
        this.adv_info = adv_info;
        this.adv_id = adv_id;
        this.mAdvServiceCb = mAdvServiceCb;
        // Advertiser
        mAdvertiser = mBTAdapter.getBluetoothLeAdvertiser();
        //Set ADV callback
        mAdvCallback = new AdvCallback();
        // Adv Set callback
        mAdvSetCallback = new AdvSetCallback();
    }

    public Adv getAdv_info() {
        return adv_info;
    }

    public void setAdv_info(Adv adv_info) {
        this.adv_info = adv_info;
    }

    public AdvertiseCallback getmAdvCallback() {
        return mAdvCallback;
    }

    public void setmAdvCallback(AdvertiseCallback mAdvCallback) {
        this.mAdvCallback = mAdvCallback;
    }

    public AdvertiseSettings getmAdvSettings() {
        return mAdvSettings;
    }

    public void setmAdvSettings(AdvertiseSettings mAdvSettings) {
        this.mAdvSettings = mAdvSettings;
    }

    public AdvertiseData getmAdvData() {
        return mAdvData;
    }

    public void setmAdvData(AdvertiseData mAdvData) {
        this.mAdvData = mAdvData;
    }

    public BluetoothLeAdvertiser getmAdvertiser() {
        return mAdvertiser;
    }

    public void setmAdvertiser(BluetoothLeAdvertiser mAdvertiser) {
        this.mAdvertiser = mAdvertiser;
    }

    public AdvertisingSetParameters getmAdvParams() {
        return mAdvParams;
    }

    public void setmAdvParams(AdvertisingSetParameters mAdvParams) {
        this.mAdvParams = mAdvParams;
    }

    public boolean isInuse() {
        return inuse;
    }

    public void setInuse(boolean inuse) {
        this.inuse = inuse;
    }

    public int getAdv_status() {
        return adv_status;
    }

    public boolean BuildAdvertisementParameters(){
        Log.d(TAG,"BuildAdvertisementParameters");
        try {
            if(adv_info.Legacy) {
                mAdvSettings = new AdvertiseSettings.Builder()
                        .setAdvertiseMode(adv_info.AdvertiseMode)
                        .setTxPowerLevel(adv_info.TxPower)
                        .setConnectable(adv_info.Connectable)
                        .setTimeout(adv_info.TimeOutLegacy)
                        .build();
                Log.d(TAG,"BuildAdvertisementParameters Legacy done");
                return true;
            } else {
                mAdvParams = new AdvertisingSetParameters.Builder()
                        .setConnectable(adv_info.Connectable)
                        .setScannable(adv_info.Scannable)
                        .setLegacyMode(adv_info.Legacy)
                        .setAnonymous(adv_info.Anonymous)
                        .setIncludeTxPower(adv_info.IncludePower)
                        .setPrimaryPhy(adv_info.PrimaryPhy)
                        .setSecondaryPhy(adv_info.SecondaryPhy)
                        .setInterval(adv_info.Interval)
                        .setTxPowerLevel(adv_info.TxPower)
                        .build();
                Log.d(TAG,"BuildAdvertisementParameters Ext done");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG,"Exception : "+ e.toString());
        }
        return false;
    }

    public boolean BuildAdvertisementData(){
        Log.d(TAG,"BuildAdvertisementData");
        try {
            if(adv_info.Legacy){
                AdvertiseData.Builder legacyData = new AdvertiseData.Builder();
                legacyData.setIncludeDeviceName(true);
                legacyData.setIncludeTxPowerLevel(adv_info.IncludePower);
                if(adv_info.ServiceUuid != null) {
                    ParcelUuid pUuid = new ParcelUuid(UUID.fromString(adv_info.ServiceUuid));
                    legacyData.addServiceUuid(pUuid);
                    Log.d(TAG, "service uuid added");
                }
                if(adv_info.ManufacturerData != null) {
                    String[] manufacturerData = adv_info.ManufacturerData.split(",");
                    if(manufacturerData!= null && (manufacturerData).length>0) {
                        manuData = new byte[manufacturerData.length];
                        for(int i=0; i< manuData.length; i++) {
                            manuData[i] = Byte.parseByte(manufacturerData[i],16);
                        }
                    }
                    if(manuData != null && manuData.length >0) {
                        for(int j=0; j< manuData.length; j++) {
                            Log.d(TAG, "manufacturerData::"+manuData[j]);
                        }
                    }
                    legacyData.addManufacturerData(adv_info.ManufacturerId,manuData);
                    Log.d(TAG, "manu data added");
                }
                mAdvData = legacyData.build();
                Log.d(TAG,"BuildAdvertisementData done");
                return true;
            } else {
                AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
                dataBuilder.setIncludeDeviceName(true);
                dataBuilder.setIncludeTxPowerLevel(adv_info.IncludePower);
                if(adv_info.ServiceUuid != null) {
                    Log.d(TAG,"Setting Service UUID");
                    ParcelUuid pUuid = new ParcelUuid(UUID.fromString(adv_info.ServiceUuid));
                    dataBuilder.addServiceUuid(pUuid);
                    if(adv_info.ServiceData != null) {
                        Log.d(TAG,"Setting Service data");
                        dataBuilder.addServiceData(pUuid,
                               adv_info.ServiceData.getBytes(Charset.forName("UTF-8")));
                     }
                }
                if((adv_info.ManufacturerId != 0) ||
                    (adv_info.ManufacturerData != null)) {
                    Log.d(TAG,"Setting Manufacture data");
                    dataBuilder.addManufacturerData(adv_info.ManufacturerId,
                            adv_info.ManufacturerData.getBytes(Charset.forName("UTF-8")));
                }
                mAdvData = dataBuilder.build();
                Log.d(TAG,"BuildAdvertisementData done");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG,"Exception : " + e.toString());
        }
        return false;
    }

    public boolean SetPeriodicAdvParams(){
        Log.d(TAG,"SetPeriodicAdvParams");
        try {
            if(adv_info.Periodic){
                mPeriodicAdvParams = new PeriodicAdvertisingParameters.Builder()
                        .setIncludeTxPower(adv_info.IncludePower)
                        .setInterval(adv_info.PerAdvInterval)
                        .build();
                return true;
            } else {
                mPeriodicAdvParams = null;
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG,"Exception : " + e.toString());
        }
        return false;
    }

    public boolean SetPeriodicAdvData(){
        Log.d(TAG,"SetPeriodicAdvData");
        if(adv_info.Periodic){
            mPeriodicData = mAdvData;
        } else {
            mPeriodicData = null;
        }
        return true;
    }

    public boolean SetScanResponseData(){
        Log.d(TAG,"SetScanResponseData");
        if(adv_info.Scannable){
            mScanResponseData = mAdvData;
            if(!adv_info.Legacy){
                mAdvData = null;
            }
        } else {
            mScanResponseData = null;
        }
        return true;
    }

    public boolean StartAdvertisement(){
        Log.d(TAG,"StartAdvertisement");
        boolean status = false;

        if(mAdvertiser == null) {
            Log.e(TAG,"Advertise is null, adv_id : "+ adv_id);
            return status;
        }

        status = BuildAdvertisementParameters();
        if(!status) {
            Log.e(TAG,"BuildAdvertisementParameters failed, adv_id : "+ adv_id);
            return status;
        }

        status =  BuildAdvertisementData();
        if(!status) {
            Log.e(TAG,"BuildAdvertisementData failed, adv_id : "+ adv_id);
            return status;
        }

        status = SetPeriodicAdvParams();
        if(!status) {
            Log.e(TAG,"SetPeriodicAdvParams failed, adv_id : "+ adv_id);
            return status;
        }

        status = SetPeriodicAdvData();
        if(!status) {
            Log.e(TAG,"SetPeriodicAdvData failed, adv_id : "+ adv_id);
            return status;
        }

       status = SetScanResponseData();
       if(!status) {
           Log.e(TAG,"SetScanResponseData failed, adv_id : "+ adv_id);
           return status;
        }

        try {
            if(adv_info.Legacy){
                mAdvertiser.startAdvertising(mAdvSettings,mAdvData,mAdvCallback);
            } else {
                Log.d(TAG, "Max Adv Events:"+adv_info.MaxAdvEvents);
                mAdvertiser.startAdvertisingSet(mAdvParams,mAdvData,
                                                mScanResponseData,mPeriodicAdvParams,
                                                mPeriodicData,0,adv_info.MaxAdvEvents,mAdvSetCallback);
            }
            return status;
        } catch (Exception e) {
            Log.e(TAG,"Exception : "+ e.toString());
            return false;
        }
    }

    public boolean StopAdvertisement() {
        Log.d(TAG,"StopAdvertisement");
        try {
            if(adv_info.Legacy){
                mAdvertiser.stopAdvertising(mAdvCallback);
                return true;
            } else {
                mAdvertiser.stopAdvertisingSet(mAdvSetCallback);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG,"Exception : "+ e.toString());
        }
        return false;
    }
}
