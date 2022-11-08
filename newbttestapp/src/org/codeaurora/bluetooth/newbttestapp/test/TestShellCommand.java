/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.UiModeManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapterCommon;
import android.bluetooth.BluetoothAdapterUtil;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.IndentingPrintWriter;
import android.util.SparseArray;

import java.util.ArrayList;

final class TestShellCommand extends ShellCommand {

    private static final String TAG = "TestShellCommand";

    private static final String COMMAND_HELP = "-h";
    /* Command for legacy Bluetooth adapter */
    private static final String COMMAND_ADAPTER = "adapter";
    /* Command for new Bluetooth adapter */
    private static final String COMMAND_ADAPTER1 = "adapter1";
    /* Command for HFP(AG) */
    private static final String COMMAND_HFP = "hfp";
    /* Command for A2DP(Source) */
    private static final String COMMAND_A2DP = "a2dp";
    /* Command for PBAP(Server) */
    private static final String COMMAND_PBAP = "pbap";
    /* Command for HID(Host) */
    private static final String COMMAND_HIDH = "hidh";
    /* Command for SPP in legacy Bluetooth adapter */
    private static final String COMMAND_SPP = "spp";
    /* Command for SPP in new Bluetooth adapter */
    private static final String COMMAND_SPP1 = "spp1";
    /* Command for GATT in legacy Bluetooth adapter */
    private static final String COMMAND_GATT = "gatt";
    /* Command for GATT in new Bluetooth adapter */
    private static final String COMMAND_GATT1 = "gatt1";

    private static final String PARAM_ENABLE = "enable";
    private static final String PARAM_DISABLE = "disable";
    private static final String PARAM_SEARCH = "search";
    private static final String PARAM_CANCEL_SEARCH = "cancel_search";
    private static final String PARAM_PAIR = "pair";
    private static final String PARAM_ACCEPT_PAIR = "accept_pair";
    private static final String PARAM_REJECT_PAIR = "reject_pair";
    private static final String PARAM_SET_PIN = "set_pin";
    private static final String PARAM_UNPAIR = "unpair";
    private static final String PARAM_GET_SUPPORTED_PROFILES = "get_supported_profiles";
    private static final String PARAM_GET_LOCAL_ADDRESS = "get_local_address";
    private static final String PARAM_GET_LOCAL_NAME = "get_local_name";
    private static final String PARAM_SET_LOCAL_NAME = "set_local_name";
    private static final String PARAM_GET_SCAN_MODE = "get_scan_mode";
    private static final String PARAM_SET_SCAN_MODE = "set_scan_mode";
    private static final String PARAM_SET_DISCOVERABLE_TOUT = "set_discoverable_tout";
    private static final String PARAM_GET_LOCAL_COD = "get_local_cod";
    private static final String PARAM_GET_REMOTE_NAME = "get_remote_name";
    private static final String PARAM_GET_ALIAS_NAME = "get_alias_name";
    private static final String PARAM_SET_ALIAS_NAME = "set_alias_name";
    private static final String PARAM_GET_REMOTE_COD = "get_remote_cod";
    private static final String PARAM_GET_RSSI = "get_rssi";
    private static final String PARAM_GET_LINKKEY = "get_linkkey";

    private static final String PARAM_CONNECT = "connect";
    private static final String PARAM_DISCONNECT = "disconnect";

    private static final String PARAM_CONNECT_AUDIO = "connect_audio";
    private static final String PARAM_DISCONNECT_AUDIO = "disconnect_audio";

    private static final String PARAM_ALLOW = "allow";
    private static final String PARAM_REJECT = "reject";

    private static final String PARAM_GET_MEDIA_PLAYER = "get_media_player";
    private static final String PARAM_SET_MEDIA_PLAYER = "set_media_player";
    private static final String PARAM_CLEAR_MEDIA_PLAYER = "clear_media_player";
    private static final String PARAM_START_MEDIA_PLAYER = "start_media_player";
    private static final String PARAM_DUMP_MEDIA_PLAYER_LIST = "dump_media_player_list";

    private static final int RESULT_OK = 0;
    private static final int RESULT_ERROR = -1; // Arbitrary value, any non-0 is fine

    private static final int ADAPTER_DEFAULT = BluetoothAdapterCommon.ADAPTER_DEFAULT;
    private static final int ADAPTER_1 = BluetoothAdapterCommon.ADAPTER_1;

