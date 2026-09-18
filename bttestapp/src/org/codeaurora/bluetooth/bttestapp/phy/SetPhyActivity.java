/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.bttestapp.phy;

import org.codeaurora.bluetooth.bttestapp.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SetPhyActivity provides a UI to test BLE PHY switching using the
 * BluetoothGatt.setPreferredPhy() API.
 *
 * TX/RX PHY bitmask:
 *   Bit 0 (0x01) - LE 1M PHY
 *   Bit 1 (0x02) - LE 2M PHY
 *   Bit 2 (0x04) - HDT2
 *   Bit 3 (0x08) - HDT3
 *   Bit 4 (0x10) - HDT4
 *   Bit 5 (0x20) - HDT6
 *   Bit 6 (0x40) - HDT7.5
 *
 * PHY Options:
 *   0 - No Preferred
 *   1 - S2
 *   2 - S8
 */
public class SetPhyActivity extends Activity {

    private static final String TAG = "SetPhyActivity";
    private static final boolean DBG = true;

    // PHY bitmask constants
    public static final int PHY_LE_1M_MASK  = 0x01; // bit 0
    public static final int PHY_LE_2M_MASK  = 0x02; // bit 1
    public static final int PHY_HDT2_MASK   = 0x04; // bit 2
    public static final int PHY_HDT3_MASK   = 0x08; // bit 3
    public static final int PHY_HDT4_MASK   = 0x10; // bit 4
    public static final int PHY_HDT6_MASK   = 0x20; // bit 5
    public static final int PHY_HDT75_MASK  = 0x40; // bit 6

    // PHY options constants
    public static final int PHY_OPTION_NO_PREFERRED = 0;
    public static final int PHY_OPTION_S2           = 1;
    public static final int PHY_OPTION_S8           = 2;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt    mBluetoothGatt;
    private BluetoothDevice  mSelectedDevice;

    // UI elements
    private TextView mStatusText;
    private Spinner  mDeviceSpinner;
    private Button   mRefreshButton;
    private Button   mConnectButton;
    private Button   mSetPhyButton;
    private Button   mReadPhyButton;

    // TX PHY checkboxes
    private CheckBox mTxPhy1m;
    private CheckBox mTxPhy2m;
    private CheckBox mTxPhyHdt2;
    private CheckBox mTxPhyHdt3;
    private CheckBox mTxPhyHdt4;
    private CheckBox mTxPhyHdt6;
    private CheckBox mTxPhyHdt75;

    // RX PHY checkboxes
    private CheckBox mRxPhy1m;
    private CheckBox mRxPhy2m;
    private CheckBox mRxPhyHdt2;
    private CheckBox mRxPhyHdt3;
    private CheckBox mRxPhyHdt4;
    private CheckBox mRxPhyHdt6;
    private CheckBox mRxPhyHdt75;

    // PHY options spinner
    private Spinner mPhyOptionsSpinner;

    private final List<BluetoothDevice> mDeviceList    = new ArrayList<>();
    private ArrayAdapter<String>        mDeviceAdapter;

