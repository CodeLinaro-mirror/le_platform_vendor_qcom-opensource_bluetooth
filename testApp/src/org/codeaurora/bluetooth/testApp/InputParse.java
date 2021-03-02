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

import android.util.Log;

public class InputParse {
    private static final String TAG = "InputParse";

    public InputParse()    {
        Log.d(TAG, "InputParse()");
    }

    public Adv AdvParse(String input){
        Log.d(TAG, "AdvParse()");
        Adv advParam = new Adv();
        String tmp[] = input.split(";");
        String[] tmp2;
        int i=0;
        for(i=0; i<tmp.length; i++){
            tmp2 = tmp[i].split(":",2);
            if(tmp2.length == 2) {
                if (tmp2[0].equals("TxPower")) {
                    advParam.TxPower = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("Legacy")) {
                    advParam.Legacy = Boolean.parseBoolean(tmp2[1]);
                } else if (tmp2[0].equals("Periodic")) {
                    advParam.Periodic = Boolean.parseBoolean(tmp2[1]);
                } else if (tmp2[0].equals("PerAdvInterval")) {
                    advParam.PerAdvInterval = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("Connectable")) {
                    advParam.Connectable = Boolean.parseBoolean(tmp2[1]);
                } else if (tmp2[0].equals("Scannable")) {
                    advParam.Scannable = Boolean.parseBoolean(tmp2[1]);
                } else if (tmp2[0].equals("Anonymous")) {
                    advParam.Anonymous = Boolean.parseBoolean(tmp2[1]);
                } else if (tmp2[0].equals("IncludePower")) {
                    advParam.IncludePower = Boolean.parseBoolean(tmp2[1]);
                } else if (tmp2[0].equals("PrimaryPhy")) {
                    advParam.PrimaryPhy = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("SecondaryPhy")) {
                    advParam.SecondaryPhy = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("MaxExtAdvEvents")) {
                    advParam.MaxAdvEvents = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("Interval")) {
                    advParam.Interval = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("TimeOutLegacy")) {
                    advParam.TimeOutLegacy = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("AdvertiseMode")) {
                    advParam.AdvertiseMode = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("ServiceUuid")) {
                    advParam.ServiceUuid = tmp2[1];
                } else if (tmp2[0].equals("ManufacturerId")) {
                    advParam.ManufacturerId = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("ManufacturerData")) {
                    advParam.ManufacturerData = tmp2[1];
                } else if (tmp2[0].equals("ServiceDataUuid")) {
                    advParam.ServiceDataUuid = tmp2[1];
                } else if (tmp2[0].equals("ServiceData")) {
                    advParam.ServiceData = tmp2[1];
                } else {
                    break;
                }
            }else{
                break;
            }
        }

        if(i == tmp.length){
            return advParam;
        }else{
            return null;
        }
    }

    public Scan ScanParse(String input){
        Log.d(TAG, "ScanParse()");
        Scan scanParam = new Scan();
        String tmp[] = input.split(";");
        String[] tmp2;
        int i=0;
        for(i=0; i<tmp.length; i++){
            tmp2 = tmp[i].split(":",2);
            if(tmp2.length == 2) {
                if (tmp2[0].equals("DeviceName")) {
                    scanParam.DeviceName = tmp2[1];
                } else if (tmp2[0].equals("DeviceAddress")) {
                    scanParam.DeviceAddress = tmp2[1];
                } else if (tmp2[0].equals("ServiceUuid")) {
                    scanParam.ServiceUuid = tmp2[1];
                } else if (tmp2[0].equals("SvcMaskUuid")) {
                    scanParam.SvcMaskUuid = tmp2[1];
                } else if (tmp2[0].equals("ManufacturerId")) {
                    scanParam.ManufacturerId = tmp2[1];
                } else if (tmp2[0].equals("ManufacturerData")) {
                    scanParam.ManufacturerData = tmp2[1];
                } else if (tmp2[0].equals("ManuMaskData")) {
                    scanParam.ManufacturerMaskData = tmp2[1];
                } else if (tmp2[0].equals("ServiceDataUuid")) {
                    scanParam.ServiceDataUuid = tmp2[1];
                } else if (tmp2[0].equals("ServiceData")) {
                    scanParam.ServiceData = tmp2[1];
                } else if (tmp2[0].equals("SvcDataMask")) {
                    scanParam.SvcDataMask = tmp2[1];
                } else if (tmp2[0].equals("ScanMode")) {
                    scanParam.ScanMode = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("CallbackType")) {
                    scanParam.CallbackType = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("ResultType")) {
                    scanParam.ResultType = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("NumOfAdvMatches")) {
                    scanParam.NumOfAdvMatches = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("MatchMode")) {
                    scanParam.MatchMode = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("ReportDelay")) {
                    scanParam.ReportDelay = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("Legacy")) {
                    scanParam.legacy = Boolean.parseBoolean(tmp2[1]);
                } else {
                    break;
                }
            }else{
                break;
            }
        }

        if(i == tmp.length){
            return scanParam;
        }else{
            return null;
        }
    }

    public ConnUpdate ConnUpdateParse(String input){
        Log.d(TAG, "ConnUpdateParse()");
        ConnUpdate connUpdateParam = new ConnUpdate();
        String tmp[] = input.split(";");
        String[] tmp2;
        int i=0;
        for(i=0; i<tmp.length; i++){
            tmp2 = tmp[i].split(":",2);
            if(tmp2.length == 2) {
                if (tmp2[0].equals("ConnIntervalMin")) {
                    connUpdateParam.ConnIntervalMin = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("ConnIntervalMax")) {
                    connUpdateParam.ConnIntervalMax = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("ConnSlaveLatency")) {
                    connUpdateParam.ConnSlaveLatency = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("ConnSupTO")) {
                    connUpdateParam.ConnSupTO = Integer.parseInt(tmp2[1]);
                } else {
                    break;
                }
            }else{
                break;
            }
        }

        if(i == tmp.length){
            return connUpdateParam;
        }else{
            return null;
        }
    }

