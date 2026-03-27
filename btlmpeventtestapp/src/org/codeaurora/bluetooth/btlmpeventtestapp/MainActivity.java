/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package org.codeaurora.bluetooth.btlmpeventtestapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.util.Collections;
//For conn
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import android.hardware.bluetooth.lmp_event.IBluetoothLmpEvent;
import android.hardware.bluetooth.lmp_event.IBluetoothLmpEventCallback;
import android.hardware.bluetooth.lmp_event.AddressType;
import android.hardware.bluetooth.lmp_event.Direction;
import android.hardware.bluetooth.lmp_event.LmpEventId;
import android.hardware.bluetooth.lmp_event.Timestamp;

public class MainActivity extends Activity {
    private static final String TAG = "BtLmpEventActivity";

    private TextView tvStatus;
    //for conn
    private android.bluetooth.BluetoothGatt mBluetoothGatt;

    private EditText etRemoteAddress;
    private IBluetoothLmpEvent mIBluetoothLmpEvent;
    private Handler mainThreadHandler;
    private boolean isRegistered = false;

    // BLE scanner members for scan-before-connect
    private BluetoothLeScanner mLeScanner;
    private ScanCallback mScanCallback;
    private Runnable mScanTimeoutRunnable;
    private static final long SCAN_TIMEOUT_MS = 15000; // 15-second scan window