    private static boolean sTestDefaultAdapter = false;

    private final Context mContext;
    private IndentingPrintWriter mPrintWriter;

    private final TestAdapter mTestAdapter;
    private final TestAdapter mTestAdapter1;
    private final TestHfp mTestHfp;
    private final TestA2dp mTestA2dp;
    private final TestPbap mTestPbap;
    private final TestHidh mTestHidh;
    private final TestSpp mTestSpp;
    private final TestSpp mTestSpp1;
    private final TestGatt mTestGatt;
    private final TestGatt mTestGatt1;

    private ExecThread mExecThread;

    @NonNull
    private static String getHelpString(@NonNull String name, @NonNull SparseArray<String> values) {
        StringBuilder help = new StringBuilder("Valid ").append(name).append(" are: ");
        int size = values.size();
        for (int i = 0; i < size; i++) {
            help.append(values.valueAt(i));
            if (i != size - 1) {
                help.append(", ");
            }
        }
        return help.append('.').toString();
    }

    TestShellCommand(Context context) {
        mContext = context;

        mTestAdapter = sTestDefaultAdapter ? new TestAdapter(mContext) : null;
        mTestAdapter1 = new TestAdapter(mContext, ADAPTER_1);
        mTestHfp = new TestHfp(mContext);
        mTestA2dp = new TestA2dp(mContext);
        mTestPbap = new TestPbap(mContext);
        mTestHidh = new TestHidh(mContext);
        mTestSpp = sTestDefaultAdapter ? new TestSpp(mContext) : null;
        mTestSpp1 = new TestSpp(mContext, ADAPTER_1);
        mTestGatt = sTestDefaultAdapter ? new TestGatt(mContext) : null;
        mTestGatt1 = new TestGatt(mContext, ADAPTER_1);
    }

    @Override
    public int onCommand(String cmd) {
        if (cmd == null) {
            onHelp();
            return RESULT_ERROR;
        }
        ArrayList<String> argsList = new ArrayList<>();
        argsList.add(cmd);
        String arg = null;
        do {
            arg = getNextArg();
            if (arg != null) {
                argsList.add(arg);
            }
        } while (arg != null);
        String[] args = new String[argsList.size()];
        argsList.toArray(args);
        try (IndentingPrintWriter pw = new IndentingPrintWriter(getOutPrintWriter())) {
            return exec(args, pw);
        }
    }

    @Override
    public void onHelp() {
        try (IndentingPrintWriter pw = new IndentingPrintWriter(getOutPrintWriter())) {
            showHelp(pw);
        }
    }

    private static void showHelp(IndentingPrintWriter pw) {
        pw.println("NewBTTestApp commands:");
        pw.println("\t-h");
        pw.println("\t  Print this help text.");
        if (sTestDefaultAdapter) {
            pw.println("\tadapter enable|disable|search|cancel_search|pair" +
                    "|accept_pair|reject_pair|get_supported_profiles");
            pw.println("\t  Test default Bluetooth adapter(0).");
        }
        pw.println("\tadapter1 enable|disable|search|cancel_search|pair" +
                "|accept_pair|reject_pair|set_pin|unpair|get_supported_profiles" +
                "|get_local_address|get_local_name|set_local_name|get_scan_mode" +
                "|set_scan_mode|set_discoverable_tout|get_local_cod|get_remote_name" +
                "|get_alias_name|set_alias_name|get_remote_cod|get_rssi|get_linkkey");
        pw.println("\t  Test new Bluetooth adapter(1).");
        pw.println("\thfp connect|disconnect|connect_audio|disconnect_audio device");
        pw.println("\t  Test HFP(AG) in new Bluetooth adapter.");
        pw.println("\t  device is remote Bluetooth device's address. E.g. 11:22:33:44:AA:BB");
        pw.println("\ta2dp connect|disconnect|get_media_player|set_media_player" +
                "|start_media_player|dump_media_player_list");
        pw.println("\t  Test A2DP(Source) in new Bluetooth adapter.");
        pw.println("\t    get_media_player device");
        pw.println("\t    set_media_player device media_player");
        pw.println("\t      - device: remote Bluetooth device's address");
        pw.println("\t      - media_player: media player's name");
        pw.println("\t    clear_media_player device");
        pw.println("\t    start_media_player media_player");
        pw.println("\t    dump_media_player_list");
        pw.println("\tpbap disconnect|allow|reject device");
        pw.println("\t  Test PBAP(PSE) in new Bluetooth adapter.");
        pw.println("\thidh connect|disconnect device");
        pw.println("\t  Test HID(Host) in new Bluetooth adapter.");
        if (sTestDefaultAdapter) {
            pw.println("\tspp connect|disconnect device");
            pw.println("\t  Test SPP in default Bluetooth adapter.");
        }
        pw.println("\tspp1 connect|disconnect device");
        pw.println("\t  Test SPP in new Bluetooth adapter.");
    }

