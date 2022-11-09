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

import android.util.Log;
import android.os.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AncsParse {
    private static final String TAG = "AncsParse";

    // UUIDs
    public static UUID ANCS_SERVICE_UUID = UUID.fromString("7905F431-B5CE-4E99-A40F-4B1E122D00D0");
    public static UUID ANCS_NOTIFICATION_SOURCE_UUID =
                        UUID.fromString("9FBF120D-6301-42D9-8C58-25E699A21DBD");
    public static UUID ANCS_CONTROL_POINT_UUID =
                        UUID.fromString("69D1D8F3-45E1-49A8-9821-9BBDFDAAD9D9");
    public static UUID ANCS_DATA_SOURCE_UUID =
                        UUID.fromString("22EAC6E9-24D6-4BB5-BE44-B36ACE7C7BFB");
    public static final UUID CONFIG_DESCRIPTOR_UUID =
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // EventID values
    public static final byte EventIDNotificationAdded = 0;
    public static final byte EventIDNotificationModified = 1;
    public static final byte EventIDNotificationRemoved = 2;

    // EventFlags
    public static final byte EventFlagSilent = (1 << 0);
    public static final byte EventFlagImportant = (1 << 1);
    public static final byte EventFlagPreExisting = (1 << 2);
    public static final byte EventFlagPositiveAction = (1 << 3);
    public static final byte EventFlagNegativeAction = (1 << 4);

    // CategoryID values
    public static final byte CategoryIDOther = 0;
    public static final byte CategoryIDIncomingCall = 1;
    public static final byte CategoryIDMissedCall = 2;
    public static final byte CategoryIDVoicemail = 3;
    public static final byte CategoryIDSocial = 4;
    public static final byte CategoryIDSchedule = 5;
    public static final byte CategoryIDEmail = 6;
    public static final byte CategoryIDNews = 7;
    public static final byte CategoryIDHealthAndFitness = 8;
    public static final byte CategoryIDBusinessAndFinance = 9;
    public static final byte CategoryIDLocation = 10;
    public static final byte CategoryIDEntertainment = 11;

    // CommandID values
    public static final byte CommandIDGetNotificationAttributes = 0;
    public static final byte CommandIDGetAppAttributes = 1;
    public static final byte CommandIDPerformNotificationAction = 2;

    // NotificationAttributeID values
    public static final byte NotificationAttributeIDAppIdentifier = 0;
    // (Needs to be followed by a 2-bytes max length parameter)
    public static final byte NotificationAttributeIDTitle = 1;
    // (Needs to be followed by a 2-bytes max length parameter)
    public static final byte NotificationAttributeIDSubtitle = 2;
    // (Needs to be followed by a 2-bytes max length parameter)
    public static final byte NotificationAttributeIDMessage = 3;
    public static final byte NotificationAttributeIDMessageSize = 4;
    public static final byte NotificationAttributeIDDate = 5;
    public static final byte NotificationAttributeIDPositiveActionLabel = 6;
    public static final byte NotificationAttributeIDNegativeActionLabel = 7;

    // AppAttributeID values
    public static final byte AppAttributeIDDisplayName = 0;

    // ActionID values
    public static final byte ActionIDPositive = 0;
    public static final byte ActionIDNegative = 1;

    // Error Codes
    /* The commandID was not recognized by the NP. */
    public static final byte UnknownCommand = (byte) 0xA0;
    /* The command was improperly formatted. */
    public static final byte InvalidCommand = (byte) 0xA1;
    /* One of the parameters (for example, the NotificationUID)
    does not refer to an existing object on the NP. */
    public static final byte InvalidParameter = (byte) 0xA2;
    /* The action was not performed. */
    public static final byte ActionFailed = (byte) 0xA3;

    public static final int NOTIFICATION_SOURCE_LENGTH = 8;
    public static boolean PowerTesting = false;
    public static boolean SubtitleFound = false;
    public static int NumofNotifications = 0;

    public static LinkedList<NotificationSource> NotificationSourceList =
                    new LinkedList<NotificationSource>();

    public static class NotificationSource {
        byte EventID;
        byte EventFlag;
        byte CategoryID;
        byte CategoryCount;
        byte[] NotificationUID = new byte[4];

        public NotificationSource(byte[] value) {
            EventID = value[0];
            EventFlag = value[1];
            CategoryID = value[2];
            CategoryCount = value[3];
            NotificationUID = Arrays.copyOfRange(value, 4, 8);
        }
    }

    public static class NotificationAttr {
        byte[] NotificationUID = new byte[4];
        byte[] NotificationAttributes;
    }

    public static class NotificationAction {
        byte[] NotificationUID = new byte[4];
        String NotificationAction;
    }

    private static void frameGetNotificationAttrCmd(byte[] uid) {
        Log.i(TAG, "frameGetNotificationAttrCmd");
        ByteArrayOutputStream commandAttr = new ByteArrayOutputStream(1024);

        /* frame the Get Notification Attributes cmd */
        NotificationAttr notificationAttr =
            new NotificationAttr();
        notificationAttr.NotificationUID = Arrays.copyOfRange(uid, 0, 3);
        commandAttr.write(AncsParse.NotificationAttributeIDAppIdentifier);
        commandAttr.write(AncsParse.NotificationAttributeIDSubtitle);
        int subtitle_len = 20;
        commandAttr.write((byte) (subtitle_len & 0xFF));
        commandAttr.write((byte) ((subtitle_len >> 8) & 0xFF));
        commandAttr.write(AncsParse.NotificationAttributeIDMessage);
        int len = 30;
        commandAttr.write((byte) (len & 0xFF));
        commandAttr.write((byte) ((len >> 8) & 0xFF));

        notificationAttr.NotificationAttributes = commandAttr.toByteArray();

        /* write to CP */
        Message msg;
        msg = AncsService.mStateMachine.obtainMessage(
           AncsService.NCStateMachine.MSG_NC_SM_NOTIFICATION_ATTR,
           notificationAttr);
        AncsService.mStateMachine.sendMessage(msg);
    }

    public static void processNotificationSource(byte[] value) {
        if (NOTIFICATION_SOURCE_LENGTH == value.length) {
            NotificationSource notification = new NotificationSource(value);

            Log.i(TAG, "processNotificationSource EventId: " + notification.EventID +
                        " EventFlag: " + notification.EventFlag +
                        " CategoryID: " + notification.CategoryID +
                        " CategoryCount: " + notification.CategoryCount +
                        " NotificationUID: " + Arrays.toString(notification.NotificationUID) +
                        " payload: " + Arrays.toString(value));

            int index = searchNotificationSource(notification);
            if (0xff != index) {
                NotificationSourceList.remove(index);
                Log.i(TAG, "notifications is removed");
            }

            if (notification.EventID != EventIDNotificationRemoved) {
                NotificationSourceList.add(notification);
                Log.i(TAG, "notifications is added");
                /* If category type is mail and power testing is enabled,
                request for notification attr on data source, so that notification
                received on data source can be parsed for PowerTesting subject without
                user intervention*/
                if((AncsService.power_testing == true) &&
                   (notification.CategoryID == CategoryIDEmail) &&
                   (NumofNotifications == 0)) {
                    Log.i(TAG, "framing cmd for power testing");
                    frameGetNotificationAttrCmd(notification.NotificationUID);
                }
            }
        } else {
            Log.e(TAG, "processNotificationSource invalid length");
        }
    }

    private static void frameNotificationAttrCmd(byte[] uid) {
        Log.i(TAG, "frameNotificationAttrCmd, numNotifications"+NumofNotifications);
        ByteArrayOutputStream commandAttr = new ByteArrayOutputStream(1024);

        /* If number of notifications is non zero, keep writing to Contol point to get notifications */
        if(NumofNotifications != 0){
            /* frame the cmd */
            NotificationAttr notificationAttr =
                            new NotificationAttr();
            notificationAttr.NotificationUID = Arrays.copyOfRange(uid, 0, 3);
            commandAttr.write(AncsParse.NotificationAttributeIDAppIdentifier);
            commandAttr.write(AncsParse.NotificationAttributeIDMessage);
            /* payload length should be 236 to get the maximum pdu length without splitting */
            int len = 236;
            commandAttr.write((byte) (len & 0xFF));
            commandAttr.write((byte) ((len >> 8) & 0xFF));

            notificationAttr.NotificationAttributes = commandAttr.toByteArray();

            /* write to CP */
            Message msg;
            msg = AncsService.mStateMachine.obtainMessage(
                AncsService.NCStateMachine.MSG_NC_SM_NOTIFICATION_ATTR,
                notificationAttr);
            AncsService.mStateMachine.sendMessage(msg);
            NumofNotifications--;
        } else {
            PowerTesting = false;
            Log.d(TAG, "frameNotificationAttrCmd, pwr test false");
        }
    }

    public static void processDataSource(byte[] value) {
        Log.i(TAG, "processDataSource");
        StringBuilder printStr = new StringBuilder();
        int i = 0, size = 0;
        byte[] uid = new byte[4];

        if (value.length > 0) {
            if (value[0] == CommandIDGetNotificationAttributes) {
                printStr.setLength(0);
                printStr.append('\n');
                printStr.append("NotificationAttr:");
                printStr.append(NumofNotifications);
                printStr.append("\nuid : ");
                uid = Arrays.copyOfRange(value, 1, 5);
                printStr.append(SocketServer.byteArrayToInt(Arrays.copyOfRange(value, 1, 5)));
                i = 5;
                for (; i < value.length; ) {
                    printStr.append("\n");
                    printStr.append(getNotificationAttrString(value[i]));
                    if(Objects.equals("SubTitle",getNotificationAttrString(value[i]))) {
                        SubtitleFound = true;
                    }
                    printStr.append(" : ");
                    size = (((value[i + 2] & 0xFF) << 8) | (value[i + 1] & 0xFF));
                    i += 3;
                    if (size > 0) {
                        String attr = new String(Arrays.copyOfRange(value, i, i + size));
                        printStr.append(attr);
                        /*If received mail has subject, check if it is PowerTesting,
                        if yes get number of times the notificatins is to be sent */
                        if(SubtitleFound) {
                            String[] tmp;
                            tmp = attr.split(":");
                            if(tmp[0].equals("PowerTesting")) {
                                NumofNotifications = Integer.parseInt(tmp[1]);
                                PowerTesting = true;
                            }
                            SubtitleFound = false;
                        }
                        i = i + size;
                    }
                }
                Log.i(TAG, "bytesToBeSent: " + printStr.toString().getBytes().length);
                /* If Subject is PowerTesting then frame the
                getNotificationAttr cmd and write to CP */
                if((PowerTesting == true) && (AncsService.power_testing == true)) {
                    frameNotificationAttrCmd(uid);
                }
                printStr.append('\n');
            } else if (value[0] == CommandIDGetAppAttributes) {
                printStr.setLength(0);
                printStr.append("AppAttr\t");
                for (i = 1; i < value.length; i++) {
                    if (value[i] == '\0') {
                        printStr.append(new String(Arrays.copyOfRange(value, 1, i)));
                        printStr.append("\t");
                        i++;
                        if (value[i] == AppAttributeIDDisplayName) {
                            size = (value[i + 2] << 8) | value[i + 1];
                            i += 3;
                            if (size > 0) {
                                printStr.append(new String(Arrays.copyOfRange(
                                                                value, i, i + size)));
                            }
                        }
                        break;
                    }
                }
                printStr.append('\n');
            } else if (value[0] == UnknownCommand) {
                printStr.append("Unknown command\n");
            } else if (value[0] == InvalidCommand) {
                printStr.append("Invalid command\n");
            } else if (value[0] == InvalidParameter) {
                printStr.append("Invalid parameter\n");
            } else if (value[0] == ActionFailed) {
                printStr.append("Action failed\n");
            } else {
                printStr.append("processDataSource Error\n");
                Log.e(TAG, "processDataSource unknown commandID");
            }
            SocketServer.sendSocketData(printStr.toString());
        } else {
            Log.e(TAG, "processDataSource invalid length");
        }
    }

    public static int searchNotificationSource(NotificationSource notification) {
        int index = 0xff;
        for (int i = 0; i < NotificationSourceList.size(); i++) {
            if (Arrays.equals(NotificationSourceList.get(i).NotificationUID,
                                notification.NotificationUID)) {
                index = i;
                break;
            }
        }
        return index;
    }

    public static byte[] getNotificationAttributes(NotificationAttr attr) {
        ByteArrayOutputStream command = new ByteArrayOutputStream(1024);
        command.write(CommandIDGetNotificationAttributes); // CommandID
        try {
            command.write(attr.NotificationUID); // NotificationUID
            command.write(attr.NotificationAttributes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return command.toByteArray();
    }

    public static byte[] getAppAttributes(String appID) {
        ByteArrayOutputStream command = new ByteArrayOutputStream(1024);
        command.write(CommandIDGetAppAttributes); // CommandID
        try {
            command.write(appID.getBytes()); // App ID
        } catch (IOException e) {
            e.printStackTrace();
        }
        command.write(AppAttributeIDDisplayName); // Attribute
        return command.toByteArray();
    }

    public static byte[] performNotificationAction(NotificationAction action) {
        ByteArrayOutputStream command = new ByteArrayOutputStream(1024);
        command.write(CommandIDPerformNotificationAction); // CommandID
        try {
            command.write(action.NotificationUID); // NotificationUID
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (action.NotificationAction.equals("Positive")) {
            command.write(ActionIDPositive); // Action
        } else {
            command.write(ActionIDNegative); // Action
        }
        return command.toByteArray();
    }

    public static void printNotificationSource() {
        StringBuilder printStr = new StringBuilder();

        printStr.setLength(0);
        printStr.append("\n*********************** Notifications **************************\n");
        for (int i = 0; i < NotificationSourceList.size(); i++) {
            printStr.append(getCategoryIdString(NotificationSourceList.get(i).CategoryID));
            printStr.append("\tCount: ");
            printStr.append(NotificationSourceList.get(i).CategoryCount);
            printStr.append("\tUID: ");
            printStr.append(
                SocketServer.byteArrayToInt(NotificationSourceList.get(i).NotificationUID));
            if ((NotificationSourceList.get(i).EventFlag & EventFlagPositiveAction) != 0) {
                printStr.append("\tPositive");
            }
            if ((NotificationSourceList.get(i).EventFlag & EventFlagNegativeAction) != 0) {
                printStr.append("\tNegative");
            }
            printStr.append('\n');
        }
        printStr.append("******************************************************************\n");
        SocketServer.sendSocketData(printStr.toString());
    }

    public static String getCategoryIdString(byte categoryId) {
        String categoryIdString = null;
        switch (categoryId) {
            case CategoryIDOther:
                categoryIdString = "Other";
                break;
            case CategoryIDIncomingCall:
                categoryIdString = "IncomingCall";
                break;
            case CategoryIDMissedCall:
                categoryIdString = "MissedCall";
                break;
            case CategoryIDVoicemail:
                categoryIdString = "VoiceMail";
                break;
            case CategoryIDSocial:
                categoryIdString = "Social";
                break;
            case CategoryIDSchedule:
                categoryIdString = "Schedule";
                break;
            case CategoryIDEmail:
                categoryIdString = "Email";
                break;
            case CategoryIDNews:
                categoryIdString = "News";
                break;
            case CategoryIDHealthAndFitness:
                categoryIdString = "HealthAndFitness";
                break;
            case CategoryIDBusinessAndFinance:
                categoryIdString = "BusinessAndFinance";
                break;
            case CategoryIDLocation:
                categoryIdString = "Location";
                break;
            case CategoryIDEntertainment:
                categoryIdString = "Entertainment";
                break;
        }
        return categoryIdString;
    }

    public static String getNotificationAttrString (byte notificationAttr) {
        String notificationAttrString = null;

        switch (notificationAttr) {
            case NotificationAttributeIDAppIdentifier:
                notificationAttrString = "AppID";
                break;

            case NotificationAttributeIDTitle:
                notificationAttrString = "Title";
                break;

            case NotificationAttributeIDSubtitle:
                notificationAttrString ="SubTitle";
                break;

            case NotificationAttributeIDMessage:
                notificationAttrString ="Message";
                break;

            case NotificationAttributeIDMessageSize:
                notificationAttrString ="MessageSize";
                break;

            case NotificationAttributeIDDate:
                notificationAttrString ="Date";
                break;

            case NotificationAttributeIDPositiveActionLabel:
                notificationAttrString ="PositiveActionLabel";
                break;

            case NotificationAttributeIDNegativeActionLabel:
                notificationAttrString ="NegativeActionLabel";
                break;
        }

        return notificationAttrString;
    }
}
