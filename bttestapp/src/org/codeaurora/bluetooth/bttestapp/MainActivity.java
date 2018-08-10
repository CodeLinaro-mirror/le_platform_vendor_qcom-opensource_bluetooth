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
 */

package org.codeaurora.bluetooth.bttestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.SdpMasRecord;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.ParcelUuid;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;

import java.util.ArrayList;

public class MainActivity extends MonkeyActivity {

    private final static String TAG = "MainActivity";

    public static final String PREF_DEVICE = "device";
    public static final String PREF_SERVICES = "services";

    public static final String PICKER_ACTION = "android.bluetooth.devicepicker.action.LAUNCH";
    public static final String PICKER_SELECTED = "android.bluetooth.devicepicker.action.DEVICE_SELECTED";

    private BluetoothA2dpSink ma2dpSink = null;

    BluetoothDevice mDevice;

    ProfileService mProfileService;

    private boolean mIsBound = false;

    private boolean mDiscoveryInProgress = false;

    private ServicesFragment mServicesFragment = null;

    private BluetoothAdapter mBtAdapter;
    private Button mBtnDiscoverService,mBtnSelectDevice, mSinkButton, mSourceButton;
    private static long current_time, switch_time;

    private final BroadcastReceiver mPickerReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.v(TAG, "mPickerReceiver got " + action);

            if (PICKER_SELECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                updateDevice(device);
                unregisterReceiver(this);

                mServicesFragment.removeService(null);
                mServicesFragment.persistServices();
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.v(TAG, "mReceiver got " + action);

            if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!dev.equals(mDevice)) {
                    return;
                }

                Parcelable uuids[] = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

                if (uuids != null) {
                    for (Parcelable uuid : uuids) {
                        Log.v(TAG, "Received UUID: " + uuid.toString());
                        if (BluetoothUuid.PBAP_PSE.equals(uuid)) {
                            Log.d(TAG, "Adding PBAP");
                            mServicesFragment.addService(ServicesFragment.Service.Type.PBAP, null);
                        } else if (BluetoothUuid.Handsfree_AG.equals(uuid)) {
                            Log.d(TAG, "Adding HFP");
                            mServicesFragment.addService(ServicesFragment.Service.Type.HFP, null);
                        } else if (BluetoothUuid.AvrcpTarget.equals(uuid) ||
                                    BluetoothUuid.AudioSource.equals(uuid) ||
                                    BluetoothUuid.AudioSink.equals(uuid)) {
                            Log.d(TAG, "Adding A2DP/AVRCP");
                            mServicesFragment.addService(ServicesFragment.Service.Type.AVRCP, null);
                        }
                        if (BluetoothUuid.AvrcpController.equals(uuid)) {
                            Log.d(TAG, "Adding AVRCP");
                            mServicesFragment.addService(ServicesFragment.Service.Type.AVRCP, null);
                        }
                    }
                }

                mServicesFragment.persistServices();

