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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import java.util.ArrayList;

public class AdvertiserService extends Service {

    private static String TAG = "ADV SERVICE";
    private ArrayList<AdvertiserEntity> advertisements;
    private static final int MAX_ADVERTISEMENTS = 4;
    private AdvServiceCallback mAdvServiceCb;

    public class LocalBinder extends Binder {
        AdvertiserService getService() {
            return AdvertiserService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind");
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    public AdvertiserService() {
        Log.d(TAG,"AdvertiserService");
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"onCreate");
        // max size of ArrayList is limited to 4 using MAX_ADVERTISEMENTS
        advertisements = new ArrayList<AdvertiserEntity>();
        mAdvServiceCb = new AdvServiceCallback();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        for(int i = 0; i < advertisements.size(); i++) {
            AdvertiserEntity advInstance = advertisements.get(i);
            if(advInstance.adv_status == AdvertiserEntity.ADV_STARTED) {
                advInstance.StopAdvertisement();
            }
        }
        advertisements.clear();
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG,"onTaskRemoved");
        super.onTaskRemoved(rootIntent);
    }

    public int startAdvertising(Adv adv_info){
        boolean status = false;
        int index = advertisements.size();//since array index starts a 0, size is used for next index
        if(index >= MAX_ADVERTISEMENTS){
            Log.e(TAG,"Max advertisements limit reached, abort");
            return MAX_ADVERTISEMENTS;
        }
        AdvertiserEntity advInstance = new AdvertiserEntity(adv_info,index, mAdvServiceCb);
        advertisements.add(advInstance);
        status = advInstance.StartAdvertisement();
        Log.d(TAG,"new Adv instance is created, total advs : " + advertisements.size());
        return index;
    }

    public boolean stopAdvertising(int Adv_id) {
        boolean status = false;
        if(Adv_id >= MAX_ADVERTISEMENTS){
            Log.e(TAG,"stopAdvertising invalid adv_id");
            return status;
        }
        AdvertiserEntity advInstance = advertisements.get(Adv_id);
        status = advInstance.StopAdvertisement();
        return status;
    }

    public class AdvServiceCallback {
        public void onAdvStarted(int adv_id){
            Log.d(TAG,"Advertising Started on adv_id : " + adv_id);
        }
        public void onAdvStopped(int adv_id){
            Log.d(TAG,"Advertising Stopped on adv_id : " + adv_id +
                    "Deleting the adv instance");
            advertisements.remove(adv_id);
            Log.d(TAG,"Total adv instances after deleting: " + advertisements.size());
        }
    }
}
