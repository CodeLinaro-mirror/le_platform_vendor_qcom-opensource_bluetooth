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

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;
import android.Manifest;
import android.util.Log;
import android.util.*;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Message;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.RemoteException;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.Scanner;
import java.lang.*;
import java.util.concurrent.TimeUnit;
import java.io.*;

import libcore.io.IoUtils;

import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattCharacteristic;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;


public class GattClient {
    public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR
                                 = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final String TAG = "GattClient";
    public static int LOG_LEVEL = 6;

    /* MTU size required for MTU exchange */
    public static final int MTU_SIZE_MIN = 23;
    public static final int MTU_SIZE_MAX = 512;
    public static final int TRANSPORT_LE = 2;

    public int mtu_size = MTU_SIZE_MIN;

    /* Variable to update phy */
    public static int txPhyReq;
    public static int rxPhyReq;

    public static BluetoothAdapter bleAdapter;

    public BleGattClient mgattClient;
    private Context mcontext;
    private BluetoothDevice mDevice = null;

    //Actions
    public GattClientMessageHandler mGattClientHandler = null;
    public static final int MSG_BLE_SCAN_DEV_FOUND = 0;
    public static final int MSG_START_BLE_CONNECT = 1;
    public static final int MSG_START_BLE_CONN_UPDATE = 2;
    public static final int MSG_START_BLE_PHY_UPDATE = 3;
    public static final int MSG_START_BLE_READ_PHY = 4;
    public static final int MSG_START_BLE_GATT_DISC = 5;
    public static final int MSG_START_BLE_GATT_WRITE_READ_CHAR = 6;
    public static final int MSG_START_BLE_GATT_WRITE_READ_DESC = 7;
    public static final int MSG_START_BLE_GATT_CONFIGURE_MTU_SIZE = 8;
    public static final int MSG_START_BLE_GATT_REFRESH_SERVICES = 9;
    public static final int MSG_START_BLE_PAIR_DEV = 10;
    public static final int MSG_START_BLE_UNPAIR_DEV = 11;
    public static final int MSG_START_BLE_GATT_DISCONNECT = 12;
    public static final int MSG_REGISTER_BLE_GATT_NOTIFICATIONS = 13;
    public static final int MSG_DEREGISTER_BLE_GATT_NOTIFICATIONS = 14;
    public static final int MSG_START_BLE_GATT_RELIABLE_WRITE = 15;
    public static final int MSG_START_BLE_GATT_ABORT_RELIABLE_WRITE = 16;
    public static final int MSG_START_BLE_GATT_EXECUTE_WRITE = 31;
  
    public static final int MSG_REM_DEV_FAILED_TO_CONNECT = 17;
    public static final int MSG_REFRESH_SERV_DONE = 18;
    public static final int MSG_REM_DEV_DISCONNECTED = 19;
    public static final int MSG_REM_DEV_CONNECTED = 20;
    public static final int MSG_SERVC_DISC_DONE = 21;
    public static final int MSG_CONN_UPDATE_DONE = 22;
    public static final int MSG_PHY_UPDATE_DONE = 23;
    public static final int MSG_CHAR_READ_DONE = 24;
    public static final int MSG_CHAR_WRITE_DONE = 25;
    public static final int MSG_DESC_READ_DONE = 26;
    public static final int MSG_DESC_WRITE_DONE = 27;
    public static final int MSG_PHY_READ_DONE = 28;
    public static final int MSG_MTU_EXCHANGE_DONE = 29;
    public static final int MSG_GC_REM_DEV_PAIRED = 30;
    public static final int MSG_CHAR_CHANGED = 32;

    private static final int GATT_WRITE = 1;
    private static final int GATT_READ = 2;
    private static final int GATT_OPTYPE_UUID = 1;
    private static final int GATT_OPTYPE_INSID = 2;
  
    private static int length_offset = 0;
    private static String written_value;
    private static String offset_value;
    private boolean reliable_write = false;
    private static int total_length = 0;
    private ReadWriteOp RdWrReliableClass;
  
    private List<UUID> mServiceUUID;
    private List<UUID> mCharUUID;
    private List<UUID> mDescUUID;
    private List<BluetoothGattService> mServices;
    private List<BluetoothGattCharacteristic> mCharacteristics;
    private List<BluetoothGattDescriptor> mDescriptors;

    StringBuilder PrintStr = new StringBuilder();