    // -------------------------------------------------------------------------
    // GATT callback
    // -------------------------------------------------------------------------
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (DBG) Log.d(TAG, "onConnectionStateChange status=" + status + " newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                updateStatus("Connected to " + gatt.getDevice().getAddress());
                runOnUiThread(() -> {
                    mSetPhyButton.setEnabled(true);
                    mReadPhyButton.setEnabled(true);
                    mConnectButton.setText("Disconnect");
                });
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                updateStatus("Disconnected from " + gatt.getDevice().getAddress());
                runOnUiThread(() -> {
                    mSetPhyButton.setEnabled(false);
                    mReadPhyButton.setEnabled(false);
                    mConnectButton.setText("Connect");
                });
                mBluetoothGatt = null;
            }
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            String msg = "PHY Update - TX: " + phyMaskToString(txPhy)
                    + ", RX: " + phyMaskToString(rxPhy)
                    + ", Status: " + status;
            if (DBG) Log.d(TAG, msg);
            updateStatus(msg);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            String msg = "PHY Read - TX: " + phyMaskToString(txPhy)
                    + ", RX: " + phyMaskToString(rxPhy)
                    + ", Status: " + status;
            if (DBG) Log.d(TAG, msg);
            updateStatus(msg);
        }
    };

    // -------------------------------------------------------------------------
    // Activity lifecycle
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DBG) Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_set_phy);

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        initializeViews();
        setupClickListeners();
        refreshDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DBG) Log.d(TAG, "onDestroy");
        disconnectGatt();
    }

    // -------------------------------------------------------------------------
    // UI initialisation
    // -------------------------------------------------------------------------
    private void initializeViews() {
        mStatusText    = findViewById(R.id.phy_status_text);
        mDeviceSpinner = findViewById(R.id.device_spinner);
        mRefreshButton = findViewById(R.id.refresh_devices_button);
        mConnectButton = findViewById(R.id.connect_button);
        mSetPhyButton  = findViewById(R.id.set_phy_button);
        mReadPhyButton = findViewById(R.id.read_phy_button);

        // TX PHY checkboxes
        mTxPhy1m    = findViewById(R.id.tx_phy_1m);
        mTxPhy2m    = findViewById(R.id.tx_phy_2m);
        mTxPhyHdt2  = findViewById(R.id.tx_phy_hdt2);
        mTxPhyHdt3  = findViewById(R.id.tx_phy_hdt3);
        mTxPhyHdt4  = findViewById(R.id.tx_phy_hdt4);
        mTxPhyHdt6  = findViewById(R.id.tx_phy_hdt6);
        mTxPhyHdt75 = findViewById(R.id.tx_phy_hdt75);

        // RX PHY checkboxes
        mRxPhy1m    = findViewById(R.id.rx_phy_1m);
        mRxPhy2m    = findViewById(R.id.rx_phy_2m);
        mRxPhyHdt2  = findViewById(R.id.rx_phy_hdt2);
        mRxPhyHdt3  = findViewById(R.id.rx_phy_hdt3);
        mRxPhyHdt4  = findViewById(R.id.rx_phy_hdt4);
        mRxPhyHdt6  = findViewById(R.id.rx_phy_hdt6);
        mRxPhyHdt75 = findViewById(R.id.rx_phy_hdt75);

        // PHY options spinner
        mPhyOptionsSpinner = findViewById(R.id.phy_options_spinner);
        String[] phyOptions = {
            "No Preferred (0)",
            "S2 (1)",
            "S8 (2)"
        };
        ArrayAdapter<String> optionsAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, phyOptions);
        optionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPhyOptionsSpinner.setAdapter(optionsAdapter);

        // Device spinner
        mDeviceAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new ArrayList<String>());
        mDeviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDeviceSpinner.setAdapter(mDeviceAdapter);

        // PHY buttons disabled until a GATT connection is established
        mSetPhyButton.setEnabled(false);
        mReadPhyButton.setEnabled(false);

        // Default: 1M PHY selected for both TX and RX
        mTxPhy1m.setChecked(true);
        mRxPhy1m.setChecked(true);
    }

    private void setupClickListeners() {
        mRefreshButton.setOnClickListener(v -> refreshDevices());
        mConnectButton.setOnClickListener(v -> toggleConnection());
        mSetPhyButton.setOnClickListener(v -> setPreferredPhy());
        mReadPhyButton.setOnClickListener(v -> readPhy());
    }

    // -------------------------------------------------------------------------
    // Device management
    // -------------------------------------------------------------------------
    private void refreshDevices() {
        mDeviceList.clear();
        mDeviceAdapter.clear();

        if (mBluetoothAdapter == null) {
            updateStatus("Bluetooth not available");
            return;
        }

        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            mDeviceList.add(device);
            String name = device.getName();
            if (name == null || name.isEmpty()) name = "Unknown";
            mDeviceAdapter.add(name + "  [" + device.getAddress() + "]");
        }
        mDeviceAdapter.notifyDataSetChanged();

        if (mDeviceList.isEmpty()) {
            updateStatus("No bonded devices found. Please pair a BLE device first.");
        } else {
            updateStatus("Found " + mDeviceList.size() + " bonded device(s). Select one and press Connect.");
        }
    }

    // -------------------------------------------------------------------------
    // Connection management
    // -------------------------------------------------------------------------
    private void toggleConnection() {
        if (mBluetoothGatt != null) {
            disconnectGatt();
            mConnectButton.setText("Connect");
            mSetPhyButton.setEnabled(false);
            mReadPhyButton.setEnabled(false);
            updateStatus("Disconnected");
        } else {
            int selectedPos = mDeviceSpinner.getSelectedItemPosition();
            if (selectedPos < 0 || selectedPos >= mDeviceList.size()) {
                showToast("Please select a device first");
                return;
            }
            mSelectedDevice = mDeviceList.get(selectedPos);
            updateStatus("Connecting to " + mSelectedDevice.getAddress() + " …");
            mBluetoothGatt = mSelectedDevice.connectGatt(
                    this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        }
    }

    private void disconnectGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    // -------------------------------------------------------------------------
    // PHY operations
    // -------------------------------------------------------------------------
    private void setPreferredPhy() {
        if (mBluetoothGatt == null) {
            showToast("Not connected to any device");
            return;
        }

        int txPhy = buildPhyMask(
                mTxPhy1m, mTxPhy2m,
                mTxPhyHdt2, mTxPhyHdt3, mTxPhyHdt4, mTxPhyHdt6, mTxPhyHdt75);
        int rxPhy = buildPhyMask(
                mRxPhy1m, mRxPhy2m,
                mRxPhyHdt2, mRxPhyHdt3, mRxPhyHdt4, mRxPhyHdt6, mRxPhyHdt75);
        int phyOptions = mPhyOptionsSpinner.getSelectedItemPosition();

        if (txPhy == 0) {
            showToast("Please select at least one TX PHY");
            return;
        }
        if (rxPhy == 0) {
            showToast("Please select at least one RX PHY");
            return;
        }

        if (DBG) {
            Log.d(TAG, "setPreferredPhy - txPhy=0x" + Integer.toHexString(txPhy)
                    + " rxPhy=0x" + Integer.toHexString(rxPhy)
                    + " phyOptions=" + phyOptions);
        }

        mBluetoothGatt.setPreferredPhy(txPhy, rxPhy, phyOptions);
        updateStatus("Set PHY requested — TX: " + phyMaskToString(txPhy)
                + ", RX: " + phyMaskToString(rxPhy)
                + ", Options: " + phyOptionsToString(phyOptions));
    }

    private void readPhy() {
        if (mBluetoothGatt == null) {
            showToast("Not connected to any device");
            return;
        }
        if (DBG) Log.d(TAG, "readPhy");
        mBluetoothGatt.readPhy();
        updateStatus("Read PHY requested …");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private int buildPhyMask(CheckBox phy1m, CheckBox phy2m,
            CheckBox phyHdt2, CheckBox phyHdt3,
            CheckBox phyHdt4, CheckBox phyHdt6, CheckBox phyHdt75) {
        int mask = 0;
        if (phy1m.isChecked())    mask |= PHY_LE_1M_MASK;
        if (phy2m.isChecked())    mask |= PHY_LE_2M_MASK;
        if (phyHdt2.isChecked())  mask |= PHY_HDT2_MASK;
        if (phyHdt3.isChecked())  mask |= PHY_HDT3_MASK;
        if (phyHdt4.isChecked())  mask |= PHY_HDT4_MASK;
        if (phyHdt6.isChecked())  mask |= PHY_HDT6_MASK;
        if (phyHdt75.isChecked()) mask |= PHY_HDT75_MASK;
        return mask;
    }

    private String phyMaskToString(int phy) {
        if (phy == 0) return "None";
        List<String> parts = new ArrayList<>();
        if ((phy & PHY_LE_1M_MASK)  != 0) parts.add("1M");
        if ((phy & PHY_LE_2M_MASK)  != 0) parts.add("2M");
        if ((phy & PHY_HDT2_MASK)   != 0) parts.add("HDT2");
        if ((phy & PHY_HDT3_MASK)   != 0) parts.add("HDT3");
        if ((phy & PHY_HDT4_MASK)   != 0) parts.add("HDT4");
        if ((phy & PHY_HDT6_MASK)   != 0) parts.add("HDT6");
        if ((phy & PHY_HDT75_MASK)  != 0) parts.add("HDT7.5");
        return String.join("+", parts) + " (0x" + Integer.toHexString(phy) + ")";
    }

    private String phyOptionsToString(int options) {
        switch (options) {
            case PHY_OPTION_NO_PREFERRED: return "No Preferred (0)";
            case PHY_OPTION_S2:           return "S2 (1)";
            case PHY_OPTION_S8:           return "S8 (2)";
            default:                      return "Unknown (" + options + ")";
        }
    }

    private void updateStatus(String message) {
        if (DBG) Log.d(TAG, message);
        runOnUiThread(() -> {
            if (mStatusText != null) {
                mStatusText.setText(message);
            }
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
