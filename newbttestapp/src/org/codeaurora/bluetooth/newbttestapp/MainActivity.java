/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *            notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *            notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *        * Neither the name of The Linux Foundation nor
 *            the names of its contributors may be used to endorse or promote
 *            products derived from this software without specific prior written
 *            permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Changes from Qualcomm Innovation Center are provided under the following license:
 *
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.util.IndentingPrintWriter;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import org.codeaurora.bluetooth.newbttestapp.util.Logger;

public class MainActivity extends MonkeyActivity {

    private final static String TAG = "MainActivity";

    private Context mContext;
    private AdapterMain mAdapterMain;
    private HfpMain mHfpMain;
    private A2dpMain mA2dpMain;
    private HidhMain mHidhMain;
    private PbapMain mPbapMain;
    private SppMain mSppMain;
    private TestShellCommand mShellCommand;

    private void initMain(Context context) {
        mAdapterMain = new AdapterMain(context);
        mHfpMain = new HfpMain(context);
        mA2dpMain = new A2dpMain(context);
        mHidhMain = new HidhMain(context);
        mPbapMain = new PbapMain(context);
        mSppMain = new SppMain(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.v(TAG, "onCreate");
        mContext = getApplicationContext();
        // [TODO] Restore later when UI is added into test app
        // initMain(mContext);
        mShellCommand = new TestShellCommand(mContext);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Logger.v(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.v(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.v(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.v(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.v(TAG, "onDestroy");
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        dumpInternal(fd, writer, args);
    }

    private void dumpInternal(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            writer.println("Permission Denial: can't dump NewBTTestApp from from pid="
                    + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid()
                    + " without permission " + android.Manifest.permission.DUMP);
            return;
        }

        try (IndentingPrintWriter pw = new IndentingPrintWriter(writer)) {
            dumpIndenting(fd, pw, args);
        }
    }

    private void dumpIndenting(FileDescriptor fd, IndentingPrintWriter writer, String[] args) {
        if ("--help".equals(args[0])) {
            showDumpHelp(writer);
        } else {
            execShellCmd(args, writer);
        }
    }

    private void execShellCmd(String[] args, IndentingPrintWriter writer) {
        mShellCommand.exec(args, writer);
    }

    private TestShellCommand newTestShellCommand() {
        return new TestShellCommand(mContext);
    }

    private void showDumpHelp(IndentingPrintWriter writer) {
        writer.println("NewBTTestApp dump usage:");
        writer.println("--help");
        writer.println("\t  shows this help");
        writer.println("-h");
        writer.println("\t  shows commands usage (NOTE: commands are not available on USER builds");
        writer.println("[ANYTHING ELSE]");
        writer.println("\t  runs the given command (use --h to see the available commands)");
    }
}
