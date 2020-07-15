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

package org.codeaurora.bluetooth.wearos_ble_testapp;

import android.os.Bundle;
import android.os.Build;

import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import android.bluetooth.BluetoothAdapter;
import android.app.Activity;
import android.view.View;
import android.view.Menu;
import android.content.Intent;
import android.view.MenuItem;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;

//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.*;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothManager;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattCharacteristic;

//import android.bluetooth.le.BluetoothLeScanner;
//import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.Message;
import android.os.SystemClock;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;

import android.util.Log;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.Scanner;
import java.lang.*;
import java.util.concurrent.TimeUnit;

import libcore.io.IoUtils;


public class MainActivity extends Activity {

    public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR
        = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final String TAG = "MainActivity";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 2;
    private static final int INITIAL_REQUEST=1337;
    public static final int MTU_SIZE_MIN = 23;
    public static final int MTU_SIZE_MAX = 512;
    public static final int TRANSPORT_LE = 2;
    public BluetoothManager mBluetoothManager;
    public BleConnectionClass mBleConnect;
    public static TestAppConnectionStateMachine mStateMachine;
    private BluetoothDevice mDevice = null;
    private Context mContext;
    private String conn_item;
    public static final int TA_SM_DEV_FOUND = 2;
    public int mtu_size = MTU_SIZE_MIN;
    private final Object mtu_mutex = new Object();
    private final Object write_mutex = new Object();
    private final Object discovery_mutex = new Object();
    private final Object conn_update_mutex = new Object();

    public static boolean advPresentflag=false,scanPresentflag=false;
    public static boolean rxinprog = false;
    public static boolean rxTestDone = false;
    public boolean gatt_discovery_done=false;
    public static boolean connUpdateInProg=false;
    public static boolean was_signalled = false;