    public GattClient(Context mcontext) {
        this.mcontext = mcontext;
        /* Initialize classes */
        mgattClient = new BleGattClient(mcontext);
        /* Start Message handler */
        HandlerThread thread = new HandlerThread("GattClientHandler");
        thread.start();
        Looper looper = thread.getLooper();

        mGattClientHandler = new GattClientMessageHandler(mcontext, looper);

        mServices = new ArrayList<BluetoothGattService>();
        mCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
        mDescriptors = new ArrayList<BluetoothGattDescriptor>();

        mServiceUUID = new ArrayList<UUID>();
        mCharUUID = new ArrayList<UUID>();
        mDescUUID = new ArrayList<UUID>();
    }

    /* function to print the message on display */
    private void showMessage(String msg) {
        Toast.makeText(mcontext, msg, Toast.LENGTH_SHORT).show();
    }

    /* Connection Class */
    public class BleGattClient {
        private static final String TAG = "BleGattClient";

        private BluetoothGatt mBluetoothGatt;
        private BluetoothGattService mService;
        private BluetoothGattCharacteristic mCharacteristic;
        private BluetoothGattCharacteristic mreadChar;
        private Context context;
        private int mState;
        private int GATT_SUCCESS = 0x00;
    
        Message msg;

        public BleGattClient(Context context) {
            this.context = context;
        }

        /**
         * GATT callbacks
         */
        private final BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.i(TAG, "onConnectionStateChange device :" + gatt.getDevice() +
                      " status :" + status + " newState :" + newState);
                mState = newState;
                if (gatt.getDevice() == null || status != GATT_SUCCESS) {
                    if(GattClient.LOG_LEVEL >= 1) {
                        Log.e(TAG, "onConnectionStateChange:Unexpected error! mstate: " +  mState);
                    }
                    /*Send Message to Message Handler */
                    msg = mGattClientHandler.obtainMessage(MSG_REM_DEV_FAILED_TO_CONNECT, null);
                    mGattClientHandler.sendMessage(msg);
                    return;
                }