    private static int showInvalidArguments(IndentingPrintWriter pw) {
        pw.println("Incorrect number of arguments.");
        showHelp(pw);
        return RESULT_ERROR;
    }

    public int exec(String[] args, IndentingPrintWriter writer) {
        mExecThread = new ExecThread(args, writer);
        mExecThread.start();
        return RESULT_OK;
    }

    private class ExecThread extends Thread {
        private String[] mArgs;
        private IndentingPrintWriter mWriter;

        ExecThread(String[] args, IndentingPrintWriter writer) {
            mArgs = args;
            mWriter = writer;
        }

        @Override
        public void run() {
            execCmd(mArgs, mWriter);
        }
    }

    private int execCmd(String[] args, IndentingPrintWriter writer) {
        mPrintWriter = writer;
        String cmd = args[0];
        logd("execute cmd: " + cmd);
        switch (cmd) {
            case COMMAND_HELP: {
                showHelp(writer);
                break;
            }
            case COMMAND_ADAPTER: {
                if (args.length < 2 ||
                    !sTestDefaultAdapter) {
                    return showInvalidArguments(writer);
                }
                runAdapter(args);
                break;
            }
            case COMMAND_ADAPTER1: {
                if (args.length < 2) {
                    return showInvalidArguments(writer);
                }
                runAdapter1(args);
                break;
            }
            case COMMAND_HFP: {
                if (args.length < 3) {
                    return showInvalidArguments(writer);
                }
                runHfp(args);
                break;
            }
            case COMMAND_A2DP: {
                if (args.length < 2) {
                    return showInvalidArguments(writer);
                }
                runA2dp(args);
                break;
            }
            case COMMAND_PBAP: {
                if (args.length < 3) {
                    return showInvalidArguments(writer);
                }
                runPbap(args);
                break;
            }
            case COMMAND_HIDH: {
                if (args.length < 3) {
                    return showInvalidArguments(writer);
                }
                runHidh(args);
                break;
            }
            case COMMAND_SPP: {
                if (args.length < 3 ||
                    !sTestDefaultAdapter) {
                    return showInvalidArguments(writer);
                }
                runSpp(args);
                break;
            }
            case COMMAND_SPP1: {
                if (args.length < 3) {
                    return showInvalidArguments(writer);
                }
                runSpp1(args);
                break;
            }
            case COMMAND_GATT: {
                if (!sTestDefaultAdapter) {
                    return showInvalidArguments(writer);
                }
                runGatt(args);
                break;
            }
            case COMMAND_GATT1: {
                runGatt1(args);
                break;
            }
            default: {
                writer.println("Unknown command: \"" + cmd + "\"");
                showHelp(writer);
                return RESULT_ERROR;
            }
        }
        return RESULT_OK;
    }

