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

import android.content.Context;

import android.os.Build;
import android.os.SystemProperties;
import android.os.Message;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.lang.*;

import libcore.io.IoUtils;

import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattDescriptor;

public class GattClient {
    public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR
                                 = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final String TAG = "GattClient";
    public static int LOG_LEVEL = 6;

    /* MTU size required for MTU exchange */
    public static final int MTU_SIZE_MIN = 23;
    public static final int TRANSPORT_LE = 2;

    public int mtu_size = MTU_SIZE_MIN;

    /* Variable to update phy */
    public static int txPhyReq = 1;
    public static int rxPhyReq = 1;

    public Looper glooper;

    public BleGattClient mgattClient = null;
    private Context mcontext = null;
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
    public static final int MSG_START_CANCEL_CONNECT = 17;
    public static final int MSG_BLE_GATT_REQ_CONN_PRIORITY = 18;
    public static final int MSG_START_BLE_CONNECT_TO_BDADDR = 19;
    public static final int MSG_GC_ACTION_MAX_VALUE = MSG_START_BLE_CONNECT_TO_BDADDR;

    public static final int MSG_REM_DEV_FAILED_TO_CONNECT = MSG_GC_ACTION_MAX_VALUE + 1;
    public static final int MSG_REFRESH_SERV_DONE = MSG_GC_ACTION_MAX_VALUE + 2;
    public static final int MSG_REM_DEV_DISCONNECTED = MSG_GC_ACTION_MAX_VALUE + 3;
    public static final int MSG_REM_DEV_CONNECTED = MSG_GC_ACTION_MAX_VALUE + 4;
    public static final int MSG_SERVC_DISC_DONE = MSG_GC_ACTION_MAX_VALUE + 5;
    public static final int MSG_CONN_UPDATE_DONE = MSG_GC_ACTION_MAX_VALUE + 6;
    public static final int MSG_PHY_UPDATE_DONE = MSG_GC_ACTION_MAX_VALUE + 7;
    public static final int MSG_CHAR_READ_DONE = MSG_GC_ACTION_MAX_VALUE + 8;
    public static final int MSG_CHAR_WRITE_DONE = MSG_GC_ACTION_MAX_VALUE + 9;
    public static final int MSG_DESC_READ_DONE = MSG_GC_ACTION_MAX_VALUE + 10;
    public static final int MSG_DESC_WRITE_DONE = MSG_GC_ACTION_MAX_VALUE + 11;
    public static final int MSG_PHY_READ_DONE = MSG_GC_ACTION_MAX_VALUE + 12;
    public static final int MSG_MTU_EXCHANGE_DONE = MSG_GC_ACTION_MAX_VALUE + 13;
    public static final int MSG_CHAR_CHANGED = MSG_GC_ACTION_MAX_VALUE + 14;
    public static final int MSG_GC_ACTION_DONE_MAX_VALUE = MSG_CHAR_CHANGED;

    private static final int GATT_WRITE = 1;
    private static final int GATT_READ = 2;
    private static final int GATT_FORMAT_STRING = 1;
    private static final int GATT_FORMAT_INT = 2;

    // Connection States
    public static final int BLE_STATE_CONNECTING = 1;
    public static final int BLE_STATE_CONNECTED = 2;
    public static final int BLE_STATE_DISCONNECTING = 3;
    public static final int BLE_STATE_DISCONNECTED = 4;

    private static int length_offset = 0;
    private static String offset_value = null;
    private boolean reliable_write = false;
    private static int total_length = 0;
    private boolean reliable_write_no_more_data = false;
    private boolean is_op_in_progress = false;
    private ReadWriteOp RdWrReliableClass = null;
    private ReadWriteOp RdWrClass = null;
    private static int mConnectionStatus = BLE_STATE_DISCONNECTED;

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
        glooper = thread.getLooper();

        mGattClientHandler = new GattClientMessageHandler(mcontext, glooper);

        mServices = new ArrayList<BluetoothGattService>();
        mCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
        mDescriptors = new ArrayList<BluetoothGattDescriptor>();

