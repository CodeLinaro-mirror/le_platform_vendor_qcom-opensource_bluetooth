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

package vendor.qti.ancs_ble_testapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Message;
import android.util.Log;
import java.util.Arrays;

public class SocketServer {
    private static SocketServer INSTANCE = new SocketServer();
    private static Semaphore mutex = new Semaphore(1);

    private static final String TAG = "SocketServer";
    final String SOCKET_ADDRESS = "ANCSAppSocket";
    static int socSendBufferSize = 4096;
    int socRcvBufferSize = 512;
    byte[] socRcvBuffer;
    int bytesRead;
    private static boolean socketOpen = false;
    private static boolean closeReceived = false;
    String toSendString;
    InputStream input;
    private static OutputStream output;
    LocalServerSocket server;
    LocalSocket client;
    localServerSocket localServer;
    communicationHandler commHandler;

    static final int MAIN_MENU = 1;
    static final int INVALID_INPUT = 2;
    static final int SOC_CLOSE_ACK = 3;
    static final int NONE = 4;

    static int mainMenuState = MAIN_MENU;
    static int processOutputState = MAIN_MENU;

    private SocketServer() {
        Log.d(TAG, "SocketServer()");
        socRcvBuffer = new byte[socRcvBufferSize];

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

                if (bytesRead > 0) {
                    String inputStr = new String(socRcvBuffer, 0, bytesRead);
                    Log.i(TAG, "Received: " + inputStr);
                    bytesRead = 0;
                    processInput(inputStr);
                } else {
                    processOutputState = INVALID_INPUT;
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
            StringBuilder sendStr = new StringBuilder();

            switch (processOutputState) {
                case MAIN_MENU:
                    sendStr.append("\n******************** Bt Test App ********************\n");
                    sendStr.append("                     AdvStart\n");
                    sendStr.append("                     AdvStop\n");
                    sendStr.append("                     GetNotificationAttr      \
                                (Ex: GetNotificationAttr NotificationUID AppIdentifier;Title:10; \
                                Subtitle:20;Message:30;MessageSize;Date;PositiveActionLabel; \
                                NegativeActionLabel)\n");
                    sendStr.append("                     GetAppAttr               \
                                (Ex: GetAppAttr com.vendor.app)\n");
                    sendStr.append("                     Close\n");
                    sendStr.append("*****************************************************\n");
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
                    tmp = inputString.split(" ");
                    if (tmp.length == 1) {
                        if (inputString.equals("Close")) {
                            closeReceived = true;
                            mainMenuState = MAIN_MENU;
                            processOutputState = SOC_CLOSE_ACK;
                        } else if (tmp[0].equals("AdvStart")) {
                            processOutputState = NONE;
                            msg = AncsService.mStateMachine.obtainMessage(
                                    AncsService.NCStateMachine.MSG_NC_SM_START_ADV, null);
                            AncsService.mStateMachine.sendMessage(msg);
                        } else if (tmp[0].equals("AdvStop")) {
                            processOutputState = NONE;
                            msg = AncsService.mStateMachine.obtainMessage(
                                    AncsService.NCStateMachine.MSG_NC_SM_STOP_ADV, null);
                            AncsService.mStateMachine.sendMessage(msg);
                        } else {
                            processOutputState = INVALID_INPUT;
                        }
                    } else if (tmp.length == 2) {
                        if (tmp[0].equals("GetAppAttr")) {
                            msg = AncsService.mStateMachine.obtainMessage(
                                   AncsService.NCStateMachine.MSG_NC_SM_APP_ATTR, (tmp[1] + '\0'));
                            AncsService.mStateMachine.sendMessage(msg);
                        } else {
                            processOutputState = INVALID_INPUT;
                        }
                    } else if (tmp.length == 3) {
                        if (tmp[0].equals("GetNotificationAttr")) {
                            AncsParse.NotificationAttr notificationAttr =
                                            new AncsParse.NotificationAttr();
                            notificationAttr.NotificationUID =
                                            intToByteArray(Integer.parseInt(tmp[1]));
                            notificationAttr.NotificationAttributes =
                                            notificationAttrParse(tmp[2]);
                            if(notificationAttr.NotificationAttributes != null) {
                                msg = AncsService.mStateMachine.obtainMessage(
                                            AncsService.NCStateMachine.MSG_NC_SM_NOTIFICATION_ATTR,
                                            notificationAttr);
                                AncsService.mStateMachine.sendMessage(msg);
                            } else {
                                processOutputState = INVALID_INPUT;
                            }
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

    public byte[] notificationAttrParse (String input){
        Log.i(TAG, "notificationAttrParse()");

        ByteArrayOutputStream commandAttr = new ByteArrayOutputStream(1024);

        String tmp[] = input.split(";");
        String[] tmp2;
        int i=0;
        for(i=0; i<tmp.length; i++){
            tmp2 = tmp[i].split(":",2);
            if(tmp2.length == 2) {
                if (tmp2[0].equals("Title")) {
                    commandAttr.write(AncsParse.NotificationAttributeIDTitle);
                    int len = Integer.parseInt(tmp2[1]);
                    commandAttr.write((byte) (len & 0xFF));
                    commandAttr.write((byte) ((len >> 8) & 0xFF));
                } else if (tmp2[0].equals("Subtitle")) {
                    commandAttr.write(AncsParse.NotificationAttributeIDSubtitle);
                    int len = Integer.parseInt(tmp2[1]);
                    commandAttr.write((byte) (len & 0xFF));
                    commandAttr.write((byte) ((len >> 8) & 0xFF));
                } else if (tmp2[0].equals("Message")) {
                    commandAttr.write(AncsParse.NotificationAttributeIDMessage);
                    int len = Integer.parseInt(tmp2[1]);
                    commandAttr.write((byte) (len & 0xFF));
                    commandAttr.write((byte) ((len >> 8) & 0xFF));
                } else {
                    break;
                }
            } else if (tmp2.length == 1) {
                if (tmp2[0].equals("AppIdentifier")) {
                    commandAttr.write(AncsParse.NotificationAttributeIDAppIdentifier);
                } else if (tmp2[0].equals("MessageSize")) {
                    commandAttr.write(AncsParse.NotificationAttributeIDMessageSize);
                } else if (tmp2[0].equals("Date")) {
                    commandAttr.write(AncsParse.NotificationAttributeIDDate);
                } else if (tmp2[0].equals("PositiveActionLabel")) {
                    commandAttr.write(AncsParse.NotificationAttributeIDPositiveActionLabel);
                } else if (tmp2[0].equals("NegativeActionLabel")) {
                    commandAttr.write(AncsParse.NotificationAttributeIDNegativeActionLabel);
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        if(i == tmp.length){
            return commandAttr.toByteArray();
        }else{
            return null;
        }
    }

    public static byte[] intToByteArray(int a)
    {
        byte[] byteArr = new byte[4];
        byteArr[0] = (byte) (a & 0xFF);
        byteArr[1] = (byte) ((a >> 8) & 0xFF);
        byteArr[2] = (byte) ((a >> 16) & 0xFF);
        byteArr[3] = (byte) ((a >> 24) & 0xFF);
        return byteArr;
    }

    public static int byteArrayToInt(byte[] b)
    {
        return ((b[3] & 0xFF) << 24) |
                ((b[2] & 0xFF) << 16) |
                ((b[1] & 0xFF) << 8) |
                ((b[0] & 0xFF) << 0);
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
                        } else {
                            Log.i(TAG, "Data length is more than " + socSendBufferSize
                                + " , ignore packet");
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