    private void runAdapter(String[] args, TestAdapter testAdapter, int adapterIndex) {
        String para = args[1].toLowerCase();
        switch (para) {
            case PARAM_ENABLE: {
                testAdapter.enable();
                break;
            }
            case PARAM_DISABLE: {
                testAdapter.disable();
                break;
            }
            case PARAM_SEARCH: {
                testAdapter.search();
                break;
            }
            case PARAM_CANCEL_SEARCH: {
                testAdapter.cancelSearch();
                break;
            }
            case PARAM_PAIR: {
                checkArgsLength(args, PARAM_PAIR, 3);
                BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
                testAdapter.pair(device);
                break;
            }
            case PARAM_ACCEPT_PAIR: {
                checkArgsLength(args, PARAM_ACCEPT_PAIR, 3);
                BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
                testAdapter.acceptPair(device);
                break;
            }
            case PARAM_REJECT_PAIR: {
                checkArgsLength(args, PARAM_REJECT_PAIR, 3);
                BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
                testAdapter.rejectPair(device);
                break;
            }
            case PARAM_SET_PIN: {
                checkArgsLength(args, PARAM_SET_PIN, 3);
                testAdapter.setPin(args[2]);
                break;
            }
            case PARAM_UNPAIR: {
                checkArgsLength(args, PARAM_SET_PIN, 3);
                BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
                testAdapter.unpair(device);
                break;
            }
            case PARAM_GET_SUPPORTED_PROFILES: {
                testAdapter.getSupportedProfiles();
                break;
            }
            case PARAM_GET_LOCAL_ADDRESS: {
                testAdapter.getLocalAddress();
                break;
            }
            case PARAM_GET_LOCAL_NAME: {
                testAdapter.getLocalName();
                break;
            }
            case PARAM_SET_LOCAL_NAME: {
                checkArgsLength(args, PARAM_SET_LOCAL_NAME, 3);
                testAdapter.setLocalName(args[2]);
                break;
            }
            case PARAM_GET_SCAN_MODE: {
                testAdapter.getScanMode();
                break;
            }
            case PARAM_SET_SCAN_MODE: {
                checkArgsLength(args, PARAM_SET_SCAN_MODE, 3);
                testAdapter.setScanMode(Integer.parseInt(args[2]));
                break;
            }
            case PARAM_SET_DISCOVERABLE_TOUT: {
                checkArgsLength(args, PARAM_SET_DISCOVERABLE_TOUT, 3);
                testAdapter.setDiscoverableTout(Integer.parseInt(args[2]));
                break;
            }
            case PARAM_GET_LOCAL_COD: {
                testAdapter.getLocalCod();
                break;
            }
            case PARAM_GET_REMOTE_NAME: {
                checkArgsLength(args, PARAM_GET_REMOTE_NAME, 3);
                BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
                testAdapter.getRemoteName(device);
                break;
            }
            case PARAM_GET_ALIAS_NAME: {
                checkArgsLength(args, PARAM_GET_ALIAS_NAME, 3);
                BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
                testAdapter.getAliasName(device);
                break;
            }
            case PARAM_SET_ALIAS_NAME: {
                checkArgsLength(args, PARAM_SET_ALIAS_NAME, 4);
                BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
                testAdapter.setAliasName(device, args[3]);
                break;
            }
            case PARAM_GET_REMOTE_COD: {
                checkArgsLength(args, PARAM_GET_REMOTE_COD, 3);
                BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
                testAdapter.getRemoteCod(device);
                break;
            }
            case PARAM_GET_RSSI: {
                checkArgsLength(args, PARAM_GET_RSSI, 3);
                BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
                testAdapter.getRssi(device);
                break;
            }
            case PARAM_GET_LINKKEY: {
                checkArgsLength(args, PARAM_GET_LINKKEY, 3);
                BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
                testAdapter.getLinkKey(device, mContext);
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid adapter parameter: " + para);
            }
        }
    }

    private void runAdapter(String[] args) {
        logd("runAdapter args: " + args);
        runAdapter(args, mTestAdapter, ADAPTER_DEFAULT);
    }

    private void runAdapter1(String[] args) {
        logd("runAdapter1 args: " + args);
        runAdapter(args, mTestAdapter1, ADAPTER_1);
    }

    private void runHfp(String[] args) {
        logd("runHfp args: " + args);
        String para = args[1];
        BluetoothDevice device = getRemoteDevice(args[2]);
        if (PARAM_CONNECT.equalsIgnoreCase(para)) {
            mTestHfp.connect(device);
        } else if (PARAM_DISCONNECT.equalsIgnoreCase(para)) {
            mTestHfp.disconnect(device);
        } else if (PARAM_CONNECT_AUDIO.equalsIgnoreCase(para)) {
            mTestHfp.connectAudio(device);
        } else if (PARAM_DISCONNECT_AUDIO.equalsIgnoreCase(para)) {
            mTestHfp.disconnectAudio(device);
        } else {
            throw new IllegalArgumentException("Invalid hfp parameter: " + para);
        }
    }

