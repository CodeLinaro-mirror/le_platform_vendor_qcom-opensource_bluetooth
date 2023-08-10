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
 *
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 *
 */

package org.codeaurora.bluetooth.wearos_bluetooth_rfcomm_testapp;


import org.codeaurora.bluetooth.wearos_bluetooth_rfcomm_testapp.Utils.UUIDConstants;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.bluetooth.BluetoothSocket;

import com.android.internal.util.IState;
import com.android.internal.util.StateMachine;
import com.android.internal.util.State;

import java.io.InputStream;
import java.io.IOException;

public class AppControlStateMachine extends StateMachine {
    private static final String TAG = "BluetoothTxRxApp AppControlStateMachine";

    // Declaring Service Object

    AppControlService mAppControlService;

    // Declaring the States
    private InitState mInitState;
    private ReadyToConnect mReadyToConnect;
    private ConnectedState mConnectedState;
    private ReadyState mReadyState;
    private DataTxState mDataTxState;
    private DataRxState mDataRxState;
    private DisconnectedState mDisconnectedState;
    private GapTestState mGapTestState;
    private ReadyToAcceptConnection mReadyToAcceptConnection;

    public AppControlStateMachine(AppControlService service) {
        super("AppControlStateMachine");

        mAppControlService = service;

        // Initializing States
        mInitState = new InitState();
        mReadyToConnect = new ReadyToConnect();
        mConnectedState = new ConnectedState();
        mReadyState = new ReadyState();
        mDataTxState = new DataTxState();
        mDataRxState = new DataRxState();
        mDisconnectedState = new DisconnectedState();
        mGapTestState = new GapTestState();
        mReadyToAcceptConnection = new ReadyToAcceptConnection();

        // Adding States
        addState(mInitState);
        addState(mReadyToConnect);
        addState(mConnectedState);
        addState(mReadyState);
        addState(mDataTxState);
        addState(mDataRxState);
        addState(mDisconnectedState);
        addState(mGapTestState);
        addState(mReadyToAcceptConnection);

        // set initial state to Paired state
        Log.d(TAG, "setting initial state as Init state");
        setInitialState(mInitState);
    }// end of constructor

    public void cleanUp() {
        mAppControlService = null;
        quitNow();
    }

    private class InitState extends State {
        private static final String TAG = "BluetoothTxRxApp Init State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");

        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.StateMachineMessageConstants.STATE_CONNECTED:
                transitionTo(mConnectedState);
                String bt_addr_uuid_direction = (String) message.obj;
                Log.d(TAG, "Going to Connected state :: "+bt_addr_uuid_direction);
                Rx rx = new Rx();
                rx.bt_addr_uuid = bt_addr_uuid_direction;
                rx.socket = Utils.btAddrUUIDToBTSocketMap.get(bt_addr_uuid_direction);
                mAppControlService.startRxOperation(rx);
                String[] tmp = bt_addr_uuid_direction.split(" ",3);
                if(tmp != null){
                    BluetoothDevice connectedDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(tmp[0].toUpperCase());
                    if (connectedDevice != null) {
                        if (!connectedDevice.getName().isEmpty()) {
                            SocketServer.sendSocketData("Device "
                                    + connectedDevice.getName()
                                    + " is Connected with UUID :: "
                                    + tmp[1]);
                        } else {
                            SocketServer.sendSocketData("Device "
                                    + connectedDevice.getAddress()
                                    + " is Connected with UUID :: "
                                    + tmp[1]);
                        }
                    } else {
                        SocketServer.sendSocketData("Device is Connected");
                    }
                }
                break;

            case Utils.StateMachineMessageConstants.STATE_CONNECTION_FAILED:
                transitionTo(mInitState);
                Log.d(TAG, "Going to Init state");
                break;

            case Utils.StateMachineMessageConstants.STATE_READY_TO_CONNECT:
                transitionTo(mReadyToConnect);
                Log.d(TAG, "Going to Ready to Connect state");
                break;

