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

import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.telecom.Call;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public final class ScanParams {
    private static final String TAG = "ScanParams";

    public String invalidParam = "XX";

    public String DeviceName;
    public String DeviceAddress;
    public String ServiceUuid;
    public String SvcMaskUuid;
    public String ManufacturerId;
    public String ManufacturerData;
    public String ManuMaskData;
    public String ServiceDataUuid;
    public String ServiceData;
    public String SvcDataMask;
    public int ScanMode;
    public int CallbackType;
    public int ResultType;
    public int NumOfAdvMatches;
    public int MatchMode;
    public int ReportDelay;
    public static int ScanTO;

    private String mDeviceName;
    private String mDeviceAddress;
    private ParcelUuid mUuid;
    private ParcelUuid mUuidMask;
    private ParcelUuid mServiceDataUuid;
    private byte[] mServiceData;
    private byte[] mServiceDataMask;
    private int manufacturerId;
    private byte[] manufacturerData;
    private byte[] manufacturerDataMask;

    public ArrayList<ScanFilter> mScanFilters;

    public ScanParams(String devName, String DevAddress, String ServUUID, String srvcMaskUUID,
            String ManuId, String ManuData, String ManuMaskData, String SrvcDataUUID,
            String SrvcData, String srvcDataMask, int scanMode, int cbType, int resultType,
            int NumAdvMatches, int matchMode, int reportDelay, int scanTO) {
        DeviceName = devName;
        DeviceAddress = DevAddress;
        ServiceUuid = ServUUID;
        SvcMaskUuid = srvcMaskUUID;
        ServiceData = SrvcData;
        SvcDataMask = srvcDataMask;
        ScanMode = scanMode;
        CallbackType = cbType;
        ResultType = resultType;
        NumOfAdvMatches = NumAdvMatches;
        MatchMode = matchMode;
        ReportDelay = reportDelay;
        ScanTO = scanTO;
    }

    public void parseScanParams() {
        Log.d(TAG, "parseScanParams");
        if(this.DeviceName != null && (this.DeviceName.length() > 2)
          && (this.DeviceName != invalidParam)) {
            Log.d(TAG, "DeviceName::" + this.DeviceName);
            mDeviceName = this.DeviceName;
        }

        if(this.DeviceAddress != null && (this.DeviceAddress.length() > 2)
          && (this.DeviceAddress != invalidParam)) {
            Log.d(TAG, "DeviceAddress::" + this.DeviceAddress);
            mDeviceAddress = this.DeviceAddress;
        }

        if(this.ServiceUuid != null && (this.ServiceUuid.length() > 2)
          && (this.ServiceUuid != invalidParam)) {
            Log.d(TAG, "mServiceUuid ::"+this.ServiceUuid );
            mUuid = ParcelUuid.fromString(this.ServiceUuid );
        }

        if(this.SvcMaskUuid != null && (this.SvcMaskUuid.length() > 2)
          && (this.SvcMaskUuid != invalidParam)) {
            Log.d(TAG, "mServiceUuidMask ::"+this.SvcMaskUuid );
            mUuidMask = ParcelUuid.fromString(this.SvcMaskUuid );
        }

        if(this.ManufacturerId != null && (this.ManufacturerId.length() > 2)
          && (this.ManufacturerId != invalidParam)) {
            Log.d(TAG, "ManufacturerId  ::"+this.ManufacturerId );
            manufacturerId = Integer.parseInt(this.ManufacturerId );
        }

        if(this.ManufacturerData != null && (this.ManufacturerData.length() > 2)
          && (this.ManufacturerData != invalidParam)) {
            String[] manuData = this.ManufacturerData.split(",");
            if(manuData!= null && manuData.length>0) {
                manufacturerData = new byte[manuData.length];
                for(int i=0; i< manuData.length; i++) {
                    manufacturerData[i] = Byte.parseByte(manuData[i]);
                }
                if(manufacturerData != null && manufacturerData.length >0) {
                    for(int j=0; j< manufacturerData.length; j++) {
                        Log.d(TAG, "manufacturerData::"+manufacturerData[j]);
                    }
                }
            }
        }

        if(this.ManuMaskData != null && (this.ManuMaskData.length() > 2)
          && (this.ManuMaskData != invalidParam)) {
            String[] manuDataMask = this.ManuMaskData.split(",");
            if(manuDataMask!= null && manuDataMask.length>0) {
                manufacturerDataMask = new byte[manuDataMask.length];
                for(int i=0; i< manuDataMask.length; i++) {
                    manufacturerDataMask[i] = Byte.parseByte(manuDataMask[i]);
                }
                if(manufacturerDataMask != null && manufacturerDataMask.length >0) {
                    for(int j=0; j< manufacturerDataMask.length; j++) {
                        Log.d(TAG, "manufacturerDataMask::"+manufacturerDataMask[j]);
                    }
                }
            }
        }

        if(this.ServiceDataUuid != null && (this.ServiceDataUuid.length() > 2)
          && (this.ServiceDataUuid != invalidParam)) {
            Log.d(TAG, "parsedData::"+this.ServiceDataUuid);
            mServiceDataUuid = ParcelUuid.fromString(this.ServiceDataUuid);
        }

        if(this.ServiceData != null && (this.ServiceData .length() > 2)
          && (this.ServiceData != invalidParam)) {
            String[] svcData = this.ServiceData.split(",");
            if(svcData!= null && svcData.length>0) {
                mServiceData = new byte[svcData.length];
                for(int i=0; i< svcData.length; i++) {
                    mServiceData[i] = Byte.parseByte(svcData[i]);
                }
                if(mServiceData != null && mServiceData.length >0) {
                    for(int j=0; j< mServiceData.length; j++) {
                        Log.d(TAG, "mServiceData::"+mServiceData[j]);
                    }
                }
            }
        }

        if(this.SvcDataMask != null && (this.SvcDataMask.length() > 2)
          && (this.SvcDataMask != invalidParam)) {
            String[] svcDataMask = this.SvcDataMask.split(",");
            if(svcDataMask!= null && svcDataMask.length>0) {
                mServiceDataMask = new byte[svcDataMask.length];
                for(int i=0; i< svcDataMask.length; i++) {
                    mServiceDataMask[i] = Byte.parseByte(svcDataMask[i]);
                }
                if(mServiceDataMask != null && mServiceDataMask.length >0) {
                    for(int j=0; j< mServiceDataMask.length; j++) {
                        Log.d(TAG, "mServiceDataMask::"+mServiceDataMask[j]);
                    }
                }
            }
        }
    }

    private void setScanFilters() {
        mScanFilters = new ArrayList<ScanFilter>();
        Log.d(TAG, "setScanFilters ");

        if(mDeviceAddress != null && mDeviceAddress.length() > 0) {
            try {
                if(ScannerService.LOG_LEVEL >= 2)
                    Log.d(TAG, "Device address filter set to " + mDeviceAddress);
                mScanFilters.add(new ScanFilter.Builder().setDeviceAddress(mDeviceAddress).build());
            }
            catch(IllegalArgumentException exe){
                if(ScannerService.LOG_LEVEL >= 1)
                    Log.e(TAG, "Error: " + exe);
                return;
            }
        }

        if(mDeviceName != null && mDeviceName.length() > 0) {
            Log.d(TAG, "Device name filter set to " + mDeviceName);
            mScanFilters.add(new ScanFilter.Builder().setDeviceName(mDeviceName).build());
        }

        if(mUuid != null) {
            try {
                if(ScannerService.LOG_LEVEL >= 2)
                    //Log.d(TAG, "mUuidMask::"+mUuidMask);
                mScanFilters.add(new ScanFilter.Builder().setServiceUuid(mUuid).build());
            }
            catch(IllegalArgumentException exe){
                if(ScannerService.LOG_LEVEL >= 1)
                    Log.e(TAG, "Error: " + exe);
                return;
            }
        }

        if(manufacturerId > 0 && (manufacturerData!= null && manufacturerData.length > 0)) {
            try {
                if(ScannerService.LOG_LEVEL >= 2)
                    Log.d(TAG, "manufacturerId >0:: Applying filter for Manufacturer data");
                mScanFilters.add(new ScanFilter.Builder().setManufacturerData(manufacturerId,
                  manufacturerData, manufacturerDataMask).build());
            }
            catch(IllegalArgumentException exe){
                if(ScannerService.LOG_LEVEL >= 1)
                    Log.e(TAG, "Error: " + exe);
                return;
            }
        }

        if(mServiceDataUuid != null && (mServiceData!= null && mServiceData.length > 0)) {
            try {
                if(ScannerService.LOG_LEVEL >= 2) {
                    Log.d(TAG, "mServiceDataUuid != null:: Applying filter for Service data");
                    Log.d(TAG, "mServiceDataUuid::" + mServiceDataUuid);
                }

                if (mServiceData != null && mServiceData.length > 0) {
                    for (int j = 0; j < mServiceData.length; j++) {
                        if(ScannerService.LOG_LEVEL >= 2)
                            Log.d(TAG, "mServiceData::" + mServiceData[j]);
                    }
                }
                if (mServiceDataMask != null && mServiceDataMask.length > 0) {
                    for (int j = 0; j < mServiceDataMask.length; j++) {
                        if(ScannerService.LOG_LEVEL >= 2)
                            Log.d(TAG, "mServiceDataMask::" + mServiceDataMask[j]);
                    }
                }
                mScanFilters.add(new ScanFilter.Builder().setServiceData(mServiceDataUuid,
                  mServiceData, mServiceDataMask).build());
            }
            catch (IllegalArgumentException exe){
                if(ScannerService.LOG_LEVEL >= 1)
                    Log.e(TAG, "Error: " + exe);
                return;
            }
        }
    }

    public ArrayList<ScanFilter> parseScanFilter() {
        Log.d(TAG, "parseScanfilter");
        this.parseScanParams();
        this.setScanFilters();
        return mScanFilters;
    }

    public ScanSettings getScanSettings() {
        return new ScanSettings.Builder()
                .setCallbackType(this.CallbackType)
                .setReportDelay(this.ReportDelay)
                .setNumOfMatches(this.NumOfAdvMatches)
                .setScanMode(this.ScanMode)
                .setMatchMode(this.MatchMode)
                .setScanResultType(this.ResultType)
                .build();
    }
}