                if (mDiscoveryInProgress) {
                    Log.v(TAG, "Searching MAS instances");
                    mDevice.sdpSearch(BluetoothUuid.MAS);
                }

            } else if (action.equals(BluetoothDevice.ACTION_SDP_RECORD)){
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!dev.equals(mDevice)) {
                    return;
                }

                ParcelUuid uuid = intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID);
                Log.v(TAG, "Received UUID: " + uuid.toString());
                Log.v(TAG, "expected UUID: " +
                        BluetoothUuid.MAS.toString());
                Log.v(TAG, "mDiscoveryInProgress: " + mDiscoveryInProgress);
                if(uuid.equals(BluetoothUuid.MAS)){
                    SdpMasRecord masrec = intent.getParcelableExtra(BluetoothDevice.EXTRA_SDP_RECORD);
                    Log.v(TAG, "masrec: " + masrec);

                    if (masrec != null) {
                        mProfileService.setMasInstances(masrec);
                        mServicesFragment.addService(ServicesFragment.Service.Type.MAP, masrec);
                    }

                    mServicesFragment.persistServices();

                    mDiscoveryInProgress = false;
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                                   BluetoothDevice.ERROR);

                Log.d(TAG, "bonded state" + bondState);
                boolean sent = true ? bondState == BluetoothDevice.BOND_BONDED : false;

                Toast.makeText(MainActivity.this, "added bond device " + sent, Toast.LENGTH_SHORT).show();
            }
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                Log.d(TAG, " Action " + action + " state :" + state);
                setBTState();
                if (state == BluetoothAdapter.STATE_OFF) {
                    if (!mBtAdapter.isEnabled()) {
                        Log.d(TAG, " Enabling BT... ");
                        mBtAdapter.enable();
                    }
                } else if (state == BluetoothAdapter.STATE_ON) {
                    current_time = System.currentTimeMillis();
                    Log.d(TAG, "Time for BT OFF->ON : " + (current_time - switch_time) + " ms");
                    mSinkButton.setEnabled(true);
                    mSourceButton.setEnabled(true);
                }

            } else if (action.equals(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED)) {
                int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                Log.d(TAG, " Action " + action + ", new A2DP Sink State :" + newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, " BT-OFF to reconnection time = " +
                        (System.currentTimeMillis() - switch_time) +"ms");
                }
            } else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                Log.d(TAG, " Action " + action + ", new A2DP Source State :" + newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, " BT-OFF to reconnection time = " +
                        (System.currentTimeMillis() - switch_time) +"ms");
                }
            }
        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mProfileService = ((ProfileService.LocalBinder) service).getService();
            mProfileService.setDevice(mDevice);
            mServicesFragment.restoreServices();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mProfileService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCreate");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        setContentView(R.layout.activity_main);

        mServicesFragment = (ServicesFragment) getFragmentManager().findFragmentById(
                R.id.services_list);

        Intent intent = new Intent(this, ProfileService.class);
        startService(intent);

        if (bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            mIsBound = true;
        }
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(getApplicationContext(),
                                            ma2dpSinkServiceListener, BluetoothProfile.A2DP_SINK);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.v(TAG, "onStart");

        String addr = getPreferences(MODE_PRIVATE).getString(PREF_DEVICE, null);

        try {
            BluetoothDevice dev = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(addr);
            updateDevice(dev);
        } catch (IllegalArgumentException e) {
            // just leave device unset
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.v(TAG, "onResume");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothDevice.ACTION_SDP_RECORD);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mBtnDiscoverService=(Button) findViewById(R.id.discover_services);
        mBtnSelectDevice=(Button) findViewById(R.id.select_device);
        mBtnSelectDevice=(Button) findViewById(R.id.add_oob_bond_dev);
        mSinkButton = (Button) findViewById(R.id.id_a2dp_sink);
        mSourceButton = (Button) findViewById(R.id.id_a2dp_source);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        setBTState();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.v(TAG, "onPause");

        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.v(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mIsBound) {
            unbindService(mConnection);
        }

        Log.v(TAG, "onDestroy");
    }

    public void onButtonClick(View v) {
        if (v.getId() == R.id.select_device) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(PICKER_SELECTED);
            registerReceiver(mPickerReceiver, filter);

            Intent intent = new Intent(PICKER_ACTION);
            intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);

        } else if (v.getId() == R.id.discover_services) {
            if (mDevice == null || mDiscoveryInProgress) {
                return;
            }

            mServicesFragment.removeService(null);

            Log.v(TAG, "fetching UUIDs");
            mDiscoveryInProgress = mDevice.fetchUuidsWithSdp();

        } else if (v.getId() == R.id.add_oob_bond_dev) {
            if (mDevice != null && mProfileService != null) {
                Log.v(TAG, "add bond device");

                char[] keys = new char[32];
                int[]  dev  = new int[2];

                String fileName = "/etc/bluetooth/remote_dev.txt";
                File   file     = new File(fileName);

                getDevInfoFromFile(file, keys, dev);
                String linkKey = new String(keys);

                Log.d(TAG, "linkKey " + linkKey + " keyType " + dev[0] + " pinLen " + dev[1]);
                mProfileService.addOutOfBandBondDevice(mDevice, linkKey, dev[0], dev[1]);
            }
        }
    }

    public void showCoveArtActivity(View v) {
        Log.i(TAG," showCoveArtActivity");
        startActivity(new Intent(this,AvrcpCoverArtActivity.class));
    }

    public void showDipTestActivity(View v) {
        Log.i(TAG," showDipTestActivity");
        startActivity(new Intent(this, DipTestActivity.class));
    }

    private BluetoothProfile.ServiceListener ma2dpSinkServiceListener =
                              new BluetoothProfile.ServiceListener() {

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothA2dpSink.A2DP_SINK) {
                Log.v(TAG, "onServiceDisconnected ");
                ma2dpSink = null;
            }
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP_SINK) {
                Log.v(TAG, "onServiceConnected ");
                ma2dpSink = (BluetoothA2dpSink) proxy;
            }
        }
    };

    public void onToggleClicked(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();
        Log.v(TAG, "onToggleClicked is_on: " + on);
    }

    private void updateDevice(BluetoothDevice device) {
        SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE).edit();

        TextView name = (TextView) findViewById(R.id.device_name);
        TextView addr = (TextView) findViewById(R.id.device_address);

        if (device == null) {
            name.setText(R.string.blank);
            addr.setText(R.string.blank);

            prefs.remove(PREF_DEVICE);
        } else {
            Intent intent = new Intent(BluetoothConnectionReceiver.ACTION_NEW_BLUETOOTH_DEVICE);
            intent.putExtra(BluetoothConnectionReceiver.EXTRA_DEVICE_ADDRESS, device.getAddress());
            sendBroadcast(intent);

            name.setText(device.getName());
            addr.setText(device.getAddress());

            prefs.putString(PREF_DEVICE, device.getAddress());
        }

        prefs.commit();

        mDevice = device;

        if (mProfileService != null) {
            mProfileService.setDevice(mDevice);
        }
    }

    public void onRadioButtonClicked(View v) {
        switch_time = System.currentTimeMillis();
        boolean isA2dpSinkEnabled = SystemProperties.getBoolean("persist.service.bt.a2dp.sink",
                false);

        // Switch role to A2DP Source
        if (v.getId() == R.id.id_a2dp_source) {
            if (!isA2dpSinkEnabled) {
                Log.d(TAG, "Already in Source role, ignore user action ignored");
                return;
            }
            Log.d(TAG, "Switch role to A2DP Source");
            SystemProperties.set("persist.service.bt.a2dp.sink", false + "");
            SystemProperties.set("persist.service.bt.avrcp.controller", false + "");

        // Switch role to A2DP Sink
        } else if (v.getId() == R.id.id_a2dp_sink) {
            if (isA2dpSinkEnabled) {
                Log.d(TAG, "Already in A2DP Sink role, user action ignored");
                return;
            }
            Log.d(TAG, "Switch role to A2DP Sink");
            SystemProperties.set("persist.service.bt.a2dp.sink", true + "");
            SystemProperties.set("persist.service.bt.avrcp.controller", true + "");
        }
        mSinkButton.setEnabled(false);
        mSourceButton.setEnabled(false);

        // Turn BT OFF and ON in order to reflect property change
        if (mBtAdapter.isEnabled())
            mBtAdapter.disable();
        else
            mBtAdapter.enable();
    }

    private void setBTState() {
        boolean isBtEnabled = mBtAdapter.isEnabled();
        if (isBtEnabled) {
            mBtnDiscoverService.setEnabled(true);
            mBtnSelectDevice.setEnabled(true);
        } else {
            mBtnDiscoverService.setEnabled(false);
            mBtnSelectDevice.setEnabled(false);
        }
    }

    private void getDevInfoFromFile(File file, char[] keys, int[] dev) {
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
            BufferedReader br        = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("linkkey:")) {
                    String linkKey = line.substring(8, line.length());
                    for (int i = 0 ; i < linkKey.length(); i++) {
                        keys[i] = linkKey.charAt(i);
                    }

                    keys = linkKey.toCharArray();
                    Log.d(TAG, "linkKey =" + linkKey);
                } else if (line.contains("keytype:")) {
                    try {
                        dev[0] = Integer.parseInt(line.substring(8, line.length()));
                        Log.d(TAG, "keyType = " + dev[0]);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else if (line.contains("pinlen:")) {
                    try {
                        dev[1] = Integer.parseInt(line.substring(7, line.length()));
                        Log.d(TAG, "pinLen = " + dev[1]);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