            case Utils.StateMachineMessageConstants.STATE_READY_TO_ACCEPT_CONNECTION:
                transitionTo(mReadyToAcceptConnection);
                Log.d(TAG, "Going to Ready to Accept Connection state");
                break;
            case Utils.StateMachineMessageConstants.STATE_START_GAP_TEST_CASES:
                transitionTo(mGapTestState);
                Log.d(TAG, "Going to GAP Test Cases state");
                break;
            }
            return retvalue;
        }
    }

    private class ReadyToConnect extends State {
        private static final String TAG = "BluetoothTxRxApp ReadyToConnect State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");
            SocketServer.sendSocketData("Connecting Device...Please wait...!!!");
        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.StateMachineMessageConstants.STATE_CONNECTED:
                transitionTo(mConnectedState);
                String bt_addr_uuid_direction = (String) message.obj;
                Log.d(TAG, "Going to Connected state :: "+bt_addr_uuid_direction);
                Rx rx = new Rx();
                rx.bt_addr_uuid = bt_addr_uuid_direction;
                rx.socket = Utils.btAddrUUIDToBTSocketMap.get(bt_addr_uuid_direction);
                mAppControlService.startRxOperation(rx);
                String[] tmp = bt_addr_uuid_direction.split(" ",3);
                if(tmp != null){
                    BluetoothDevice connectedDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(tmp[0].toUpperCase());
                    if (connectedDevice != null) {
                        if (!connectedDevice.getName().isEmpty()) {
                            SocketServer.sendSocketData("Device "
                                    + connectedDevice.getName()
                                    + " is Connected with UUID :: "
                                    + tmp[1]);
                        } else {
                            SocketServer.sendSocketData("Device "
                                    + connectedDevice.getAddress()
                                    + " is Connected with UUID :: "
                                    + tmp[1]);
                        }
                    } else {
                        SocketServer.sendSocketData("Device is Connected");
                    }
                }else{
                    Log.d(TAG, "Something went wrong in string split");
                }
                break;

            case Utils.StateMachineMessageConstants.STATE_CONNECTION_FAILED:
                transitionTo(mInitState);
                Log.d(TAG, "Going to Init state");
                SocketServer
                        .sendSocketData("Connection Failed...Please Restart Phone App");
                break;

            case Utils.StateMachineMessageConstants.STATE_DISCONNECTED:
                String disconnectedHandle = (String) message.obj;
                mAppControlService.closeConnection(disconnectedHandle);
                // transitionTo(mInitState);
                // SocketServer.mainMenuState = SocketServer.INIT_MENU;
                // SocketServer.processOutputState = SocketServer.INIT_MENU;
                // SocketServer.updateSocketClient();
                break;
            }
            return retvalue;
        }
    }

    private class ConnectedState extends State {
        private static final String TAG = "BluetoothTxRxApp Connected State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");
            transitionTo(mReadyState);
            Log.d(TAG, "Going to Ready state");
            SocketServer.mainMenuState = SocketServer.MAIN_MENU;
            SocketServer.processOutputState = SocketServer.MAIN_MENU;
        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.StateMachineMessageConstants.STATE_DISCONNECTED:
                String disconnectedHandle = (String) message.obj;
                mAppControlService.closeConnection(disconnectedHandle);
                break;
            }
            return retvalue;
        }
    }

    private class ReadyState extends State {
        private static final String TAG = "BluetoothTxRxApp ReadyState State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");
            SocketServer.updateSocketClient();
        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.StateMachineMessageConstants.STATE_START_DATA_TX:
                Tx txParam = (Tx) message.obj;
                SocketServer.sendSocketData("Tx Operation Started with chunkSize :" + txParam.chunkSize);
                mAppControlService.startTxOperation(txParam);
                SocketServer.mainMenuState = SocketServer.DEVICE_SELECTION_MENU;
                SocketServer.processOutputState = SocketServer.DEVICE_SELECTION_MENU;
                SocketServer.updateSocketClient();
                break;

            case Utils.StateMachineMessageConstants.STATE_START_DATA_RX:
                Rx rxParam = (Rx) message.obj;
                SocketServer.sendSocketData("Rx Operation Started");
                SocketServer.mainMenuState = SocketServer.DEVICE_SELECTION_MENU;
                SocketServer.processOutputState = SocketServer.DEVICE_SELECTION_MENU;
                SocketServer.updateSocketClient();
                break;

            case Utils.StateMachineMessageConstants.STATE_DISCONNECTED:
                String disconnectedHandle = (String) message.obj;
                mAppControlService.closeConnection(disconnectedHandle);
                //transitionTo(mInitState);
                // SocketServer.mainMenuState = SocketServer.INIT_MENU;
                // SocketServer.processOutputState = SocketServer.INIT_MENU;
                // SocketServer.updateSocketClient();
                break;
            }
            return retvalue;
        }
    }

    private class DataTxState extends State {
        private static final String TAG = "BluetoothTxRxApp DataTx State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");

        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.StateMachineMessageConstants.STATE_END_DATA_TX:
                transitionTo(mReadyState);
                Log.d(TAG, "Going to Ready state");
                SocketServer.processOutputState = SocketServer.DEVICE_SELECTION_MENU;
                SocketServer.mainMenuState = SocketServer.DEVICE_SELECTION_MENU;
                break;

            case Utils.StateMachineMessageConstants.STATE_DATA_TX_FAILED:
                transitionTo(mReadyState);
                Log.d(TAG, "Going to Ready state");
                SocketServer.processOutputState = SocketServer.DEVICE_SELECTION_MENU;
                SocketServer.mainMenuState = SocketServer.DEVICE_SELECTION_MENU;
                break;
            case Utils.StateMachineMessageConstants.STATE_DISCONNECTED:
                String disconnectedHandle = (String) message.obj;
                mAppControlService.closeConnection(disconnectedHandle);
                //transitionTo(mInitState);
                // SocketServer.mainMenuState = SocketServer.INIT_MENU;
                // SocketServer.processOutputState = SocketServer.INIT_MENU;
                // SocketServer.updateSocketClient();
                break;
            }
            return retvalue;
        }
    }

    private class DataRxState extends State {
        private static final String TAG = "BluetoothTxRxApp DataRx State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");

        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.StateMachineMessageConstants.STATE_END_DATA_RX:
                transitionTo(mReadyState);
                String bt_addr_uuid_direction = (String) message.obj;
                Log.d(TAG, "Going to Ready state "+bt_addr_uuid_direction);
                SocketServer.processOutputState = SocketServer.DEVICE_SELECTION_MENU;
                SocketServer.mainMenuState = SocketServer.DEVICE_SELECTION_MENU;
                break;

            case Utils.StateMachineMessageConstants.STATE_DATA_RX_FAILED:
                transitionTo(mReadyState);
                Log.d(TAG, "Going to Ready state");
                SocketServer.processOutputState = SocketServer.DEVICE_SELECTION_MENU;
                SocketServer.mainMenuState = SocketServer.DEVICE_SELECTION_MENU;
                break;
            case Utils.StateMachineMessageConstants.STATE_DISCONNECTED:
                String disconnectedHandle = (String) message.obj;
                mAppControlService.closeConnection(disconnectedHandle);
                //transitionTo(mInitState);
                // SocketServer.mainMenuState = SocketServer.INIT_MENU;
                // SocketServer.processOutputState = SocketServer.INIT_MENU;
                // SocketServer.updateSocketClient();
                break;
            }
            return retvalue;
        }
    }

    private class DisconnectedState extends State {
        private static final String TAG = "BluetoothTxRxApp Disconnected State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");

        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.StateMachineMessageConstants.STATE_DISCONNECTED:
                break;
            }
            return retvalue;
        }
    }

    private class GapTestState extends State {
        private static final String TAG="BluetoothTxRxApp GapTestState State";
        @Override
        public void enter() {
            Log.d(TAG, "enter()");
        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
            mAppControlService.stopStateMachineForGAP();
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_OFF_ON:
                ConnectionTest offOn = (ConnectionTest) message.obj;
                mAppControlService.startOnOffTestCase(offOn.count);
                break;

            case Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_SCAN_MODE:
                SetScanMode setScan = (SetScanMode) message.obj;
                mAppControlService.setScanMode(setScan.scanMode);
                break;

            case Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_END_SCAN_MODE:
                break;

            case Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_END_OFF_ON:
                break;

            case Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_START_DISCOVERY:
                mAppControlService.startDiscovery();
                break;

            case Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_END_DISCOVERY:

                break;

            case Utils.StateMachineMessageConstants.STATE_END_GAP_TEST_CASES:
                transitionTo(mInitState);
                Log.d(TAG, "Going to Init state");
                break;

            case Utils.StateMachineMessageConstants.STATE_DISCONNECTED:
                break;

            case Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_IS_CONNECTED:
                Utils.IsConnected isConnected = (Utils.IsConnected) message.obj;
                mAppControlService.checkIsConnected(isConnected.BDaddress);
                break;
            }

            return retvalue;
        }
    }

    private class ReadyToAcceptConnection extends State {
        private static final String TAG = "BluetoothTxRxApp ReadyToAcceptConnection State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");
         //   if (mAppControlService != null) {
         //       mAppControlService.startReadyToAcceptConnection();
         //   }
         //   SocketServer
         //   .sendSocketData("Accepting Incoming Connection with UUID :: "
         //           + Utils.UUIDConstants.INCOMING_CONNECTION_UUID
          //          .toString());
        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.StateMachineMessageConstants.STATE_CONNECTED:
                transitionTo(mConnectedState);
                String bt_addr_uuid_direction = (String) message.obj;
                Log.d(TAG, "Going to Connected state :: "+bt_addr_uuid_direction);
                Rx rx = new Rx();
                rx.bt_addr_uuid = bt_addr_uuid_direction;
                rx.socket = Utils.btAddrUUIDToBTSocketMap.get(bt_addr_uuid_direction);
                mAppControlService.startRxOperation(rx);
                String[] tmp = bt_addr_uuid_direction.split(" ",3);
                if(tmp != null){
                    BluetoothDevice connectedDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(tmp[0].toUpperCase());
                    if (connectedDevice != null) {
                        if (!connectedDevice.getName().isEmpty()) {
                            SocketServer.sendSocketData("Device "
                                    + connectedDevice.getName()
                                    + " is Connected with UUID :: "
                                    + tmp[1]);
                        } else {
                            SocketServer.sendSocketData("Device "
                                    + connectedDevice.getAddress()
                                    + " is Connected with UUID :: "
                                    + tmp[1]);
                        }
                    } else {
                        SocketServer.sendSocketData("Device is Connected");
                    }
                }
                break;
            case Utils.StateMachineMessageConstants.STATE_CONNECTION_FAILED:
                transitionTo(mInitState);
                Log.d(TAG, "Going to Init state");
                SocketServer
                .sendSocketData("Connection Failed...Please Retry");
                break;
            case Utils.StateMachineMessageConstants.STATE_DISCONNECTED:
                break;

            }

            return retvalue;
        }
    }

}
