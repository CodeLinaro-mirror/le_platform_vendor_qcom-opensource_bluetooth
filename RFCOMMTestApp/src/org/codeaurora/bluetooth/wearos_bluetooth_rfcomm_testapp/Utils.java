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
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 *
 */

package org.codeaurora.bluetooth.wearos_bluetooth_rfcomm_testapp;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;

public class Utils {

    public static String bdAddressFromConfig = null;
    public static AppControlStateMachine appControlStateMachineforGAP = null;
    public static AppControlService mAppControlService = null;
    public static SocketServer socServer = SocketServer.getInstance();
    public static boolean isThroughputStateMachineUnderProcessing = false;
    public static boolean isSppFileTransferOngoing = false;
    public static boolean isSppConnection = false;

    // Hashmap storing <BTAddr_UUID_ConnectionDirection, StateMachine>
    public static HashMap<String, AppControlStateMachine> btAddrUUIDToStateMachineMap = new HashMap<String,AppControlStateMachine>();
    // Hashmap storing <BTAddr_UUID_ConnectionDirection, Socket>
    public static HashMap<String, BluetoothSocket> btAddrUUIDToBTSocketMap = new HashMap<String, BluetoothSocket>();
    // Hashmap storing <SerialNum, HashMap<BTAddr_UUID_ConnectionDirection, BluetoothSocket>>
    public static HashMap<Integer, HashMap<String, BluetoothSocket>> srlNumToConnectionMap = new HashMap<Integer, HashMap<String, BluetoothSocket>>();
    // Current Connection Under Process
    public static HashMap<String, BluetoothSocket> currentConnection = null;

    public static class AppControlConstants {

        // Activity Processing Commands
        static final String CREATE_BOND = "CREATE_BOND";
        static final String START_DISCOVERY = "START_DISCOVERY";
        static final String READY_FOR_COMMANDS = "READY_FOR_COMMANDS";
        static final String APP_BAD_STATE = "APP_BAD_STATE";
        static final String BD_ADDRESS_NOT_AVAILABLE = "BD_ADDRESS_NOT_AVAILABLE";
        static final String END_TX_OPERATION = "END_TX_OPERATION";
        static final String END_RX_OPERATION = "END_RX_OPERATION";
        static final String BT_OFF_COMMAND = "svc bluetooth disable";
        static final String BT_ON_COMMAND = "svc bluetooth enable";

        // RFCOMM Use-case Commands
        static final String USE_CASE_RFCOMM_TPUT_TX = "USE_CASE_RFCOMM_TPUT_TX";
        static final String USE_CASE_RFCOMM_TPUT_RX = "USE_CASE_RFCOMM_TPUT_RX";

    }

    public static class StateMachineMessageConstants {
        static final int STATE_DISCONNECTED = 0;
        static final int STATE_READY_TO_CONNECT = 1;
        static final int STATE_CONNECTED = 2;
        static final int STATE_CONNECTION_FAILED = 3;

        static final int STATE_START_DATA_TX = 4;
        static final int STATE_END_DATA_TX = 5;
        static final int STATE_DATA_TX_FAILED = 6;

        static final int STATE_START_DATA_RX = 7;
        static final int STATE_END_DATA_RX = 8;
        static final int STATE_DATA_RX_FAILED = 9;

        static final int STATE_START_GAP_TEST_CASES = 16;
        static final int STATE_GAP_TEST_CASE_OFF_ON = 17;
        static final int STATE_GAP_TEST_CASE_SCAN_MODE = 18;
        static final int STATE_GAP_TEST_CASE_END_SCAN_MODE = 19;
        static final int STATE_GAP_TEST_CASE_END_OFF_ON = 20;
        static final int STATE_GAP_TEST_CASE_START_DISCOVERY = 21;
        static final int STATE_GAP_TEST_CASE_END_DISCOVERY = 22;
        static final int STATE_END_GAP_TEST_CASES = 23;
        static final int STATE_READY_TO_ACCEPT_CONNECTION = 24;
        static final int STATE_REGISTER_BLUETOOTH_HID = 25;
        static final int STATE_GAP_TEST_CASE_IS_CONNECTED = 30;
        static final int STATE_START_SPP_TEST_CASES = 25;
        static final int STATE_SPP_CONNECTED = 26;
        static final int STATE_START_SEND_FILE = 27;
        static final int STATE_END_SEND_FILE = 28;
        static final int STATE_START_RECEIVE_FILE = 29;
        static final int STATE_END_RECEIVE_FILE = 30;
    }

    public static class UUIDConstants
    {
        static final UUID APP_UUID = UUID
                .fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
        static final UUID APP_SERVER_UUID = UUID
            .fromString("8ce255c0-200a-11e0-ac64-0800200c9a77");
        static UUID INCOMING_CONNECTION_UUID = null;

        static final UUID SPP_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805f9b34fb");
    }

    public static class ConnectionUnderOperation{
        String bt_addr_uuid;
        BluetoothSocket socket;
    }

    public static String ellipsize(String input, int maxLength) {
        String ellip = "...";
        if (input == null || input.length() <= maxLength
               || input.length() < ellip.length()) {
            return input;
        }
        return input.substring(0, maxLength - ellip.length()).concat(ellip);
    }

    public static class IsConnected {
        String BDaddress;
    }

    public static class TxForSpp {
        String bt_addr_uuid;
        BluetoothSocket socket;
        String fileName;
    }
}