    public AdvertiserService mAdvertiseService = null;
    public ScannerService mScannerService=null;
    public static BluetoothAdapter bleAdapter = BluetoothAdapter.getDefaultAdapter();
    /** Flag indicating whether we have called bind on the service. */
    private boolean boundA=false;//used by connection service..not required outside the class
    private boolean boundS=false;//used by connection service..not required outside the class
    private boolean statMachinestarted=false;//used by connection service..not required outside the class

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //moveTaskToBack(true);
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null)
        {
           Log.e(TAG, "mBluetoothManager is null");
           return;
        }

        if (!initAdapter()) {
             Log.e(TAG, "Bluetooth is not turned on");
             Toast.makeText(getApplicationContext(),"Bluetooth is not turned ON",Toast.LENGTH_SHORT).show();
             return;
         }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
        }

        try {
            TextParse T=new TextParse();
            Log.d("Queue CREATED", "SUCCESSFULLY");
            Object o;
            int p=T.objects.size();
            Log.d("size", "size: "+p);
        } catch (Exception e) {
            Log.d("Exception thrown", "onCreate:error ");
            e.printStackTrace();
        }

        if(scanPresentflag==true){
            // Bind to the "SCANNER" service
            Log.d("CREATION", "BINDING TO SCANNER ");
            Intent intent = new Intent(this, ScannerService.class);
            startService(intent);
            bindService(intent, mscannerConnection,
                    Context.BIND_AUTO_CREATE);
        }

        if(advPresentflag==true) {
            // Bind to the "ADVERTISER" service
            Log.d("CREATION", "BINDING TO ADVERTISER");
            Intent adv_intent = new Intent(this, AdvertiserService.class);
            startService(adv_intent);
            bindService(adv_intent, madvertiserConnection,
                    Context.BIND_AUTO_CREATE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
       switch (requestCode) {
          case PERMISSION_REQUEST_FINE_LOCATION: {
              if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted!");
              } else {
                 Log.e(TAG, "Needs location permission");
              }
          }
        }
    }

    private boolean initAdapter() {

      bleAdapter = mBluetoothManager.getAdapter();
      if (bleAdapter == null) {
          Log.e(TAG, "bleAdapter is null");
          return false;
      }

      boolean isBtEnabled = bleAdapter.isEnabled();
      if (!isBtEnabled) {
         return false;
      }
      else {
          Log.e(TAG, "bt is enabled");
          Toast.makeText(getApplicationContext(), "Bluetooth is ON", Toast.LENGTH_SHORT).show();
      }
      return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBleConnect = new BleConnectionClass(getApplicationContext());
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop called");
        super.onStop();
        // Unbind from the "ADVERTISER" service
        if (boundA) {
            unbindService(madvertiserConnection);
            boundA = false;
            advPresentflag = false;
        }
        //Unbind from the "SCANNER" service
        if (boundS) {
            unbindService(mscannerConnection);
            boundS = false;
            scanPresentflag = false;
        }
        /*Stopping state machine */
        if (mStateMachine != null) {
            mStateMachine.doQuit();
        }
    }

    private void start_testapp_state_machine() {
        if((boundA == true || boundS == true) && statMachinestarted == false){
          mStateMachine = new TestAppConnectionStateMachine(mContext);
          Log.i("TestAppConnectionStateMachine", "make");
          mStateMachine.start();
        }
    }

    //Advertiser connection service
    private ServiceConnection madvertiserConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            AdvertiserService.LocalBinder binderA=(AdvertiserService.LocalBinder) service;
            mAdvertiseService = binderA.getService();
            boundA = true;
            start_testapp_state_machine();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mAdvertiseService = null;
            boundA = false;
        }
    };

    private ServiceConnection mscannerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the scanner service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a scanner, so here we get a client-side
            // representation of that from the raw IBinder object.
            ScannerService.LocalBinder binderS = (ScannerService.LocalBinder) service;
            mScannerService = binderS.getService();
            boundS = true;
            start_testapp_state_machine();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mScannerService = null;
            boundS = false;
        }
    };

    public class BleConnectionClass {

        private static final String TAG = "BleConnectionClass";

        private BluetoothDevice mDevice;
        private BluetoothGatt mBluetoothGatt;
        private BluetoothGattService mService;
        private BluetoothGattCharacteristic mCharacteristic;
        private BluetoothGattCharacteristic mreadChar;
        private Context context;
        private int mState;
        private int GATT_SUCCESS = 0x00;
        long tx_end_time_stamp, rx_end_time_stamp, rx_data_size, rx_start_time_stamp;

        public BleConnectionClass(Context context) {
            this.context = context;
        }

        /**
         * GATT callbacks
         */
        private final BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.i(TAG, "onConnectionStateChange device :" + gatt.getDevice() + " status :" + status +
                        " newState :" + newState);
                mState = newState;

                if (gatt.getDevice() == null || status != GATT_SUCCESS) {
                    Log.e(TAG, "onConnectionStateChange:Unexpected error! mstate: " +  mState);
                    mStateMachine.sendMessage(TestAppConnectionStateMachine
                          .TA_REM_DEV_FAILED_TO_CONNECT);
                    return;
                }

                int bondState = mDevice.getBondState();

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                    Log.i(TAG, "onConnectionStateChange:DISCONNECTED "
                            + " remoteDevice: " + gatt.getDevice().getAddress());
                    /*Send Message to SM */
                    mStateMachine.sendMessage(TestAppConnectionStateMachine
                          .TA_REM_DEV_DISCONNECTED);
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                       mDevice.removeBond();
                       Log.i(TAG, "Device unpaired");
                    }

                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "onConnectionStateChange:CONNECTED "
                            + " remoteDevice: " + gatt.getDevice().getAddress());
                    /*Send Message to SM*/
                    mStateMachine.sendMessage(TestAppConnectionStateMachine
                          .TA_REM_DEV_CONNECTED);
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                       Log.i(TAG, "Device paired");
                    }
                    Log.d(TAG, "starting discover services");
                    gatt.discoverServices();
                }
            }

             @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "onService discovery success");
                    gatt_discovery_done = true;

                    synchronized (discovery_mutex) {
                        discovery_mutex.notifyAll();
                    }
                } else {
                    Log.d(TAG, "onServicesDiscovered received: " + status);
                }
            }

             @Override
             public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency,
                         int timeout, int status) {
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "on Conn updated:"
                                 + " interval=" + interval + " latency=" + latency
                                 + " timeout=" + timeout + " status=" + status);
                    if(connUpdateInProg == true){
                       mStateMachine.sendMessage(TestAppConnectionStateMachine
                           .TA_CONNECTION_UPDATED);
                       connUpdateInProg = false;
                   }

                }
                else {
                    Log.i(TAG, "conn update failed");
                }
                synchronized (conn_update_mutex) {
                    conn_update_mutex.notify();
                  }

             }

             @Override
             public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy,
                         int status) {
                if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "on Phy updated:"
                                 + " tx phy " + txPhy + " rx phy " + rxPhy +" status " + status);
                     mStateMachine.sendMessage(TestAppConnectionStateMachine
                          .TA_PHY_UPDATED);

                }
                else {
                    Log.i(TAG, "phy update failed");
                }
             }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                 int status){
                   if ((status == GATT_SUCCESS)) {
                    Log.i(TAG, "Characteristic read is "+ characteristic.getValue().toString());
                }
                else {
                    Log.i(TAG, "Char read failed");
                }
            }
             @Override
            public void onCharacteristicWrite(BluetoothGatt gatt,
                  BluetoothGattCharacteristic characteristic, int status) {
                  if ((status == GATT_SUCCESS)) {
                      Log.i(TAG, "onCharacteristicWrite: " + status);
                  }
                  else {
                    Log.i(TAG, "write characteristic failed");
                  }
                  synchronized (write_mutex) {
                    was_signalled = true;
                    write_mutex.notify();
                  }
             }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt,
                  BluetoothGattDescriptor desc, int status) {
                  if ((status == GATT_SUCCESS)) {
                      Log.i(TAG, "onDescriptorWrite: " + status);
                  }
                  else {
                    Log.i(TAG, "write descriptor failed");
                  }
                  synchronized (write_mutex) {
                    was_signalled = true;
                    write_mutex.notifyAll();
                    Log.d(TAG, "Mutex unlock");
                  }
             }


           @Override
             public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                   if(status == GATT_SUCCESS){
                      Log.i(TAG, "Read Phy: Tx Phy-"+txPhy+"Rx Phy:"+rxPhy);
                      mStateMachine.sendMessage(TestAppConnectionStateMachine
                          .TA_PHY_READ);

                   }
                   else{
                      Log.i(TAG, "Read Phy failed");
                   }
             }

          @Override
           public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                 //Wait here for notifications
                 if (rxinprog) {
                     //Calculate the data size
                     byte[] charByte = characteristic.getValue();
                     rx_data_size += charByte.length;
                     Log.d(TAG, "rx_data_size:"+rx_data_size);
                     //Check for last notification
                     String checklast = characteristic.getStringValue(0);
                     if (checklast.contains("end"))
                     {
                         rx_end_time_stamp = SystemClock.elapsedRealtime();
                         float rxTput = (((float) rx_data_size  /
                                          ((float) (rx_end_time_stamp - rx_start_time_stamp))) *8 *1000);
                         float rxTputk = rxTput / 1000;
                         float rxTputm = rxTputk / 1000;
                         Log.d(TAG, "Rx tput in kbps: " + rxTputk + " in mbps: " + rxTputm);
                         rxinprog = false;
                         rxTestDone = true;
                         rx_data_size = 0;
                         rx_start_time_stamp = 0;
                         rx_end_time_stamp = 0;
                     }
                  } else {
                      //check for start
                      String charValue = characteristic.getStringValue(0);
                      if (charValue.contains("start")) {
                         rx_start_time_stamp = SystemClock.elapsedRealtime();
                         Log.d(TAG, "rx_start_time_stamp:"+rx_start_time_stamp);
                         byte[] charByte = characteristic.getValue();
                         rx_data_size += charByte.length;
                         if (charValue.contains("end"))
                         {
                            rx_end_time_stamp = SystemClock.elapsedRealtime();
                            Log.d(TAG, "rx_end_time_stamp:"+rx_end_time_stamp);
                            float rxTput = (((float) rx_data_size * 8 * 1000) / ((float) (rx_end_time_stamp - rx_start_time_stamp)));
                            float rxTputk = rxTput / 1024;
                            float rxTputm = rxTputk / 1024;
                            Log.d(TAG, "Rx tput in kbps: " + rxTputk + " in mbps: " + rxTputm);
                            rxinprog = false;
                            rxTestDone = true;
                            rx_data_size = 0;
                            rx_start_time_stamp = 0;
                            rx_end_time_stamp = 0;
                         }   else {
                             rxinprog = true;
                          }
                       }
                    }
                if(rxinprog == false && rxTestDone == true){
                  mStateMachine.sendMessage(TestAppConnectionStateMachine.TA_RX_TEST_DONE);
                  rxTestDone = false;
                }
            }

          @Override
            public void onMtuChanged (BluetoothGatt gatt, int mtu, int status) {
               if (status == GATT_SUCCESS) {
                  Log.i(TAG, "Gatt updated MTU" + mtu);
                  mtu_size = mtu;
                  synchronized (mtu_mutex) {
                  // It is done so we notify the waiting threads
                   mtu_mutex.notifyAll();
                  }
                }
            }
        };

         public void connect(BluetoothDevice device){
            if(bleAdapter!=null) {
               Log.i(TAG, "Gatt Connect");
               mDevice = device;
               mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallbacks,TRANSPORT_LE);
             }
         }

        public void pair(){
            if(mDevice.getBondState() != BluetoothDevice.BOND_BONDED){
              Log.i(TAG, "Pairing!");
              if(!mDevice.createBond(TRANSPORT_LE)) {
                Log.i(TAG, "couldn't start pairing");
              }
            }
         }

        public BluetoothGattService getGattService(UUID serv_uuid){
            BluetoothGattService mService = mBluetoothGatt.getService(serv_uuid);
            if(mService != null){
                Log.d(TAG, "Get Gatt Service");
            }
            else {
                Log.d(TAG, "Get Gatt Service not found");
            }
            return mService;
        }
    }


    public void wait_for_gatt_service_discovery() {
        Log.d(TAG, "Wait for service disc, thread name:" + Thread.currentThread().getName() +
              "thread id:" + Thread.currentThread().getId());

        if(!gatt_discovery_done){
           synchronized (discovery_mutex) {
             try {
                Log.d(TAG,"discovery lock ");
                discovery_mutex.wait();
             }
             catch (InterruptedException e) {
                 Log.d(TAG, "Interrupted while waiting for operation to complete");
             }
           }
        }
    }

    public class TestAppConnectionStateMachine extends StateMachine {

        public static final int TA_BT_ADAPTER_OFF = 1;
        public static final int TA_SM_ADV_STOPPED = 3;
        public static final int TA_REM_DEV_CONNECTED = 4;
        public static final int TA_REM_DEV_FAILED_TO_CONNECT = 5;
        public static final int TA_DEV_DISCONNECT = 6;
        public static final int TA_REM_DEV_DISCONNECTED = 7;
        public static final int TA_TX_TEST_DONE = 8;
        public static final int TA_RX_TEST_DONE = 9;
        public static final int TA_LAT_TEST_DONE = 10;
        public static final int TA_CONNECTION_UPDATED = 11;
        public static final int TA_PHY_UPDATED = 12;
        public static final int TA_PHY_READ = 13;


        TextParse txtParse;
        // Test App Connection states.
        private TAIdle mTAIdle;
        private TAConnectPending mTAConnectPending;
        private TAConnected mTAConnected;
        private TADataTx mTADataTx;
        private TADataRx mTADataRx;
        private TALatencyMeasurement mTALatencyMeasurement;
        private TADisconnect mTADisconnect;
        private Context mContext;

        private TestAppConnectionStateMachine(Context context) {
            super("TestAppConnectionStateMachine");
            mContext = context;
            statMachinestarted = true;

            mTAIdle = new TAIdle();
            mTAConnectPending = new TAConnectPending();
            mTAConnected = new TAConnected();
            mTADataTx = new TADataTx();
            mTADataRx = new TADataRx();
            mTALatencyMeasurement = new TALatencyMeasurement();
            mTADisconnect = new TADisconnect();

            addState(mTAIdle);
            addState(mTAConnectPending);
            addState(mTAConnected);
            addState(mTADataTx);
            addState(mTADataRx);
            addState(mTALatencyMeasurement);
            addState(mTADisconnect);

            setInitialState(mTAIdle);
        }


        public void doQuit() {
            Log.i("TestAppConnectionStateMachine", "Quit");
            synchronized (TestAppConnectionStateMachine.this) {
                statMachinestarted = false;
                quitNow();
            }
        }

        private class TAIdle extends State {
            private static final String TAG = "TAIdle";

            ArrayList<ScanFilter> mfilter;
            ScanSettings settings;

            @Override
            public void enter() {
                Log.i(TAG, "Enter ");
                if(!txtParse.objects.isEmpty()) {
                   /* according to the flag send the message to respective service along with class obj */
                   if("advflag".equals(txtParse.objects.remove())){
                       Log.i(TAG, "advflag set, service bound:" + boundA);
                       Adv adv = (Adv)txtParse.objects.remove();
                       mAdvertiseService.startAdvertising(adv);
                       Toast.makeText(getApplicationContext(), "Enabling Advertising!", Toast.LENGTH_SHORT).show();
                   }
                   if("scanflag".equals(txtParse.objects.remove())){
                       Log.i(TAG, "scanflag set,service bound: " + boundS);
                       Scan scn = (Scan)txtParse.objects.remove();
                       set_scan_parameters(scn);
                       mScannerService.startScan(mfilter, settings);
                   }
                }
                else {
                    Log.d(TAG, "Queue is Empty. Nothing more to do!");
                }
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit");
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);

                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                        break;
                    case TA_SM_DEV_FOUND:
                        BluetoothDevice device = (BluetoothDevice) message.obj;
                        processSMDevFoundEvent(device);
                        break;
                    case TA_SM_ADV_STOPPED:
                         /* ?? Adv has stopped */
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }

            private void processSMDevFoundEvent(BluetoothDevice device) {
                Log.i(TAG, "matchFoundEvent Address:" + device.getAddress());
                if("Connectflag".equals(txtParse.objects.remove())){
                   mBleConnect.connect(device);
                   transitionTo(mTAConnectPending);
                }
            }

            private void set_scan_parameters(Scan scn) {
                ScanParams params = new ScanParams(scn.DeviceName, scn.DeviceAddress, scn.ServiceUuid,
                         scn.SvcMaskUuid, scn.ManufacturerId, scn.ManufacturerData, scn.ManuMaskData,
                         scn.ServiceDataUuid, scn.ServiceData, scn.SvcDataMask, scn.ScanMode,
                         scn.CallbackType, scn.ResultType, scn.NumOfAdvMatches, scn.MatchMode,
                         scn.ReportDelay, scn.ScanTO);
                Log.i(TAG, "cbtpe:" + scn.CallbackType + "scan mode:" + scn.ScanMode + "noOfadvmatches:" + scn.NumOfAdvMatches
                  + "match mode:" + scn.MatchMode + "result type:" + scn.ResultType + "report delay:" + scn.ReportDelay);
                if(params == null) {
                    Log.i(TAG, "params NULL");
                }
                mfilter = params.parseScanFilter();
                if(mfilter == null) {
                     Log.i(TAG, "mfilter NULL");
                }
                settings = params.getScanSettings();
                if(settings == null) {
                    Log.i(TAG, "settings NULL");
                }
            }
        }

        private class TAConnectPending extends State {
            private static final String TAG = "TAConnectPending";

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);

                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                        break;
                    case TA_REM_DEV_CONNECTED:
                        Toast.makeText(getApplicationContext(), "Device Connected", Toast.LENGTH_SHORT).show();
                        if("Pairflag".equals(txtParse.objects.remove())){
                           //mBleConnect.pair();
                        }
                        mScannerService.stopScan();
                        transitionTo(mTAConnected);
                        break;
                    case TA_REM_DEV_FAILED_TO_CONNECT:
                        Log.i(TAG, "Connection failed to establish, please try again");
                        transitionTo(mTAIdle);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }
        }

        private class TAConnected extends State {
            private static final String TAG = "TAConnected";
            private int DATA_TX = 1;
            private int DATA_RX = 2;
            private int LATENCY_MEASUREMENT = 3;
            private int CONN_UPDATE = 4;
            private int DISCONNECT = 5;

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                change_state_acc_next_item_in_queue();
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }


            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);

                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                        break;
                    case TA_CONNECTION_UPDATED:
                         Log.d(TAG, "CONNECTION PARAM UPDATED");
                         change_state_acc_next_item_in_queue();
                         break;
                    case TA_PHY_UPDATED:
                         Log.d(TAG, "PHY UPDATED");
                         change_state_acc_next_item_in_queue();
                         break;
                    case TA_PHY_READ:
                         Log.d(TAG, "PHY READ");
                         change_state_acc_next_item_in_queue();
                         break;
                    case TA_DEV_DISCONNECT:
                        transitionTo(mTADisconnect);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }

            private void processConnUpdateReq(){
                /* Pop Connection update parameters */
               ConnUpdate ConnUpdateClass = (ConnUpdate)txtParse.objects.remove();
               Log.i(TAG, "Conn Update");
               /* Android version check */
               try {
                    if (Build.VERSION.SDK_INT >= 28) {
                        mBleConnect.mBluetoothGatt.requestLeConnectionUpdate(ConnUpdateClass.ConnIntervalMin, ConnUpdateClass.ConnIntervalMax,
                               ConnUpdateClass.ConnSlaveLatency, ConnUpdateClass.ConnSupTO, 0, 0);
                        connUpdateInProg = true;
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Conn Update can't be done", Toast.LENGTH_SHORT).show();
                    }
                }
                catch (Exception e) {
                 Log.d(TAG, "Interrupted while waiting for operation to complete");
                }

            }

            private void processReadPhyReq(){
                /* Pop Connection update parameters */
               Log.i(TAG, "Read Phy");
               mBleConnect.mBluetoothGatt.readPhy();
            }

            private void processPhyUpdateReq(){
                /* Pop Connection update parameters */
               PhyUpdate phyUpdate = (PhyUpdate)txtParse.objects.remove();
               Log.i(TAG, "Phy Update");
               mBleConnect.mBluetoothGatt.setPreferredPhy(phyUpdate.txPhy, phyUpdate.rxPhy, phyUpdate.phyOpt);
            }

            private void change_state_acc_next_item_in_queue() {
                conn_item = (String)txtParse.objects.remove();
                Log.d(TAG,"conn_item_flag: " + conn_item.toString());

                switch(conn_item)
                {
                    case "TxPowerTestflag":
                        Log.d(TAG,"TxPowerTestflag");
                        break;
                    case "DataTxflag":
                        Log.d(TAG,"DataTxflag");
                        transitionTo(mTADataTx);
                        break;
                    case "DataRxflag":
                        Log.d(TAG,"DataRxflag");
                        transitionTo(mTADataRx);
                        break;
                    case "LatencyTestflag":
                        Log.d(TAG,"LatencyTestflag");
                        transitionTo(mTALatencyMeasurement);
                        break;
                    case "ConnUpdateflag":
                        Log.d(TAG,"ConnUpdateflag");
                        processConnUpdateReq();
                        break;
                    case "PhyUpdateflag":
                        Log.d(TAG,"PhyUpdateflag");
                        processPhyUpdateReq();
                        break;
                    case "ReadPhyflag":
                        Log.d(TAG,"ReadPhyflag");
                        processReadPhyReq();
                        break;
                    case "Disconnectflag":
                        /*Disconnect*/
                        Log.d(TAG,"Disconnectflag");
                        transitionTo(mTADisconnect);
                        break;
                    default:
                        Log.i(TAG, "Unknown Operation");
                        break;
                }
            }
        }

        private class TADataTx extends State {
            private static final String TAG = "TADataTx";
            public String char_data;
            DataTx DataTxClass;

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                /* read parameters */
                DataTxClass = (DataTx)txtParse.objects.remove();
                char_data = String.valueOf(DataTxClass.charData);
                DataTxthread tt = new DataTxthread();
                Thread t = new Thread(tt);
                if(!t.isAlive()) {
                  t.start();
                }
                else {
                    Log.i(TAG, "Thread is already running");
                }
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);

                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                    case TA_DEV_DISCONNECT:
                        transitionTo(mTADisconnect);
                        break;
                    case TA_TX_TEST_DONE:
                        Log.d(TAG, "state change to connected");
                        transitionTo(mTAConnected);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }

            public class DataTxthread implements Runnable {


                @Override
                public void run() {
                    Log.i(TAG, "data tx thread start");
                    final UUID UUID_TX_SERVICE = UUID.fromString(DataTxClass.txService);
                    final UUID UUID_TX_CHAR = UUID.fromString(DataTxClass.txChar);
                    int offset = 0;
                    int length = 25600;
                    /*Thread handling*/
                    if (mtu_size != MTU_SIZE_MAX) {
                        if(mBleConnect.mBluetoothGatt.requestMtu(MTU_SIZE_MAX)) {
                           Log.i(TAG, "MTU size requested to Max Value");
                           synchronized (mtu_mutex) {
                                // It is done so we notify the waiting threads
                                try {
                                    mtu_mutex.wait();
                                }
                                catch (InterruptedException e) {
                                    Log.d(TAG, "Interrupted while waiting for operation to complete");
                                }
                           }
                        }
                    }
                    wait_for_gatt_service_discovery();
					/*Connection update to 7.5msec*/
                    mBleConnect.mBluetoothGatt.requestLeConnectionUpdate(6,6,0, 0x0c80, 0, 0);
                    synchronized(conn_update_mutex){
                        try{
                           conn_update_mutex.wait();
                        }
                        catch (InterruptedException e) {
                                    Log.d(TAG, "Interrupted while waiting for operation to complete");
                        }
                    }
                    char[] str = new char[mtu_size-3];
                    BluetoothGattService mService = mBleConnect.mBluetoothGatt.getService(UUID_TX_SERVICE);
                    if (mService != null) {
                       BluetoothGattCharacteristic mCharacteristic = mService.getCharacteristic(UUID_TX_CHAR);
                       if (mCharacteristic != null) {
                           long tx_start_time_stamp = SystemClock.elapsedRealtime();
                           mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                           //Log.d(TAG, "chardata len:"+length+"tx start time:"+tx_start_time_stamp);
                           for (int i = 1; i <= (length-1)/(mtu_size-3); i++, offset+=(mtu_size-3)) {
                               //Max packet size that can be sent using write without response is MTU-3 Bytes
                              // Log.d(TAG, "offset in loop:"+offset+"i:"+i);
                               Arrays.fill(str,0, mtu_size-4,(char)(i+'0'));
                              // Log.d(TAG, "String:"+String.valueOf(str.toString()));
                               mCharacteristic.setValue(String.valueOf(str));
                               mBleConnect.mBluetoothGatt.writeCharacteristic(mCharacteristic);
                               //Log.d(TAG, "after write");
                                synchronized (write_mutex) {
                               // Wait for write response
                                if(!was_signalled) {
                                  try {
                                //     Log.d(TAG, "before mutex lock");
                                     write_mutex.wait();
                                  //   Log.d(TAG, "mutex lock:i="+i);
                                  }
                                  catch (InterruptedException e) {
                                     Log.d(TAG, "Interrupted while waiting for operation to complete");
                                  }
                               }
                               was_signalled = false;
                              }
                           }
                           mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                           //Log.d(TAG, "offset:"+offset);
                           char[] offset_str = new char[length-offset];
                           Arrays.fill(offset_str,0, length-offset,'x');
                           mCharacteristic.setValue(String.valueOf(offset_str));
                           mBleConnect.mBluetoothGatt.writeCharacteristic(mCharacteristic);
                           synchronized (write_mutex) {
                               // Wait for write response
                               if(!was_signalled){
                                 try {
                                     //Log.d(TAG, "mutex lock");
                                     write_mutex.wait();
                                 }
                                 catch (InterruptedException e) {
                                     Log.d(TAG, "Interrupted while waiting for operation to complete");
                                 }
                              }
                              was_signalled = false;
                           }
                           long tx_end_time_stamp = SystemClock.elapsedRealtime();
                           //Log.d(TAG, "chardata len2:"+length + "tx end time:"+tx_end_time_stamp);
                           float txTput = (((float) length /
                                           ((float) (tx_end_time_stamp - tx_start_time_stamp))) *8 *1000);
                           float txTputk = txTput / 1000;
                           float txTputm = txTputk / 1000;
                           Log.d(TAG, "Tx tput in kbps: " + txTputk + " in mbps: " + txTputm);
                           mStateMachine.sendMessage(TestAppConnectionStateMachine.TA_TX_TEST_DONE);
                       } else {
                           Log.e(TAG, "writeToCharacteristic() - Characteristic is null! " + mBleConnect.mCharacteristic);
                        }
                    } else {
                       Log.d(TAG, "onConnectionStateChange: service with UUID not found ");
                    }
                }
            }
        }

        private class TADataRx extends State {
            private static final String TAG = "TADataRx";
            DataRx DataRxClass;

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                /* read parameters */
                DataRxClass = (DataRx)txtParse.objects.remove();
                DataRxthread rt = new DataRxthread();
                Thread t = new Thread(rt);
                if(!t.isAlive()) {
                  t.start();
                }
                else {
                    Log.i(TAG, "Thread is already running");
                }
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);

                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                    case TA_DEV_DISCONNECT:
                        transitionTo(mTADisconnect);
                        break;
                    case TA_RX_TEST_DONE:
                        Log.d(TAG, "state change to connected");
                        transitionTo(mTAConnected);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }

            public class DataRxthread implements Runnable {

                public void run() {
                    Log.i(TAG, "data rx thread start");
                    final UUID UUID_RX_SERVICE = UUID.fromString(DataRxClass.rxService);
                    final UUID UUID_RX_CHAR = UUID.fromString(DataRxClass.rxChar);
                    final UUID UUID_CCCD = CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR;
                    wait_for_gatt_service_discovery();
                    //battery service and characteristic
                    BluetoothGattService mService = mBleConnect.mBluetoothGatt.getService(UUID_RX_SERVICE);
                    if (mService != null) {
                        BluetoothGattCharacteristic mreadChar = mService.getCharacteristic(UUID_RX_CHAR);
                        if (mreadChar != null) {
                            Log.i(TAG, "Found Characteristic: " + mreadChar.getUuid().toString());
                            //enable cccd to send notification
                            mBleConnect.mBluetoothGatt.setCharacteristicNotification(mreadChar, true);
                            BluetoothGattDescriptor descriptor = mreadChar.getDescriptor(UUID_CCCD);
                            if (descriptor != null) {
                                //start of writing
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mBleConnect.mBluetoothGatt.writeDescriptor(descriptor);
                                synchronized (write_mutex) {
                               // Wait for write response
                                if(!was_signalled){
                                  try {
                                     Log.d(TAG, "mutex lock");
                                     write_mutex.wait();
                                  }
                                  catch (InterruptedException e) {
                                     Log.d(TAG, "Interrupted while waiting for operation to complete");
                                 }
                               }
                               was_signalled = false;
                           }
                                //since oncharacteristic changed will be called , the code for Tput is written there
                            }
                            mreadChar.setValue(DataRxClass.numNotifications,BluetoothGattCharacteristic.FORMAT_UINT8,0);
                            mBleConnect.mBluetoothGatt.writeCharacteristic(mreadChar);
                            Log.d(TAG, "after char write");
                             synchronized (write_mutex) {
                               // Wait for write response
                                if(!was_signalled) {
                                  try {
                                     Log.d(TAG, "before mutex lock");
                                     write_mutex.wait();
                                     Log.d(TAG, "mutex lock");
                                  }
                                  catch (InterruptedException e) {
                                     Log.d(TAG, "Interrupted while waiting for operation to complete");
                                  }
                               }
                               was_signalled = false;
                              }

                            long currentTime = SystemClock.elapsedRealtime();
                        } else {
                            Log.d(TAG, "onConnectionStateChange: null characteristic ");
                          }
                    } else {
                        Log.d(TAG, "onConnectionStateChange: null service");
                      }
                }
            }
        }

        private class TALatencyMeasurement extends State {
            private static final String TAG = "TALatencyMeasurement";
            LatencyTest LatClass;

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                LatClass = (LatencyTest)txtParse.objects.remove();
                Log.d(TAG, "LatService:"+LatClass.latService.toString());
                Log.i(TAG, "latency thread creation");
                LatencyTestthread lt = new LatencyTestthread();
                Thread t = new Thread(lt);
                if(!t.isAlive()) {
                  t.start();
                }
                else {
                    Log.i(TAG, "Thread is already running");
                }
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);

                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                    case TA_DEV_DISCONNECT:
                        transitionTo(mTADisconnect);
                        break;
                    case TA_LAT_TEST_DONE:
                        Log.d(TAG, "state change to connected");
                        transitionTo(mTAConnected);
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }
            class LatencyTestthread implements Runnable {

                public void run() {
                    final UUID UUID_LAT_SERVICE = UUID.fromString(LatClass.latService);
                    final UUID UUID_LAT_CHAR = UUID.fromString(LatClass.latChar);
                    Log.i(TAG, "Latency test start");
                    wait_for_gatt_service_discovery();
                    String str = "LatencyTest";
                    BluetoothGattService mService = mBleConnect.mBluetoothGatt.getService(UUID_LAT_SERVICE);
                    if (mService != null) {
                        BluetoothGattCharacteristic mCharacteristic = mService.getCharacteristic(UUID_LAT_CHAR);
                        if (mCharacteristic != null) {
                            mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            mCharacteristic.setValue(str);
                            long latency_start_time_stamp = SystemClock.elapsedRealtime();
                            mBleConnect.mBluetoothGatt.writeCharacteristic(mCharacteristic);
                            synchronized (write_mutex) {
                                // Wait for write response
                             if(!was_signalled) {
                                try {
                                    write_mutex.wait();
                                }
                                catch (InterruptedException e) {
                                    Log.d(TAG, "Interrupted while waiting for operation to complete");
                                }
                             }
                             was_signalled = false;
                            }
                            long latency_end_time_stamp = SystemClock.elapsedRealtime();
                            float latency = (float) (latency_end_time_stamp - latency_start_time_stamp);
                            Log.d(TAG, "Latency in msec " + latency);
                            mStateMachine.sendMessage(TestAppConnectionStateMachine.TA_LAT_TEST_DONE);
                        } else {
                            Log.e(TAG, "writeToCharacteristic() - Characteristic is null! " + mBleConnect.mCharacteristic);
                        }
                    } else {
                        Log.d(TAG, "service with UUID not found ");
                     }
                }
            }
        }

        private class TADisconnect extends State {
            private static final String TAG = "TADisconnect";

            @Override
            public void enter() {
                Log.i(TAG, "Enter: " + getCurrentMessage().what);
                disconnect();
            }

            @Override
            public void exit() {
                Log.i(TAG, "Exit: " + getCurrentMessage().what);
            }

            private void disconnect() {
                mBleConnect.mBluetoothGatt.disconnect();
            }

            @Override
            public boolean processMessage(Message message) {
                Log.i(TAG, "processMessage: " + message.what);

                boolean retValue = HANDLED;
                switch (message.what) {
                    case TA_BT_ADAPTER_OFF:
                        transitionTo(mTAIdle);
                        Log.i(TAG, "BT Adapter is off");
                    case TA_REM_DEV_DISCONNECTED:
                        transitionTo(mTAIdle);
                        Toast.makeText(getApplicationContext(), "Device Disconnected", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return retValue;
            }
        }
    }
}
