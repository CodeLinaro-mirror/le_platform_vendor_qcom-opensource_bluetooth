/*
 * Copyright (c) 2020-2021, The Linux Foundation. All rights reserved.
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

import android.content.Context;

import android.os.SystemProperties;
import android.os.Message;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import java.util.*;
import java.util.List;
import java.util.UUID;
import java.lang.*;

import libcore.io.IoUtils;

import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothProfile;

public class GattServer{
    /* Variable to update phy */
    public static int txPhyReq = 1;
    public static int rxPhyReq = 1;
    public GattServerMessageHandler mGattServerHandler = null;
    public BleGattServer mgattServer;
    public List<BluetoothDevice> connectedDevices;
    public HashMap<String,BluetoothGattService> Service_List;
    private BluetoothDevice mdevice = null;
    private Context mcontext = null;

    public static final int MSG_START_BLE_ADD_SERVICE = 0;
    public static final int MSG_START_BLE_REMOVE_SERVICE = 1;
    public static final int MSG_START_BLE_CLEAR_SERVICES = 2;
    public static final int MSG_START_BLE_GET_SERVICES = 3;
    public static final int MSG_START_BLE_PHY_UPDATE = 4;
    public static final int MSG_START_BLE_READ_PHY = 5;
    public static final int MSG_START_GET_CONNECTED_DEVICES = 6;
    public static final int MSG_GS_ACTION_MAX_VALUE = MSG_START_GET_CONNECTED_DEVICES;

    public static final int MSG_ADD_SERVICE_DONE = MSG_GS_ACTION_MAX_VALUE + 1;
    public static final int MSG_PHY_UPDATE_DONE = MSG_GS_ACTION_MAX_VALUE + 2;
    public static final int MSG_PHY_READ_DONE = MSG_GS_ACTION_MAX_VALUE + 3;
    public static final int MSG_GS_ACTION_DONE_MAX_VALUE = MSG_PHY_READ_DONE;

    public static int LOG_LEVEL = 3;
    public static final int GATT_SERVER = 8;
    public static final int BLE_STATE_CONNECTED = 10;
    public static final int BLE_STATE_DISCONNECTED = 11;
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private static int mConnectionStatus = BLE_STATE_DISCONNECTED;
    public static final String base_uuid = "0000-1000-8000-00805f9b34fb";
    StringBuilder PrintStr = new StringBuilder();

    public GattServer(Context mcontext) {
        this.mcontext = mcontext;
        /* Initialize classes */
        mgattServer = new BleGattServer(mcontext);
        mgattServer.startServer();
        /* Start Message handler */
        HandlerThread thread = new HandlerThread("GattServerHandler");
        thread.start();
        Looper looper = thread.getLooper();
        mGattServerHandler = new GattServerMessageHandler(mcontext, looper);
        Service_List = new HashMap<String,BluetoothGattService>();
    }

    /* Connection Class */
    public class BleGattServer {
        private static final String TAG = "BleGattServer";
        private BluetoothGattServer mBluetoothGattserver;
        private BluetoothGattService mService;
        private BluetoothGattCharacteristic mCharacteristic;
        private BluetoothGattCharacteristic mreadChar;
        private Context context;
        private int mState;
        private int GATT_SUCCESS = 0x00;
        Message msg;

        public BleGattServer(Context context) {
            this.context = context;
        }

        private final BluetoothGattServerCallback mGattServerCallbacks =
                                                      new BluetoothGattServerCallback() {
             @Override
             public void onServiceAdded(int status, BluetoothGattService service) {
                if ((status == GATT_SUCCESS)) {
                 Log.d(TAG, "onServiceAdded() - handle=" + service.getInstanceId()
                                      + " uuid=" + service.getUuid() + " status=" + status);
                 msg = mGattServerHandler.obtainMessage(MSG_ADD_SERVICE_DONE,
                                                               service.getUuid().toString());
                 mGattServerHandler.sendMessage(msg);
                } else {
                     Log.i(TAG, "conn update failed");
                     PrintStr.setLength(0);
                     PrintStr.append("Connection Update failed with status: "+ status);
                     SocketServer.sendSocketData(PrintStr.toString());
                 }
             }

             @Override
             public void onConnectionStateChange(BluetoothDevice device, int status,int newState) {
                 mdevice = device;
                 Log.d(TAG, "onConnectionStateChange() got connection event");
                 if(newState == BluetoothProfile.STATE_CONNECTED &&
                                                    mConnectionStatus == BLE_STATE_DISCONNECTED ) {
                    mGattServerHandler.processConnectReq();
                    PrintStr.setLength(0);
                    PrintStr.append("Device Connected : ");
                    PrintStr.append(device.getAddress());
                    SocketServer.sendSocketData(PrintStr.toString());
                    mConnectionStatus = BLE_STATE_CONNECTED;
               } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                    PrintStr.setLength(0);
                    PrintStr.append("Device Disonnected : ");
                    PrintStr.append(mdevice.getAddress());
                    SocketServer.sendSocketData(PrintStr.toString());
                    mConnectionStatus = BLE_STATE_DISCONNECTED;
               }

             }

             @Override
             public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {

                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "on Phy updated:"
                         + " tx phy " + txPhy + " rx phy " + rxPhy +" status " + status);
                    if(txPhyReq == txPhy || rxPhyReq == rxPhy) {
                        PhyUpdate tmp_phy = new PhyUpdate();
                        tmp_phy.txPhy = txPhy;
                        tmp_phy.rxPhy = rxPhy;
                        msg = mGattServerHandler.obtainMessage(
                                         MSG_PHY_UPDATE_DONE, tmp_phy);
                        mGattServerHandler.sendMessage(msg);
                        txPhyReq = rxPhyReq = 0;
                    }
                } else {
                    Log.i(TAG, "phy update failed");
                    PrintStr.setLength(0);
                    PrintStr.append("Phy Update failed with status: "+ status);
                    SocketServer.sendSocketData(PrintStr.toString());
                }
             }

            @Override
            public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
                if(status == GATT_SUCCESS){
                     Log.i(TAG, "Read Phy: Tx Phy-"+txPhy+"Rx Phy:"+rxPhy);
                     PhyUpdate tmp_phy = new PhyUpdate();
                     tmp_phy.txPhy = txPhy;
                     tmp_phy.rxPhy = rxPhy;
                     msg = mGattServerHandler.obtainMessage(MSG_PHY_READ_DONE, tmp_phy);
                     mGattServerHandler.sendMessage(msg);
                 } else{
                     Log.i(TAG, "Read Phy failed");
                     PrintStr.setLength(0);
                     PrintStr.append("Read Phy failed with status: "+ status);
                     SocketServer.sendSocketData(PrintStr.toString());
                 }
            }
        };

        public void startServer(){
        mBluetoothGattserver= MainActivity.mBluetoothManager.openGattServer(mcontext,
                                      mGattServerCallbacks,BluetoothDevice.TRANSPORT_LE);
        }
    }

    public class GattServerMessageHandler extends Handler {
        Context mMsgContext;
        private static final String TAG = "GattServerMessageHandler";
        int operation_request;
        public GattServerMessageHandler(Context contxt, Looper looper) {
            super(looper);
            mMsgContext = contxt;
            if (GattClient.LOG_LEVEL >= 2)
                Log.d(TAG, "GattServerMessageHandler");
        }

        @Override
        public void handleMessage(Message msg) {
            if (GattClient.LOG_LEVEL >= 2)
                Log.d(TAG, "Handler(): msg = " + msg.what);
            int status;
            AddServices AddServ;
            switch (msg.what) {
                case MSG_START_BLE_ADD_SERVICE:
                    AddServ = (AddServices) msg.obj;
                    processGattAddServiceReq(AddServ);
                    break;
                case MSG_START_BLE_REMOVE_SERVICE:
                    String uuid= (String)msg.obj;
                    processGattRemoveServiceReq(uuid);
                    PrintStr.setLength(0);
                    PrintStr.append("service removed sucessfully");
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_START_BLE_CLEAR_SERVICES:
                    processGattClearServiceReq();
                    PrintStr.setLength(0);
                    PrintStr.append("All services sucessfully cleared");
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
                case MSG_START_BLE_GET_SERVICES:
                    processGattGetServiceReq();
                    break;
                case MSG_START_BLE_PHY_UPDATE:
                    PhyUpdate phyUpdate = (PhyUpdate) msg.obj;
                    processPhyUpdateReq(phyUpdate);
                    break;
                case MSG_START_BLE_READ_PHY:
                    processReadPhyReq();
                    break;
                case MSG_START_GET_CONNECTED_DEVICES:
                    processGetConnectedDevices();
                    break;
                case MSG_ADD_SERVICE_DONE:
                    PrintStr.setLength(0);
                    String interal = (String) msg.obj;
                    PrintStr.append("service added with UUID :");
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
                case MSG_PHY_READ_DONE:
                    PrintStr.setLength(0);
                    phyUpdate = (PhyUpdate) msg.obj;
                    PrintStr.append("Current Phy: Tx Phy :");
                    PrintStr.append(phyUpdate.txPhy);
                    PrintStr.append(" Rx Phy :");
                    PrintStr.append(phyUpdate.rxPhy);
                    SocketServer.sendSocketData(PrintStr.toString());
                    break;
            }
        }

        private BluetoothGattService createService(UUID srvcUUID, List<UUID> charuuids,
                                 List<Integer> props, List<Integer>perms, byte[] value){

            if(LOG_LEVEL >=2)
                Log.d(TAG, "srvcUUID to be added is:" + srvcUUID);
            BluetoothGattService srvc = new BluetoothGattService(srvcUUID,
                                                 BluetoothGattService.SERVICE_TYPE_PRIMARY);

            int prop_ored = 0;
            for(int x:props){
                prop_ored = prop_ored | x;
            }
            int perm_ored = 0;
            for(int x:perms){
                perm_ored = perm_ored | x;
            }
            BluetoothGattCharacteristic charAdd;
            for(UUID Char_uuid: charuuids){

                charAdd = new BluetoothGattCharacteristic(Char_uuid, prop_ored, perm_ored);
                if(value != null)
                    charAdd.setValue(value);

                if((prop_ored & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0){
                    BluetoothGattDescriptor desc =  new BluetoothGattDescriptor
                            (UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG),
                                    perm_ored);

                    desc.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    charAdd.addDescriptor(desc);
                }

                srvc.addCharacteristic(charAdd);
            }
            if(srvc != null){
                return srvc;
            }
            return null;
        }

        private void processGattAddServiceReq(AddServices AddServ) {

            BluetoothGattService lService = createService(AddServ.lserviceUUID,
                              AddServ.lcharUUIDs, AddServ.lProps,AddServ.lPerms, AddServ.lvalue);
            if(lService != null) {
            Log.d(TAG, AddServ.lserviceUUID.toString());
            Service_List.put(AddServ.lserviceUUID.toString().toUpperCase(),lService);
            mgattServer.mBluetoothGattserver.addService(lService);
            }
        }

        private void processGattRemoveServiceReq(String srvc_uuid) {

          if(Service_List.containsKey(srvc_uuid.toUpperCase())) {
            mgattServer.mBluetoothGattserver.removeService(
                                          Service_List.get(srvc_uuid.toUpperCase()));
            Service_List.remove(srvc_uuid.toUpperCase());
            Log.d(TAG, "Service Removed");
           } else {
            Log.d(TAG, "Service Not Found");
           }
         }

        private void processPhyUpdateReq(PhyUpdate phyUpdate) {

            txPhyReq = phyUpdate.txPhy;
            rxPhyReq = phyUpdate.rxPhy;
            Log.i(TAG, "Phy Update");
            mgattServer.mBluetoothGattserver.setPreferredPhy(mdevice,phyUpdate.txPhy,
                                        phyUpdate.rxPhy, phyUpdate.phyOpt);
        }

        private void processGattClearServiceReq() {
            Log.d(TAG, "Clearing all the services");
            mgattServer.mBluetoothGattserver.clearServices();
            Service_List.clear();
        }



        private void processGattGetServiceReq() {
            Log.d(TAG, "Listing all the services");
            PrintStr.setLength(0);
            PrintStr.append("Services UUIDS :");
            SocketServer.sendSocketData(PrintStr.toString());
            PrintStr.setLength(0);
            for ( String key : Service_List.keySet() ) {
                    PrintStr.append(key);
                    PrintStr.append("  ");
             }
            SocketServer.sendSocketData(PrintStr.toString());
        }

        private void processReadPhyReq() {
            Log.i(TAG, "Read Phy");
            mgattServer.mBluetoothGattserver.readPhy(mdevice);
        }

        private void processGetConnectedDevices() {
             connectedDevices=MainActivity.mBluetoothManager.getConnectedDevices(GATT_SERVER);
             PrintStr.setLength(0);
             PrintStr.append("Connected Device:");
             for (int i = 0; i < connectedDevices.size(); i++)  {
                 Log.i(TAG,connectedDevices.get(i).getAddress());
                 PrintStr.append(connectedDevices.get(i).getAddress());
                 PrintStr.append("  ");
             }
             SocketServer.sendSocketData(PrintStr.toString());
        }

        private void processConnectReq() {
            mgattServer.mBluetoothGattserver.connect(mdevice,false);
        }


    }
}
