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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.util.Log;

public class ConfigFileParser {
    private static final String TAG = "BluetoothTxRxApp ConfigFileParser";

    ConfigFileParser() {

    }

    public void parseText() {
        String file = "/system/bin/config.txt";
        try {
            BufferedInputStream bf = new BufferedInputStream(
                    new FileInputStream(file));
            BufferedReader br = new BufferedReader(new InputStreamReader(bf));
            String data;
            // Read till end of file
            while ((data = br.readLine()) != null) {
                String[] splitData = data.split("=");
                if (splitData.length != 0) {
                    Utils.bdAddressFromConfig = splitData[1];
                }
            }
            br.close();
        } catch (Exception e) {
            System.out.println("Error is :: " + e);
        }
    }

    public Connect connectParse(String input) {
        Connect connect = new Connect();
        String tmp[] = input.split(":", 2);
        if (tmp.length == 2) {
            if (tmp[0].equals("bdAddress")) {
                connect.BDaddress = tmp[1];
            }
        }
        return connect;
    }

    public Tx txParse(String input) {
        Log.d(TAG, "txParse input is ::" + input);
        Tx tx = new Tx();
        String tmp[] = input.split(":", 2);
        Log.d(TAG, "tmp.length is ::" + tmp.length);
        if (tmp.length == 2) {
            if (tmp[0].equals("chunkSize")) {
                Log.d(TAG, "tmp[1] is ::" + tmp[1]);
                tx.chunkSize = Integer.parseInt(tmp[1]);
            } else {
                tx = null;
            }
        } else {
            tx = null;
        }
        return tx;
    }

    public Wakeable wakeableParse(String input) {
        Wakeable wakeable = new Wakeable();
        String tmp[] = input.split(":", 2);
        if (tmp.length == 2) {
            if (tmp[0].equals("timer")) {
                wakeable.timer = Integer.parseInt(tmp[1]);
            } else {
                wakeable = null;
            }
        } else {
            wakeable = null;
        }
        return wakeable;
    }

    public Actionable actionableParse(String input) {
        Actionable actionable = new Actionable();
        String tmp[] = input.split(":", 2);
        if (tmp.length == 2) {
            if (tmp[0].equals("timer")) {
                actionable.timer = Integer.parseInt(tmp[1]);
            } else {
                actionable = null;
            }
        } else {
            actionable = null;
        }
        return actionable;
    }

    public Cacheable cacheableParse(String input) {
        Cacheable cacheable = new Cacheable();
        String tmp[] = input.split(":", 2);
        if (tmp.length == 2) {
            if (tmp[0].equals("timer")) {
                cacheable.timer = Integer.parseInt(tmp[1]);
            } else {
                cacheable = null;
            }
        } else {
            cacheable = null;
        }
        return cacheable;
    }

    public ConnectionTest connectionTestParse(String input) {
        ConnectionTest connectionTest = new ConnectionTest();
        String tmp[] = input.split(":", 2);
        if (tmp.length == 2) {
            if (tmp[0].equals("count")) {
                connectionTest.count = Integer.parseInt(tmp[1]);
            } else {
                connectionTest = null;
            }
        } else {
            connectionTest = null;
        }
        return connectionTest;
    }

    public SetScanMode setScanModeParse(String input) {
        SetScanMode scanMode = new SetScanMode();
        String tmp[] = input.split(":", 2);
        if (tmp.length == 2) {
            if (tmp[0].equals("mode")) {
                scanMode.scanMode = Integer.parseInt(tmp[1]);
            } else {
                scanMode = null;
            }
        } else {
            scanMode = null;
        }
        return scanMode;
    }

    public IncomingConnection incomingConnectionParse(String input) {
        IncomingConnection incomingConnection = new IncomingConnection();
        String tmp[] = input.split(":", 2);
        if (tmp.length == 2) {
            if (tmp[0].equals("uuid")) {
                incomingConnection.uuid = tmp[1];
            } else {
                incomingConnection = null;
            }
        } else {
            incomingConnection = null;
        }
        return incomingConnection;
    }

    public StringBuilder notificationInfoParse(byte[] bytes) {
        StringBuilder notString = new StringBuilder();
        String att_value;
        byte att_id = bytes[5];
        int att_length = 0xff & bytes[6];
        att_value = new String(bytes, 7, att_length);
        notString.append("\n*********Received***********\n");
        switch (att_id) {
        case 0x01:
            notString.append("\nCaller Number : ");
            break;
        case 0x02:
            notString.append("\nCaller Name : ");
            break;
        case 0x03:
            notString.append("\nEmail Address : ");
            break;
        case 0x04:
            notString.append("\nEmail Subject\n");
            break;
        case 0x05:
            notString.append("\nEmail Body\n");
            break;
        default:
            Log.e(TAG, "Invalid get Attribute ID");
            break;
        }
        notString.append(att_value);
        return notString;
    }

    public byte[] getActionPacket(byte action, byte getID) {
        byte[] actionPacket = new byte[7];
        actionPacket[0] = 0x00;
        actionPacket[1] = 0x06; // packet length
        actionPacket[2] = (byte) 0xE4; // packet code
        actionPacket[3] = 0x00; //
        actionPacket[4] = 0x01; // Notification handle
        actionPacket[5] = getID; // Notification Attribute ID
        actionPacket[6] = action; // Notification Action ID
        return actionPacket;
    }

    public byte[] getReadInfoPacket(byte getID) {
        byte[] readPacket = new byte[6];
        readPacket[0] = 0x00;
        readPacket[1] = 0x06; // packet length
        readPacket[2] = (byte) 0xE2; // packet code
        readPacket[3] = 0x00; //
        readPacket[4] = 0x01; // Notification handle
        readPacket[5] = getID; // Notification Attribute ID
        return readPacket;
    }

}
