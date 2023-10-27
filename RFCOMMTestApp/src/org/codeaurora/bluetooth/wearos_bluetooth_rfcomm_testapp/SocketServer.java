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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Message;
import android.util.Log;
import android.bluetooth.BluetoothSocket;

public class SocketServer {

    private static final String TAG = "SocketServer";
    final String SOCKET_ADDRESS = "RfCommTestAppSocket";

    private static SocketServer INSTANCE = new SocketServer();
    private static Semaphore mutex = new Semaphore(1);

    private static boolean socketOpen = false;
    private static boolean closeReceived = false;

    static int socSendBufferSize = 4096;
    int socRcvBufferSize = 512;
    byte[] socRcvBuffer;
    int bytesRead;

    // Client and Sever Socket
    LocalServerSocket server;
    LocalSocket client;

    createServerSocketThread createSeverSocket;
    InputStream input;
    private static OutputStream output;
    connectionHandler cnd;
    communicationHandler commHandler;

    static final int INIT_MENU = 0;
    static final int MAIN_MENU = 1;
    static final int THROUGHPUT_TESTING_MENU = 2;
    static final int DEVICE_SELECTION_MENU = 4;
    static final int NOT_RCV = 5;
    static final int CTL_PT = 6;
    static final int INVALID_INPUT = 7;
    static final int SOC_CLOSE_ACK = 8;
    static final int NONE = 9;
    static final int TX_RX_MENU = 10;
    //static final int BLUETOOTH_HID_TESTING_MENU = 10;
    static final int CONNECTION_TEST_MENU = 999;

     // SPP
    static final int BLUETOOTH_SPP_CONNECT_MENU = 15;
    static final int BLUETOOTH_SPP_TESTING_MENU = 16;
    static final int BLUETOOTH_SPP_MENU=17;

    static int mainMenuState = INIT_MENU;
    static int processOutputState = INIT_MENU;

    ConfigFileParser parser;

    private SocketServer() {
        Log.d(TAG, "SocketServer()");
        socRcvBuffer = new byte[socRcvBufferSize];
        parser = new ConfigFileParser();
        createSeverSocket = new createServerSocketThread();
        createSeverSocket.start();
    }

    public static SocketServer getInstance() {
        return (INSTANCE);
    }

    private class createServerSocketThread extends Thread {
        public createServerSocketThread() {
            Log.d(TAG, "createServerSocketThread()");
            bytesRead = 0;

            try {
                server = new LocalServerSocket(SOCKET_ADDRESS);
                Log.d(TAG, "LocalSocketServer created");
            } catch (IOException e) {
                Log.e(TAG, "LocalSocketServer created failed !!!");
                e.printStackTrace();
            }

            LocalSocketAddress localSocketAddress;
            localSocketAddress = server.getLocalSocketAddress();
            String str = localSocketAddress.getName();
            Log.d(TAG, "LocalSocketAddress = " + str);
        }

        public void run() {
            Log.d(TAG, "localServerSocket run()");
            if (null != server) {
                try {
                    Log.d(TAG, "localSocketServer begins to accept()");
                    while(true)
                    {
                        client = server.accept();
                        socketOpen = true;
                        Log.d(TAG, "localSocket accepted");
                        cnd = new connectionHandler();
                        cnd.start();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "localSocketServer accept() failed !!!");
                    e.printStackTrace();
                }

            } else {
                Log.d(TAG, "The LocalServerSocket is NULL");
            }
        }
    }

    private class connectionHandler extends Thread{
        public void run(){
            try {
                   input = client.getInputStream();
                   Log.d(TAG, "getInputStream");
            } catch(IOException e) {
                   Log.e(TAG, "getInputStream() failed !!!");
                   e.printStackTrace();
                }

                try {
                    output = client.getOutputStream();
                    Log.d(TAG, "getOutputStream");
                } catch (IOException e) {
                    Log.e(TAG, "getOutputStream() failed !!!");
                    e.printStackTrace();
                }

                commHandler = new communicationHandler();
                commHandler.start();
        }
    }

    private class communicationHandler extends Thread {

