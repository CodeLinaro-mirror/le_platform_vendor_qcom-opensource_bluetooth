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
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.OobData;
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.io.IOException;
import java.util.concurrent.Executors;

import org.codeaurora.bluetooth.bttestapp.util.Logger;

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

    private Button mBtnDiscoverService, mBtnSelectDevice, mSinkButton, mSourceButton, mBtnAddOobBond,
            mBtnGetLinkKey, mBtnReadLocalOobDataButton, mBtnCreateOobBond, mBtnLoadRemoteOobDataButton,
            mBtnRemoveBond;

    private static long current_time, switch_time;

    private Eir128bitUUIDSample EirSample1 = null,EirSample2 = null,EirSample3 = null;
    private String mLinkKey;
    private int mKeyType = -1;

    private final BroadcastReceiver mPickerReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Logger.v(TAG, "mPickerReceiver got " + action);

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

            Logger.v(TAG, "mReceiver got " + action);

            if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!dev.equals(mDevice)) {
                    return;
                }

                Parcelable uuids[] = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

                if (uuids != null) {
                    for (Parcelable uuid : uuids) {
                        Logger.v(TAG, "Received UUID: " + uuid.toString());
                        if (BluetoothUuid.PBAP_PSE.equals(uuid)) {
                            Logger.d(TAG, "Adding PBAP");
                            mServicesFragment.addService(ServicesFragment.Service.Type.PBAP, null);
                        } else if (BluetoothUuid.HFP_AG.equals(uuid)) {
                            Logger.d(TAG, "Adding HFP");
                            mServicesFragment.addService(ServicesFragment.Service.Type.HFP, null);
                        } else if (BluetoothUuid.AVRCP_TARGET.equals(uuid) ||
                                    BluetoothUuid.A2DP_SOURCE.equals(uuid) ||
                                    BluetoothUuid.A2DP_SINK.equals(uuid)) {
                            Logger.d(TAG, "Adding A2DP/AVRCP");
                            mServicesFragment.addService(ServicesFragment.Service.Type.AVRCP, null);
                        }
                        if (BluetoothUuid.AVRCP_CONTROLLER.equals(uuid)) {
                            Logger.d(TAG, "Adding AVRCP");
                            mServicesFragment.addService(ServicesFragment.Service.Type.AVRCP, null);
                        }
                    }
                }

                mServicesFragment.persistServices();

                if (mDiscoveryInProgress) {
                    Logger.v(TAG, "Searching MAS instances");
                    mDevice.sdpSearch(BluetoothUuid.MAS);
                }

            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                                   BluetoothDevice.ERROR);

                Logger.d(TAG, "bonded state" + bondState);
                boolean sent = true ? bondState == BluetoothDevice.BOND_BONDED : false;

                Toast.makeText(MainActivity.this, "added bond device " + sent, Toast.LENGTH_SHORT).show();
            } else if (action.equals(BluetoothDevice.ACTION_SDP_RECORD)){
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!dev.equals(mDevice)) {
                    return;
                }

                ParcelUuid uuid = intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID);
                Logger.v(TAG, "Received UUID: " + uuid.toString());
                Logger.v(TAG, "expected UUID: " +
                        BluetoothUuid.MAS.toString());
                Logger.v(TAG, "mDiscoveryInProgress: " + mDiscoveryInProgress);
                if (uuid.equals(BluetoothUuid.MAS)) {
                    SdpMasRecord masrec = intent.getParcelableExtra(BluetoothDevice.EXTRA_SDP_RECORD);
                    Logger.v(TAG, "masrec: " + masrec);

                    if (masrec != null) {
                        mServicesFragment.addService(ServicesFragment.Service.Type.MAP, masrec);
                    }

                    mServicesFragment.persistServices();
                    mDiscoveryInProgress = false;
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                                   BluetoothDevice.ERROR);

                Logger.d(TAG, "bonded state" + bondState);
                boolean sent = true ? bondState == BluetoothDevice.BOND_BONDED : false;

                Toast.makeText(MainActivity.this, "added bond device " + sent, Toast.LENGTH_SHORT).show();
            }
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                Logger.d(TAG, " Action " + action + " state :" + state);
                setBTState();
                if (state == BluetoothAdapter.STATE_OFF) {
                    if (!mBtAdapter.isEnabled()) {
                        Logger.d(TAG, " Enabling BT... ");
                        mBtAdapter.enable();
                    }
                } else if (state == BluetoothAdapter.STATE_ON) {
                    current_time = System.currentTimeMillis();
                    Logger.d(TAG, "Time for BT OFF->ON : " + (current_time - switch_time) + " ms");
                    mSinkButton.setEnabled(true);
                    mSourceButton.setEnabled(true);
                }

            } else if (action.equals(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED)) {
                int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                Logger.d(TAG, " Action " + action + ", new A2DP Sink State :" + newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Logger.d(TAG, " BT-OFF to reconnection time = " +
                        (System.currentTimeMillis() - switch_time) +"ms");
                }
            } else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                Logger.d(TAG, " Action " + action + ", new A2DP Source State :" + newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Logger.d(TAG, " BT-OFF to reconnection time = " +
                        (System.currentTimeMillis() - switch_time) +"ms");
                }
            } else if (action.equals(ProfileService.ACTION_CUSTOM_ACTION_RESULT)) {
                String address = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                mLinkKey = intent.getStringExtra(ProfileService.KEY_LINK_KEY);
                mKeyType = intent.getIntExtra(ProfileService.KEY_LINK_KEY_TYPE, -1);

                if (!mDevice.getAddress().equals(address)) {
                    Logger.w(TAG, "Device not match");
                    Toast.makeText(MainActivity.this, "Device not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isValidLinkKey()) {
                    Logger.d(TAG, " Action " + action + " Remote device = " + address + " linkKey= " + mLinkKey + " keyType= " + mKeyType);
                    Toast.makeText(MainActivity.this, "linkKey = " + mLinkKey +
                                   " keyType = " + mKeyType , Toast.LENGTH_LONG).show();
                } else {
                    Logger.e(TAG, " can not find linkkey ");
                    Toast.makeText(MainActivity.this, "can not find linkkey ", Toast.LENGTH_LONG).show();
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

        Logger.v(TAG, "onCreate");

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

        Logger.v(TAG, "onStart");

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

        Logger.v(TAG, "onResume");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothDevice.ACTION_SDP_RECORD);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(ProfileService.ACTION_CUSTOM_ACTION_RESULT);
        registerReceiver(mReceiver, filter);

        if (BluetoothAdapter.getDefaultAdapter().isEnabled() == false) {
            Button b = (Button) findViewById(R.id.discover_services);
            b.setEnabled(false);

            b = (Button) findViewById(R.id.select_device);
            b.setEnabled(false);
        }

        mBtnDiscoverService=(Button) findViewById(R.id.discover_services);
        mBtnSelectDevice=(Button) findViewById(R.id.select_device);
        mBtnAddOobBond=(Button) findViewById(R.id.add_oob_bond_dev);
        mBtnReadLocalOobDataButton = (Button) findViewById(R.id.read_local_oob_data);
        mBtnCreateOobBond = (Button) findViewById(R.id.create_oob_bond);
        mBtnLoadRemoteOobDataButton = (Button) findViewById(R.id.load_remote_oob_data);
        mBtnRemoveBond = (Button) findViewById(R.id.remove_bond);
        mSinkButton = (Button) findViewById(R.id.id_a2dp_sink);
        mSourceButton = (Button) findViewById(R.id.id_a2dp_source);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        setBTState();

        if (mBtAdapter.isEnabled()) {
            unregister128Uuid();
            register128Uuid();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Logger.v(TAG, "onPause");

        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Logger.v(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mIsBound) {
            unbindService(mConnection);
        }

        Logger.v(TAG, "onDestroy");
    }


    void register128Uuid() {
        Logger.v(TAG, "register128Uuid");
        if (EirSample1 == null)
        {
            Logger.v(TAG, "128UUID_SAMPLE1, 00112233-4455-6677-8899-aabbccddeeff");
            EirSample1  = new Eir128bitUUIDSample("128UUID_SAMPLE1",  "00112233-4455-6677-8899-aabbccddeeff");
            EirSample1.start();
        }

        if (EirSample2 == null)
        {
            Logger.v(TAG, "128UUID_SAMPLE2, 00000000-2222-2222-3333-555555555559");
            EirSample2    = new Eir128bitUUIDSample("128UUID_SAMPLE2",  "00000000-2222-2222-3333-555555555559");
            EirSample2.start();
        }

        if (EirSample3 == null)
        {
            Logger.v(TAG, "128UUID_SAMPLE3, 11112222-8877-aabb-ccdd-666666666666");
            EirSample3 = new Eir128bitUUIDSample("128UUID_SAMPLE3",  "11112222-8877-aabb-ccdd-666666666666");
            EirSample3.start();
        }

    }

    void unregister128Uuid() {
        Logger.v(TAG, "unregister128Uuid");
        if (EirSample1 != null) {
            EirSample1.cancel();
            EirSample1 = null;
        }

        if (EirSample2 != null) {
            EirSample2.cancel();
            EirSample2 = null;
        }

        if (EirSample3 != null) {
            EirSample3.cancel();
            EirSample3 = null;
        }
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
                Logger.e(TAG, "mDevice " + mDevice + " mDiscoveryInProgress " + mDiscoveryInProgress);
                return;
            }

            mServicesFragment.removeService(null);

            Logger.v(TAG, "fetching UUIDs");
            mDiscoveryInProgress = mDevice.fetchUuidsWithSdp();

        } else if (v.getId() == R.id.read_local_oob_data) {
            Logger.v(TAG, "read local oob data");
            mBtAdapter.generateLocalOobData(BluetoothDevice.TRANSPORT_BREDR, Executors.newSingleThreadExecutor(), mOobDataCallback);

        } else if (v.getId() == R.id.create_oob_bond) {
           Logger.v(TAG, "click create_oob_bond");
           if (mDevice != null && mProfileService != null) {
                Logger.v(TAG, "start oob bonding");
                String fileName = "/data/misc/bluedroid/remote.key";
                File file = new File(fileName);

                if (file.exists()) {
                    getRemoteOobFromFile(file, true);
                } else {
                    Logger.d(TAG, "No remote oob data, createbond instead");
                    mDevice.createBond();
                }
            } else {
               Logger.v(TAG, mDevice == null ? "mDevice is null" : "mDevice is not null");
               Logger.v(TAG, mProfileService == null ? "mProfileService is null" : "mProfileService is not null");
            }
        } else if (v.getId() == R.id.load_remote_oob_data) {
            Logger.v(TAG, "click load_remote_oob_data");
            if (mDevice != null && mProfileService != null) {
                 Logger.v(TAG, "start load remote's oob data");
                 String fileName = "/data/misc/bluedroid/remote.key";
                 File file = new File(fileName);

                 if (file.exists()) {
                     getRemoteOobFromFile(file, false);
                 } else {
                     Logger.d(TAG, "No remote oob data");
                 }
             } else {
                Logger.v(TAG, mDevice == null ? "mDevice is null" : "mDevice is not null");
                Logger.v(TAG, mProfileService == null ? "mProfileService is null" : "mProfileService is not null");
             }

        } else if (v.getId() == R.id.remove_bond) {
            Logger.v(TAG, "click remove_bond");
            if (mDevice != null && mProfileService != null) {
                Logger.v(TAG, "start remove bond");
                mDevice.removeBond();
            } else {
                Logger.v(TAG, mDevice == null ? "mDevice is null" : "mDevice is not null");
                Logger.v(TAG, mProfileService == null ? "mProfileService is null" : "mProfileService is not null");
            }
        }
    }

    private BluetoothAdapter.OobDataCallback mOobDataCallback = new BluetoothAdapter.OobDataCallback() {
        @Override
        public void onOobData(int transport, OobData oobData) {
            Logger.v(TAG, "onOobData transport = " + transport);
            Logger.v(TAG, oobData.toString());
            writeLocalOobToFile(oobData);
        }
        public void onError(int errorCode) {
            Logger.v(TAG, "errorCode = " + errorCode);
        }
    };

    public void showCoveArtActivity(View v) {
        Log.i(TAG," showCoveArtActivity");
        startActivity(new Intent(this,AvrcpCoverArtActivity.class));
    }

    private BluetoothProfile.ServiceListener ma2dpSinkServiceListener =
                              new BluetoothProfile.ServiceListener() {

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothA2dpSink.A2DP_SINK) {
                Logger.v(TAG, "onServiceDisconnected ");
                ma2dpSink = null;
            }
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP_SINK) {
                Logger.v(TAG, "onServiceConnected ");
                ma2dpSink = (BluetoothA2dpSink) proxy;
            }
        }
    };

    public void onToggleClicked(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();
        Logger.v(TAG, "onToggleClicked is_on: " + on);
    }

    public void showHidHost(View v) {
        Log.i(TAG," showHidHost");
        startActivity(new Intent(this, HidTestApp.class));
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
        /*
            Intent intent = new Intent(BluetoothConnectionReceiver.ACTION_NEW_BLUETOOTH_DEVICE);
            intent.putExtra(BluetoothConnectionReceiver.EXTRA_DEVICE_ADDRESS, device.getAddress());
            sendBroadcast(intent);
         */
            BluetoothConnectionReceiver.notifyObserversDeviceChanged(device);
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
                Logger.d(TAG, "Already in Source role, ignore user action ignored");
                return;
            }
            Logger.d(TAG, "Switch role to A2DP Source");
            SystemProperties.set("persist.service.bt.a2dp.sink", false + "");
            SystemProperties.set("persist.service.bt.avrcp.controller", false + "");

        // Switch role to A2DP Sink
        } else if (v.getId() == R.id.id_a2dp_sink) {
            if (isA2dpSinkEnabled) {
                Logger.d(TAG, "Already in A2DP Sink role, user action ignored");
                return;
            }
            Logger.d(TAG, "Switch role to A2DP Sink");
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
            mBtnAddOobBond.setEnabled(true);
            mBtnReadLocalOobDataButton.setEnabled(true);
            mBtnCreateOobBond.setEnabled(true);
            mBtnLoadRemoteOobDataButton.setEnabled(true);
            mBtnRemoveBond.setEnabled(true);
        } else {
            mBtnDiscoverService.setEnabled(false);
            mBtnSelectDevice.setEnabled(false);
            mBtnAddOobBond.setEnabled(false);
            mBtnReadLocalOobDataButton.setEnabled(false);
            mBtnCreateOobBond.setEnabled(false);
            mBtnLoadRemoteOobDataButton.setEnabled(false);
            mBtnRemoveBond.setEnabled(false);
        }
    }

    private boolean isValidLinkKey() {
        boolean ret = ((mLinkKey != null && mLinkKey.isEmpty()) || mKeyType < 0) ? false : true;
        return ret;
    }

    private static String byteArrayToString(byte[] valueBuf) {
        StringBuilder sb = new StringBuilder();
        if (valueBuf != null) {
            for (int idx = 0; idx < valueBuf.length; idx++) {
                if (idx != 0) {
                    sb.append(" ");
                }
                sb.append(String.format("%02x", valueBuf[idx]));
            }
        }
        return sb.toString();
    }

    private static byte[] stringToByteArray(String String) {
        String[] str = String.split(" ");
        int len = str.length;
        byte[] bytes =  new byte[len];

        for (int i = 0; i < len; i++) {
            bytes[i] = (byte) Integer.parseInt(str[i],16);
        }

        return bytes;
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
                    Logger.d(TAG, "linkKey =" + linkKey);
                } else if (line.contains("keytype:")) {
                    try {
                        dev[0] = Integer.parseInt(line.substring(8, line.length()));
                        Logger.d(TAG, "keyType = " + dev[0]);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else if (line.contains("pinlen:")) {
                    try {
                        dev[1] = Integer.parseInt(line.substring(7, line.length()));
                        Logger.d(TAG, "pinLen = " + dev[1]);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else {
                    Logger.d(TAG, "no linkkey info in file");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeLocalOobToFile(OobData oob) {
        String file_local = "/data/misc/bluedroid/local.key";
        File localOobDataFile = new File(file_local);

        if (localOobDataFile.exists()){
            localOobDataFile.delete();
            Logger.d(TAG, "Old local OOB data file exists, delete it");
        }
        try {
            localOobDataFile.createNewFile();
            Logger.d(TAG, "creat new file to store local OOB data");

            FileWriter writer = new FileWriter(localOobDataFile);

            writer.write(byteArrayToString(oob.getClassicLength()) + "\n"
                + byteArrayToString(oob.getDeviceAddressWithType()) + "\n"
                + byteArrayToString(oob.getConfirmationHash()) + "\n"
                + byteArrayToString(oob.getRandomizerHash()) + "\n"
                + byteArrayToString(oob.getConfirmationExtendedHash()) + "\n"
                + byteArrayToString(oob.getRandomizerExtendedHash()));

            writer.flush();
            writer.close();

         } catch (Exception e){
            Log.e(TAG, "Error write OOB data", e);
            e.printStackTrace();
         }
    }

    private void getRemoteOobFromFile(File file, boolean isStartBonding) {
        Log.i(TAG," getRemoteOobFromFile");
        try {

            byte[] classicLength = new byte[2];
            byte[] deviceAddress = new byte[7];
            byte[] c192 = new byte[16];
            byte[] r192 = new byte[16];
            byte[] c256 = new byte[16];
            byte[] r256 = new byte[16];
            byte[] no_data = new byte[16];

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";

            if ((line = br.readLine()) != null)
                classicLength = stringToByteArray(line);
            if ((line = br.readLine()) != null)
                deviceAddress = stringToByteArray(line);
            if ((line = br.readLine()) != null)
                c192 = stringToByteArray(line);
            if ((line = br.readLine()) != null)
                r192 = stringToByteArray(line);
            if ((line = br.readLine()) != null)
                c256 = stringToByteArray(line);
            if ((line = br.readLine()) != null)
                r256 = stringToByteArray(line);

            br.close();

            OobData oob192 = new OobData(classicLength,deviceAddress,c192,r192,no_data,no_data);
            OobData oob256 = new OobData(classicLength,deviceAddress,c256,r256,no_data,no_data);
            Logger.v(TAG, oob192.toString());
            Logger.v(TAG, oob256.toString());
            if (isStartBonding)
                mDevice.createBondOutOfBand(BluetoothDevice.TRANSPORT_BREDR, oob192, oob256); // transport:1 BR/EDR
            else
                mDevice.loadRemoteOobData(BluetoothDevice.TRANSPORT_BREDR, oob192, oob256);

        } catch (Exception e) {
            Log.e(TAG, "Error reading part data", e);
            e.printStackTrace();
        }
    }

    private class Eir128bitUUIDSample extends Thread {

       private final BluetoothServerSocket mmServerSocket;

       public Eir128bitUUIDSample(String serviceName, String uuid) {

           BluetoothServerSocket tmp = null;
           try {
               tmp = mBtAdapter.listenUsingRfcommWithServiceRecord(serviceName, UUID.fromString(uuid));
           } catch (IOException e) { }
           mmServerSocket = tmp;
       }
       public void run() {
           BluetoothSocket socket = null;
           while (true) {
               try {
                   socket = mmServerSocket.accept();
               } catch (IOException e) {
                   break;
               }
               if (socket != null) {
                   try{
                       mmServerSocket.close();
                   }catch(IOException e) {
                       e.printStackTrace();
                   }
                break;
              }
           }
       }
       public void cancel() {
           try {
               mmServerSocket.close();
           } catch (IOException e) { }
       }
   }
}