    private void runA2dp(String[] args) {
        logd("runA2dp args: " + args);
        String para = args[1].toLowerCase();
        BluetoothDevice device = !para.equals(PARAM_DUMP_MEDIA_PLAYER_LIST)
                && !para.equals(PARAM_START_MEDIA_PLAYER) ?
                getRemoteDevice(args[2]) :
                null;
        switch (para) {
            case PARAM_CONNECT: {
                mTestA2dp.connect(device);
                break;
            }
            case PARAM_DISCONNECT: {
                mTestA2dp.disconnect(device);
                break;
            }
            case PARAM_GET_MEDIA_PLAYER: {
                mTestA2dp.getMediaPlayer(device);
                break;
            }
            case PARAM_SET_MEDIA_PLAYER: {
                checkArgsLength(args, PARAM_SET_MEDIA_PLAYER, 4);
                String mediaPlayer = args[3];
                mTestA2dp.setMediaPlayer(device, mediaPlayer);
                break;
            }
            case PARAM_CLEAR_MEDIA_PLAYER: {
                mTestA2dp.clearMediaPlayer(device);
                break;
            }
            case PARAM_START_MEDIA_PLAYER: {
                checkArgsLength(args, PARAM_START_MEDIA_PLAYER, 3);
                String mediaPlayer = args[2];
                mTestA2dp.startMediaPlayer(mediaPlayer);
                break;
            }
            case PARAM_DUMP_MEDIA_PLAYER_LIST: {
                mTestA2dp.dumpMediaPlayerList();
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid a2dp parameter: " + para);
            }
        }
    }

    private void runPbap(String[] args) {
        logd("runPbap args: " + args);
        String para = args[1];
        BluetoothDevice device = getRemoteDevice(args[2]);
        if (PARAM_DISCONNECT.equalsIgnoreCase(para)) {
            mTestPbap.disconnect(device);
        } else if (PARAM_ALLOW.equalsIgnoreCase(para)) {
            mTestPbap.allowConnection(device);
        } else if (PARAM_REJECT.equalsIgnoreCase(para)) {
            mTestPbap.rejectConnection(device);
        } else {
            throw new IllegalArgumentException("Invalid pbap parameter: " + para);
        }
    }

    private void runHidh(String[] args) {
        logd("runHidh args: " + args);
        String para = args[1];
        BluetoothDevice device = getRemoteDevice(args[2]);
        if (PARAM_CONNECT.equalsIgnoreCase(para)) {
            mTestHidh.connect(device);
        } else if (PARAM_DISCONNECT.equalsIgnoreCase(para)) {
            mTestHidh.disconnect(device);
        } else {
            throw new IllegalArgumentException("Invalid hidh parameter: " + para);
        }
    }

    private void runSpp(String[] args, TestSpp testSpp, int adapterIndex) {
        String para = args[1];
        BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
        if (PARAM_CONNECT.equalsIgnoreCase(para)) {
            testSpp.connect(device);
        } else if (PARAM_DISCONNECT.equalsIgnoreCase(para)) {
            testSpp.disconnect(device);
        } else {
            throw new IllegalArgumentException("Invalid spp parameter: " + para);
        }
    }

    private void runSpp(String[] args) {
        logd("runSpp args: " + args);
        runSpp(args, mTestSpp, ADAPTER_DEFAULT);
    }

    private void runSpp1(String[] args) {
        logd("runSpp1 args: " + args);
        runSpp(args, mTestSpp1, ADAPTER_1);
    }

    private void runGatt(String[] args, TestGatt testGatt, int adapterIndex) {
        String para = args[1];
        BluetoothDevice device = getRemoteDevice(args[2], adapterIndex);
        // TODO
    }

    private void runGatt(String[] args) {
        logd("runGatt args: " + args);
        runGatt(args, mTestGatt, ADAPTER_DEFAULT);
    }

    private void runGatt1(String[] args) {
        logd("runGatt1 args: " + args);
        runGatt(args, mTestGatt1, ADAPTER_1);
    }

    private BluetoothDevice getRemoteDevice(String address) {
        return Adapter.getRemoteDevice(address.toUpperCase(), ADAPTER_1);
    }

    private BluetoothDevice getRemoteDevice(String address, int adapterIndex) {
        return Adapter.getRemoteDevice(address.toUpperCase(), adapterIndex);
    }

    private void checkArgsLength(String[] args, String command, int expectedLen) {
        if (args.length != expectedLen) {
            throw new IllegalArgumentException("Invalid " + command + "parameter: " + args);
        }
    }

    private static void logd(String msg) {
        Log.d(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
