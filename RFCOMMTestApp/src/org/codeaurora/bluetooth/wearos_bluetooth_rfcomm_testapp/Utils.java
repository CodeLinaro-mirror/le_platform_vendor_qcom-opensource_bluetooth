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

package org.codeaurora.bluetooth.wearos_bluetooth_rfcomm_testapp;

import java.util.UUID;

public class Utils {

    public static String bdAddressFromConfig = null;
    public static AppControlStateMachine appControlStateMachine = null;
    public static NotificationOffloadStateMachine notificationOffloadStateMachine = null;
    public static SocketServer socServer = SocketServer.getInstance();
    public static boolean isThroughputStateMachineUnderProcessing = false;
    public static boolean isOffloadStateMachineUnderProcessing = false;

    public static class AppControlConstants {

        // Activity Processing Commands
        static final String CREATE_BOND = "CREATE_BOND";
        static final String START_DISCOVERY = "START_DISCOVERY";
        static final String READY_FOR_COMMANDS = "READY_FOR_COMMANDS";
        static final String APP_BAD_STATE = "APP_BAD_STATE";
        static final String BD_ADDRESS_NOT_AVAILABLE = "BD_ADDRESS_NOT_AVAILABLE";
        static final String END_TX_OPERATION = "END_TX_OPERATION";
        static final String END_RX_OPERATION = "END_RX_OPERATION";
        static final String BT_OFF_COMMAND = "service call bluetooth_manager 8";
        static final String BT_ON_COMMAND = "service call bluetooth_manager 6";

        // RFCOMM Use-case Commands
        static final String USE_CASE_RFCOMM_TPUT_TX = "USE_CASE_RFCOMM_TPUT_TX";
        static final String USE_CASE_RFCOMM_TPUT_RX = "USE_CASE_RFCOMM_TPUT_RX";
        static final String USE_CASE_RFCOMM_OFFLOAD_WAKEABLE_NOTIFICATION = "USE_CASE_RFCOMM_OFFLOAD_WAKEABLE_NOTIFICATION";
        static final String USE_CASE_RFCOMM_OFFLOAD_ACTIONABLE_NOTIFICATION = "USE_CASE_RFCOMM_OFFLOAD_ACTIONABLE_NOTIFICATION";
        static final String USE_CASE_RFCOMM_OFFLOAD_CACHEABLE_NOTIFICATION = "USE_CASE_RFCOMM_OFFLOAD_CACHEABLE_NOTIFICATION";

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

        static final int STATE_START_DATA_TX_WAKEABLE = 10;
        static final int STATE_END_DATA_TX_WAKEABLE = 11;

        static final int STATE_START_DATA_TX_ACTIONABLE = 12;
        static final int STATE_END_DATA_TX_ACTIONABLE = 13;

        static final int STATE_START_DATA_TX_CACHEABLE = 14;
        static final int STATE_END_DATA_TX_CACHEABLE = 15;

        static final int STATE_START_GAP_TEST_CASES = 16;
        static final int STATE_GAP_TEST_CASE_OFF_ON = 17;
        static final int STATE_GAP_TEST_CASE_SCAN_MODE = 18;
        static final int STATE_GAP_TEST_CASE_END_SCAN_MODE = 19;
        static final int STATE_GAP_TEST_CASE_END_OFF_ON = 20;
        static final int STATE_GAP_TEST_CASE_START_DISCOVERY = 21;
        static final int STATE_GAP_TEST_CASE_END_DISCOVERY = 22;
        static final int STATE_END_GAP_TEST_CASES = 23;
        static final int STATE_READY_TO_ACCEPT_CONNECTION = 24;

    }

    public static class NotificationOffloadStateMachineMessageConstants {
        static final int STATE_DISCONNECTED = 0;
        static final int STATE_READY_TO_CONNECT = 1;
        static final int STATE_READY_TO_ACCEPT_CONNECTION = 2;
        static final int STATE_CONNECTED = 3;
        static final int STATE_CONNECTION_FAILED = 4;
        static final int STATE_NOTIFICATION_RECEIVE = 5;
        static final int STATE_NOTIFICATION_RECEIVED = 6;
        static final int STATE_CONTROL_POINT = 7;
        static final int STATE_INFO_RESPONSE = 8;
        static final int STATE_NOT_PROCESS_START = 9;
        static final int STATE_NOT_PROCESS_END = 10;
    }

    /* Offload Service Actions */
    public static final int MSG_AS_REGISTER_OFFLODABLE_ADAPTER = 0;
    public static final int MSG_AS_DREGISTER_OFFLODABLE_ADAPTER = 1;
    public static final int MSG_AS_SET_MODE = 2;

    //Offload states
    public static final int MSG_NC_SM_OFFLOADED = 21;
    public static final int MSG_NC_SM_ACTIVE = 22;

    public static class UUIDConstants {
        static final UUID APP_UUID = UUID
                .fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
        static final UUID APP_SERVER_UUID = UUID
            .fromString("8ce255c0-200a-11e0-ac64-0800200c9a77");
        static UUID INCOMING_CONNECTION_UUID = null;
    }

}
