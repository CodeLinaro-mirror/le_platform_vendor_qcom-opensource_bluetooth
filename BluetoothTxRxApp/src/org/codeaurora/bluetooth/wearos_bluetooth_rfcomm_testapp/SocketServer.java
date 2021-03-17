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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Message;
import android.util.Log;

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

    communicationHandler commHandler;

    static final int CONNECT_INIT = 0;
    static final int MAIN_MENU = 1;
    static final int THROUGHPUT_MENU = 3;
    static final int INVALID_INPUT = 6;
    static final int SOC_CLOSE_ACK = 7;
    static final int NONE = 8;
    static final int CONNECTION_TEST_MENU = 999;

    static int mainMenuState = CONNECT_INIT;
    static int processOutputState = CONNECT_INIT;

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
                    client = server.accept();
                } catch (IOException e) {
                    Log.e(TAG, "localSocketServer accept() failed !!!");
                    e.printStackTrace();
                }

                socketOpen = true;
                Log.d(TAG, "localSocket accepted");

                try {
                    input = client.getInputStream();
                    Log.d(TAG, "getInputStream");
                } catch (IOException e) {
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
            } else {
                Log.d(TAG, "The LocalServerSocket is NULL");
            }
        }
    }

    private class communicationHandler extends Thread {

        public void run() {
            Log.d(TAG, "communicationHandler run()");

            // Display main menu and start receiving socket data
            mainMenuState = CONNECT_INIT;
            processOutputState = CONNECT_INIT;
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

                    if (server != null) {
                        try {
                            server.close();
                            Log.i(TAG, "server closed");
                        } catch (IOException e) {
                            Log.e(TAG, "server close failed");
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
        case CONNECT_INIT:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     Connect (Ex: Connect bdAddress:AA:BB:CC:DD:EE:FF)\n");
            sendStr.append("                     GAP\n");
            sendStr.append("                     Close\n");
            sendStr.append("**************************************************************\n");
            break;

        case CONNECTION_TEST_MENU:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     OFF_ON (Ex: OFF_ON count:10)\n");
            sendStr.append("                     Set_Scan (Ex: Set_Scan mode:0/1/2)\n");
            sendStr.append("                     Discovery (Ex: Discovery)\n");
            sendStr.append("                     Back\n");
            sendStr.append("**************************************************************\n");
            break;

        case MAIN_MENU:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     Throughput\n");
            sendStr.append("                     Back\n");
            sendStr.append("**************************************************************\n");
            break;

        case THROUGHPUT_MENU:
            sendStr.append("\n******************** Bt RFCOMM Test App ********************\n");
            sendStr.append("                     Tx (Ex: Tx chunkSize:1000)\n");
            sendStr.append("                     Rx (Ex: Rx )\n");
            sendStr.append("                     Wakeable (Ex: Wakeable timer:1000)\n");
            sendStr.append("                     Actionable (Ex: Actionable timer:1000)\n");
            sendStr.append("                     Cacheable (Ex: Cacheable timer:1000)\n");
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

        case CONNECT_INIT:
            try {
                tmp = inputString.split(" ", 2);
                if (tmp.length == 2) {
                    if (tmp[0].equals("Connect")) {
                        Connect connectParam = parser.connectParse(tmp[1]);
                        if (connectParam != null) {
                            Utils.bdAddressFromConfig = connectParam.BDaddress;
                            processOutputState = NONE;
                            Message message = Message.obtain();
                            message.what = Utils.StateMachineMessageConstants.STATE_READY_TO_CONNECT;
                            Utils.appControlStateMachine.sendMessage(message);
                        } else {
                            processOutputState = INVALID_INPUT;
                            mainMenuState = CONNECT_INIT;
                        }
                    }
                }else if (inputString.equals("Close")) {
                    closeReceived = true;
                    mainMenuState = CONNECT_INIT;
                    processOutputState = SOC_CLOSE_ACK;
                } else if(inputString.equals("GAP")){
                    Log.d(TAG,"Gap Menu");
                    mainMenuState = CONNECTION_TEST_MENU;
                    processOutputState = CONNECTION_TEST_MENU;
                }else {
                    Log.d(TAG,"Invalid");
                    processOutputState = INVALID_INPUT;
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainMenuState = CONNECT_INIT;
                processOutputState = INVALID_INPUT;
            }

            break;

        case MAIN_MENU:
            if (inputString.equals("Throughput")) {
                mainMenuState = THROUGHPUT_MENU;
                processOutputState = THROUGHPUT_MENU;
            } else if (inputString.equals("Back")) {
                Message message = Message.obtain();
                message.what = Utils.StateMachineMessageConstants.STATE_DISCONNECTED;
                Utils.appControlStateMachine.sendMessage(message);
                mainMenuState = CONNECT_INIT;
                processOutputState = CONNECT_INIT;
            } else {
                processOutputState = INVALID_INPUT;
            }
            break;

        case CONNECTION_TEST_MENU:
            Message gapTestMessage = Message.obtain();
            gapTestMessage.what = Utils.StateMachineMessageConstants.STATE_START_GAP_TEST_CASES;
            Utils.appControlStateMachine.sendMessage(gapTestMessage);

            tmp = inputString.split(" ", 2);
            if (tmp.length == 2) {
                if (tmp[0].equalsIgnoreCase("OFF_ON")) {
                    ConnectionTest connectionTestParam = parser.connectionTestParse(tmp[1]);
                    if (connectionTestParam != null) {
                        processOutputState = NONE;
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_OFF_ON;
                        message.obj = connectionTestParam;
                        Utils.appControlStateMachine.sendMessage(message);
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
                        Utils.appControlStateMachine.sendMessage(message);
                    } else {
                        processOutputState = INVALID_INPUT;
                    }
                }
            }else if (tmp.length == 1) {
                if (tmp[0].equals("Back")) {
                    mainMenuState = CONNECT_INIT;
                    processOutputState = CONNECT_INIT;
                    gapTestMessage = Message.obtain();
                    gapTestMessage.what = Utils.StateMachineMessageConstants.STATE_END_GAP_TEST_CASES;
                    Utils.appControlStateMachine.sendMessage(gapTestMessage);
                }else if(tmp[0].equals("Discovery")){
                    processOutputState = NONE;
                    Message message = Message.obtain();
                    message.what = Utils.StateMachineMessageConstants.STATE_GAP_TEST_CASE_START_DISCOVERY;
                    Utils.appControlStateMachine.sendMessage(message);
                }else {
                    processOutputState = INVALID_INPUT;
                }
            }
            break;

        case THROUGHPUT_MENU:
            tmp = inputString.split(" ", 2);
            if (tmp.length == 2) {
                if (tmp[0].equals("Tx")) {
                    Tx txParam = parser.txParse(tmp[1]);
                    if (txParam != null) {
                        processOutputState = NONE;
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_START_DATA_TX;
                        message.obj = txParam;
                        Utils.appControlStateMachine.sendMessage(message);
                    } else {
                        processOutputState = INVALID_INPUT;
                    }
                } else if (tmp[0].equals("Wakeable")) {
                    Wakeable wakeableParam = parser.wakeableParse(tmp[1]);
                    if (wakeableParam != null) {
                        processOutputState = NONE;
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_START_DATA_TX_WAKEABLE;
                        message.obj = wakeableParam;
                        Utils.appControlStateMachine.sendMessage(message);
                    } else {
                        processOutputState = INVALID_INPUT;
                    }
                } else if (tmp[0].equals("Actionable")) {
                    Actionable actionableParam = parser.actionableParse(tmp[1]);
                    if (actionableParam != null) {
                        processOutputState = NONE;
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_START_DATA_TX_ACTIONABLE;
                        message.obj = actionableParam;
                        Utils.appControlStateMachine.sendMessage(message);
                    } else {
                        processOutputState = INVALID_INPUT;
                    }
                } else if (tmp[0].equals("Cacheable")) {
                    Cacheable cacheableParam = parser.cacheableParse(tmp[1]);
                    if (cacheableParam != null) {
                        processOutputState = NONE;
                        Message message = Message.obtain();
                        message.what = Utils.StateMachineMessageConstants.STATE_START_DATA_TX_CACHEABLE;
                        message.obj = cacheableParam;
                        Utils.appControlStateMachine.sendMessage(message);
                    } else {
                        processOutputState = INVALID_INPUT;
                    }
                } else {
                    processOutputState = INVALID_INPUT;
                }
            } else if (tmp.length == 1) {
                if (tmp[0].equals("Rx")) {
                    processOutputState = NONE;
                    Message message = Message.obtain();
                    message.what = Utils.StateMachineMessageConstants.STATE_START_DATA_RX;
                    Utils.appControlStateMachine.sendMessage(message);
                } else if (tmp[0].equals("Back")) {
                    mainMenuState = MAIN_MENU;
                    processOutputState = MAIN_MENU;
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

    public static void cleanUp(){
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