        public void run() {
            Log.d(TAG, "communicationHandler run()");

            // Display main menu and start receiving socket data
            mainMenuState = INIT_MENU;
            processOutputState = INIT_MENU;
            sendSocketData(processOutput());

            while (true) {
                try {
                    bytesRead = input.read(socRcvBuffer, 0, socRcvBufferSize);
                    Log.i(TAG, "Received data from socket, bytesRead = "
                            + bytesRead);
                } catch (IOException e) {
                    Log.e(TAG, "There is an exception when reading socket");
                    e.printStackTrace();
                    break;
                }

                if (bytesRead >= 0) {
                    String inputStr = new String(socRcvBuffer, 0, bytesRead);
                    Log.i(TAG, "Received: " + inputStr);
                    bytesRead = 0;
                    processInput(inputStr);
                } else {
                    processOutputState = NONE;
                    closeReceived = true;
                }

                if (processOutputState != NONE) {
                    sendSocketData(processOutput());
                }

                if (closeReceived) {
                    socketOpen = false;
                    closeReceived = false;
                    if (client != null) {
                        try {
                            client.close();
                            Log.i(TAG, "client socket closed");
                        } catch (IOException e) {
                            Log.e(TAG, "client socket close failed");
                            e.printStackTrace();
                        }
                    }
                    break;
                }
            }

            Log.d(TAG, "communicationHandler Stopped");
        }
    }

    public String processOutput() {
        StringBuilder sendStr = new StringBuilder();
        Log.d(TAG, "processOutputState is ::" + processOutputState);
        switch (processOutputState) {
        case INIT_MENU:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     Throughput_Testing\n");
            sendStr.append("                     Spp_Testing\n");
            sendStr.append("                     GAP\n");
            sendStr.append("                     Close\n");
            sendStr.append("**************************************************************\n");
            break;

        case THROUGHPUT_TESTING_MENU:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     Connect (Ex: Connect bdAddress:AA:BB:CC:DD:EE:FF uuid:aaaa:bbbbbb:cccc)\n");
            sendStr.append("                     Incoming_Connection<space><uuid>:<value> (Ex: Incoming_Connection uuid:aaaa:bbbbbb:cccc\n");
            if(Utils.btAddrUUIDToBTSocketMap.size() != 0){
            sendStr.append("                     Previous_Connections\n");
            }
            sendStr.append("                     Back\n");
            sendStr.append("**************************************************************\n");
            break;

        case BLUETOOTH_SPP_CONNECT_MENU:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     Connect (Ex: Connect bdAddress:AA:BB:CC:DD:EE:FF)\n");
            sendStr.append("                     Incoming_Connection<space><uuid>:<value> (Ex: Incoming_Connection uuid:aaaa:bbbbbb:cccc\n");
            if(Utils.btAddrUUIDToBTSocketMap.size() != 0)
                sendStr.append("                     Previous_Connections\n");
            sendStr.append("                     Back\n");
            sendStr.append("**************************************************************\n");
        break;

        case BLUETOOTH_SPP_TESTING_MENU:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     Send_File file_name\n");
            sendStr.append("                     Receive_File\n");
            sendStr.append("                     Disconnect (Ex: Disconnect)\n");
            sendStr.append("                     Back\n");
            sendStr.append("**************************************************************\n");
        break;

        case BLUETOOTH_SPP_MENU:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     Spp\n");
            sendStr.append("                     Back\n");
            sendStr.append("**************************************************************\n");
        break;

        case CONNECTION_TEST_MENU:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     OFF_ON (Ex: OFF_ON count:10)\n");
            sendStr.append("                     Set_Scan (Ex: Set_Scan mode:0/1/2)\n");
            sendStr.append("                     Discovery (Ex: Discovery)\n");
            sendStr.append("                     isConnected (Ex: isConnected bdAddress:AA:BB:CC:DD:EE:FF)\n");
            sendStr.append("                     Back\n");
            sendStr.append("**************************************************************\n");
            break;

        case MAIN_MENU:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     Throughput\n");
            sendStr.append("                     Back\n");
            sendStr.append("**************************************************************\n");
            break;

        case DEVICE_SELECTION_MENU:
            int srlNum = 0;
            String direction;
            sendStr.append("\n******************** Use S.No to Select Device ********************\n");
            sendStr.append("\nS.No." + "   Device Name"+ "      Device Address"+ "      UUID" +"                                     Connection Direction" +"\n");
            Iterator<Map.Entry<String, BluetoothSocket>> itr = Utils.btAddrUUIDToBTSocketMap.entrySet().iterator();
            while(itr.hasNext())
            {
                Map.Entry<String, BluetoothSocket> entry = itr.next();
                BluetoothSocket socket = entry.getValue();
                String address = socket.getRemoteDevice().getAddress();
                String name = socket.getRemoteDevice().getName();
                srlNum++;
                HashMap<String, BluetoothSocket> connectionHandle = new HashMap<String, BluetoothSocket>();
                connectionHandle.put(entry.getKey(),entry.getValue());
                Utils.srlNumToConnectionMap.put(srlNum,connectionHandle);
                String[] tmp = entry.getKey().split(" ", 3);
                if(tmp[2] != null && tmp[2].equals("CLIENT")){
                    direction = "Client(DUT to Remote)";
                }else{
                    direction = "Server(Remote to DUT)";
                }
                sendStr.append(srlNum+"       "+Utils.ellipsize(name,11)+"      "+address+"   "+tmp[1]+ "     "+direction+ "\n");
            }
            sendStr.append("Back\n");
            sendStr.append("*********************************************************************\n");
            break;

        case TX_RX_MENU:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     Tx(Select chunkSize and pattern 1(Default) 2(Binary) or 3(PRBS9)) (Ex: Tx chunkSize:1000 pattern:1)\n");
            sendStr.append("                     Rx (Ex: Rx )\n");
            sendStr.append("                     Disconnect (Ex: Disconnect)\n");
            sendStr.append("                     Back\n");
            sendStr.append("**************************************************************\n");
            break;

        case INVALID_INPUT:
            sendStr.append("\nInvalid Input\n");
            break;

        case SOC_CLOSE_ACK:
            sendStr.append("A_Close");
            break;

        default:
            sendStr.append("\nError\n");
            break;
        }

        return sendStr.toString();
    }

