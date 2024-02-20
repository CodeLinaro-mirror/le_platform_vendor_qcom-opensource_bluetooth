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
    private static SocketServer INSTANCE = new SocketServer();
    private static Semaphore mutex = new Semaphore(1);

    private static final String TAG = "SocketServer";
    final String SOCKET_ADDRESS = "BtTestAppSocket";
    static int socSendBufferSize = 4096;
    int socRcvBufferSize = 4096;
    byte[] socRcvBuffer;
    int bytesRead;
    private static boolean socketOpen = false;
    private static boolean closeReceived = false;
    InputStream input;
    private static OutputStream output;
    LocalServerSocket server;
    LocalSocket client;
    localServerSocket localServer;
    communicationHandler commHandler;
    static InputParse parse;
    StringBuilder sendStr = new StringBuilder();

    static final int MAIN_MENU = 1;
    static final int THROUGHPUT_MENU = 2;
    static final int ADV_MENU = 3;
    static final int SCAN_MENU = 4;
    static final int GATT_CLIENT_MENU = 5;
    static final int INVALID_INPUT = 6;
    static final int SOC_CLOSE_ACK = 7;
    static final int NONE = 8;
    static final int GATT_SERVER_MENU = 9;

    static int mainMenuState = MAIN_MENU;
    static int processOutputState = MAIN_MENU;

    private SocketServer() {
        Log.d(TAG, "SocketServer()");
        socRcvBuffer = new byte[socRcvBufferSize];

        parse = new InputParse();
        localServer = new localServerSocket();
        localServer.start();
    }

    public static SocketServer getInstance() {
        return (INSTANCE);
    }

    private class localServerSocket extends Thread {
        public localServerSocket() {
            Log.d(TAG, "localServerSocket()");
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
            mainMenuState = MAIN_MENU;
            processOutputState = MAIN_MENU;
            sendSocketData(processOutput());

            while (true) {
                try {
                    bytesRead = input.read(socRcvBuffer, 0, socRcvBufferSize);
                    Log.i(TAG, "Received data from socket, bytesRead = " + bytesRead);
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

        private String processOutput() {
            sendStr.setLength(0);

            switch (processOutputState) {
                case MAIN_MENU:
                    sendStr.append("\n******************** Bt Test App ********************\n");
                    sendStr.append("                     Advertiser\n");
                    sendStr.append("                     Scanner\n");
                    sendStr.append("                     Throughput\n");
                    sendStr.append("                     GattClient\n");
                    sendStr.append("                     HoldWakeLock\n");
                    sendStr.append("                     ReleaseWakeLock\n");
                    sendStr.append("                     GattServer\n");
                    sendStr.append("                     Close\n");
                    sendStr.append("*****************************************************\n");
                    break;

                case ADV_MENU:
                    sendStr.append("\n******************** Bt Test App ********************\n");
                    sendStr.append("                     AdvStart      (Ex: AdvStart TxPower:1;Legacy:true;Periodic:false;PerAdvInterval:200;Connectable:true;\n");
                    sendStr.append("                                                 Scannable:true;Anonymous:false;IncludePower:true;PrimaryPhy:1;SecondaryPhy:1;\n");
                    sendStr.append("                                                 MaxExtAdvEvents:0;Interval:160;TimeOutLegacy:10000;AdvertiseMode:0;\n");
                    sendStr.append("                                                 ServiceUuid:0000FF01-0000-1000-8000-00805F9B34FB;ManufacturerId:32;ManufacturerData:1,1,1;\n");
                    sendStr.append("                                                 ServiceDataUuid:0000FF01-0000-1000-8000-00805F9B34FB;ServiceData:abcdabcd(only for ext adv)\n");
                    sendStr.append("                     AdvStop       (Ex: AdvStop 1)\n");
                    sendStr.append("                     Back\n");
                    sendStr.append("*****************************************************\n");
                    break;

                case SCAN_MENU:
                    sendStr.append("\n******************** Bt Test App ********************\n");
                    sendStr.append("                     ScanStart      (Ex: ScanStart DeviceName:Minato;DeviceAddress:73:B5:C0:E6:62:A4;ServiceUuid:0000180f-0000-1000-8000-00805f9b34fb;\n");
                    sendStr.append("                                                   SvcMaskUuid:ffffffff-ffff-ffff-ffff-ffffffffffff;ManufacturerId:158;ManufacturerData:1,1,1;ManuMaskData:f,f,f;\n");
                    sendStr.append("                                                   ServiceDataUuid:0000180f-0000-1000-8000-00805f9b34fb;ServiceData:12;SvcDataMask:ff;ScanMode:1;CallbackType:1;\n");
                    sendStr.append("                                                   ResultType:0;NumOfAdvMatches:3;MatchMode:1;ReportDelay:0;Legacy:false)\n");
                    sendStr.append("                     ScanStop\n");
                    sendStr.append("                     Back\n");
                    sendStr.append("*****************************************************\n");
                    break;

                case THROUGHPUT_MENU:
                    sendStr.append("\n******************** Throughput Menu ********************\n");
                    sendStr.append("                     Connect        (Ex: Connect DeviceName:Minato;DeviceAddress:73:B5:C0:E6:62:A4;ServiceUuid:0000180f-0000-1000-8000-00805f9b34fb;\n");
                    sendStr.append("                                                 SvcMaskUuid:ffffffff-ffff-ffff-ffff-ffffffffffff;ManufacturerId:158;ManufacturerData:1,1,1;ManuMaskData:f,f,f;\n");
                    sendStr.append("                                                 ServiceDataUuid:0000180f-0000-1000-8000-00805f9b34fb;ServiceData:12;SvcDataMask:ff;ScanMode:1;CallbackType:1;\n");
                    sendStr.append("                                                 ResultType:0;NumOfAdvMatches:3;MatchMode:1;ReportDelay:0;Legacy:false)\n");
                    sendStr.append("                     ConnUpdate     (Ex: ConnUpdate ConnIntervalMin:20;ConnIntervalMax:20;ConnSlaveLatency:0;ConnSupTO:180)\n");
                    sendStr.append("                     ReadPhy\n");
                    sendStr.append("                     SetPhy         (Ex: SetPhy Tx_Phy:2;Rx_Phy:2;Phy_Opt:00)\n");
                    sendStr.append("                     Pair\n");
                    sendStr.append("                     UnPair\n");
                    sendStr.append("                     Tx             (Ex: Tx Mtu_Size:512;Packet_Size:244;Num_Packets:50;\n");
                    sendStr.append("                                            TxService:0000FF01-0000-1000-8000-00805F9B34FB;TxChar:0000FF04-0000-1000-8000-00805F9B34FB)\n");
                    sendStr.append("                     Rx             (Ex: Rx RxService:0000FF01-0000-1000-8000-00805F9B34FB;RxChar:0000FF03-0000-1000-8000-00805F9B34FB;NotificationsTimeInSec:2;NotificationsTimeInMin:5;MtuSize:512)\n");
                    sendStr.append("                     Latency        (Ex: Latency LatencyService:0000FF01-0000-1000-8000-00805F9B34FB;LatencyChar:0000FF02-0000-1000-8000-00805F9B34FB)\n");
                    sendStr.append("                     Disconnect\n");
                    sendStr.append("                     Back\n");
                    sendStr.append("*********************************************************\n");
                    break;

                case GATT_CLIENT_MENU:
                    sendStr.append("\n******************** Gatt Client Menu ********************\n");
                    sendStr.append("                     Connect        (Ex:    Connect DeviceName:Minato;DeviceAddress:73:B5:C0:E6:62:A4;ServiceUuid:0000180f-0000-1000-8000-00805f9b34fb;\n");
                    sendStr.append("                                                    SvcMaskUuid:ffffffff-ffff-ffff-ffff-ffffffffffff;ManufacturerId:158;ManufacturerData:1,1,1;ManuMaskData:f,f,f;\n");
                    sendStr.append("                                                    ServiceDataUuid:0000180f-0000-1000-8000-00805f9b34fb;ServiceData:12;SvcDataMask:ff;ScanMode:1;CallbackType:1;\n");
                    sendStr.append("                                                    ResultType:0;NumOfAdvMatches:3;MatchMode:1;ReportDelay:0;Legacy:false)\n");
                    sendStr.append("                     CancelConnect\n");
                    sendStr.append("                     ConnUpdate                     (Ex: ConnUpdate ConnIntervalMin:20;ConnIntervalMax:20;ConnSlaveLatency:0;ConnSupTO:180)\n");
                    sendStr.append("                     ReadPhy\n");
                    sendStr.append("                     SetPhy                         (Ex: SetPhy Tx_Phy:2;Rx_Phy:2;Phy_Opt:00)\n");
                    sendStr.append("                     ConfigureMTU                   (Ex: ConfigureMTU 512)\n");
                    sendStr.append("                     Pair\n");
                    sendStr.append("                     UnPair\n");
                    sendStr.append("                     DiscoverServices\n");
                    sendStr.append("                     RefreshServices\n");
                    sendStr.append("                     RW_Char                        (Ex: RW_Char Operation:1(1->Write,2->Read);ServiceUuid:0000FF01-0000-1000-8000-00805F9B34FB;CharUuid:0000FF03-0000-1000-8000-00805F9B34FB;Value:10;WriteType:2;FormatType:1(1->string,2->int))\n");
                    sendStr.append("                     RW_Desc                        (Ex: RW_Desc Operation:2(1->Write,2->Read);ServiceUuid:0000FF03-0000-1000-8000-00805F9B34FB;CharUuid:0000FF03-0000-1000-8000-00805F9B34FB;DescUuid:00002902-0000-1000-8000-00805F9B34FB;)\n");
                    sendStr.append("                     RegNotifications               (Ex: RegNotifications ServiceUuid:0000FF03-0000-1000-8000-00805F9B34FB;CharUuid:0000FF03-0000-1000-8000-00805F9B34FB)\n");
                    sendStr.append("                     DeRegNotifications             (Ex: DeRegNotifications ServiceUuid:0000FF03-0000-1000-8000-00805F9B34FB;CharUuid:0000FF03-0000-1000-8000-00805F9B34FB)\n");
                    sendStr.append("                     ReliableWrite                  (Ex: ReliableWrite Operation:1;ServiceUuid:0000FF03-0000-1000-8000-00805F9B34FB;CharUuid:0000FF03-0000-1000-8000-00805F9B34FB;Value:10;WriteType:2)\n");
                    sendStr.append("                     AbortReliableWrite\n");
                    sendStr.append("                     Disconnect\n");
                    sendStr.append("                     Back\n");
                    sendStr.append("**********************************************************\n");
                    break;
                case GATT_SERVER_MENU:
                    sendStr.append("\n******************** Gatt Server Menu ********************\n");
                    sendStr.append("                       AddService                   (Ex: AddService ServiceUuid:0000FF01-0000-1000-8000-00805F9B34FB;CharUuid:00002a06-0000-1000-8000-00805f9b34fb;Properties:0x10;Permissions:0x01;Value:0x12)\n");
                    sendStr.append("                       RemoveService                (Ex: RemoveService ServiceUuid:0000FF01-0000-1000-8000-00805F9B34FB)\n");
                    sendStr.append("                       ClearServices\n");
                    sendStr.append("                       GetServices\n");
                    sendStr.append("                       SetPhy                       (Ex: SetPhy Tx_Phy:2;Rx_Phy:2;Phy_Opt:00)\n");
                    sendStr.append("                       ReadPhy\n");
                    sendStr.append("                       GetConnectedDevices\n");
                    sendStr.append("                       Back\n");
                    sendStr.append("**********************************************************\n");
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

        private void processInput(String inputString) {
            String[] tmp;
            Message msg = null;

            switch (mainMenuState) {
                case MAIN_MENU:
                    if (inputString.equals("Advertiser")) {
                        mainMenuState = ADV_MENU;
                        processOutputState = ADV_MENU;
                    } else if (inputString.equals("Scanner")) {
                        mainMenuState = SCAN_MENU;
                        processOutputState = SCAN_MENU;
                    } else if (inputString.equals("Throughput")) {
                        mainMenuState = THROUGHPUT_MENU;
                        processOutputState = THROUGHPUT_MENU;
                    } else if (inputString.equals("GattClient")) {
                        mainMenuState = GATT_CLIENT_MENU;
                        processOutputState = GATT_CLIENT_MENU;
                    } else if (inputString.equals("HoldWakeLock")) {
                        mainMenuState = MAIN_MENU;
                        processOutputState = NONE;
                        MainActivity.wl.acquire();
                        MainActivity.wl_acquired = true;
                        Log.d(TAG,"Wakelock acquired");
                        sendStr.setLength(0);
                        sendStr.append("Wakelock acquired");
                        SocketServer.sendSocketData(sendStr.toString());
                    } else if (inputString.equals("ReleaseWakeLock")) {
                        mainMenuState = MAIN_MENU;
                        processOutputState = NONE;
                        MainActivity.wl.release();
                        MainActivity.wl_acquired = false;
                        Log.d(TAG,"Wakelock released");
                        sendStr.setLength(0);
                        sendStr.append("Wakelock released");
                        SocketServer.sendSocketData(sendStr.toString());
                    } else if (inputString.equals("GattServer")) {
                        mainMenuState = GATT_SERVER_MENU;
                        processOutputState = GATT_SERVER_MENU;
                    } else if (inputString.equals("Close")) {
                        closeReceived = true;
                        mainMenuState = MAIN_MENU;
                        processOutputState = SOC_CLOSE_ACK;
                    } else {
                        mainMenuState = MAIN_MENU;
                        processOutputState = INVALID_INPUT;
                    }
                    break;

                case ADV_MENU:
                    tmp = inputString.split(" ", 2);
                    if (tmp.length == 2) {
                        if (tmp[0].equals("AdvStart")) {
                            Adv advParam = parse.AdvParse(tmp[1]);
                            if (advParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_MA_START_BLE_ADV, advParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("AdvStop")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(MainActivity.MSG_MA_STOP_BLE_ADV,
                                    Integer.parseInt(tmp[1]));
                            MainActivity.msghandler.sendMessage(msg);
                        } else {
                            processOutputState = INVALID_INPUT;
                        }
                    } else if (tmp.length == 1) {
                        if (tmp[0].equals("Back")) {
                            mainMenuState = MAIN_MENU;
                            processOutputState = MAIN_MENU;
                        } else {
                            processOutputState = INVALID_INPUT;
                        }
                    } else {
                        processOutputState = INVALID_INPUT;
                    }
                    break;

                case SCAN_MENU:
                    tmp = inputString.split(" ", 2);
                    if (tmp.length == 2) {
                        if (tmp[0].equals("ScanStart")) {
                            Scan scanParam = parse.ScanParse(tmp[1]);
                            if (scanParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(MainActivity.MSG_MA_START_BLE_SCAN,
                                        scanParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else {
                            processOutputState = INVALID_INPUT;
                        }
                    } else if (tmp.length == 1) {
                        if (tmp[0].equals("ScanStop")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(MainActivity.MSG_MA_STOP_BLE_SCAN, null);
                            MainActivity.msghandler.sendMessage(msg);
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

                case THROUGHPUT_MENU:
                    tmp = inputString.split(" ", 2);
                    if (tmp.length == 2) {
                        if (tmp[0].equals("Connect")) {
                            Scan scanParam = parse.ScanParse(tmp[1]);
                            if (scanParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(MainActivity.MSG_SM_START_BLE_CONNECT,
                                        scanParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("ConnUpdate")) {
                            ConnUpdate connUpdateParam = parse.ConnUpdateParse(tmp[1]);
                            if (connUpdateParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_SM_START_BLE_CONN_UPDATE, connUpdateParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("SetPhy")) {
                            PhyUpdate phyUpdateParam = parse.PhyUpdateParse(tmp[1]);
                            if (phyUpdateParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_SM_START_BLE_PHY_UPDATE, phyUpdateParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("Tx")) {
                            DataTx dataTxParam = parse.DataTxParse(tmp[1]);
                            if (dataTxParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_SM_START_BLE_DATA_TX_TEST, dataTxParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("Rx")) {
                            DataRx dataRxParam = parse.DataRxParse(tmp[1]);
                            if (dataRxParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_SM_START_BLE_DATA_RX_TEST, dataRxParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("Latency")) {
                            LatencyTest latencyTestParam = parse.LatencyTestParse(tmp[1]);
                            if (latencyTestParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_SM_START_BLE_LATENCY_TEST, latencyTestParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else {
                            processOutputState = INVALID_INPUT;
                        }
                    } else if (tmp.length == 1) {
                        if (tmp[0].equals("ReadPhy")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_SM_START_BLE_READ_PHY, null);
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("Pair")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_SM_START_BLE_PAIR, null);
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("UnPair")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_SM_START_BLE_UNPAIR, null);
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("Disconnect")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_SM_START_BLE_GATT_DISC, null);
                            MainActivity.msghandler.sendMessage(msg);
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

                case GATT_CLIENT_MENU:
                    tmp = inputString.split(" ", 2);
                    if (tmp.length == 2) {
                        if (tmp[0].equals("Connect")) {
                            Scan scanParam = parse.ScanParse(tmp[1]);
                            if (scanParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                       MainActivity.MSG_GC_START_BLE_CONNECT, scanParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("ConnUpdate")) {
                            ConnUpdate connUpdateParam = parse.ConnUpdateParse(tmp[1]);
                            if (connUpdateParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_GC_START_BLE_CONN_UPDATE, connUpdateParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("SetPhy")) {
                            PhyUpdate phyUpdateParam = parse.PhyUpdateParse(tmp[1]);
                            if (phyUpdateParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_GC_START_BLE_PHY_UPDATE, phyUpdateParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("ConfigureMTU")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_GC_START_BLE_GATT_CONFIGURE_MTU_SIZE,
                                    Integer.parseInt(tmp[1]));
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("RW_Char")) {
                            ReadWriteOp readWriteCharOpParam = parse.ReadWriteOpParse(tmp[1]);
                            if (readWriteCharOpParam != null) {
                                Log.i(TAG, "RW_Char SrvUUID: " +readWriteCharOpParam.Srvc_uuid + " Value: " +readWriteCharOpParam.Value);
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_GC_START_BLE_GATT_WRITE_READ_CHAR, readWriteCharOpParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("RW_Desc")) {
                            ReadWriteOp readWriteDescOpParam = parse.ReadWriteOpParse(tmp[1]);
                            if (readWriteDescOpParam != null) {
                                Log.i(TAG, "RW_Desc SrvUUID: " +readWriteDescOpParam.Srvc_uuid + " Value: " +readWriteDescOpParam.Value);
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_GC_START_BLE_GATT_WRITE_READ_DESC, readWriteDescOpParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("RegNotifications")) {
                            ReadWriteOp regNotifParam = parse.ReadWriteOpParse(tmp[1]);
                            if (regNotifParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_GC_REGISTER_BLE_GATT_NOTIFICATIONS, regNotifParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("DeRegNotifications")) {
                            ReadWriteOp deregNotifParam = parse.ReadWriteOpParse(tmp[1]);
                            if (deregNotifParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_GC_DEREGISTER_BLE_GATT_NOTIFICATIONS, deregNotifParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if (tmp[0].equals("ReliableWrite")) {
                            ReadWriteOp reliableWriteParam = parse.ReadWriteOpParse(tmp[1]);
                            if (reliableWriteParam != null) {
                                Log.i(TAG, "ReliableWrite SrvUUID: " +reliableWriteParam.Srvc_uuid + " Value: " +reliableWriteParam.Value);
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_GC_START_BLE_GATT_RELIABLE_WRITE, reliableWriteParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else {
                            processOutputState = INVALID_INPUT;
                        }
                    } else if (tmp.length == 1) {
                        if (tmp[0].equals("ReadPhy")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_GC_START_BLE_READ_PHY, null);
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("Pair")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_GC_START_BLE_PAIR, null);
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("UnPair")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_GC_START_BLE_UNPAIR, null);
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("DiscoverServices")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_GC_START_BLE_GATT_DISCOVER, null);
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("RefreshServices")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_GC_START_BLE_GATT_REFRESH_SERVICES, null);
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("AbortReliableWrite")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_GC_START_BLE_GATT_ABORT_RELIABLE_WRITE, null);
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("CancelConnect")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_GC_START_BLE_GATT_CANCEL_CONNECT, null);
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("Disconnect")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                    MainActivity.MSG_GC_START_BLE_GATT_DISC, null);
                            MainActivity.msghandler.sendMessage(msg);
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
                case GATT_SERVER_MENU:
                    tmp = inputString.split(" ", 2);
                    if(tmp.length == 2) {
                        if (tmp[0].equals("AddService")) {
                            AddServices AddServiceParam = parse.AddServicesParse(tmp[1]);
                            if ( AddServiceParam != null) {
                                processOutputState = NONE;
                                msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_GS_START_BLE_ADD_SERVICE,AddServiceParam);
                                MainActivity.msghandler.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
                        } else if(tmp[0].equals("RemoveService")) {
                            String [] tmp2 = tmp[1].split(":");
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_GS_START_BLE_REMOVE_SERVICE, tmp2[1]);
                            MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("SetPhy")) {
                            PhyUpdate phyUpdateParam = parse.PhyUpdateParse(tmp[1]);
                            if (phyUpdateParam != null) {
                              processOutputState = NONE;
                              msg = MainActivity.msghandler.obtainMessage(
                                     MainActivity.MSG_GS_START_BLE_PHY_UPDATE, phyUpdateParam);
                              MainActivity.msghandler.sendMessage(msg);
                            } else {
                              processOutputState = INVALID_INPUT;
                            }
                        } else {
                            processOutputState = INVALID_INPUT;
                        }
                    } else if(tmp.length == 1) {
                        if (tmp[0].equals("Back")) {
                            mainMenuState = MAIN_MENU;
                            processOutputState = MAIN_MENU;
                        } else if(tmp[0].equals("ClearServices")) {
                             processOutputState = NONE;
                             msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_GS_START_BLE_CLEAR_SERVICES, null);
                             MainActivity.msghandler.sendMessage(msg);
                        } else if(tmp[0].equals("GetServices")) {
                             processOutputState = NONE;
                             msg = MainActivity.msghandler.obtainMessage(
                                        MainActivity.MSG_GS_START_BLE_GET_SERVICES, null);
                             MainActivity.msghandler.sendMessage(msg);
                        } else if (tmp[0].equals("ReadPhy")) {
                             processOutputState = NONE;
                             msg = MainActivity.msghandler.obtainMessage(
                                     MainActivity.MSG_GS_START_BLE_READ_PHY, null);
                             MainActivity.msghandler.sendMessage(msg);
                        } else if(tmp[0].equals("GetConnectedDevices")) {
                            processOutputState = NONE;
                            msg = MainActivity.msghandler.obtainMessage(
                                     MainActivity.MSG_GS_START_GET_CONNECTED_DEVICES, null);
                            MainActivity.msghandler.sendMessage(msg);
                        } else {
                            processOutputState = INVALID_INPUT;
                        }
                    } else {
                     processOutputState = INVALID_INPUT;
                    }
                    break;
            }
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
                            output.write(data.getBytes(), 0, data.getBytes().length);
                            output.flush();
                            Log.i(TAG, "Sent: " + data);
                        }else{
                            Log.i(TAG, "Data length is more than " + socSendBufferSize + " , ignore packet");
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "There is an exception when writing to socket");
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
}