                int bondState = mDevice.getBondState();

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "onConnectionStateChange:DISCONNECTED "
                            + " remoteDevice: " + gatt.getDevice().getAddress());
                    msg = mGattClientHandler.obtainMessage(MSG_REM_DEV_DISCONNECTED, null);
                    mGattClientHandler.sendMessage(msg);
                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "onConnectionStateChange:CONNECTED "
                            + " remoteDevice: " + gatt.getDevice().getAddress());
                    msg = mGattClientHandler.obtainMessage(
                               MSG_REM_DEV_CONNECTED, gatt.getDevice().getName());
                    mGattClientHandler.sendMessage(msg);
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                       Log.i(TAG, "Device paired");
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                PrintStr.setLength(0);
                PrintStr.append("Gatt Service discovery!!");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "onService discovery success");
                    mServices = gatt.getServices();
                    if (mServices == null || mServices.size() <= 0) {
                        Log.e(TAG, "no services found");
                        return;
                    }
                    for (BluetoothGattService service : mServices) {
                        Log.d(TAG, "Found service: " + service.getUuid());
                        PrintStr.append("\n------------------------------------------------\n");
                        PrintStr.append("Service UUID:");
                        PrintStr.append(service.getUuid());
                        mServiceUUID.add(service.getUuid());
                        mCharacteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic
                                      characteristic : mCharacteristics) {
                            Log.d(TAG, "Found Char: " + characteristic.getUuid());
                            PrintStr.append("\nCharacteristic UUID:");
                            PrintStr.append(characteristic.getUuid());
                            mCharUUID.add(characteristic.getUuid());
                            mDescriptors = characteristic.getDescriptors();
                            for (BluetoothGattDescriptor descriptor : mDescriptors) {
                                Log.d(TAG, "Found Desc: " + descriptor.getUuid());
                                PrintStr.append("\nDescriptor UUID:");
                                PrintStr.append(descriptor.getUuid());
                                mDescUUID.add(descriptor.getUuid());
                             }
                         }
                    }
                    SocketServer.sendSocketData(PrintStr.toString());
                    msg = mGattClientHandler.obtainMessage(MSG_SERVC_DISC_DONE, null);
                    mGattClientHandler.sendMessage(msg);
                  } else {
                     Log.d(TAG, "onServicesDiscovered received: " + status);
                  }
            }

            @Override
            public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency,
                                                int timeout, int status) {
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "on Conn updated:"
                         + " interval=" + interval + " latency=" + latency
                        + " timeout=" + timeout + " status=" + status);
                    if(GattClient.LOG_LEVEL >= 2)
                        Log.d(TAG, "Conn Update Interval matched");
          
                     msg = mGattClientHandler.obtainMessage(MSG_CONN_UPDATE_DONE,
                                   Integer.toString(interval));
                     mGattClientHandler.sendMessage(msg);

                } else {
                    Log.i(TAG, "conn update failed");
                }
            }

            @Override
            public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy,
                                        int status) {
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "on Phy updated:"
                         + " tx phy " + txPhy + " rx phy " + rxPhy +" status " + status);
                    if(txPhyReq == txPhy || rxPhyReq == rxPhy) {
                        PhyUpdate tmp_phy = new PhyUpdate();
                        tmp_phy.txPhy = txPhy;
                        tmp_phy.rxPhy = rxPhy;
                        msg = mGattClientHandler.obtainMessage(
                                         MSG_PHY_UPDATE_DONE, tmp_phy);
                        mGattClientHandler.sendMessage(msg);
                        txPhyReq = rxPhyReq = 0;
                    }
                } else {
                    Log.i(TAG, "phy update failed");
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic,
                                                int status){
                if ((status == GATT_SUCCESS)) {
                    String value = new String(characteristic.getValue());
                    Log.i(TAG, "Characteristic read is "+ value);
                    msg = mGattClientHandler.obtainMessage(MSG_CHAR_READ_DONE, value);
                    mGattClientHandler.sendMessage(msg);
                } else {
                    Log.i(TAG, "Char read failed");
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic,
                                                int status) {
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "onCharacteristicWrite: " + status);
                    msg = mGattClientHandler.obtainMessage(MSG_CHAR_WRITE_DONE,
                              characteristic.getValue().toString());
                    mGattClientHandler.sendMessage(msg);
                    if(reliable_write) {
                        String value = new String(characteristic.getValue());
                        /* check the value written is correct or not */
                        if(offset_value.equals(value)) {
                            Log.d(TAG, "Data matched, proceeding!!");
                        } else {
                        /* abort reliable write if the value written doesn't match*/
                            Log.e(TAG, "Data doesn't match");
                            /*abort*/
                            mgattClient.mBluetoothGatt.abortReliableWrite();
                        }
                       /*check if the total data is written, if no write*/
                       if(total_length > length_offset + mtu_size-5) {
                           offset_value = RdWrReliableClass.Value.substring(
                                  length_offset,(length_offset + mtu_size-5));
                           length_offset +=  (mtu_size - 5);
                           characteristic.setValue(offset_value.getBytes());
                           mgattClient.mBluetoothGatt.writeCharacteristic(
                                        characteristic);
                        } else if(total_length < length_offset + mtu_size - 5) {
                            offset_value = RdWrReliableClass.Value.substring(length_offset,
                                  total_length);
                            length_offset = total_length - (mtu_size - 5);
                            characteristic.setValue(offset_value.getBytes());
                            mgattClient.mBluetoothGatt.writeCharacteristic(
                                        characteristic);
                        } else {
                        /*execute write*/
                            if(mgattClient.mBluetoothGatt.executeReliableWrite()) {
                                Log.i(TAG, "Execute Write Successful!");
                                length_offset = 0;
                            } else {
                                Log.e(TAG, "Execute Write Failed!");
                            }
                        }
                }
                } else {
                    Log.i(TAG, "write characteristic failed");
                }
             }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                int status) {
                if ((status == GATT_SUCCESS)) {
                    String value = new String(descriptor.getValue());
                    Log.i(TAG, "onDescriptorRead: " + value);
                     msg = mGattClientHandler.obtainMessage(MSG_DESC_READ_DONE,
                                  value);
                    mGattClientHandler.sendMessage(msg);
                } else {
                    Log.i(TAG, "read descriptor failed");
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt,
                                            BluetoothGattDescriptor desc, int status) {
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "onDescriptorWrite: " + status);
                    String value = new String(desc.getValue());
                    msg = mGattClientHandler.obtainMessage(MSG_DESC_WRITE_DONE,
                                  value);
                    mGattClientHandler.sendMessage(msg);
                } else {
                    Log.i(TAG, "write descriptor failed");
                }
             }

            @Override
            public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                if(status == GATT_SUCCESS){
                    Log.i(TAG, "Read Phy: Tx Phy-"+txPhy+"Rx Phy:"+rxPhy);
                    PhyUpdate tmp_phy = new PhyUpdate();
                    tmp_phy.txPhy = txPhy;
                    tmp_phy.rxPhy = rxPhy;
                    msg = mGattClientHandler.obtainMessage(MSG_PHY_READ_DONE, tmp_phy);
                    mGattClientHandler.sendMessage(msg);
                } else{
                    Log.i(TAG, "Read Phy failed");
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {
                Log.i(TAG, "onCharacteristicChanged");
                String value = new String(characteristic.getValue());
                msg = mGattClientHandler.obtainMessage(MSG_CHAR_CHANGED,
                                value);
                mGattClientHandler.sendMessage(msg);
            }

            @Override
            public void onMtuChanged (BluetoothGatt gatt, int mtu, int status) {
               if (status == GATT_SUCCESS) {
                    Log.i(TAG, "Gatt updated MTU" + mtu);
                    mtu_size = mtu;
                    msg = mGattClientHandler.obtainMessage(MSG_MTU_EXCHANGE_DONE,
                              Integer.toString(mtu));
                    mGattClientHandler.sendMessage(msg);
                } else {
                    Log.i(TAG, "Failed to change Mtu size");
                }
            }
        };

        public void connect(BluetoothDevice device){
            if(MainActivity.bleAdapter!=null) {
                Log.i(TAG, "Gatt Connect");
                mDevice = device;
                mBluetoothGatt = mDevice.connectGatt(mcontext, false, mGattCallbacks,TRANSPORT_LE);
            }
        }

        public void pair(){
            if(mDevice.getBondState() != BluetoothDevice.BOND_BONDED){
                Log.i(TAG, "Pairing!");
                if(!mDevice.createBond(TRANSPORT_LE)) {
                    Log.i(TAG, "couldn't start pairing");
                }
                MainActivity.pairing_called = MainActivity.PAIRING_REQ_FROM_GATT_CLIENT;
            }
        }

        public void disconnect() {
            mgattClient.mBluetoothGatt.disconnect();
        }

        public void unpair(){
            if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                Log.i(TAG, "Unpairing!");
                if(!mDevice.removeBond()) {
                    Log.i(TAG, "couldn't start unpairing");
                }
        MainActivity.pairing_called = 0;
            }
        }

        /*public BluetoothGattCharacteristic getCharacteristicById(BluetoothDevice device,
             int instanceId) {
            for (BluetoothGattService svc : mBluetoothGatt.mServices) {
                for (BluetoothGattCharacteristic charac : svc.getCharacteristics()) {
                    if (charac.getInstanceId() == instanceId) {
                        return charac;
                    }
                }
            }
            return null;
        }*/
    }

    public class GattClientMessageHandler extends Handler {
        Context mMsgContext;
        private static final String TAG = "GattClientMessageHandler";

        int operation_request;

        public GattClientMessageHandler(Context contxt, Looper looper) {
            super(looper);
            mMsgContext = contxt;
            if(GattClient.LOG_LEVEL >= 2)
                Log.d(TAG, "GattClientMessageHandler");
        }

        @Override
        public void handleMessage(Message msg) {
            if (GattClient.LOG_LEVEL >= 2)
                Log.d(TAG, "Handler(): msg = " + msg.what);
            int status;
      
            switch (msg.what) {
                case MSG_START_BLE_CONNECT:
                    /* start scan with filters and initiate conn with the result */
                    Scan scn = (Scan) msg.obj;
                    processCheckAndStartBleScan(scn);
                    break;
                case MSG_BLE_SCAN_DEV_FOUND:
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    processScanDevFound(device);
                    break;
                case MSG_START_BLE_CONN_UPDATE:
                    ConnUpdate ConnUpdateClass = (ConnUpdate) msg.obj;
                    processConnUpdateReq(ConnUpdateClass);
                    break;
                case MSG_START_BLE_PHY_UPDATE:
                    PhyUpdate phyUpdate = (PhyUpdate) msg.obj;
                    processPhyUpdateReq(phyUpdate);
                    break;
                case MSG_START_BLE_GATT_CONFIGURE_MTU_SIZE:
                    int Mtu_Size = (int) msg.obj;
                    processConfigureMtuReq(Mtu_Size);
                    break;
                case MSG_START_BLE_PAIR_DEV:
                    processStartPair();
                    break;
                case MSG_START_BLE_UNPAIR_DEV:
                    processStartUnpair();
                    break;
                case MSG_START_BLE_READ_PHY:
                    processReadPhyReq();
                    break;
                case MSG_START_BLE_GATT_DISC:
                    processGattDiscovery();
                    break;
                case MSG_START_BLE_GATT_REFRESH_SERVICES:
                    processRefreshServices();
                    break;
                case MSG_START_BLE_GATT_EXECUTE_WRITE:
                    processExecuteWriteReq();
                    break;
                case MSG_START_BLE_GATT_DISCONNECT:
                    processDisconnectReq();
                    break;
                case MSG_START_BLE_GATT_WRITE_READ_CHAR:
                    ReadWriteOp RdWrClass = (ReadWriteOp) msg.obj;
                    processGattReadWriteCharReq(RdWrClass);
                    break;
                case MSG_START_BLE_GATT_WRITE_READ_DESC:
                    RdWrClass = (ReadWriteOp) msg.obj;
                    processGattReadWriteDescReq(RdWrClass);
                    break;
                case MSG_REGISTER_BLE_GATT_NOTIFICATIONS:
                    RdWrClass = (ReadWriteOp) msg.obj;
                    processGattRegisterNotifications(RdWrClass);
                    break;
                case MSG_DEREGISTER_BLE_GATT_NOTIFICATIONS:
                    RdWrClass = (ReadWriteOp) msg.obj;
                    processGattDeregisterNotifications(RdWrClass);
                    break;
                case MSG_START_BLE_GATT_RELIABLE_WRITE:
                    RdWrReliableClass = (ReadWriteOp) msg.obj;
                    processGattStartReliableWrite(RdWrReliableClass);
                    break;
                case MSG_START_BLE_GATT_ABORT_RELIABLE_WRITE:
                    processGattAbortReliableWrite();
                    break;
                case MSG_REM_DEV_FAILED_TO_CONNECT:
                    PrintStr.setLength(0);
                    PrintStr.append("Failed to connect, please try again!!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_REFRESH_SERV_DONE:
                    PrintStr.setLength(0);
                    PrintStr.append("Gatt Service refresh done!!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_REM_DEV_DISCONNECTED:
                    PrintStr.setLength(0);
                    PrintStr.append("Disconnected with remote device!!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_REM_DEV_CONNECTED:
                    PrintStr.setLength(0);
                    String name = (String) msg.obj;
                    PrintStr.append("Connected to remote device:");
                    PrintStr.append(name);
                    SocketServer.sendSocketData(PrintStr.toString());
                    MainActivity.mScannerService.stopScan();
                    break;
                case MSG_SERVC_DISC_DONE:
                    PrintStr.setLength(0);
                    PrintStr.append("Gatt Service discovery done!!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_CHAR_CHANGED:
                    PrintStr.setLength(0);
                    String value = (String) msg.obj;
                    PrintStr.append("Characteristic value changed to ");
                    PrintStr.append(value);
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_CONN_UPDATE_DONE:
                    PrintStr.setLength(0);
                    String interal = (String) msg.obj;
                    PrintStr.append("Connection Updated to :");
                    PrintStr.append(interal);            
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_PHY_UPDATE_DONE:
                    PrintStr.setLength(0);
                    phyUpdate = (PhyUpdate) msg.obj;
                    PrintStr.append("Phy Update done, Tx Phy :");
                    PrintStr.append(phyUpdate.txPhy);
                    PrintStr.append(" Rx Phy :");
                    PrintStr.append(phyUpdate.rxPhy);          
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_CHAR_READ_DONE:
                    PrintStr.setLength(0);
                    String read_char = (String) msg.obj;
                    PrintStr.append("Char Value is :");
                    PrintStr.append(read_char);          
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_CHAR_WRITE_DONE:
                    PrintStr.setLength(0);
                    String write_char = (String) msg.obj;
                    PrintStr.append("Characteristic Value Written Successfully to ");
                    PrintStr.append(write_char);  
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_DESC_READ_DONE:
                    PrintStr.setLength(0);
                    String read_desc = (String) msg.obj;
                    PrintStr.append("Descriptor Value is :");
                    PrintStr.append(read_desc);          
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_DESC_WRITE_DONE:
                    PrintStr.setLength(0);
                    String write_desc = (String) msg.obj;
                    PrintStr.append("Descriptor Value Written Successfully to ");
                    PrintStr.append(write_desc);
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_PHY_READ_DONE:
                    PrintStr.setLength(0);
                    phyUpdate = (PhyUpdate) msg.obj;
                    PrintStr.append("Current Phy: Tx Phy :");
                    PrintStr.append(phyUpdate.txPhy);
                    PrintStr.append(" Rx Phy :");
                    PrintStr.append(phyUpdate.rxPhy);          
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_MTU_EXCHANGE_DONE:
                    PrintStr.setLength(0);
                    String mtu = (String) msg.obj;
                    PrintStr.append("MTU updated to :");
                    PrintStr.append(mtu);          
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_GC_REM_DEV_PAIRED:
                    PrintStr.setLength(0);
                    PrintStr.append("Device is paired!!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                default:
                    Log.e(TAG, "Unknown Operation");
                    break;
            }
        }

        private void processCheckAndStartBleScan(Scan scn) {
            Log.i(TAG, "scanflag set,service bound: " + MainActivity.boundS);
            MainActivity.mScannerService.set_scan_parameters(scn);
      
            PrintStr.setLength(0);
            PrintStr.append("Scanning Started!");  
            SocketServer.sendSocketData(PrintStr.toString());
        }

        private void processScanDevFound(BluetoothDevice device) {
            Log.i(TAG, "matchFoundEvent Address:" + device.getAddress());
            mgattClient.connect(device);
        }

        private void processConnUpdateReq(ConnUpdate ConnUpdateClass){
            Log.i(TAG, "Conn Update");
            /* Android version check */
            try {
                if (Build.VERSION.SDK_INT >= 28) {
                    mgattClient.mBluetoothGatt.requestLeConnectionUpdate(
                                                ConnUpdateClass.ConnIntervalMin,
                                                ConnUpdateClass.ConnIntervalMax,
                                                ConnUpdateClass.ConnSlaveLatency,
                                                ConnUpdateClass.ConnSupTO, 0, 0);
                } else {
                    Log.d(TAG, "Conn Update can't be done");
                }
            } catch (Exception e) {
                Log.d(TAG, "Interrupted while waiting for operation to complete");
            }
        }

        private void processReadPhyReq(){
            Log.i(TAG, "Read Phy");
            mgattClient.mBluetoothGatt.readPhy();
        }

        private void processStartPair(){
            Log.i(TAG, "Starting Pairing");
            mgattClient.pair();
        }
    
        private void processExecuteWriteReq() {
             /*execute write*/
            if(reliable_write) {
                if(mgattClient.mBluetoothGatt.executeReliableWrite()) {
                    Log.i(TAG, "Execute Write Successful!");
                } else {
                    Log.e(TAG, "Execute Write Failed!");
                }
                reliable_write = false;
                length_offset = 0;
           }
        }

        private void processStartUnpair(){
            Log.i(TAG, "Starting Unpair");
            mgattClient.unpair();
            PrintStr.setLength(0);
            PrintStr.append("Device unpaired");
            SocketServer.sendSocketData(PrintStr.toString());
        }

        private void processDisconnectReq() {
            Log.i(TAG, "Disconnecting!");
            mgattClient.disconnect();
        }

        private void processPhyUpdateReq(PhyUpdate phyUpdate){
            txPhyReq = phyUpdate.txPhy;
            rxPhyReq = phyUpdate.rxPhy;
            Log.i(TAG, "Phy Update");
            mgattClient.mBluetoothGatt.setPreferredPhy(phyUpdate.txPhy,
                                        phyUpdate.rxPhy, phyUpdate.phyOpt);
        }

        private void processConfigureMtuReq(int Mtu_Size) {
            Log.i(TAG, "Configure mtu");
            mgattClient.mBluetoothGatt.requestMtu(Mtu_Size);
        }

        private void processRefreshServices() {
            Log.i(TAG, "Refresh Services");
            if(!(mgattClient.mBluetoothGatt.refresh())) {
                Log.e(TAG, "Refresh services failed");
            } else {
                Message msg;
                msg = mGattClientHandler.obtainMessage(MSG_REFRESH_SERV_DONE, null);
                mGattClientHandler.sendMessage(msg);
            }
        }

        private void processGattDiscovery() {
            Log.i(TAG, "Gatt Service Discovery");
            mgattClient.mBluetoothGatt.discoverServices();
        }

        private void processGattReadWriteCharReq(ReadWriteOp RdWrClass) {
            BluetoothGattCharacteristic mCharacteristic;
            BluetoothGattService mService;
            boolean status = true;

            Log.d(TAG, "Service UUID:"+RdWrClass.Srvc_uuid);

            if(mServiceUUID.contains(UUID.fromString(RdWrClass.Srvc_uuid))) {
                Log.d(TAG, "Service is found");
                if (!mCharUUID.contains(UUID.fromString(RdWrClass.Char_uuid))) {
                    Log.e(TAG, "Characteristic not found");
                    return;
                }
            } else {
                Log.e(TAG, "Service is not found");
                return;
            }
            mService = mgattClient.mBluetoothGatt.getService(UUID.fromString(
                                   RdWrClass.Srvc_uuid));
            mCharacteristic = mService.getCharacteristic(UUID.fromString(RdWrClass.Char_uuid));
            Log.d(TAG, "Srvc uuid" + mService.getUuid().toString() +
                       "char uuid:" + mCharacteristic.getUuid().toString());

            if(RdWrClass.operation == GATT_READ) {
                Log.d(TAG, "Read Char, Srvc uuid" + RdWrClass.Srvc_uuid + "char uuid:" + RdWrClass.Char_uuid);
                status = mgattClient.mBluetoothGatt.readCharacteristic(mCharacteristic);
                if(status != true)
                    Log.e(TAG, "Read Char failed");
                return;
            }

            if(RdWrClass.operation == GATT_WRITE) {
                /* Check for write type */
                if(RdWrClass.Write_type != 0) {
                    mCharacteristic.setWriteType(RdWrClass.Write_type);
                    /* Check for length and set value */
                    if((RdWrClass.Value.length() >= (mtu_size -15)) &&
                        (RdWrClass.Write_type ==
                                BluetoothGattCharacteristic.WRITE_TYPE_SIGNED)) {
                        Log.e(TAG, "Write failed: Length cannot be more than" +
                                "ATT_MTU-15 for signed write");
                        return;
                    }
                    else if ((RdWrClass.Value.length() >= (mtu_size - 3)) &&
                            (RdWrClass.Write_type !=
                                    BluetoothGattCharacteristic.WRITE_TYPE_SIGNED)) {
                        Log.e(TAG, "Write failed: Length cannot be more than" +
                                "ATT_MTU-3 for write");
                        return;
                    }
                } else {
                    mCharacteristic.setWriteType(
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    if(RdWrClass.Value.length() >= (mtu_size - 3)) {
                        Log.e(TAG, "Write failed: Length cannot be more than" +
                                "ATT_MTU-3 for write");
                        return;
                    }
                }
                mCharacteristic.setValue((RdWrClass.Value).getBytes());
                written_value = String.valueOf(RdWrClass.Value);
                mgattClient.mBluetoothGatt.writeCharacteristic(
                                            mCharacteristic);
            } else {
                Log.e(TAG, "invalid operation");
            }
        }
    
        private void processGattRegisterNotifications(ReadWriteOp RdWrClass){
            BluetoothGattCharacteristic mCharacteristic;
            BluetoothGattService mService;
            BluetoothGattDescriptor mDescriptor;
            boolean status = true;

            Log.d(TAG, "Service UUID:"+RdWrClass.Srvc_uuid);

            if(mServiceUUID.contains(UUID.fromString(RdWrClass.Srvc_uuid))) {
                if (!mCharUUID.contains(UUID.fromString(RdWrClass.Char_uuid))) {
                    Log.e(TAG, "Characteristic not found");
                    return;
                }
                mService = mgattClient.mBluetoothGatt.getService(UUID.fromString(
                                   RdWrClass.Srvc_uuid));
                mCharacteristic = mService.getCharacteristic(UUID.fromString(
                                       RdWrClass.Char_uuid));
                mDescriptor = mCharacteristic.getDescriptor(
                                          CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
                if (mDescriptor == null) {
                    Log.e(TAG, "Descriptor not found");
                    return;
                }
             } else {
                Log.e(TAG, "Service is not found");
                return;
            }
      
            mgattClient.mBluetoothGatt.setCharacteristicNotification(mCharacteristic, true);
            mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mgattClient.mBluetoothGatt.writeDescriptor(mDescriptor);
            Log.d(TAG, "Registering notifications");
       }
    
        private void processGattDeregisterNotifications(ReadWriteOp RdWrClass){
            BluetoothGattCharacteristic mCharacteristic;
            BluetoothGattService mService;
            BluetoothGattDescriptor mDescriptor;
            boolean status = true;

           if(mServiceUUID.contains(UUID.fromString(RdWrClass.Srvc_uuid)))  {
                if (!mCharUUID.contains(UUID.fromString(RdWrClass.Char_uuid))) {
                    Log.e(TAG, "Characteristic not found");
                    return;
                }
                mService = mgattClient.mBluetoothGatt.getService(UUID.fromString(
                                   RdWrClass.Srvc_uuid));
                mCharacteristic = mService.getCharacteristic(UUID.fromString(
                                       RdWrClass.Char_uuid));
                mDescriptor = mCharacteristic.getDescriptor(
                                          CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
                if (mDescriptor == null) {
                    Log.e(TAG, "Descriptor not found");
                    return;
                }
            } else {
                Log.e(TAG, "Service is not found");
                return;
            }
            mgattClient.mBluetoothGatt.setCharacteristicNotification(mCharacteristic, false);
            mDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            mgattClient.mBluetoothGatt.writeDescriptor(mDescriptor);
            Log.d(TAG, "Deregistering notifications");
       }

        private void processGattStartReliableWrite(ReadWriteOp RdWrReliableClass){
            BluetoothGattCharacteristic mCharacteristic;
            BluetoothGattService mService;
            boolean status = true;

            if(mServiceUUID.contains(UUID.fromString(RdWrReliableClass.Srvc_uuid)))  {
                Log.d(TAG, "mService is not null");
                if (!mCharUUID.contains(UUID.fromString(RdWrReliableClass.Char_uuid))) {
                    Log.e(TAG, "Characteristic not found");
                    return;
                }
            } else {
                Log.e(TAG, "Service is not found");
                return;
            }
      
            mService = mgattClient.mBluetoothGatt.getService(UUID.fromString(
                                   RdWrReliableClass.Srvc_uuid));
            mCharacteristic = mService.getCharacteristic(UUID.fromString(
                                       RdWrReliableClass.Char_uuid));
            if(mgattClient.mBluetoothGatt.beginReliableWrite()) {        
                reliable_write = true;
                Log.i(TAG, "beginReliableWrite successful!");
            } else {
                Log.e(TAG, "beginReliableWrite failed");
                return;
            }
      
            mCharacteristic.setWriteType(
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            total_length = RdWrReliableClass.Value.length();
            if(total_length >= (mtu_size - 5)) {
                offset_value = RdWrReliableClass.Value.substring(
                        length_offset, (mtu_size - 5));
                length_offset +=  (mtu_size - 5);
                mCharacteristic.setValue(offset_value.getBytes());
            }
            mgattClient.mBluetoothGatt.writeCharacteristic(
                                        mCharacteristic);
        }
        
        private void processGattAbortReliableWrite() {
            mgattClient.mBluetoothGatt.abortReliableWrite();
        }

        private void processGattReadWriteDescReq(ReadWriteOp RdWrClass) {
            BluetoothGattCharacteristic mCharacteristic;
            BluetoothGattService mService;
            BluetoothGattDescriptor mDescriptor;
            boolean status = true;

           if(mServiceUUID.contains(UUID.fromString(RdWrClass.Srvc_uuid)))  {
                if (!mCharUUID.contains(UUID.fromString(RdWrClass.Char_uuid))) {
                    Log.e(TAG, "Characteristic not found");
                    return;
                }
               if (!mDescUUID.contains(UUID.fromString(RdWrClass.Desc_uuid))) {
                    Log.e(TAG, "Descriptor not found");
                    return;
                }
            } else {
                Log.e(TAG, "Service is not found");
                return;
            }
      
            mService = mgattClient.mBluetoothGatt.getService(UUID.fromString(
                                   RdWrClass.Srvc_uuid));
            mCharacteristic = mService.getCharacteristic(UUID.fromString(
                                       RdWrClass.Char_uuid));
            mDescriptor = mCharacteristic.getDescriptor(UUID.fromString(
                                          RdWrClass.Desc_uuid));

            if(RdWrClass.operation == GATT_READ) {
                status = mgattClient.mBluetoothGatt.readDescriptor(mDescriptor);
                if(status != true)
                    Log.e(TAG, "Read Desc failed");
                return;
            }

            if(RdWrClass.operation == GATT_WRITE) {
                mDescriptor.setValue((RdWrClass.Value).getBytes());
                mgattClient.mBluetoothGatt.writeDescriptor(mDescriptor);
            } else {
                Log.e(TAG, "invalid operation");
            }
        }
     }

    protected void finalize() {

    }
}