        mServiceUUID = new ArrayList<UUID>();
        mCharUUID = new ArrayList<UUID>();
        mDescUUID = new ArrayList<UUID>();
    }

    public void cleanup() {
        Log.i(TAG, "cleanup");
        /* disconnect the link */
        if(mConnectionStatus == BLE_STATE_CONNECTED) {
            Log.e(TAG, "in cleanup disconnect");
            mGattClientHandler.processDisconnectReq();
        }
        /* stop the looper */
        glooper.quitSafely();
        mGattClientHandler.processCloseReq();
    }

    /* Connection Class */
    public class BleGattClient {
        private static final String TAG = "BleGattClient";

        private BluetoothGatt mBluetoothGatt;
        private BluetoothGattService mService;
        private BluetoothGattCharacteristic mCharacteristic;
        private Context context;
        private int GATT_SUCCESS = 0x00;
        Message msg;
        StringBuilder PrintStr = new StringBuilder();

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
                if (gatt.getDevice() == null || status != GATT_SUCCESS) {
                    if(GattClient.LOG_LEVEL >= 1) {
                        Log.e(TAG, "onConnectionStateChange:Unexpected error! state: " + newState);
                    }
                    mConnectionStatus = BLE_STATE_DISCONNECTED;
                    /*Send Message to Message Handler */
                    msg = mGattClientHandler.obtainMessage(MSG_REM_DEV_FAILED_TO_CONNECT, null);
                    mGattClientHandler.sendMessage(msg);
                    return;
                }

                int bondState = mDevice.getBondState();

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "onConnectionStateChange:DISCONNECTED "
                            + " remoteDevice: " + gatt.getDevice().getAddress());
                    mConnectionStatus = BLE_STATE_DISCONNECTED;
                    msg = mGattClientHandler.obtainMessage(MSG_REM_DEV_DISCONNECTED, null);
                    mGattClientHandler.sendMessage(msg);
                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "onConnectionStateChange:CONNECTED "
                            + " remoteDevice: " + gatt.getDevice().getAddress());
                    mConnectionStatus = BLE_STATE_CONNECTED;
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
                        PrintStr.setLength(0);
                        PrintStr.append("No services found!");
                        SocketServer.sendSocketData(PrintStr.toString());
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
                    PrintStr.setLength(0);
                    PrintStr.append("Service Discovery failed with status: "+ status);
                    SocketServer.sendSocketData(PrintStr.toString());
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
                    PrintStr.setLength(0);
                    PrintStr.append("Connection Update failed with status: "+ status);
                    SocketServer.sendSocketData(PrintStr.toString());
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
                    PrintStr.setLength(0);
                    PrintStr.append("Phy Update failed with status: "+ status);
                    SocketServer.sendSocketData(PrintStr.toString());
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic,
                                                int status){
                if ((status == GATT_SUCCESS)) {
                    /* get value based on format type */
                    String value = fetchFormatValue(characteristic);
                    msg = mGattClientHandler.obtainMessage(MSG_CHAR_READ_DONE, value);
                    mGattClientHandler.sendMessage(msg);
                } else {
                    Log.i(TAG, "Char read failed");
                    PrintStr.setLength(0);
                    PrintStr.append("Characteristic Read failed with status: "+ status);
                    SocketServer.sendSocketData(PrintStr.toString());
                }
                is_op_in_progress = false;
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic,
                                                int status) {
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "onCharacteristicWrite: " + status);
                    msg = mGattClientHandler.obtainMessage(MSG_CHAR_WRITE_DONE,
                            new String(characteristic.getValue()));
                    mGattClientHandler.sendMessage(msg);
                    if(reliable_write) {
                        String value = new String(characteristic.getValue());
                        /* check the value written is correct or not */
                        if(offset_value.equals(value)) {
                            Log.d(TAG, "Data matched, proceeding!!");
                            if(!reliable_write_no_more_data) {
                                /*check if the total data is written, if no write*/
                                if(total_length > length_offset + mtu_size-5) {
                                    offset_value = RdWrReliableClass.Value.substring(
                                           length_offset,(length_offset + mtu_size-5));
                                    length_offset +=  (mtu_size - 5);
                                    /* set value according to format type */
                                    if(RdWrReliableClass.Format_type == GATT_FORMAT_STRING) {
                                         characteristic.setValue(new String(offset_value));
                                    }
                                    else if(RdWrReliableClass.Format_type == GATT_FORMAT_INT){
                                         characteristic.setValue(offset_value.getBytes());
                                    }
                                    else{
                                        Log.e(TAG, "using default format");
                                        characteristic.setValue(offset_value.getBytes());
                                    }
                                    mgattClient.mBluetoothGatt.writeCharacteristic(
                                                characteristic);
                                } else if(total_length <= length_offset + mtu_size - 5) {
                                    /* last chunk */
                                    offset_value = RdWrReliableClass.Value.substring(length_offset,
                                          total_length);
                                    reliable_write_no_more_data = true;
                                    length_offset = total_length - (mtu_size - 5);
                                    /* set value according to format type */
                                    if(RdWrReliableClass.Format_type == GATT_FORMAT_STRING) {
                                        characteristic.setValue(new String(offset_value));
                                    }
                                    else if(RdWrReliableClass.Format_type == GATT_FORMAT_INT){
                                        characteristic.setValue(offset_value.getBytes());
                                    }
                                    else{
                                        Log.e(TAG, "using default format");
                                        characteristic.setValue(offset_value.getBytes());
                                    }
                                    mgattClient.mBluetoothGatt.writeCharacteristic(
                                                characteristic);
                                }
                            } else {
                                    /*execute write*/
                                    if(mgattClient.mBluetoothGatt.executeReliableWrite()) {
                                        Log.i(TAG, "Execute Write Successful!");
                                        length_offset = 0;
                                        reliable_write = false;
                                        reliable_write_no_more_data = false;
                                        offset_value = null;
                                    } else {
                                        Log.e(TAG, "Execute Write Failed!");
                                        PrintStr.setLength(0);
                                        PrintStr.append("Execute Write failed!");
                                        SocketServer.sendSocketData(PrintStr.toString());
                                        length_offset = 0;
                                        reliable_write = false;
                                        reliable_write_no_more_data = false;
                                        offset_value = null;
                                    }
                            }
                        } else {
                            /* abort reliable write if the value written doesn't match*/
                            Log.e(TAG, "Data doesn't match");
                            /*abort*/
                            mgattClient.mBluetoothGatt.abortReliableWrite();
                            reliable_write = false;
                            length_offset = 0;
                            reliable_write_no_more_data = false;
                            offset_value = null;
                        }
                    }
                } else {
                    Log.i(TAG, "write characteristic failed");
                    PrintStr.setLength(0);
                    PrintStr.append("Characteristic Write failed with status: "+ status);
                    SocketServer.sendSocketData(PrintStr.toString());
                    reliable_write = false;
                    length_offset = 0;
                    reliable_write_no_more_data = false;
                    offset_value = null;
                }
                is_op_in_progress = false;
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
                    PrintStr.setLength(0);
                    PrintStr.append("Descriptor Read failed with status: "+ status);
                    SocketServer.sendSocketData(PrintStr.toString());
                }
                is_op_in_progress = false;
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
                    PrintStr.setLength(0);
                    PrintStr.append("Descriptor Write failed with status: "+ status);
                    SocketServer.sendSocketData(PrintStr.toString());
                }
                is_op_in_progress = false;
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
                    PrintStr.setLength(0);
                    PrintStr.append("Read Phy failed with status: "+ status);
                    SocketServer.sendSocketData(PrintStr.toString());
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
                    PrintStr.setLength(0);
                    PrintStr.append("MTU Exchange failed with status: "+ status);
                    SocketServer.sendSocketData(PrintStr.toString());
                }
            }
        };

        public void connect(BluetoothDevice device){
            if((BleAppService.bleAdapter!=null) && (mConnectionStatus == BLE_STATE_DISCONNECTED)) {
                Log.i(TAG, "Gatt Connect");
                mDevice = device;
                mConnectionStatus = BLE_STATE_CONNECTING;
                mBluetoothGatt = mDevice.connectGatt(mcontext, false, mGattCallbacks,TRANSPORT_LE);
            }
        }

        public void pair(){
            if(mDevice.getBondState() != BluetoothDevice.BOND_BONDED){
                Log.i(TAG, "Pairing!");
                if(!mDevice.createBond(TRANSPORT_LE)) {
                    Log.i(TAG, "couldn't start pairing");
                    PrintStr.setLength(0);
                    PrintStr.append("Pairing failed!");
                    SocketServer.sendSocketData(PrintStr.toString());
                }
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
                    PrintStr.setLength(0);
                    PrintStr.append("Unpairing failed!");
                    SocketServer.sendSocketData(PrintStr.toString());
                }
            }
        }

        public String fetchFormatValue(BluetoothGattCharacteristic characteristic) {
            /* String format */
            if(RdWrClass.Format_type == GATT_FORMAT_STRING) {
                String value = characteristic.getStringValue(0);
                Log.i(TAG, "Characteristic value(string) is "+ value);
                return value;
            }
            /* Int format */
            else if(RdWrClass.Format_type == GATT_FORMAT_INT){
                byte[] value = characteristic.getValue();
                StringBuilder result = new StringBuilder();
                for (byte temp : value) {
                    result.append(String.format("%d ", temp));
                }
                Log.i(TAG, "Characteristic value(int) is "+ result.toString());
                return (result.toString());
            }
            /* Default format - Hex */
            else{
                Log.e(TAG, "Default format");
                byte[] value = characteristic.getValue();
                StringBuilder result = new StringBuilder();
                for (byte temp : value) {
                    result.append(String.format("%02x ", temp));
                }
                Log.i(TAG, "Characteristic value(hex) is "+ result.toString());
                return (result.toString());
            }
        }
    }

    public class GattClientMessageHandler extends Handler {
        Context mMsgContext;
        private static final String TAG = "GattClientMessageHandler";

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
                    if(BleAppService.mScannerService.mScanstatus) {
                        PrintStr.setLength(0);
                        PrintStr.append("Connect failed, there is an ongoing scan");
                        SocketServer.sendSocketData(PrintStr.toString());
                    } else {
                        processCheckAndStartBleScan(scn);
                    }
                    break;
              case MSG_START_BLE_CONNECT_TO_BDADDR:
                    String bdAddr = (String) msg.obj;
                    processConnectToBdaddr(bdAddr);
                    break;
                case MSG_START_CANCEL_CONNECT:
                    processCancelConnect();
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
                case MSG_BLE_GATT_REQ_CONN_PRIORITY:
                    int conn_priority = (int) msg.obj;
                    processConnPriorityReq(conn_priority);
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
                case MSG_START_BLE_GATT_DISCONNECT:
                    processDisconnectReq();
                    break;
                case MSG_START_BLE_GATT_WRITE_READ_CHAR:
                    RdWrClass = (ReadWriteOp) msg.obj;
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
                default:
                    Log.e(TAG, "Unknown Operation");
                    break;
            }
        }

        private void processCheckAndStartBleScan(Scan scn) {
            Log.i(TAG, "scanflag set,service bound: " + BleAppService.boundS);
            BleAppService.mScannerService.set_scan_parameters(scn);

            PrintStr.setLength(0);
            PrintStr.append("Scanning Started!");
            SocketServer.sendSocketData(PrintStr.toString());
        }

        private void processConnectToBdaddr(String bdAddr) {
            if(BleAppService.bleAdapter != null) {
                Log.i(TAG, "Connect to Address: " + bdAddr);
                BluetoothDevice remoteDevice = BleAppService.bleAdapter.getRemoteDevice(bdAddr);
                mgattClient.connect(remoteDevice);
            }
        }

        private void processCancelConnect() {
            Log.i(TAG, "processCancelConnect mConnectionStatus: " + mConnectionStatus
                    + " mScanStatus: "+ BleAppService.mScannerService.mScanstatus);
            if(BleAppService.mScannerService.mScanstatus) {
                BleAppService.mScannerService.stopScan();
                PrintStr.setLength(0);
                PrintStr.append("Scan Stopped!");
                SocketServer.sendSocketData(PrintStr.toString());
            } else if (mConnectionStatus == BLE_STATE_CONNECTING) {
                mConnectionStatus = BLE_STATE_DISCONNECTED;
                mgattClient.disconnect();
                PrintStr.setLength(0);
                PrintStr.append("Connection cancelled!");
                SocketServer.sendSocketData(PrintStr.toString());
            }
        }

        private void processScanDevFound(BluetoothDevice device) {
            Log.i(TAG, "matchFoundEvent Address:" + device.getAddress());
            if(BleAppService.mScannerService.mScanstatus) {
                BleAppService.mScannerService.stopScan();
            }
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
                    PrintStr.setLength(0);
                    PrintStr.append("Conn update failed: SDK version should be 28 or higher!");
                    SocketServer.sendSocketData(PrintStr.toString());

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

        private void processStartUnpair(){
            Log.i(TAG, "Starting Unpair");
            mgattClient.unpair();
            PrintStr.setLength(0);
            PrintStr.append("Device unpaired");
            SocketServer.sendSocketData(PrintStr.toString());
        }

        private void processDisconnectReq() {
            Log.i(TAG, "Disconnecting!");
            mConnectionStatus = BLE_STATE_DISCONNECTING;
            mgattClient.disconnect();
        }

        private void processCloseReq() {
            Log.i(TAG, "Close!");
            mgattClient.mBluetoothGatt.close();
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

        private void processConnPriorityReq(int conn_pri) {
            Log.i(TAG, "Request connection priority");
            mgattClient.mBluetoothGatt.requestConnectionPriority(conn_pri);
        }

        private void processRefreshServices() {
            Log.i(TAG, "Refresh Services");
            if(!(mgattClient.mBluetoothGatt.refresh())) {
                Log.e(TAG, "Refresh services failed");
                PrintStr.setLength(0);
                PrintStr.append("Refresh services failed!");
                SocketServer.sendSocketData(PrintStr.toString());
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

            if(is_op_in_progress) {
                Log.d(TAG, "Operation in progress");
                PrintStr.setLength(0);
                PrintStr.append("Operation in Progress, please wait until it is finished!!");
                SocketServer.sendSocketData(PrintStr.toString());
                return;
            }
            is_op_in_progress = true;

            Log.d(TAG, "Service UUID:"+RdWrClass.Srvc_uuid);

            if(mServiceUUID.contains(UUID.fromString(RdWrClass.Srvc_uuid))) {
                Log.d(TAG, "Service is found");
                if (!mCharUUID.contains(UUID.fromString(RdWrClass.Char_uuid))) {
                    Log.e(TAG, "Characteristic not found");
                    PrintStr.setLength(0);
                    PrintStr.append("Characteristic not found!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    is_op_in_progress = false;
                    return;
                }
            } else {
                Log.e(TAG, "Service is not found");
                PrintStr.setLength(0);
                PrintStr.append("Service not found!");
                SocketServer.sendSocketData(PrintStr.toString());
                is_op_in_progress = false;
                return;
            }
            mService = mgattClient.mBluetoothGatt.getService(UUID.fromString(
                                   RdWrClass.Srvc_uuid));
            mCharacteristic = mService.getCharacteristic(UUID.fromString(RdWrClass.Char_uuid));
            Log.d(TAG, "Srvc uuid" + mService.getUuid().toString() +
                       "char uuid:" + mCharacteristic.getUuid().toString());

            if(RdWrClass.operation == GATT_READ) {
                status = mgattClient.mBluetoothGatt.readCharacteristic(mCharacteristic);
                if(status != true)
                    Log.e(TAG, "Read Char failed");
                    PrintStr.setLength(0);
                    PrintStr.append("Read Char failed!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    is_op_in_progress = false;
                return;
            }

            else if(RdWrClass.operation == GATT_WRITE) {
                /* Check for write type */
                if(RdWrClass.Write_type != 0) {
                    mCharacteristic.setWriteType(RdWrClass.Write_type);
                    /* Check for length and set value */
                    if((RdWrClass.Value.length() >= (mtu_size -15)) &&
                        (RdWrClass.Write_type ==
                                BluetoothGattCharacteristic.WRITE_TYPE_SIGNED)) {
                        Log.e(TAG, "Write failed: Length cannot be more than" +
                                "ATT_MTU-15 for signed write");
                        PrintStr.setLength(0);
                        PrintStr.append("Write failed: Length cannot be more than" +
                                "ATT_MTU-15 for write!");
                        SocketServer.sendSocketData(PrintStr.toString());
                        is_op_in_progress = false;
                        return;
                    }
                    else if ((RdWrClass.Value.length() >= (mtu_size - 3)) &&
                            (RdWrClass.Write_type !=
                                    BluetoothGattCharacteristic.WRITE_TYPE_SIGNED)) {
                        Log.e(TAG, "Write failed: Length cannot be more than" +
                                "ATT_MTU-3 for write");
                        PrintStr.setLength(0);
                        PrintStr.append("Write failed: Length cannot be more than" +
                                "ATT_MTU-3 for write!");
                        SocketServer.sendSocketData(PrintStr.toString());
                        is_op_in_progress = false;
                        return;
                    }
                } else {
                    mCharacteristic.setWriteType(
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    if(RdWrClass.Value.length() >= (mtu_size - 3)) {
                        Log.e(TAG, "Write failed: Length cannot be more than" +
                                "ATT_MTU-3 for write");
                        PrintStr.setLength(0);
                        PrintStr.append("Write failed: Length cannot be more than" +
                                "ATT_MTU-3 for write!");
                        SocketServer.sendSocketData(PrintStr.toString());
                        is_op_in_progress = false;
                        return;
                    }
                }
                if(RdWrClass.Format_type == GATT_FORMAT_STRING) {
                    mCharacteristic.setValue((RdWrClass.Value).getBytes());
                }
                else if(RdWrClass.Format_type == GATT_FORMAT_INT){
                    mCharacteristic.setValue(
                          Integer.parseInt(RdWrClass.Value),
                            BluetoothGattCharacteristic.FORMAT_UINT32,0);
                }
                else{
                    Log.e(TAG, "using default format");
                    mCharacteristic.setValue((RdWrClass.Value).getBytes());
                }
                mgattClient.mBluetoothGatt.writeCharacteristic(
                             mCharacteristic);
            } else {
                is_op_in_progress = false;
                Log.e(TAG, "invalid operation");
                PrintStr.setLength(0);
                PrintStr.append("Invalid Operation!");
                SocketServer.sendSocketData(PrintStr.toString());
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
                    PrintStr.setLength(0);
                    PrintStr.append("Characteristic not found!");
                    SocketServer.sendSocketData(PrintStr.toString());
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
                    PrintStr.setLength(0);
                    PrintStr.append("Characteristic not found!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    return;
                }
             } else {
                Log.e(TAG, "Service is not found");
                PrintStr.setLength(0);
                PrintStr.append("Service not found!");
                SocketServer.sendSocketData(PrintStr.toString());
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
                    PrintStr.setLength(0);
                    PrintStr.append("Characteristic not found!");
                    SocketServer.sendSocketData(PrintStr.toString());
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
                    PrintStr.setLength(0);
                    PrintStr.append(" Descriptor not found!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    return;
                }
            } else {
                Log.e(TAG, "Service is not found");
                PrintStr.setLength(0);
                PrintStr.append("Service not found!");
                SocketServer.sendSocketData(PrintStr.toString());
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

            if(is_op_in_progress) {
                Log.d(TAG, "Operation in progress");
                PrintStr.setLength(0);
                PrintStr.append("Operation in Progress, please wait until it is finished!!");
                SocketServer.sendSocketData(PrintStr.toString());
                return;
            }
            is_op_in_progress = true;


            if(mServiceUUID.contains(UUID.fromString(RdWrReliableClass.Srvc_uuid)))  {
                Log.d(TAG, "mService is not null");
                if (!mCharUUID.contains(UUID.fromString(RdWrReliableClass.Char_uuid))) {
                    Log.e(TAG, "Characteristic not found");
                    PrintStr.setLength(0);
                    PrintStr.append("Characteristic not found!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    is_op_in_progress = false;
                    return;
                }
            } else {
                Log.e(TAG, "Service is not found");
                PrintStr.setLength(0);
                PrintStr.append("Service not found!");
                SocketServer.sendSocketData(PrintStr.toString());
                is_op_in_progress = false;
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
                PrintStr.setLength(0);
                PrintStr.append("ReliableWrite failed!");
                SocketServer.sendSocketData(PrintStr.toString());
                is_op_in_progress = false;
                return;
            }

            mCharacteristic.setWriteType(
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            total_length = RdWrReliableClass.Value.length();
            if(total_length >= (mtu_size - 5)) {
                reliable_write_no_more_data = false;
                offset_value = RdWrReliableClass.Value.substring(
                        length_offset, (mtu_size - 5));
                length_offset +=  (mtu_size - 5);
                /* set value according to format type */
                if(RdWrReliableClass.Format_type == GATT_FORMAT_STRING) {
                    mCharacteristic.setValue(new String(offset_value));
                }
                else if(RdWrReliableClass.Format_type == GATT_FORMAT_INT){
                    mCharacteristic.setValue(offset_value.getBytes());
                }
                else{
                    Log.e(TAG, "using default format");
                    mCharacteristic.setValue(offset_value.getBytes());
                }
            } else {
                length_offset = total_length;
                reliable_write_no_more_data = true;
                offset_value = String.valueOf(RdWrReliableClass.Value);
                Log.d(TAG, "leng_offset"+length_offset+"offset_value"+offset_value.toString());
                /* set value according to format type */
                if(RdWrReliableClass.Format_type == GATT_FORMAT_STRING) {
                    mCharacteristic.setValue(new String(offset_value));
                }
                else if(RdWrReliableClass.Format_type == GATT_FORMAT_INT){
                    mCharacteristic.setValue(offset_value.getBytes());
                }
                else{
                    Log.e(TAG, "using default format");
                    mCharacteristic.setValue(offset_value.getBytes());
                }
            }
            mgattClient.mBluetoothGatt.writeCharacteristic(
                                        mCharacteristic);
        }

        private void processGattAbortReliableWrite() {
            mgattClient.mBluetoothGatt.abortReliableWrite();
            reliable_write = false;
            reliable_write_no_more_data = false;
            length_offset = 0;
            is_op_in_progress = false;
        }

        private void processGattReadWriteDescReq(ReadWriteOp RdWrClass) {
            BluetoothGattCharacteristic mCharacteristic;
            BluetoothGattService mService;
            BluetoothGattDescriptor mDescriptor;
            boolean status = true;

            if(is_op_in_progress) {
                Log.d(TAG, "Operation in progress");
                PrintStr.setLength(0);
                PrintStr.append("Operation in Progress, please wait until it is finished!!");
                SocketServer.sendSocketData(PrintStr.toString());
                return;
            }
            is_op_in_progress = true;

           if(mServiceUUID.contains(UUID.fromString(RdWrClass.Srvc_uuid)))  {
                if (!mCharUUID.contains(UUID.fromString(RdWrClass.Char_uuid))) {
                    Log.e(TAG, "Characteristic not found");
                    PrintStr.setLength(0);
                    PrintStr.append("Characteristic not found!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    is_op_in_progress = false;
                    return;
                }
               if (!mDescUUID.contains(UUID.fromString(RdWrClass.Desc_uuid))) {
                    Log.e(TAG, "Descriptor not found");
                    PrintStr.setLength(0);
                    PrintStr.append("Descriptor not found!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    is_op_in_progress = false;
                    return;
                }
            } else {
                PrintStr.setLength(0);
                PrintStr.append("Service not found!");
                SocketServer.sendSocketData(PrintStr.toString());
                Log.e(TAG, "Service is not found");
                is_op_in_progress = false;
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
                if(status != true) {
                    Log.e(TAG, "Read Desc failed");
                    PrintStr.setLength(0);
                    PrintStr.append("Read Desc failed!");
                    SocketServer.sendSocketData(PrintStr.toString());
                    is_op_in_progress = false;
                }

                return;
            }

            if(RdWrClass.operation == GATT_WRITE) {
                mDescriptor.setValue((RdWrClass.Value).getBytes());
                mgattClient.mBluetoothGatt.writeDescriptor(mDescriptor);
            } else {
                Log.e(TAG, "invalid operation");
                PrintStr.setLength(0);
                PrintStr.append("Invalid Operation!");
                SocketServer.sendSocketData(PrintStr.toString());
                is_op_in_progress = false;
            }
        }
    }
}
