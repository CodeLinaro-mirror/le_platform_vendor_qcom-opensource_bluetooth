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
import android.os.Message;
import java.util.ArrayList;

public class AdvertiserService extends Service {

    private static String TAG = "ADV SERVICE";
    private ArrayList<AdvertiserEntity> advertisements;
    private static final int MAX_ADVERTISEMENTS = 16;
    private int[] removed_indices = new int[MAX_ADVERTISEMENTS];
    private static int INVALID_INDEX = -1;
    private static int removed_count = INVALID_INDEX;
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
        // max size of ArrayList is limited to 16 using MAX_ADVERTISEMENTS
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
            if(advInstance.getAdv_status() == AdvertiserEntity.ADV_STARTED) {
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

    public int getAdvertisingIndex() {
        int i;
        int temp_index = 0;
        for(i=0; i< MAX_ADVERTISEMENTS; i++) {
            if(advertisements.size() > 0){
                if(removed_count != INVALID_INDEX) {
                    temp_index = removed_indices[removed_count];
                    removed_count --;
                    break;
                } else {
                    temp_index = advertisements.size();
                    break;
                }
            }
            else {
                temp_index = 0;
                break;
            }
        }
        return temp_index;
    }

    public int startAdvertising(Adv adv_info){
        boolean status = false;
        int index = getAdvertisingIndex();

        //since array index starts a 0, size is used for next index
        if(index >= MAX_ADVERTISEMENTS){
            Log.e(TAG,"Max advertisements limit reached, abort");
            StringBuilder PrintStr = new StringBuilder();
            PrintStr.setLength(0);
            PrintStr.append("Max advertisements limit reached!");
            SocketServer.sendSocketData(PrintStr.toString());
            return MAX_ADVERTISEMENTS;
        }
        AdvertiserEntity advInstance = new AdvertiserEntity(adv_info,index, mAdvServiceCb);
        advertisements.add(index,advInstance);
        status = advInstance.StartAdvertisement();
        Log.d(TAG,"new Adv instance is created, total advs : " + advertisements.size());
        return index;
    }

    public int getAdvInfoArrayIndex(int Adv_id) {
        for(int i = 0; i < advertisements.size(); i++) {
            AdvertiserEntity advEnt = advertisements.get(i);
            if(advEnt.getAdv_id() == Adv_id) {
                Log.d(TAG, "found index to be deleted,i="+i);
                return i;
            }
        }
        return INVALID_INDEX;
    }

    public boolean stopAdvertising(int Adv_id) {
        boolean status = false;
        Log.d(TAG,"Adv id"+ Adv_id + "is deleted");
        if(Adv_id >= MAX_ADVERTISEMENTS){
            Log.e(TAG,"stopAdvertising invalid adv_id");
            return status;
        }
        int adv_index = getAdvInfoArrayIndex(Adv_id);
        if(INVALID_INDEX != adv_index){
            AdvertiserEntity advInstance = advertisements.get(adv_index);
            status = advInstance.StopAdvertisement();
        }
        else {
            Log.e(TAG, "Adv Index not found");
        }
        return status;
    }

    public class AdvServiceCallback {
        Message msg;
        public void onAdvStarted(int adv_id){
            Log.d(TAG,"Advertising Started on adv_id : " + adv_id);
            msg = BleAppService.msghandler.obtainMessage(
                      BleAppService.MSG_MA_ADV_STARTED, Integer.toString(adv_id));
            BleAppService.msghandler.sendMessage(msg);
        }
        public void onAdvStopped(int adv_id){
            Log.d(TAG,"Advertising Stopped on adv_id : " + adv_id +
                    "Deleting the adv instance");
            int adv_index = getAdvInfoArrayIndex(adv_id);
            if(INVALID_INDEX != adv_index) {
                advertisements.remove(adv_index);
                Log.d(TAG,"Total adv instances after deleting: " + advertisements.size());
                removed_count++;
                removed_indices[removed_count] = adv_index;

                msg = BleAppService.msghandler.obtainMessage(
                          BleAppService.MSG_MA_ADV_STOPPED, Integer.toString(adv_index));
                BleAppService.msghandler.sendMessage(msg);
            }
            else {
                Log.e(TAG, "Adv Index not found");
            }
        }
    }
}