    public void processInput(String inputString) {
        String[] tmp;
        Log.d(TAG, "mainMenuState is ::" + mainMenuState);
        switch (mainMenuState) {

        case INIT_MENU:
            try {
                if (inputString.equals("Throughput_Testing")) {
                    Utils.isSppFileTransferOngoing = false;
                    Log.d(TAG, "Throughput Menu");
                    mainMenuState = THROUGHPUT_TESTING_MENU;
                    processOutputState = THROUGHPUT_TESTING_MENU;
                    Utils.isThroughputStateMachineUnderProcessing = true;
                } else if(inputString.equals("Spp_Testing")){
                    mainMenuState = BLUETOOTH_SPP_CONNECT_MENU;
                    processOutputState = BLUETOOTH_SPP_CONNECT_MENU;
                    Utils.isSppConnection = true;
                } else if (inputString.equals("Close")) {
                    closeReceived = true;
                    mainMenuState = INIT_MENU;
                    processOutputState = SOC_CLOSE_ACK;
                    Utils.isThroughputStateMachineUnderProcessing = false;
                    Utils.isSppFileTransferOngoing = false;
                } else if (inputString.equals("GAP")) {
                    Log.d(TAG, "Gap Menu");
                    Utils.isSppFileTransferOngoing = false;
                    Utils.mAppControlService.startStateMachineForGAP();
                    mainMenuState = CONNECTION_TEST_MENU;
                    processOutputState = CONNECTION_TEST_MENU;
                    Utils.isThroughputStateMachineUnderProcessing = false;
                } else {
                    Log.d(TAG, "Invalid");
                    processOutputState = INVALID_INPUT;
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainMenuState = INIT_MENU;
                processOutputState = INVALID_INPUT;
                Utils.isThroughputStateMachineUnderProcessing = false;
            }
            break;

        case THROUGHPUT_TESTING_MENU:
            tmp = inputString.split(" ", 3);
            if(tmp.length == 3)
            {
                if(tmp[0].equals("Connect"))
                {
                    Connect connectParam = parser.connectParse(tmp[1], tmp[2]);
                    boolean isAlreadyConnected = false;

                    if(connectParam.bdAddress != null && connectParam.uuid != null)
                    {
                        processOutputState = NONE;

                        if(Utils.mAppControlService != null)
                        {
                            String key = connectParam.bdAddress.toUpperCase()+" "+connectParam.uuid.toUpperCase()+ " " +"CLIENT";
                            if(Utils.btAddrUUIDToBTSocketMap.containsKey(key)){
                                isAlreadyConnected = true;
                            }
                            if(!isAlreadyConnected)
                            {
                                Log.d(TAG, "BT Address and UUID Combo is not connected, start Connection");
                                Utils.mAppControlService.initializeOutgoingConnection(connectParam);
                            }else{
                                Log.d(TAG, "BT Address and UUID Combo is already connected, ignore Connection");
                                sendSocketData("BT Address and UUID Combo is already connected\n");
                            }
                        }
                    }
                    else
                    {
                        processOutputState = INVALID_INPUT;
                        mainMenuState = INIT_MENU;
                    }
                }
                else
                {
                    processOutputState = INVALID_INPUT;
                    mainMenuState = INIT_MENU;
                }
            }
            else if(tmp.length == 2)
            {
                if(tmp[0].equals("Incoming_Connection"))
                {
                    IncomingConnection incomingConnection = parser.incomingConnectionParse(tmp[1]);
                    if(incomingConnection != null)
                    {
                        processOutputState = NONE;
                        if(Utils.mAppControlService != null)
                        {
                            Utils.mAppControlService.startReadyToAcceptConnection(incomingConnection);
                        }
                    }
                }
                else
                {
                    processOutputState = INVALID_INPUT;
                    mainMenuState = INIT_MENU;
                }
            }
            else if(inputString.equals("Back"))
            {
                mainMenuState = INIT_MENU;
                processOutputState = INIT_MENU;
                Utils.isSppConnection = false;
                Utils.isThroughputStateMachineUnderProcessing = false;
            }else if(inputString.equals("Previous_Connections")){
                mainMenuState = DEVICE_SELECTION_MENU;
                processOutputState = DEVICE_SELECTION_MENU;
            }
            else
            {
                processOutputState = INVALID_INPUT;
            }
            break;
        case BLUETOOTH_SPP_CONNECT_MENU:
        tmp = inputString.split(" ", 2);
        if (tmp.length == 2) {
            if (tmp[0].equals("Connect"))
            {
                Connect connectParam = parser.connectParse(tmp[1],"uuid:00001101-0000-1000-8000-00805f9b34fb");
                if(connectParam.bdAddress != null)
                    {
                        processOutputState = NONE;
                        if(Utils.mAppControlService != null)
                        {
                            Log.d(TAG, "BT Address and UUID Combo is not connected, start Connection");
                            Utils.mAppControlService.initializeOutgoingConnection(connectParam);
                        }
                    }
                    else
                    {
                        processOutputState = INVALID_INPUT;
                        mainMenuState = INIT_MENU;
                    }
            }
            else if(tmp[0].equals("Incoming_Connection"))
            {
                IncomingConnection incomingConnection = parser.incomingConnectionParse(tmp[1]);
                if(incomingConnection != null)
                {
                    processOutputState = NONE;
                    if(Utils.mAppControlService != null)
                    {
                        Utils.mAppControlService.startReadyToAcceptConnection(incomingConnection);
                    }
                }
            }
            else if(inputString.equals("Back"))
            {
                mainMenuState = INIT_MENU;
                processOutputState = INIT_MENU;
            }
            else
            {
                processOutputState = INVALID_INPUT;
                mainMenuState = INIT_MENU;
            }
            }else if(inputString.equals("Previous_Connections")){
                mainMenuState = DEVICE_SELECTION_MENU;
                processOutputState = DEVICE_SELECTION_MENU;
            }
        else if (inputString.equals("Back")) {
            Utils.isSppConnection = false;
            Utils.isSppFileTransferOngoing=false;
            mainMenuState = INIT_MENU;
            processOutputState = INIT_MENU;
            Utils.isThroughputStateMachineUnderProcessing = false;
        } else {
            processOutputState = INVALID_INPUT;
        }
        break;

        case BLUETOOTH_SPP_MENU:
            if(inputString.equals("Spp")) {
               mainMenuState = DEVICE_SELECTION_MENU;
               processOutputState = DEVICE_SELECTION_MENU;
            }
            else if(inputString.equals("Back"))
            {
                processOutputState = BLUETOOTH_SPP_CONNECT_MENU;
                mainMenuState = BLUETOOTH_SPP_CONNECT_MENU;
            }
            else
            {
                processOutputState = INVALID_INPUT;
            }
        break;

        case BLUETOOTH_SPP_TESTING_MENU:
            tmp = inputString.split(" ", 2);
            if (tmp.length == 2) {
                if (tmp[0].equals("Send_File")) {
                    Log.d(TAG, "Inside Send_File :: tmp[1] is :: "+tmp[1]);
                    Utils.TxForSpp txParamForSpp = new Utils.TxForSpp();
                    if(tmp[1] != null){
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_START_SEND_FILE;
                        if(Utils.currentConnection.size() == 1){
                            Log.d(TAG,"Utils.currentConnection.size is 1");
                            Iterator<Map.Entry<String, BluetoothSocket>> itr = Utils.currentConnection.entrySet().iterator();
                            while(itr.hasNext())
                            {
                                Map.Entry<String, BluetoothSocket> entry = itr.next();
                                Log.d(TAG, "Calling sendMessage");
                                txParamForSpp.bt_addr_uuid = entry.getKey();
                                txParamForSpp.socket = entry.getValue();
                                txParamForSpp.fileName = tmp[1];
                                message.obj = txParamForSpp;
                                Utils.btAddrUUIDToStateMachineMap.get(entry.getKey()).sendMessage(message);
                            }
                        }
                        else{
                            Log.d(TAG,"Disconnect Utils.currentConnection.size is not 1. Something went wrong");
                            sendSocketData("Something went wrong. Please try again\n");
                        }
                    }else{
                        processOutputState = INVALID_INPUT;
                    }
                }

            }else if (tmp.length == 1){
                if(tmp[0].equals("Receive_File")){
                    Log.d(TAG, "Inside Receive_File");
                    processOutputState = NONE;
                    Rx rxFileParam = new Rx();
                    Message message = Message.obtain();
                    message.what = Utils.StateMachineMessageConstants.STATE_START_RECEIVE_FILE;
                    if(Utils.currentConnection.size() == 1){
                            Log.d(TAG,"Utils.currentConnection.size is 1");
                            Iterator<Map.Entry<String, BluetoothSocket>> itr = Utils.currentConnection.entrySet().iterator();
                            while(itr.hasNext())
                            {
                                Map.Entry<String, BluetoothSocket> entry = itr.next();
                                rxFileParam.bt_addr_uuid = entry.getKey();
                                rxFileParam.socket = entry.getValue();
                                message.obj = rxFileParam;
                                Utils.btAddrUUIDToStateMachineMap.get(entry.getKey()).sendMessage(message);
                            }
                    }else{
                        Log.d(TAG,"Disconnect Utils.currentConnection.size is not 1. Something went wrong");
                        sendSocketData("Something went wrong. Please try again\n");
                    }

                }else if(tmp[0].equals("Disconnect")){
                    Utils.isSppFileTransferOngoing=false;
                    processOutputState = NONE;
                        if(Utils.currentConnection.size() == 1){
                            Log.d(TAG,"Utils.currentConnection.size is 1");
                            Iterator<Map.Entry<String, BluetoothSocket>> itr = Utils.currentConnection.entrySet().iterator();
                            while(itr.hasNext())
                            {
                                Map.Entry<String, BluetoothSocket> entry = itr.next();
                                Utils.mAppControlService.closeConnection(entry.getKey());
                            }
                        }else{
                            Log.d(TAG,"Disconnect Utils.currentConnection.size is not 1. Something went wrong");
                            sendSocketData("Something went wrong. Please try again\n");
                        }
                }else if (tmp[0].equals("Back")) {
                    mainMenuState = DEVICE_SELECTION_MENU;
                    processOutputState = DEVICE_SELECTION_MENU;
                    Utils.isThroughputStateMachineUnderProcessing = false;
                } else {
                    processOutputState = INVALID_INPUT;
                }
            } else {
                processOutputState = INVALID_INPUT;
            }
            break;

        case MAIN_MENU:
            if(inputString.equals("Throughput")) {
               mainMenuState = DEVICE_SELECTION_MENU;
               processOutputState = DEVICE_SELECTION_MENU;
            }
            else if(inputString.equals("Back"))
            {
                Message message = Message.obtain();
                message.what = Utils.StateMachineMessageConstants.STATE_READY_TO_CONNECT;
                processOutputState = THROUGHPUT_TESTING_MENU;
                mainMenuState = THROUGHPUT_TESTING_MENU;
            }
            else
            {
                processOutputState = INVALID_INPUT;
            }
            break;

        case CONNECTION_TEST_MENU:
            Message gapTestMessage = Message.obtain();
            gapTestMessage.what = Utils.StateMachineMessageConstants.STATE_START_GAP_TEST_CASES;
            Utils.appControlStateMachineforGAP.sendMessage(gapTestMessage);

            tmp = inputString.split(" ", 2);
            if (tmp.length == 2) {
                if (tmp[0].equalsIgnoreCase("OFF_ON")) {
                    ConnectionTest connectionTestParam = parser
                            .connectionTestParse(tmp[1]);
                    if (connectionTestParam != null) {
                        processOutputState = NONE;
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_OFF_ON;
                        message.obj = connectionTestParam;
                        Utils.appControlStateMachineforGAP.sendMessage(message);
                    } else {
                        processOutputState = INVALID_INPUT;
                    }
                }else if(tmp[0].equalsIgnoreCase("Set_Scan")){
                    SetScanMode scanTestParam = parser.setScanModeParse(tmp[1]);
                    if (scanTestParam != null) {
                        processOutputState = NONE;
                        Log.d(TAG,"Set Scan Mode :: "+scanTestParam.scanMode);
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_SCAN_MODE;
                        message.obj = scanTestParam;
                        Utils.appControlStateMachineforGAP.sendMessage(message);
                    } else {
                        processOutputState = INVALID_INPUT;
                    }
                } else if(tmp[0].equalsIgnoreCase("isConnected")){
                    Utils.IsConnected isConnectedTestParam = parser.isConnectedParse(tmp[1]);
                    if (isConnectedTestParam != null) {
                        processOutputState = NONE;
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_IS_CONNECTED;
                        message.obj = isConnectedTestParam;
                        Utils.appControlStateMachineforGAP.sendMessage(message);
                    } else {
                        processOutputState = INVALID_INPUT;
                    }
                } else {
                    processOutputState = INVALID_INPUT;
                }
            }else if (tmp.length == 1) {
                if (tmp[0].equals("Back")) {
                    mainMenuState = INIT_MENU;
                    processOutputState = INIT_MENU;
                    Utils.isThroughputStateMachineUnderProcessing = false;
                    gapTestMessage = Message.obtain();
                    gapTestMessage.what = Utils.StateMachineMessageConstants.STATE_END_GAP_TEST_CASES;
                    Utils.appControlStateMachineforGAP.sendMessage(gapTestMessage);
                } else if (tmp[0].equals("Discovery")) {
                    processOutputState = NONE;
                    Message message = Message.obtain();
                    message.what = Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_START_DISCOVERY;
                    Utils.appControlStateMachineforGAP.sendMessage(message);
                } else {
                    processOutputState = INVALID_INPUT;
                }
            } else {
                processOutputState = INVALID_INPUT;
            }
            break;

            case DEVICE_SELECTION_MENU:
                try{
                    int srlNum = Integer.parseInt(inputString);
                    if(Utils.srlNumToConnectionMap.get(srlNum) != null){
                        Utils.currentConnection = Utils.srlNumToConnectionMap.get(srlNum);
                        if(Utils.isSppConnection == true){
                            mainMenuState = BLUETOOTH_SPP_TESTING_MENU;
                            processOutputState = BLUETOOTH_SPP_TESTING_MENU;
                        }
                        else {
                        mainMenuState = TX_RX_MENU;
                        processOutputState = TX_RX_MENU;
                        }
                    }else{
                        processOutputState = INVALID_INPUT;
                    }
                }catch(NumberFormatException e){
                    processOutputState = INVALID_INPUT;
                    if(inputString.equals("Back"))
                    {
                        if(Utils.isSppConnection == true){
                            mainMenuState = BLUETOOTH_SPP_MENU;
                            processOutputState = BLUETOOTH_SPP_MENU;
                        }
                        else{
                            mainMenuState = MAIN_MENU;
                            processOutputState = MAIN_MENU;
                        }
                    }else
                    {
                        processOutputState = INVALID_INPUT;
                    }
                }
            break;

        case TX_RX_MENU:
            tmp = inputString.split(" ");
            if (tmp.length == 2 || tmp.length==3) {
                if (tmp[0].equals("Tx")) {
                    Tx txParam = new Tx();
                    if(tmp.length == 2)
                         txParam = parser.txParse(tmp[1], "pattern:1");
                    else if(tmp.length == 3)
                         txParam = parser.txParse(tmp[1], tmp[2]);
                    if (txParam != null) {
                        processOutputState = NONE;
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_START_DATA_TX;
                        Log.d(TAG,"Utils.currentConnection map is :: "+Utils.currentConnection);
                        if(Utils.currentConnection.size() == 1){
                            Log.d(TAG,"Tx Utils.currentConnection.size is 1");
                            Iterator<Map.Entry<String, BluetoothSocket>> itr = Utils.currentConnection.entrySet().iterator();
                            while(itr.hasNext())
                            {
                                Map.Entry<String, BluetoothSocket> entry = itr.next();
                                txParam.bt_addr_uuid = entry.getKey();
                                txParam.socket = entry.getValue();
                                message.obj = txParam;
                                Utils.btAddrUUIDToStateMachineMap.get(entry.getKey()).sendMessage(message);
                            }
                        }else{
                            Log.d(TAG,"Utils.currentConnection.size is not 1. Something went wrong");
                            sendSocketData("Something went wrong. Please try again\n");
                        }
                    } else {
                        processOutputState = INVALID_INPUT;
                    }
                } else {
                    processOutputState = INVALID_INPUT;
                }
            } else if (tmp.length == 1) {
                if (tmp[0].equals("Rx")) {
                    processOutputState = NONE;
                    Rx rxParam = new Rx();
                    Message message = Message.obtain();
                    message.what = Utils.StateMachineMessageConstants.STATE_START_DATA_RX;
                    if(Utils.currentConnection.size() == 1){
                        Log.d(TAG,"Rx Utils.currentConnection.size is 1");
                        Iterator<Map.Entry<String, BluetoothSocket>> itr = Utils.currentConnection.entrySet().iterator();
                        while(itr.hasNext())
                        {
                            Map.Entry<String, BluetoothSocket> entry = itr.next();
                            rxParam.bt_addr_uuid = entry.getKey();
                            rxParam.socket = entry.getValue();
                            message.obj = rxParam;
                            Utils.btAddrUUIDToStateMachineMap.get(entry.getKey()).sendMessage(message);
                        }
                    }else{
                        Log.d(TAG,"Utils.currentConnection.size is not 1. Something went wrong");
                        sendSocketData("Something went wrong. Please try again\n");
                    }
                }else if(tmp[0].equals("Disconnect")){
                    processOutputState = NONE;
                    if(Utils.currentConnection.size() == 1){
                        Log.d(TAG,"Utils.currentConnection.size is 1");
                        Iterator<Map.Entry<String, BluetoothSocket>> itr = Utils.currentConnection.entrySet().iterator();
                        while(itr.hasNext())
                        {
                            Map.Entry<String, BluetoothSocket> entry = itr.next();
                            Utils.mAppControlService.closeConnection(entry.getKey());
                        }
                    }else{
                        Log.d(TAG,"Disconnect Utils.currentConnection.size is not 1. Something went wrong");
                        sendSocketData("Something went wrong. Please try again\n");
                    }
                } else if (tmp[0].equals("Back")) {
                    mainMenuState = DEVICE_SELECTION_MENU;
                    processOutputState = DEVICE_SELECTION_MENU;
                } else {
                    processOutputState = INVALID_INPUT;
                }
            } else {
                processOutputState = INVALID_INPUT;
            }
            break;
        }
    }

    public static void sendSocketData(String data) {
        if (socketOpen) {
            try {
                mutex.acquire();
                try {
                    try {
                        if (data.getBytes().length <= socSendBufferSize) {
                            data = data + '\n';
                            Log.i(TAG, "bytesToSend: " + data.getBytes().length);
                            output.write(data.getBytes(), 0,
                                    data.getBytes().length);
                            output.flush();
                            Log.i(TAG, "Sent: " + data);
                        } else {
                            Log.i(TAG, "Data length is more than "
                                    + socSendBufferSize + " , ignore packet");
                        }
                    } catch (IOException e) {
                        Log.e(TAG,
                                "There is an exception when writing to socket");
                        e.printStackTrace();
                    }
                } finally {
                    mutex.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "There is an exception when acquiring mutex");
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Socket is not open, ignore packet");
        }
    }

    public static void updateSocketClient() {
        sendSocketData(INSTANCE.processOutput());
    }

    public static void cleanUp() {
        sendSocketData("Application is Closed... Please restart");
        if (INSTANCE.client != null) {
            try {
                INSTANCE.client.close();
                Log.i(TAG, "client socket closed");
            } catch (IOException e) {
                Log.e(TAG, "client socket close failed");
                e.printStackTrace();
            }
            if (INSTANCE.server != null) {
                try {
                    INSTANCE.server.close();
                    Log.i(TAG, "server closed");
                } catch (IOException e) {
                    Log.e(TAG, "server close failed");
                    e.printStackTrace();
                }
            }
        }
        INSTANCE.commHandler.interrupt();
    }

}
