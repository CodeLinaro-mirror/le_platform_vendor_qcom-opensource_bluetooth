/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.newbttestapp;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalServerSocket;
import android.util.Log;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

class TestVoIP {

    private static final String TAG = "TestVoIP";
    private static boolean DEBUG = true;

    private static final int SAMPLES_PER_FRAME = 1024;
    private AudioManager mAudioManager;

    private final static String VOIP_TEST_SERVER                = "qcom.voip.test.server";

    private static volatile UplinkThread mUplinkThread          = null;
    private static volatile DownlinkThread mDownlinkThread      = null;

    private static volatile LocalSocket mUplinkSocket           = null;
    private static volatile LocalSocket mDownlinkSocket         = null;
    private static volatile LocalServerSocket mServerSocket     = null;

    TestVoIP(Context context) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public boolean startVoIPSimulate() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        createServerSocket();
        startUplinkThread();
        startDownlinkThread();

        return true;
    }

    public boolean stopVoIPSimulate() {
        Log.d(TAG, "stopVoIPSimulate!");
        stopUplinkThread();
        stopDownlinkThread();
        mAudioManager.setMode(AudioManager.MODE_NORMAL);

        return true;
    }

    public void startBluetoothSco() {
        if (mAudioManager != null) {
            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            mAudioManager.startBluetoothSco();
            mAudioManager.setBluetoothScoOn(true);
        } else {
            Log.e(TAG, "mAudioManager is null!");
        }
    }

    public void stopBluetoothSco() {
        if (mAudioManager != null) {
            mAudioManager.setMode(AudioManager.MODE_NORMAL);
            mAudioManager.stopBluetoothSco();
            mAudioManager.setBluetoothScoOn(false);
        } else {
            Log.e(TAG, "mAudioManager is null!");
        }
    }

    private final boolean createServerSocket() {
        if (DEBUG) Log.d(TAG, "initUplinkSocket");
        boolean createSocketOK = false;

        try {
            mServerSocket = new LocalServerSocket(VOIP_TEST_SERVER);
        } catch (java.io.IOException e) {
            Log.e(TAG, "cant create server socket: " + e);
        }
        if (createSocketOK) {
            if (DEBUG) Log.d(TAG, "Succeed to create server socket ");
        } else {
            Log.e(TAG, "Error to create server socket");
        }
        return createSocketOK;
    }

    private final boolean initUplinkSocket() {
        if (DEBUG) Log.d(TAG, "initUplinkSocket");
        boolean initSocketOK = false;

        try {
            LocalSocketAddress locSockAddr = new LocalSocketAddress(VOIP_TEST_SERVER);
            mUplinkSocket = new LocalSocket();
            mUplinkSocket.connect(locSockAddr);
            initSocketOK = true;
        } catch (java.io.IOException e) {
            Log.e(TAG, "cant connect: " + e);
        }
        if (initSocketOK) {
            if (DEBUG) Log.d(TAG, "Succeed to create Uplink socket ");
        } else {
            Log.e(TAG, "Error to create Uplink socket");
        }
        return initSocketOK;
    }

    private final boolean initDownlinkSocket() {
        if (DEBUG) Log.d(TAG, "initDownlinkSocket");
        boolean initSocketOK = false;

        try {
            mDownlinkSocket = mServerSocket.accept();
            initSocketOK = true;
        } catch (java.io.IOException e) {
            Log.e(TAG, "cant accept: " + e);
        }
        if (initSocketOK) {
            if (DEBUG) Log.d(TAG, "Succeed to init Downlink socket ");
        } else {
            Log.e(TAG, "Error to init Downlink socket");
        }
        return initSocketOK;
    }

    private final synchronized void closeUplinkSocket() {
        Log.d(TAG, "Close Uplink Socket : ");
        if (mUplinkSocket != null) {
            try {
                mUplinkSocket.close();
                mUplinkSocket = null;
            } catch (IOException e) {
                Log.e(TAG, "Close Uplink Socket error: " + e.toString());
            }
        }
    }

    private final synchronized void closeDownlinkSocket() {
        Log.d(TAG, "Close Downlink Socket : ");
        if (mDownlinkSocket != null) {
            try {
                mDownlinkSocket.close();
                mDownlinkSocket = null;
            } catch (IOException e) {
                Log.e(TAG, "Close Downlink Socket error: " + e.toString());
            }
        }

        if (mServerSocket != null) {
            try {
                mServerSocket.close();
                mServerSocket = null;
            } catch (IOException e) {
                Log.e(TAG, "Close Server Socket error: " + e.toString());
            }
        }
    }

    private class UplinkThread extends Thread {

        private boolean stopped = false;
        //private boolean IntExit = false;
        private OutputStream mUplinkOutputStream = null;
        private AudioRecord mRecord = null;
        //ByteBuffer RawDataBuffer = ByteBuffer.allocate(SAMPLES_PER_FRAME*2);

        @Override
        public void run() {
            if (mUplinkSocket == null) {
                if (!initUplinkSocket()) {
                    return;
                }
            }
            if (mUplinkSocket != null && mUplinkSocket.isConnected()) {
                try {
                    mUplinkOutputStream = mUplinkSocket.getOutputStream();
                } catch (IOException ex) {
                    Log.w(TAG, "Handled mUplinkOutputStream exception: " + ex.toString());
                }
            } else {
                Log.w(TAG, "UplinkThread: Uplink Socket is not connected: " +  mUplinkSocket);
                return;
            }

            int minBufferSize = AudioRecord.getMinBufferSize(8000,
                              AudioFormat.CHANNEL_IN_MONO,
                              AudioFormat.ENCODING_PCM_16BIT);

            while (!stopped) {
                // Add some capture logic here
                if (mRecord == null) {
                    mRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                                              8000,
                                              AudioFormat.CHANNEL_IN_MONO,
                                              AudioFormat.ENCODING_PCM_16BIT,
                                              minBufferSize * 4);
                    if (mRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                        Log.e(TAG, "AudioRecord initialize fail!");
                        break;
                    }
                    mRecord.startRecording();
                }

                byte[] buffer = new byte[SAMPLES_PER_FRAME * 2];
                int ret = mRecord.read(buffer, 0, buffer.length);
                if (ret == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Error ERROR_INVALID_OPERATION");
                } else if (ret == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Error ERROR_BAD_VALUE");
                } else {
                    Log.d("TAG", "Audio captured: " + buffer.length);
                    try {
                        mUplinkOutputStream.write(buffer, 0, buffer.length);
                    } catch (IOException ex) {
                        break;
                    }
                }
            }

            if (mRecord != null) {
                mRecord.stop();
                mRecord.release();
            }

            closeUplinkSocket();
            Log.d(TAG, "uplink thread exited");
        }

        void shutdown() {
            stopped = true;
            interrupt();
        }
    }

    private class DownlinkThread extends Thread {

        private boolean stopped = false;
        //private boolean IntExit = false;
        private InputStream mDownlinkInputStream = null;
        private AudioTrack mTrack = null;

        @Override
        public void run() {
            if (mDownlinkSocket == null) {
                if (!initDownlinkSocket()) {
                    return;
                }
            }
            if (mDownlinkSocket != null && mDownlinkSocket.isConnected()) {
                try {
                    mDownlinkInputStream = mDownlinkSocket.getInputStream();
                } catch (IOException ex) {
                    Log.w(TAG, "Handled mDownlinkInputStream exception: " + ex.toString());
                }
            } else {
                Log.w(TAG, "DownlinkThread: Downlink Socket is not connected: " +  mDownlinkSocket);
                return;
            }

            int bufferSizeInBytes = AudioTrack.getMinBufferSize(8000,
                                                                AudioFormat.CHANNEL_OUT_MONO,
                                                                AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSizeInBytes == AudioTrack.ERROR_BAD_VALUE) {
                Log.w(TAG, "Invalid parameter!");
                return;
            }

            while (!stopped) {
                // Add some playback logic here
                if (mTrack == null) {
                    mTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000,
                                            AudioFormat.CHANNEL_OUT_MONO,
                                            AudioFormat.ENCODING_PCM_16BIT,
                                            bufferSizeInBytes,
                                            AudioTrack.MODE_STREAM);
                    if (mTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                        Log.w(TAG, "AudioTrack initialize fail!");
                        break;
                    }
                }

                try {
                    byte[] buffer = new byte[SAMPLES_PER_FRAME * 2];
                    int count = mDownlinkInputStream.read(buffer);

                    if (mTrack.write(buffer, 0, count) != count) {
                        Log.w(TAG, "Could not write all the samples to the audio device!");
                    }

                    mTrack.play();
                    Log.w(TAG, "OK, Played " + count + " bytes!");
                } catch (IOException ex) {
                    Log.w(TAG, "Handled  exception: " + ex.toString());
                }
            }

            if (mTrack != null) {
                mTrack.stop();
                mTrack.release();
            }

            closeDownlinkSocket();
            Log.d(TAG, "downlink thread exited");
        }

        void shutdown() {
            stopped = true;
            interrupt();
        }
    }

    private void startUplinkThread() {
        Log.d(TAG, "startUplinkThread");

        if (mUplinkThread != null) {
            try {
                mUplinkThread.shutdown();
                mUplinkThread.join();
                mUplinkThread = null;
            } catch (InterruptedException ex) {
                Log.w(TAG, "mUplinkThread close error" + ex);
                mUplinkThread = null;
            }
        }

        if (mUplinkThread == null) {
            mUplinkThread = new UplinkThread();
            mUplinkThread.setName("VoIPSimuUplinkThread");
            mUplinkThread.start();
        }
    }

    private void startDownlinkThread() {
        Log.d(TAG, "startDownlinkThread");

        if (mDownlinkThread != null) {
            try {
                mDownlinkThread.shutdown();
                mDownlinkThread.join();
                mDownlinkThread = null;
            } catch (InterruptedException ex) {
                Log.w(TAG, "mDownlinkThread close error" + ex);
                mDownlinkThread = null;
            }
        }

        if (mDownlinkThread == null) {
            mDownlinkThread = new DownlinkThread();
            mDownlinkThread.setName("VoIPSimuDownlinkThread");
            mDownlinkThread.start();
        }
    }

    private void stopUplinkThread() {
        Log.d(TAG, "stopUplinkThread");

        if (mUplinkThread != null) {
            try {
                mUplinkThread.shutdown();
                mUplinkThread.join();
                mUplinkThread = null;
            } catch (InterruptedException ex) {
                Log.w(TAG, "mUplinkThread close error" + ex);
                mUplinkThread = null;
            }
        }
    }

    private void stopDownlinkThread() {
        Log.d(TAG, "stopDownlinkThread");

        if (mDownlinkThread != null) {
            try {
                mDownlinkThread.shutdown();
                mDownlinkThread.join();
                mDownlinkThread = null;
            } catch (InterruptedException ex) {
                Log.w(TAG, "mDownlinkThread close error" + ex);
                mDownlinkThread = null;
            }
        }
    }

}