    public PhyUpdate PhyUpdateParse(String input){
        Log.d(TAG, "PhyUpdateParse()");
        PhyUpdate phyUpdateParam = new PhyUpdate();
        String tmp[] = input.split(";");
        String[] tmp2;
        int i=0;
        for(i=0; i<tmp.length; i++){
            tmp2 = tmp[i].split(":",2);
            if(tmp2.length == 2) {
                if (tmp2[0].equals("Tx_Phy")) {
                    phyUpdateParam.txPhy = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("Rx_Phy")) {
                    phyUpdateParam.rxPhy = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("Phy_Opt")) {
                    phyUpdateParam.phyOpt = Integer.parseInt(tmp2[1]);
                } else {
                    break;
                }
            }else{
                break;
            }
        }

        if(i == tmp.length){
            return phyUpdateParam;
        }else{
            return null;
        }
    }

    public DataTx DataTxParse(String input){
        Log.d(TAG, "DataTxParse()");
        DataTx dataTxParam = new DataTx();
        String tmp[] = input.split(";");
        String[] tmp2;
        int i=0;
        for(i=0; i<tmp.length; i++){
            tmp2 = tmp[i].split(":",2);
            if(tmp2.length == 2) {
                if (tmp2[0].equals("Mtu_Size")) {
                    dataTxParam.Mtu_Size = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("Packet_Size")) {
                    dataTxParam.Packet_Size = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("Num_Packets")) {
                    dataTxParam.Num_Packets = Long.parseLong(tmp2[1]);
                } else if (tmp2[0].equals("TxService")) {
                    dataTxParam.txService = tmp2[1];
                } else if (tmp2[0].equals("TxChar")) {
                    dataTxParam.txChar = tmp2[1];
                } else {
                    break;
                }
            }else{
                break;
            }
        }

        if(i == tmp.length){
            return dataTxParam;
        }else{
            return null;
        }
    }

    public DataRx DataRxParse(String input){
        Log.d(TAG, "DataRxParse()");
        DataRx dataRxParam = new DataRx();
        String tmp[] = input.split(";");
        String[] tmp2;
        int i=0;
        for(i=0; i<tmp.length; i++){
            tmp2 = tmp[i].split(":",2);
            if(tmp2.length == 2) {
                if (tmp2[0].equals("Mtu_Size")) {
                    dataRxParam.Mtu_Size = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("NotificationsTimeInSec")) {
                    dataRxParam.NotificationsTimeInSec = Long.parseLong(tmp2[1]);
                } else if (tmp2[0].equals("NotificationsTimeInMin")) {
                    dataRxParam.NotificationsTimeInMin = Long.parseLong(tmp2[1]);
                } else if (tmp2[0].equals("RxService")) {
                    dataRxParam.rxService = tmp2[1];
                } else if (tmp2[0].equals("RxChar")) {
                    dataRxParam.rxChar = tmp2[1];
                } else {
                    break;
                }
            }else{
                break;
            }
        }

        if(i == tmp.length){
            return dataRxParam;
        }else{
            return null;
        }
    }

    public LatencyTest LatencyTestParse(String input){
        Log.d(TAG, "LatencyTestParse()");
        LatencyTest latencyTestParam = new LatencyTest();
        String tmp[] = input.split(";");
        String[] tmp2;
        int i=0;
        for(i=0; i<tmp.length; i++){
            tmp2 = tmp[i].split(":",2);
            if(tmp2.length == 2) {
                if (tmp2[0].equals("LatencyService")) {
                    latencyTestParam.latService = tmp2[1];
                } else if (tmp2[0].equals("LatencyChar")) {
                    latencyTestParam.latChar = tmp2[1];
                } else {
                    break;
                }
            }else{
                break;
            }
        }

        if(i == tmp.length){
            return latencyTestParam;
        }else{
            return null;
        }
    }

    public ReadWriteOp ReadWriteOpParse(String input){
        Log.d(TAG, "readWriteOpParse()");
        ReadWriteOp readWriteOpParam = new ReadWriteOp();
        String tmp[] = input.split(";");
        String[] tmp2;
        int i=0;
        for(i=0; i<tmp.length; i++){
            tmp2 = tmp[i].split(":",2);
            if(tmp2.length == 2) {
                if (tmp2[0].equals("Operation")) {
                    readWriteOpParam.operation = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("ServiceUuid")) {
                    readWriteOpParam.Srvc_uuid = tmp2[1];
                } else if (tmp2[0].equals("CharUuid")) {
                    readWriteOpParam.Char_uuid = tmp2[1];
                } else if (tmp2[0].equals("DescUuid")) {
                    readWriteOpParam.Desc_uuid = tmp2[1];
                } else if (tmp2[0].equals("Value")) {
                    readWriteOpParam.Value = tmp2[1];
                } else if (tmp2[0].equals("WriteType")) {
                    readWriteOpParam.Write_type = Integer.parseInt(tmp2[1]);
                } else if (tmp2[0].equals("FormatType")) {
                    readWriteOpParam.Format_type = Integer.parseInt(tmp2[1]);
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        if(i == tmp.length) {
            return readWriteOpParam;
        } else {
            return null;
        }
    }

}
