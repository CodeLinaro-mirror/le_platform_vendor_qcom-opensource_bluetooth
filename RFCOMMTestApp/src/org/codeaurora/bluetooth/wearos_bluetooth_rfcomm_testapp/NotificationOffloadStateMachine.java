/*
 * Copyright (c) 2021, The Linux Foundation. All rights reserved.
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

package org.codeaurora.bluetooth.wearos_bluetooth_rfcomm_testapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.util.IState;
import com.android.internal.util.StateMachine;
import com.android.internal.util.State;

public class NotificationOffloadStateMachine extends StateMachine {
    private static final String TAG = "BluetoothTxRxApp NotificationOffloadStateMachine";

    // Declaring Service Object

    AppControlService mAppControlService;
    BluetoothDevice mConnectedDevice;

    // Declaring the States
    private InitState mInitState;
    private ReadyToConnect mReadyToConnect;
    private ReadyToAcceptConnection mReadyToAcceptConnection;
    private ConnectedState mConnectedState;
    public NotificationReceiveState mNotRcvState;
    private DisconnectedState mDisconnectedState;
    private ControlPointState mControlPointState;
    private Offloaded mOffloaded;
    public static IState ncPreviousState;

    private static final String name = "BluetoothRFCommNotApp";

    public NotificationOffloadStateMachine(AppControlService service) {
        super("NotificationOffloadStateMachine");

        mAppControlService = service;

        // Initializing States
        mInitState = new InitState();
        mReadyToConnect = new ReadyToConnect();
        mReadyToAcceptConnection = new ReadyToAcceptConnection();
        mConnectedState = new ConnectedState();
        mDisconnectedState = new DisconnectedState();
        mNotRcvState = new NotificationReceiveState();
        mControlPointState = new ControlPointState();
        mOffloaded = new Offloaded();

        // Adding States
        addState(mInitState);
        addState(mReadyToConnect);
        addState(mReadyToAcceptConnection);
        addState(mConnectedState);
        addState(mDisconnectedState);
        addState(mNotRcvState);
        addState(mControlPointState);
        addState(mOffloaded);

        // set initial state to Paired state
        Log.d(TAG, "setting initial state as Init state");
        setInitialState(mInitState);
    }// end of constructor

    public void cleanUp() {
        mAppControlService = null;
        quitNow();
        Utils.appControlStateMachine = null;
    }

    private class InitState extends State {
        private static final String TAG = "NotificationOffloadStateMachine Init State";

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
            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONNECTED:
                transitionTo(mConnectedState);
                Log.d(TAG, "Going to Connected state");
                break;

            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONNECTION_FAILED:
                transitionTo(mInitState);
                Log.d(TAG, "Going to Init state");
                break;

            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_READY_TO_CONNECT:
                transitionTo(mReadyToConnect);
                Log.d(TAG, "Going to Ready to Connect state");
                break;

            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_READY_TO_ACCEPT_CONNECTION:
                transitionTo(mReadyToAcceptConnection);
                Log.d(TAG, "Going to Ready to Accept Connect state");
                break;
            case Utils.MSG_NC_SM_OFFLOADED:
                Log.d(TAG, "Going to Offloaded state");
                transitionTo(mOffloaded);
                break;
            }
            return retvalue;
        }
    }

    private class ReadyToConnect extends State {
        private static final String TAG = "NotificationOffloadStateMachine ReadyToConnect State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");
            if (mAppControlService != null) {
                mAppControlService.initializeTestSetup();
            }
            SocketServer
            .sendSocketData("Connecting Device...Please wait...!!!");
        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONNECTED:
                transitionTo(mConnectedState);
                mConnectedDevice = (BluetoothDevice) message.obj;
                Log.d(TAG, "Going to Connected state");
                break;

            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONNECTION_FAILED:
                transitionTo(mInitState);
                Log.d(TAG, "Going to Init state");
                SocketServer
                .sendSocketData("Connection Failed...Please Restart Phone App");
                break;

            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_DISCONNECTED:
                transitionTo(mInitState);
                SocketServer.mainMenuState = SocketServer.INIT_MENU;
                SocketServer.processOutputState = SocketServer.INIT_MENU;
                SocketServer.updateSocketClient();
                break;
            case Utils.MSG_NC_SM_OFFLOADED:
                transitionTo(mOffloaded);
                break;
            }
            return retvalue;
        }
    }

    private class ReadyToAcceptConnection extends State {
        private static final String TAG = "NotificationOffloadStateMachine ReadyToAcceptConnection State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");
            if (mAppControlService != null) {
                mAppControlService.startReadyToAcceptConnection();
            }
            SocketServer
            .sendSocketData("Accepting Incoming Connection with UUID :: "
                    + Utils.UUIDConstants.INCOMING_CONNECTION_UUID
                    .toString());
        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONNECTED:
                transitionTo(mConnectedState);
                mConnectedDevice = (BluetoothDevice) message.obj;
                Log.d(TAG, "Going to Connected state");
                break;

            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONNECTION_FAILED:
                transitionTo(mInitState);
                Log.d(TAG, "Going to Init state");
                SocketServer
                .sendSocketData("Connection Failed...Please Restart Phone App");
                break;

            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_DISCONNECTED:
                transitionTo(mInitState);
                SocketServer.mainMenuState = SocketServer.INIT_MENU;
                SocketServer.processOutputState = SocketServer.INIT_MENU;
                SocketServer.updateSocketClient();
                break;
            case Utils.MSG_NC_SM_OFFLOADED:
                transitionTo(mOffloaded);
                break;
            }
            return retvalue;
        }
    }

    private class ConnectedState extends State {
        private static final String TAG = "NotificationOffloadStateMachine Connected State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");
            mAppControlService.startNotRcvOperation();
            mAppControlService.initServerSocket();
            transitionTo(mNotRcvState);
            Log.d(TAG, "Going to Notification Receive state");
            SocketServer.sendSocketData("Device is Connected.");
        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_DISCONNECTED:
                mAppControlService.closeConnection();
                transitionTo(mInitState);
                SocketServer.mainMenuState = SocketServer.INIT_MENU;
                SocketServer.processOutputState = SocketServer.INIT_MENU;
                SocketServer.updateSocketClient();
                break;
            case Utils.MSG_NC_SM_OFFLOADED:
                transitionTo(mOffloaded);
                break;
            }
            return retvalue;
        }
    }

    private class NotificationReceiveState extends State {
        private static final String TAG = "NotificationOffloadStateMachine NotificationReceiveState State";

        @Override
        public void enter() {
            Log.d(TAG, "enter()");
            SocketServer.mainMenuState = SocketServer.NOT_RCV;
            SocketServer.processOutputState = SocketServer.NOT_RCV;
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
                case Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONTROL_POINT:
                    transitionTo(mControlPointState);
                    SocketServer.mainMenuState = SocketServer.CTL_PT;
                    SocketServer.processOutputState = SocketServer.CTL_PT;
                    Log.d(TAG, "Going to ControlPoint state");
                    break;
                case Utils.NotificationOffloadStateMachineMessageConstants.STATE_DISCONNECTED:
                    mAppControlService.closeConnection();
                    transitionTo(mInitState);
                    SocketServer.mainMenuState = SocketServer.INIT_MENU;
                    SocketServer.processOutputState = SocketServer.INIT_MENU;
                    SocketServer.updateSocketClient();
                    break;
                case Utils.MSG_NC_SM_OFFLOADED:
                    transitionTo(mOffloaded);
                    break;
            }
            return retvalue;
        }
    }

    private class ControlPointState extends State {
        private static final String TAG = "NotificationOffloadStateMachine ControlPointState State";

        @Override
        public void enter() {
            Log.d(TAG, "Going to ControlPoint state");
            SocketServer.sendSocketData("ControlPoint State Started");
        }

        @Override
        public void exit() {
            Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retvalue = HANDLED;
            switch (message.what) {
                case Utils.NotificationOffloadStateMachineMessageConstants.STATE_CONTROL_POINT:
                    // Ignore
                    break;
                case Utils.NotificationOffloadStateMachineMessageConstants.STATE_NOT_PROCESS_START:
                    mAppControlService.startNotCTLPTOperation();
                    break;
                case Utils.NotificationOffloadStateMachineMessageConstants.STATE_INFO_RESPONSE:
                    Log.d(TAG, "STATE_INFO_RESPONSE ");
                    mAppControlService.responseFromCLI((String) message.obj);
                    break;
                case Utils.NotificationOffloadStateMachineMessageConstants.STATE_NOT_PROCESS_END:
                    transitionTo(mNotRcvState);
                    break;
                case Utils.NotificationOffloadStateMachineMessageConstants.STATE_DISCONNECTED:
                    mAppControlService.closeConnection();
                    transitionTo(mInitState);
                    SocketServer.mainMenuState = SocketServer.INIT_MENU;
                    SocketServer.processOutputState = SocketServer.INIT_MENU;
                    SocketServer.updateSocketClient();
                    break;
            }
            return retvalue;
        }
    }

    private class Offloaded extends State {
        private static final String TAG = "Offloaded";

        @Override
        public void enter() {
           Log.i(TAG, "Enter: " + getCurrentMessage().what);
           SocketServer.sendSocketData("Offloaded State");
           Log.d(TAG, "Reading context from protobuf");
           //byte[] blob = AppContextProto.getAppContextProtoBuffer();
           //if(blob.length > 0) {
           //    processSetAppContext(blob);
           //}
           //inform OffloadableAppAdapter
           mAppControlService.mNotificationAdapter.enableOffloadDone(mAppControlService.BT_OK);
        }

        @Override
        public void exit() {
             Log.d(TAG, "exit()");
        }

        @Override
        public boolean processMessage(Message message) {
            Log.i(TAG, "processMessage: " + message.what);
            boolean retValue = HANDLED;

            switch (message.what) {
                case Utils.MSG_NC_SM_ACTIVE:
                    SocketServer.sendSocketData("Transitioned to active");
                    //we need to make a note of what the previous state was
                    // and transition to that state
                    transitionTo(ncPreviousState);
                    break;
            }
            return retValue;
        }
    }

    private class DisconnectedState extends State {
        private static final String TAG = "NotificationOffloadStateMachine Disconnected State";

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
            case Utils.NotificationOffloadStateMachineMessageConstants.STATE_DISCONNECTED:
                break;
            }
            return retvalue;
        }
    }
}