    // Default test Bluetooth address (can be modified as needed)
    private byte[] mBluetoothAddress = new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55};

    // Address type derived automatically from the BLE scan result (set in onScanResult).
    // Defaults to PUBLIC; updated to the actual type when the target device is found.
    private byte mConnectedAddressType = AddressType.PUBLIC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        etRemoteAddress = findViewById(R.id.etRemoteAddress);

        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnUnregister = findViewById(R.id.btnUnregister);
        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnDisconnect = findViewById(R.id.btnDisconnect);

        mainThreadHandler = new Handler(Looper.getMainLooper());

        btnRegister.setOnClickListener(v -> {
            tvStatus.setText("Status: REGISTER clicked");
            toast("REGISTER");
            registerForLmpEvents();
        });

        btnUnregister.setOnClickListener(v -> {
            tvStatus.setText("Status: UNREGISTER clicked");
            toast("UNREGISTER");
            unregisterLmpEvents();
        });

        btnConnect.setOnClickListener(v -> {
            tvStatus.setText("Status: CONNECT clicked");
            toast("CONNECT");
            // Parse the Bluetooth address from EditText
            byte[] address = parseBluetoothAddress();
            if (address != null) {
                String macAddress = bytesToHex(address);
                Log.d(TAG, "Connecting to address: " + anonymizeAddress(macAddress));
                updateStatus("Scanning for: " + macAddress);

                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null || !adapter.isEnabled()) {
                    toast("Bluetooth is disabled or not available");
                    return;
                }

                // Disconnect previous connection if exists
                if (mBluetoothGatt != null) {
                    try {
                        mBluetoothGatt.close();
                    } catch (Exception e) {
                        Log.w(TAG, "Error closing previous GATT: " + e.getMessage());
                    }
                    mBluetoothGatt = null;
                }

                // Scan first, then connect using the device from the scan result.
                // This mirrors the reference testApp flow (processScanDevFound) and ensures:
                // 1. The BluetoothDevice object has the correct address type from the BLE stack.
                // 2. The connection is initiated immediately after the controller has seen
                //    an advertising PDU, giving optimal connection timing.
                startScanAndConnect(adapter, macAddress);
            } else {
                toast("Invalid Bluetooth Address");
                updateStatus("Invalid Bluetooth Address");
            }
        });

        btnDisconnect.setOnClickListener(v -> {
            tvStatus.setText("Status: DISCONNECT clicked");
            toast("DISCONNECT");
            // Stop any in-progress scan first
            stopScan();
            if (mBluetoothGatt != null) {
                try {
                    Log.d(TAG, "Disconnecting GATT...");

                    // 1. Disconnect the logical connection
                    mBluetoothGatt.disconnect();

                    // 2. Release the resources immediately
                    // Note: In robust production apps, you often wait for the
                    // STATE_DISCONNECTED callback before calling close(), but
                    // for test tools, closing immediately ensures the handle is freed.
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;

                    updateStatus("Disconnected and closed");
                } catch (SecurityException e) {
                    Log.e(TAG, "Permission denied during disconnect: " + e.getMessage());
                    toast("Permission denied");
                } catch (Exception e) {
                    Log.e(TAG, "Error disconnecting: " + e.getMessage());
                    toast("Error disconnecting");
                }
            } else {
                Log.w(TAG, "No active GATT connection to disconnect");
                toast("No active connection");
                updateStatus("No active connection");
            }
        });
    }

    /**
     * Scan for the target device and connect using the BluetoothDevice from the scan result.
     *
     * Using the device object from onScanResult() (rather than one created via
     * getRemoteLeDevice()) ensures the BLE controller has the correct address type and
     * connects immediately after receiving an advertising PDU — the same approach used
     * by the reference testApp (GattClient.processScanDevFound).
     *
     * A 15-second timeout is posted; if the device is not found within that window the
     * scan is stopped and the user is notified.
     */
    private void startScanAndConnect(BluetoothAdapter adapter, String macAddress) {
        mLeScanner = adapter.getBluetoothLeScanner();
        if (mLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null");
            toast("BLE scanner not available");
            updateStatus("BLE scanner not available");
            return;
        }

        ScanFilter filter = new ScanFilter.Builder()
                .setDeviceAddress(macAddress)
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                Log.d(TAG, "Scan found device: " + anonymizeAddress(device.getAddress())
                        + " addressType: " + device.getAddressType());

                // Stop the scan immediately — we have our device.
                stopScan();

                // Capture the address type from the scan result so that LMP event
                // registration uses the same type as the actual connected device.
                mConnectedAddressType = (byte) device.getAddressType();
                Log.d(TAG, "Captured address type from scan result: " + mConnectedAddressType);

                // Connect using the device object from the scan result.
                // Its address type is set correctly by the BLE scan stack.
                mainThreadHandler.post(() -> {
                    try {
                        mBluetoothGatt = device.connectGatt(
                                MainActivity.this,
                                false,
                                mGattCallback,
                                BluetoothDevice.TRANSPORT_LE);

                        if (mBluetoothGatt == null) {
                            Log.e(TAG, "connectGatt returned null");
                            toast("Connection failed - GATT is null");
                            updateStatus("Connection failed");
                        } else {
                            Log.d(TAG, "connectGatt called for scanned device: "
                                    + anonymizeAddress(device.getAddress()));
                            updateStatus("Connecting to: " + device.getAddress());
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "Security exception: " + e.getMessage());
                        toast("Permission denied");
                    } catch (Exception e) {
                        Log.e(TAG, "Error connecting to device: " + e.getMessage(), e);
                        toast("Connection failed");
                    }
                });
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "BLE scan failed with error: " + errorCode);
                stopScan();
                mainThreadHandler.post(() -> {
                    toast("Scan failed: " + errorCode);
                    updateStatus("Scan failed (error " + errorCode + ")");
                });
            }
        };

        // Post a timeout runnable in case the device is not found within SCAN_TIMEOUT_MS.
        mScanTimeoutRunnable = () -> {
            Log.w(TAG, "Scan timed out — device not found: " + anonymizeAddress(macAddress));
            stopScan();
            mainThreadHandler.post(() -> {
                toast("Device not found (scan timeout)");
                updateStatus("Device not found: " + macAddress);
            });
        };
        mainThreadHandler.postDelayed(mScanTimeoutRunnable, SCAN_TIMEOUT_MS);

        try {
            mLeScanner.startScan(Collections.singletonList(filter), settings, mScanCallback);
            Log.d(TAG, "BLE scan started for: " + anonymizeAddress(macAddress));
            updateStatus("Scanning for: " + macAddress + "...");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied starting scan: " + e.getMessage());
            mainThreadHandler.removeCallbacks(mScanTimeoutRunnable);
            mScanTimeoutRunnable = null;
            toast("Permission denied");
            updateStatus("Scan failed: permission denied");
        }
    }

    /**
     * Stop any in-progress BLE scan and cancel the associated timeout runnable.
     */
    private void stopScan() {
        if (mScanTimeoutRunnable != null) {
            mainThreadHandler.removeCallbacks(mScanTimeoutRunnable);
            mScanTimeoutRunnable = null;
        }
        if (mLeScanner != null && mScanCallback != null) {
            try {
                mLeScanner.stopScan(mScanCallback);
                Log.d(TAG, "BLE scan stopped");
            } catch (Exception e) {
                Log.w(TAG, "Error stopping scan: " + e.getMessage());
            }
            mScanCallback = null;
        }
    }

    /**
     * Register for LMP events using AIDL interface
     */
    private void registerForLmpEvents() {
        if (isRegistered) {
            Log.w(TAG, "Already registered for LMP events");
            toast("Already registered");
            updateStatus("Already registered");
            return;
        }

        mIBluetoothLmpEvent = null;

        String serviceName = IBluetoothLmpEvent.DESCRIPTOR + "/default";
        Log.d(TAG, "IBluetoothLmpEvent.DESCRIPTOR: " + IBluetoothLmpEvent.DESCRIPTOR);

        // Try to get the service binder using reflection for compatibility
        IBinder binder = getServiceBinder(serviceName);

        if (binder == null) {
            Log.e(TAG, "Failed to obtain IBinder for IBluetoothLmpEvent");
            toast("Failed to obtain IBinder");
            updateStatus("Registration failed: Service not available");
            return;
        }

        mIBluetoothLmpEvent = IBluetoothLmpEvent.Stub.asInterface(binder);
        if (mIBluetoothLmpEvent == null) {
            Log.e(TAG, "mIBluetoothLmpEvent is null");
            toast("mIBluetoothLmpEvent null");
            updateStatus("Registration failed: Service interface null");
            return;
        }

        try {
            binder.linkToDeath(deathRecipient, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to register DeathRecipient: " + e);
            toast("Failed to link death recipient");
            updateStatus("Registration failed: " + e.getMessage());
            return;
        }

        try {
            // Define LMP events to monitor - using LmpEventId array as per AIDL interface
            byte[] lmpEventIds = new byte[]{
                LmpEventId.CONNECT_IND,
                LmpEventId.LL_PHY_UPDATE_IND
            };

            // Register for LMP events
            // Note: The AIDL interface expects:
            // - IBluetoothLmpEventCallback callback
            // - AddressType addressType (byte)
            // - byte[6] address
            // - LmpEventId[] lmpEventIds (byte array)
            mIBluetoothLmpEvent.registerForLmpEvents(
                lmpEventCallback,
                mConnectedAddressType,
                mBluetoothAddress,
                lmpEventIds
            );

            Log.d(TAG, "IBluetoothLmpEvent registration initiated");
            toast("Registration initiated");
            updateStatus("Registration initiated - waiting for callback");

        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during registration: " + e.getMessage());
            e.printStackTrace();
            toast("Registration failed: " + e.getMessage());
            updateStatus("Registration failed: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception during registration: " + e.getMessage());
            e.printStackTrace();
            toast("Registration failed: " + e.getMessage());
            updateStatus("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Get service binder using reflection for compatibility across Android versions
     */
    private IBinder getServiceBinder(String serviceName) {
        IBinder binder = null;

        try {
            // First, try using ServiceManager.isDeclared() and waitForDeclaredService() via reflection
            // These methods are available in Android 11+ but may be hidden APIs
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");

            try {
                Method isDeclaredMethod = serviceManagerClass.getMethod("isDeclared", String.class);
                Boolean isDeclared = (Boolean) isDeclaredMethod.invoke(null, serviceName);
                Log.d(TAG, "Service isDeclared (via reflection): " + isDeclared);

                if (isDeclared != null && isDeclared) {
                    Method waitForDeclaredServiceMethod = serviceManagerClass.getMethod(
                            "waitForDeclaredService", String.class);
                    binder = (IBinder) waitForDeclaredServiceMethod.invoke(null, serviceName);

                    if (binder != null) {
                        binder = Binder.allowBlocking(binder);
                        Log.d(TAG, "Successfully obtained binder via waitForDeclaredService");
                        return binder;
                    }
                }
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "isDeclared/waitForDeclaredService methods not available, trying alternatives");
            }

            // Fallback 1: Try getService() method
            try {
                Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);
                binder = (IBinder) getServiceMethod.invoke(null, serviceName);
                if (binder != null) {
                    Log.d(TAG, "Successfully obtained binder via getService");
                    return binder;
                }
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "getService method not available");
            }

            // Fallback 2: Try checkService() method
            try {
                Method checkServiceMethod = serviceManagerClass.getMethod("checkService", String.class);
                binder = (IBinder) checkServiceMethod.invoke(null, serviceName);
                if (binder != null) {
                    Log.d(TAG, "Successfully obtained binder via checkService");
                    return binder;
                }
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "checkService method not available");
            }

        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ServiceManager class not found: " + e);
        } catch (Exception e) {
            Log.e(TAG, "Error getting service binder: " + e.getMessage());
            e.printStackTrace();
        }

        Log.e(TAG, "Failed to obtain service binder through all methods");
        return null;
    }

    /**
     * Unregister from LMP events
     */
    private void unregisterLmpEvents() {
        if (!isRegistered) {
            Log.w(TAG, "Not registered for LMP events");
            toast("Not registered");
            updateStatus("Not registered");
            return;
        }

        if (mIBluetoothLmpEvent != null) {
            try {
                // Call unregisterLmpEvents as per AIDL interface
                // Parameters: AddressType addressType, byte[6] address
                mIBluetoothLmpEvent.unregisterLmpEvents(mConnectedAddressType, mBluetoothAddress);
                Log.d(TAG, "IBluetoothLmpEvent unregistered successfully");
                toast("Unregistered successfully");
                updateStatus("Unregistered successfully");
                isRegistered = false;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException during unregistration: " + e);
                toast("Unregistration failed: " + e.getMessage());
                updateStatus("Unregistration failed: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "General error during unregistration: " + e);
                toast("Unregistration failed: " + e.getMessage());
                updateStatus("Unregistration failed: " + e.getMessage());
            } finally {
                try {
                    if (mIBluetoothLmpEvent != null) {
                        IBinder binder = mIBluetoothLmpEvent.asBinder();
                        if (binder != null) {
                            binder.unlinkToDeath(deathRecipient, 0);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error unlinking death recipient: " + e);
                } finally {
                    mIBluetoothLmpEvent = null;
                }
            }
        } else {
            Log.w(TAG, "mIBluetoothLmpEvent is null, cannot unregister");
            toast("Service not available");
            updateStatus("Service not available");
        }
    }

    /**
     * DeathRecipient handler for binder service disconnection
     */
    private IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.d(TAG, "binderDied - IBluetoothLmpEvent service died");
            mIBluetoothLmpEvent = null;
            isRegistered = false;

            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    toast("Service died - need to re-register");
                    updateStatus("Service died - need to re-register");
                }
            });
        }
    };

    /**
     * IBluetoothLmpEventCallback implementation for handling LMP events
     *
     * Callback interface as per AIDL:
     * - onEventGenerated(Timestamp, AddressType, byte[6], Direction, LmpEventId, char)
     * - onRegistered(boolean)
     */
    private IBluetoothLmpEventCallback lmpEventCallback = new IBluetoothLmpEventCallback.Stub() {
        @Override
        public void onEventGenerated(Timestamp timestamp, byte addressType,
                                     byte[] address, byte direction,
                                     byte lmpEventId, char connEventCounter)
                throws RemoteException {
            Log.d(TAG, "onEventGenerated callback received");
            Log.d(TAG, "  Timestamp - systemTimeUs: " + timestamp.systemTimeUs +
                       ", bluetoothTimeUs: " + timestamp.bluetoothTimeUs);
            Log.d(TAG, "  AddressType: " + addressType);
            Log.d(TAG, "  Address: " + anonymizeAddress(bytesToHex(address)));
            Log.d(TAG, "  Direction: " + direction);
            Log.d(TAG, "  LmpEventId: " + lmpEventId);
            Log.d(TAG, "  ConnEventCounter: " + (int)connEventCounter);

            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    String eventInfo = "LMP Event Generated:\n" +
                                     "Event: " + getLmpEventName(lmpEventId) + " (0x" +
                                     String.format("%02X", lmpEventId) + ")\n" +
                                     "Direction: " + getDirectionName(direction) + "\n" +
                                     "Address: " + bytesToHex(address) + "\n" +
                                     "Counter: " + (int)connEventCounter + "\n" +
                                     "BT Time: " + timestamp.bluetoothTimeUs + " us";
                    updateStatus(eventInfo);
                    toast("LMP Event: " + getLmpEventName(lmpEventId));
                }
            });
        }

        @Override
        public void onRegistered(boolean status) throws RemoteException {
            Log.d(TAG, "onRegistered callback received with status: " + status);
            isRegistered = status;

            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (status) {
                        toast("Registration successful");
                        updateStatus("Registered successfully for LMP events");
                    } else {
                        toast("Registration failed");
                        updateStatus("Registration failed");
                    }
                }
            });
        }

        @Override
        public String getInterfaceHash() {
            return IBluetoothLmpEvent.HASH;
        }

        @Override
        public int getInterfaceVersion() {
            return IBluetoothLmpEvent.VERSION;
        }
    };

    /**
     * GATT Callback to handle connection state changes
     */
    private final android.bluetooth.BluetoothGattCallback mGattCallback = new android.bluetooth.BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(android.bluetooth.BluetoothGatt gatt, int status, int newState) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                mainThreadHandler.post(() -> {
                    updateStatus("BLE Connected: " + gatt.getDevice().getAddress());
                    toast("BLE Connected");
                });

                // Attempt to discover services
                try {
                    gatt.discoverServices();
                } catch (SecurityException e) {
                    Log.e(TAG, "Permission denied discovering services");
                }
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                mainThreadHandler.post(() -> {
                    updateStatus("BLE Disconnected");
                    toast("BLE Disconnected");
                });
            }
        }
    };

    // ------------------------------------------------------------------------

    /**
     * Anonymize a Bluetooth MAC address for safe logging.
     * Redacts the first four octets, retaining only the last two,
     * e.g. "AA:BB:CC:DD:EE:FF" → "XX:XX:XX:XX:EE:FF".
     * This prevents full MAC address exposure in logs while still
     * allowing device identification during testing.
     */
    private String anonymizeAddress(String mac) {
        if (mac == null || mac.length() < 17) return "XX:XX:XX:XX:XX:XX";
        return "XX:XX:XX:XX:" + mac.substring(12);
    }

    /**
     * Helper method to convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X:", b));
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // Remove trailing colon
        }
        return sb.toString();
    }

    /**
     * Get human-readable LMP event name
     */
    private String getLmpEventName(byte lmpEventId) {
        if (lmpEventId == LmpEventId.CONNECT_IND) {
            return "CONNECT_IND";
        } else if (lmpEventId == LmpEventId.LL_PHY_UPDATE_IND) {
            return "LL_PHY_UPDATE_IND";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Get human-readable direction name
     */
    private String getDirectionName(byte direction) {
        if (direction == Direction.TX) {
            return "TX";
        } else if (direction == Direction.RX) {
            return "RX";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Parse Bluetooth address from EditText field
     * Expected format: XX:XX:XX:XX:XX:XX or XXXXXXXXXXXX
     * @return byte array of 6 bytes, or null if invalid
     */
    private byte[] parseBluetoothAddress() {
        String addressStr = etRemoteAddress.getText().toString().trim();

        Log.d(TAG, "EditText content length: " + addressStr.length());

        if (addressStr.isEmpty()) {
            // Use default address if field is empty
            Log.d(TAG, "Field is empty, using default address");
            return mBluetoothAddress;
        }

        Log.d(TAG, "Attempting to parse entered address");

        // Remove colons and spaces
        addressStr = addressStr.replace(":", "").replace(" ", "").toUpperCase();

        // Validate length (should be 12 hex characters)
        if (addressStr.length() != 12) {
            Log.e(TAG, "Invalid address length: " + addressStr.length() + " (expected 12)");
            toast("Invalid address format. Use XX:XX:XX:XX:XX:XX");
            updateStatus("Invalid address format");
            return null;
        }

        // Validate hex characters
        if (!addressStr.matches("[0-9A-F]{12}")) {
            Log.e(TAG, "Invalid hex characters in address");
            toast("Invalid address. Use hex digits only");
            updateStatus("Invalid address format");
            return null;
        }

        // Convert to byte array
        byte[] address = new byte[6];
        try {
            for (int i = 0; i < 6; i++) {
                String byteStr = addressStr.substring(i * 2, i * 2 + 2);
                address[i] = (byte) Integer.parseInt(byteStr, 16);
            }
            Log.d(TAG, "Address parsed successfully");
            return address;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing address: " + e.getMessage());
            toast("Error parsing address");
            updateStatus("Error parsing address");
            return null;
        }
    }

    /**
     * Update status TextView
     */
    private void updateStatus(String status) {
        if (tvStatus != null) {
            tvStatus.setText("Status: " + status);
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // Stop any in-progress scan
        stopScan();

        // Unregister from LMP events if registered
        if (isRegistered) {
            unregisterLmpEvents();
        }

        // Clean up BLE Connection
        if (mBluetoothGatt != null) {
            try {
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            } catch (SecurityException e) {
                Log.e(TAG, "Error closing GATT in onDestroy: " + e.getMessage());
            }
        }
    }
}