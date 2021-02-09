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
import android.os.CountDownTimer;
import android.bluetooth.BluetoothServerSocket;
import android.os.Build;

import com.android.internal.util.IState;
import com.android.internal.util.StateMachine;
import com.android.internal.util.State;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.Menu;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import android.content.pm.PackageManager;
import java.util.UUID;
import static android.bluetooth.BluetoothAdapter.SCAN_MODE_NONE;
import static android.content.ContentValues.TAG;
import static android.widget.Toast.makeText;

import java.lang.StringBuilder;

public class MainActivity extends Activity {
    private static final String TAG="BluetoothTxRxApp MainActivity";
    private static final String[] INITIAL_PERMS={
       Manifest.permission.ACCESS_FINE_LOCATION,
       Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final int INITIAL_REQUEST=1337;

    BSPPStateMachine mStateMachine;
    private Context mContext;

    //class which parses the config.txt
    TextParse T;

    //for new scanned device
    BluetoothDevice dev;

    boolean stateMachinestarted=false;

    public UUID APP_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public static BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
    String btAddressRemote;

    //HashMap to store paired devices and corresponding SM object
    HashMap<BluetoothDevice, BSPPStateMachine> pairedDevices=new HashMap<BluetoothDevice, BSPPStateMachine>();

    //HashMap to store connected device and corresponding Socket object
    HashMap<BluetoothDevice, BluetoothSocket> Sockets=new HashMap<BluetoothDevice, BluetoothSocket>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext=getApplicationContext();

        if (!canAccessLocation()) {
           requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
        }

        if(!initAdapter()) {
            Log.d(TAG, "Unexpected error: Turning off BT");
        } else {
            try {
                T = new TextParse();
                Log.d(TAG,"size: "+T.objects.size());
            } catch (Exception e) {
                e.printStackTrace();
            }

            //start discoverability if discoverability is set to true in config.txt
            if(!T.objects.isEmpty() && "Discoverability".equals(T.objects.peek())) {
                //Discoverability is ON
                Log.d(TAG, "Setting Discoverability");
                String S=(String)T.objects.remove();
                Discoverability d=(Discoverability)T.objects.remove();
                Discover(d.time);
            }

            //start Scan if Scan is set to true in config.txt
            if(!T.objects.isEmpty() && "Scan".equals(T.objects.peek())) {
                 //scanning starts and pair request will be sent
	         Log.d(TAG, "Setting up Scan");
                 String S=(String)T.objects.remove();
                 Scan o=(Scan)T.objects.remove();
                 btAddressRemote=o.BDaddress;

                 //Intent is registered when scan results are received
                 IntentFilter deviceFoundIntent=new IntentFilter(BluetoothDevice.ACTION_FOUND);
                 registerReceiver(myReceiver,deviceFoundIntent);

                 Log.d(TAG, "going to start discovery");

                 if(!bluetoothAdapter.startDiscovery())
                     Log.d(TAG, "Could not start Discovery");
           }
        }
     } //end of OnCreate()


     private final BroadcastReceiver myReceiver=new BroadcastReceiver() {
         private static final String TAG="BluetoothTxRxApp Broadcast Receiver";
         @Override
         public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Inside onReceive: ");
            String action=intent.getAction();
            //Bluetooth Adapter State Change
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
               Log.d(TAG, "ACTION_STATE_CHANGED");
               final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,bluetoothAdapter.ERROR);
               switch(state){
                   case BluetoothAdapter.STATE_OFF:
                       Log.d(TAG, "BluetoothAdapter.STATE_OFF");
                       break;
                   case BluetoothAdapter.STATE_TURNING_ON:
                       Log.d(TAG, "BluetoothAdapter.STATE_TURNING_ON");
                       break;
                   case BluetoothAdapter.STATE_ON:
                       Log.d(TAG, "BluetoothAdapter.STATE_ON");
                       Toast toastStateON=makeText(getApplicationContext(),"Bluetooth State ON",Toast.LENGTH_SHORT);
                       toastStateON.show();
                       Log.d(TAG, "going to start discovery");
                       Toast toast=makeText(getApplicationContext(),"going to start discovery",Toast.LENGTH_SHORT);
                       toast.show();

                       if(!bluetoothAdapter.startDiscovery())
                           Log.d(TAG, "Could not start Discovery");
                       break;
                   case BluetoothAdapter.STATE_TURNING_OFF:
                       Log.d(TAG, "BluetoothAdapter.STATE_TURNING_OFF");
                       Toast toastStateOFF=makeText(getApplicationContext(),"Bluetooth State OFF",Toast.LENGTH_SHORT);
                       toastStateOFF.show();
                       break;
               }
            }
            //Bluetooth Adapter Scan Mode change
            if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
                Log.d(TAG, "ACTION_SCAN_MODE_CHANGED");
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,bluetoothAdapter.ERROR);
                switch(mode){
                       case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                       Log.d(TAG, "SCAN_MODE_CONNECTABLE_DISCOVERABLE");
                       Toast toastDiscoverable=makeText(getApplicationContext(),"Scan Mode discoverable",Toast.LENGTH_SHORT);
                       toastDiscoverable.show();
                       break;
                   case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                       Log.d(TAG, "SCAN_MODE_CONNECTABLE");
                       Toast toastConnectable=makeText(getApplicationContext(),"Scan Mode connectable",Toast.LENGTH_SHORT);
                       toastConnectable.show();
                       break;
                   case BluetoothAdapter.SCAN_MODE_NONE:
                       Log.d(TAG, "SCAN_MODE_NONE");
                       break;
                }
            }

            //Bluetooth Device found in Scan
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d(TAG, "ACTION_FOUND");
                dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Bluetooth scan object" + dev.getName());
                Log.d(TAG, "Bluetooth scan object" + dev.getAddress());
                Log.d(TAG, "btAddressRemote " + btAddressRemote);

                //check if it matches the device address we have provided in config file
                //public address of the remote device
                if (btAddressRemote.equals(dev.getAddress())){
                   Toast remoteDeviceFound=makeText(getApplicationContext(),"Remote device found",Toast.LENGTH_SHORT);
                   remoteDeviceFound.show();

                   //Intent is registered to check if bond state changed once bonding procedure starts
                   IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                   registerReceiver(myReceiver, intentFilter);

                   //cancel any ongoing discovery
                   bluetoothAdapter.cancelDiscovery();
                   Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                   boolean isPaired = false;

                   //If there are paired devices
                   if (bondedDevices.size() > 0) {
                       //Loop through paired devices
                       for (BluetoothDevice device : bondedDevices) {
                           //Add the name and address to an array adapter to show in a ListView
                           if(btAddressRemote.equals(device.getAddress()) && (isPaired == false)){
                               Log.d(TAG, "The device is already bonded.");
                               isPaired = true;
                               start_state_machine(device);
                               break;
                           }
                       }
                   }

                   if(!isPaired) {
                       //attempt pairing with discovered device
                       Log.d(TAG, "attempt pairing with the device address: " + btAddressRemote);
                       dev.createBond();
                   }
               }
            }


            //Bluetooth Device Bond State Change
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                 Log.d(TAG, "ACTION_BOND_STATE_CHANGED");
                 BluetoothDevice dev=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                 if(dev.getBondState()==BluetoothDevice.BOND_BONDED){
                     Log.d(TAG, "BluetoothDevice.BOND_BONDED.");
                     Toast toastBonded=makeText(getApplicationContext(),"Device is Bonded",Toast.LENGTH_SHORT);
                     toastBonded.show();

                     bluetoothAdapter.cancelDiscovery();
                     start_state_machine(dev);
                 }
                 if(dev.getBondState()==BluetoothDevice.BOND_BONDING){
                     Log.d(TAG, "BluetoothDevice.BOND_BONDING");
                 }
                 if(dev.getBondState()==BluetoothDevice.BOND_NONE){
                     Log.d(TAG, "BluetoothDevice.BOND_NONE");
                 }
            }
        }
    }; //end of broadcast receiver

    // to turn on bluetooth if Bluetooth Adapter is not NULL
    //if bluetooth adapter is NULL it returns false else true after turning ON bluetooth
    boolean initAdapter(){
        Log.d(TAG, "initAdapter()");
        if(bluetoothAdapter==null)
        {
            Log.d(TAG, "initAdapter: Bluetooth not supported");
            return false;
        }
        else if(!bluetoothAdapter.isEnabled())
        {
            Log.d(TAG, "enabling bluetooth ");
            Intent enableBtIntent=new Intent(bluetoothAdapter.ACTION_REQUEST_ENABLE);
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(myReceiver, filter);

            startActivity(enableBtIntent);
            Log.d(TAG, "Bluetooth enabled returning true");
            return true;
        } else {
            Log.d(TAG, "initAdapter: Bluetooth has been enabled now");
            Toast toastState=makeText(getApplicationContext(),"Bluetooth State ON",Toast.LENGTH_SHORT);
            toastState.show();
            return true;
        }
    }//end of initAdapter

    public void Discover(int time) {
        Log.d(TAG, "Discover()");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, time);
        startActivity(discoverableIntent);

        //Intent is registered to receive scan mode changes
        IntentFilter scanModeIntentFilter =new IntentFilter(bluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(myReceiver,scanModeIntentFilter);

        //Intent is registered to receive bond state changes
        IntentFilter bondStateIntentFilter =new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(myReceiver,bondStateIntentFilter);
    }//end of Discover

    // start state machine
    // function to add Bluetooth device and SM object in pairedDevices HashMap
    private void start_state_machine(BluetoothDevice device){
       Log.d(TAG, "start_state_machine() device:" + device.getAddress());
       if(pairedDevices.isEmpty()) {
           Log.d(TAG,"BSPPStateMachine Adding this new device to the hashmap");
           BSPPStateMachine SM=new BSPPStateMachine(mContext);
           mStateMachine=SM;
           pairedDevices.put(device, mStateMachine);
           mStateMachine.start();
       } else {
           if(!pairedDevices.containsKey(device)) {
               BSPPStateMachine SM=new BSPPStateMachine(mContext);
               mStateMachine=SM;
               pairedDevices.put(device,mStateMachine);
               mStateMachine.start();
           }
       }
    }//end of start_state_machine

    //State Machine class
    public class BSPPStateMachine extends StateMachine {
        private static final String TAG="BluetoothTxRxApp BSPP_STATE MACHINE";
        private Context mContext;
        int i=0;
        long tx_start_time,tx_end_time;
        long rx_start_time,rx_end_time;

        //declaring states
        private Paired mpaired;
        private ConnectPending mconnectpending;
        private Connected mconnected;
        private DataRx mdatarx;
        private DataTx mdatatx;
        private Disconnecting mdisconnecting;

        // declaring message constants
        static final int STATE_CONNECTING=25;
        static final int STATE_CONNECTED=26;
        static final int STATE_CONNECTION_FAILED=27;

        static final int STATE_BONDED=29;
        static final int STATE_BONDING=30;
        static final int STATE_BOND_NONE=31;
        static final int STATE_UNPAIRING=32;
        static final int STATE_RX_DONE=33;
        static final int STATE_TX_DONE=34;
        static final int STATE_MESSAGE_SENT=35;
        static final int STATE_MESSAGE_SENDING_FAILED=36;
        static final int STATE_MESSAGE_RECEIVED=37;
        static final int STATE_MESSAGE_RECEIVE_FAILED=38;
        static final int STATE_DISCONNECTED=39;
        static final int STATE_DISCONNECTING=40;

        //Receiving msg across socket
        String readmsg;

        private BSPPStateMachine(Context context){
            super("BSPPStateMachine");
            this.mContext=context;

            //initialising states
            mpaired=new Paired();
            mconnectpending=new ConnectPending();
            mconnected=new Connected();
            Log.d(TAG,"BSPP_constructor");
            mdatarx=new DataRx();
            mdatatx=new DataTx();
            mdisconnecting=new Disconnecting();
            i=0;

            //adding states
            addState(mpaired);
            addState(mconnectpending);
            addState(mconnected);
            addState(mdatarx);
            addState(mdatatx);
            addState(mdisconnecting);

            // set initial state to Paired state
            Log.d(TAG,"setting initial state as paired state");
            setInitialState(mpaired);
       }//end of constructor

        //Quit state machine
        public void doQuit() {
            Log.d(TAG,"Quit SM");
            synchronized (BSPPStateMachine.this)
            {
                stateMachinestarted=false;
                quitNow();
            }
        }

        //function to disconnect a device with given BD address
        //If disconnect flag is set in the config file as true,
        //Once the tx/rx is done , application disconnects the
        //socket. This function is called from the disconnecting
        //state
        private void resetConnection(BluetoothDevice bd) {
            Log.d(TAG,"Inside Reset Connection function");
            BluetoothSocket sock=Sockets.get(bd);
            if (sock != null) {
                try {
                    InputStream I=sock.getInputStream();
                    OutputStream O=sock.getOutputStream();
                    if(I!=null)
                        I.close();
                    if(O!=null)
                        O.close();
                    sock.close();
                } catch (Exception e) {}

                Sockets.remove(bd);

                mStateMachine=pairedDevices.get(bd);
                Message message=Message.obtain();
                message.what=STATE_DISCONNECTED;
                mStateMachine.sendMessage(message);
            }
        }//end of resetConnection

        //First state in the state machine is "Paired"
        //it checks for flags "Connect" and"Accept"
        //and moves to "ConnectPending" state
        private class Paired extends State {
            private static final String TAG="BluetoothTxRxApp Paired State";
            @Override
            public void enter() {
                Log.d(TAG, "enter()");
                if(!T.objects.isEmpty() && "Connect".equals(T.objects.peek())){
                    // go to connection pending state and get the object of Connect type there
                    Log.d(TAG,"Connect flag is true; to connect pending now");
                    transitionTo(mconnectpending);
                }

                if(!T.objects.isEmpty() && "Accept".equals(T.objects.peek())){
                    // go to connection pending state and get the object of Connect type there
                    Log.d(TAG,"Accept flag is true; to connect pending now");
                    transitionTo(mconnectpending);
                }
            }

            @Override
            public void exit() {
                Log.d(TAG, "exit: Exiting paired state now");
            }

            @Override
            public boolean processMessage(Message message) {
                boolean retvalue=HANDLED;
                switch(message.what)
                {
                    //unpair that device
                    case STATE_UNPAIRING:
                        Log.d(TAG, "processMessage: Device is unpairing");
                        Toast toast=makeText(getApplicationContext(),"Device is unpairing now",Toast.LENGTH_SHORT);
                        toast.show();
                        if(pairedDevices.isEmpty())
                            doQuit();
                        break;
                }
                return retvalue;
            }
        }//end of Paired State

        //Paired State transitions to Connect Pending State
        //In this state it checks whether it has to start the
        //connect thread or connect accept thread
        private class ConnectPending extends State {
            private static final String TAG="BluetoothTxRxApp ConnectPending State";
            @Override
            public void enter() {
                Log.d(TAG,"enter()");
                bluetoothAdapter.cancelDiscovery();

                //create connection, start connect thread
                if(!T.objects.isEmpty() && "Connect".equals(T.objects.peek())) {
                    String S=(String)T.objects.remove();
                    Connect o = (Connect)T.objects.remove();
                    BluetoothDevice ConnectDevice = bluetoothAdapter.getRemoteDevice(o.BDaddress);
                    Log.d(TAG,"Going to connect thread");
                    ConnectThread C = new ConnectThread(ConnectDevice);
                    Log.d(TAG,"Connect thread starting now");
                    C.start();
                }

                //accept connection request, start connect accept thread
                if(!T.objects.isEmpty() && "Accept".equals(T.objects.peek())){
                    String S=(String)T.objects.remove();
                    Log.d(TAG,"Going to connect accept thread");
                    ConnectAccept CA=new ConnectAccept();
                    Log.d(TAG,"ConnectAccept thread starting now");
                    CA.start();
                }
            }

            @Override
            public void exit() {
                Log.d(TAG, "Exiting Connect pending state");
            }

            @Override
            public boolean processMessage(Message message) {
                boolean retvalue=HANDLED;
                switch(message.what)
                {
                    //if the connection is successful go to connected state
                    case STATE_CONNECTED:
                        Log.d(TAG,"Going to connected state");
                        transitionTo(mconnected);
                        break;
                    //if connection is not successful, go to paired state
                    case STATE_CONNECTION_FAILED:
                        transitionTo(mpaired);
                        Log.d(TAG,"Going to Paired state");
                        break;
                    case STATE_DISCONNECTED:
                        transitionTo(mdisconnecting);
                        Log.d(TAG,"Going to disconnecting state");
                        break;
                }
                return retvalue;
            }
        }//end of Connect Pending State

        //When the socket connection is complete either by the
        //Connect thread or Connect Accept thread, state from
        //"connect pending" goes to "Connected"
        private class Connected extends State {
            private static final String TAG="BluetoothTxRxApp Connected State";
            @Override
            public void enter() {
                Log.d(TAG, "Inside Connected state");
                // if Send is encountered go to dataTx state
                if(!T.objects.isEmpty() && "Send".equals(T.objects.peek())){
                    Log.d(TAG, "Send flag is true");
                    String S=(String)T.objects.remove();
                    transitionTo(mdatatx);
                }

                //if Receive flag is encountered go to datarx state
                if(!T.objects.isEmpty() && "Receive".equals(T.objects.peek())){
                    Log.d(TAG, "Receive flag is true");
                    String S=(String)T.objects.remove();
                    transitionTo(mdatarx);
                }

                if(!T.objects.isEmpty() && "Disconnect".equals(T.objects.peek())){
                    Log.d(TAG, "Disconect flag is true");
                    String S=(String)T.objects.remove();
                    transitionTo(mdisconnecting);
                }

                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
                filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                registerReceiver(mReceiver, filter);

            }

            @Override
            public void exit() {
                Log.d(TAG, "Exiting Connected state");
            }

            @Override
            public boolean processMessage(Message message) {
                boolean retvalue=HANDLED;
                switch(message.what)
                {
                    case STATE_DISCONNECTED:
                        transitionTo(mdisconnecting);
                        break;
                }
                return retvalue;
            }
        }//end of Connected State

        //If "Receive" flag is set in config.txt
        //Then from "Connected" state , application
        //transitions to "DataRx" state
        private class DataRx extends State {
            private static final String TAG="BluetoothTxRxApp DataRx";
            @Override
            public void enter() {
                Log.d(TAG, "Inside DataRx state");
                Receive o=(Receive)T.objects.remove();
                BluetoothDevice bd=bluetoothAdapter.getRemoteDevice(o.BDaddress);
                Log.d(TAG, "Starting Receive thread");

                if(Sockets.get(bd) == null){
                    Log.d(TAG,"Don't start receive thread");
                } else {
                    //start Receive thread
                    Toast toast=makeText(getApplicationContext(),"Starting Receive thread",Toast.LENGTH_SHORT);
                    toast.show();
                    ReceiveThread receive=new ReceiveThread(Sockets.get(bd));
                    receive.start();
                }

                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
                filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                registerReceiver(mReceiver, filter);
            }

            @Override
            public void exit() {
                Log.d(TAG, "Exiting DataRx state");
            }

            @Override
            public boolean processMessage(Message message) {
                boolean retvalue=HANDLED;
                switch(message.what)
                {
                    // after msg tranfer takes place go back to Connected State
                    case STATE_MESSAGE_RECEIVED:
                        Log.d(TAG,"Msg received successfully,going to connected state");
                        Toast textRecvToast=makeText(getApplicationContext(),"mesg recvd from Remote device",Toast.LENGTH_SHORT);
                        textRecvToast.show();
                        transitionTo(mconnected);
                        break;
                    case STATE_DISCONNECTED:
                        transitionTo(mdisconnecting);
                        break;
                    case STATE_MESSAGE_RECEIVE_FAILED:
                        transitionTo(mconnected);
                        break;
                }
                return retvalue;
            }
        }//end of DataRx State

        //If "Send" flag is set in the config.txt
        //Then from "Connected" state , application
        //transitions to "datatx" state
        private class DataTx extends State {
            private static final String TAG="BluetoothTxRxApp DataTx";
            @Override
            public void enter() {
                readmsg=null;
                Send o=(Send)T.objects.remove();
                BluetoothDevice bd=bluetoothAdapter.getRemoteDevice(o.BDaddress);
                //if the BD address is found ih the socket array, retrieve that socket
                // and send the data through that socket
                Log.d(TAG,"check if socket is available for the bdaddress");
                BluetoothSocket tmp = Sockets.get(bd);
                if(tmp == null){
                    Log.d(TAG,"tmp is null,There will be an exception in the SendThread");
                }
                Log.d(TAG, "Going to send thread");
                SendThread send=new SendThread(Sockets.get(bd));
                //go to send thread
                Log.d(TAG, "Starting send thread");
                send.start();
                int chunksize = 1000;
                chunksize = chunksize * 1024;
                StringBuilder sb = new StringBuilder(chunksize);
                for (int i=0; i < chunksize; i++) {
                    sb.append('a');
                }
                String senttext = sb.toString();
                String start = "Start";
                String end = "End";
                bluetoothAdapter.cancelDiscovery();
                tx_start_time=SystemClock.elapsedRealtime();
                Log.d(TAG,"start time: "+tx_start_time);

                send.write(start.getBytes());
                send.write(senttext.getBytes());
                send.write(end.getBytes());

                tx_end_time=SystemClock.elapsedRealtime();
                Log.d(TAG, "end time: "+tx_end_time);
                Log.d(TAG,"end_time - start_time: " + (tx_end_time - tx_start_time));

                //throughput calculations
                float TxTput=((float)senttext.length()*8*1000)/(tx_end_time-tx_start_time);
                float TxTputk=TxTput/1000;
                Log.d(TAG, "write: Through put (send) is (in kbps): "+TxTputk);

                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
                filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                registerReceiver(mReceiver, filter);
            }

            @Override
            public void exit() {
                Log.d(TAG, "Exiting send state");
            }

            @Override
            public boolean processMessage(Message message) {
                boolean retvalue=HANDLED;
                switch(message.what)
                {
                    // after message has been sent successfully get back to connected state
                    case STATE_MESSAGE_SENT:
                      Log.d(TAG,"Going to connected state");
                      transitionTo(mconnected);
                      break;
                    case STATE_DISCONNECTED:
                      transitionTo(mdisconnecting);
                      break;
                    case STATE_MESSAGE_SENDING_FAILED:
                      transitionTo(mconnected);
                      break;
                }
                return retvalue;
            }
        }//end of Send State

        private class Disconnecting extends State {
            private static final String TAG="BluetoothTxRxApp Disconnecting State";
            @Override
            public void enter() {

                Log.d(TAG, "Inside Disconnecting state");
                Disconnect o=(Disconnect)T.objects.remove();
                // remove the BDaddress and socket from socket array and move to paired state
                BluetoothDevice bd=bluetoothAdapter.getRemoteDevice(o.BDaddress);
                Log.d(TAG, "BDaddress: " + o.BDaddress);
                //close socket for device to disconnect
                Log.d(TAG,"Going to reset function");

                Toast toast=makeText(getApplicationContext(),"Disconnectiong ... ",Toast.LENGTH_SHORT);
                toast.show();
                if(bd!=null)
                    resetConnection(bd);
            }
            @Override
            public void exit() {
                Log.d(TAG, "Exiting disconnecting state");
            }
            @Override
            public boolean processMessage(Message message) {
                boolean retvalue=HANDLED;
                switch(message.what)
                {
                    case STATE_DISCONNECTED:
                    transitionTo(mpaired);
                }
                return retvalue;
            }
        }//end of Disconnected State

        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mStateMachine=pairedDevices.get(device);

                if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                    //Device is about to disconnect
                    Message message = Message.obtain();
                    message.what = STATE_DISCONNECTING;
                    mStateMachine.sendMessage(message);
                }
                else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    //Device has disconnected
                    Message message = Message.obtain();
                    message.what = STATE_DISCONNECTED;
                    mStateMachine.sendMessage(message);
                }
            }
        };

        //Connect thread is started if config.txt file
        //has "Connect" flag is set, Connect thread starts a
        //RFCOMM socket and socket connects to a remote device
        //and transitions to "Connected" state after connection
        //to a remote device
        private class ConnectThread extends Thread {
            private static final String TAG="BluetoothTxRxApp Connect Thread";
            private final BluetoothSocket mmSocket;
            private final BluetoothDevice mmDevice;

            public ConnectThread(BluetoothDevice device) {
                //Use a temporary object tmp to first create a BluetoothSocket and then assign it,
                //because mmSocket is final
                BluetoothSocket tmp = null;

                //Remote device object
                mmDevice = device;

                if (mmDevice == null){
                    Log.d(TAG, "ConnectThread: Remote Device is null");
                    Toast toast=makeText(getApplicationContext(),"ConnectThread:Remote Device is null",Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Log.d(TAG, "ConnectThread: Device is not null");
                }

                try {
                    // Get a BluetoothSocket to connect with the given BluetoothDevice.
                    // APP_UUID is the app's UUID string, also used in the server code.
                    Log.d(TAG, "Creating a BluetoothSocket to create a connection to a remote device");
                    tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
                } catch (IOException e) {
                    Log.e(TAG, "Socket's create() method failed", e);
                }

                if (tmp == null)
                {
                    Log.d(TAG, "tmp is null");
                    Toast toast=makeText(getApplicationContext(),"ConnectThread: tmp is null",Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Log.d(TAG, "Device is not null");
                }

                //Once socket was created assigning it to the final variable
                mmSocket = tmp;
            }

            @Override
            public void run() {
                // Cancel discovery because it otherwise slows down the connection.
                bluetoothAdapter.cancelDiscovery();

                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    Log.d(TAG, "connecting to a remote device");
                    mmSocket.connect();
                    Log.d(TAG, "Socket Connected");
                    mStateMachine=pairedDevices.get(mmDevice);
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    mStateMachine.sendMessage(message);

                } catch (IOException connectException) {
                    // Unable to connect; close the socket and return.
                    Log.e(TAG, "Unable to connect; close the socket and return", connectException);
                    try {
                        mmSocket.close();
                        mStateMachine=pairedDevices.get(mmDevice);
                        Message message = Message.obtain();
                        message.what = STATE_CONNECTION_FAILED;
                        mStateMachine.sendMessage(message);
                    } catch (IOException closeException) {
                        Log.e(TAG, "Could not close the client socket", closeException);
                    }
                    return;
                }
                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                //Add connected device along with socket to HashMap
                Sockets.put(mmDevice,mmSocket);
            }
            // Closes the client socket and causes the thread to finish.
            public void cancel() {
                try {
                    mmSocket.close();
                    mmSocket.getInputStream().close();
                    mmSocket.getOutputStream().close();
                    mStateMachine=pairedDevices.get(mmDevice);
                    Message message = Message.obtain();
                    message.what = STATE_DISCONNECTED;
                    mStateMachine.sendMessage(message);
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the client socket", e);
                }
            }
        }//end of Connect Thread

        //ConnectAccept thread is started if config.txt file
        //has "Accept" flag is set, Connect Accept thread starts a
        //RFCOMM Server socket and waits for a  remote device
        //to connect to it and transitions to "Connected" state
        //after connection to a remote device
        private class ConnectAccept extends Thread{
            private static final String TAG="BluetoothTxRxApp ConnectAccept Thread";
            private BluetoothServerSocket serverSocket;
            public ConnectAccept(){
                try {
                   Log.d(TAG, "Setting up BluetoothServerSocket for listening to connection requests from client");
                   serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord("BSPP_STATE",APP_UUID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            public void run(){
                BluetoothSocket socket=null;
                while(socket==null) {
                    try {
                        Log.d(TAG,"Trying to connect");
                        Message message=Message.obtain();
                        message.what=STATE_CONNECTING;
                        mStateMachine.sendMessage(message);
                        socket=serverSocket.accept();
                    } catch (IOException e) {
                        Log.d(TAG,"STATE_CONNECTION_FAILED");
                    }

                    if(socket!=null)
                    {
                        Log.d(TAG,"Connected");
                        Sockets.put(socket.getRemoteDevice(),socket);
                        Message message=Message.obtain();
                        message.what=STATE_CONNECTED;
                        mStateMachine.sendMessage(message);
                        break;
                    }
                }
            }
        }//end of Connect Accept thread

        //Send thread writes data to the output stream
        public class SendThread extends Thread{
            private static final String TAG="BluetoothTxRxApp Send Thread";
            private final BluetoothSocket mmSocket;
            public final OutputStream mmOutStream;

            public SendThread(BluetoothSocket socket){
                mmSocket = socket;
                OutputStream tmpOut=null;
                try {
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating output stream", e);
                    Toast toast=makeText(getApplicationContext(),"Error occurred when creating output stream",Toast.LENGTH_SHORT);
                    toast.show();
                }
                mmOutStream=tmpOut;
            }

            // Call this from the main activity to send data to the remote device.
            public void write(byte[] bytes) {
                try {
                    String str=new String(bytes);
                    //write to output stream
                    mmOutStream.write(bytes);

                    //If string contains "End" then go back to
                    //"Connected" state
                    if(str.contains("End")){
                        mStateMachine=pairedDevices.get(mmSocket.getRemoteDevice());
                        Message message = Message.obtain();
                        message.what = STATE_MESSAGE_SENT;
                        mStateMachine.sendMessage(message);
                    }
                } catch (IOException e) {
                   Log.e(TAG, "Error occurred when sending data", e);
                    Toast toast=makeText(getApplicationContext(),"Error occurred when sending data",Toast.LENGTH_SHORT);
                    toast.show();
                    mStateMachine=pairedDevices.get(mmSocket.getRemoteDevice());
                    Message message = Message.obtain();
                    message.what = STATE_MESSAGE_SENDING_FAILED;
                    mStateMachine.sendMessage(message);
                }
            }

            // Call this method from the main activity to shut down the connection.
            public void cancel() {
                try {
                    if(mmOutStream!=null){
                        try {
                            mmOutStream.close();
                        } catch (Exception e) {
                           Log.e(TAG, "Could not close output stream", e);
		        }
                        mmSocket.close();
                        mStateMachine=pairedDevices.get(mmSocket.getRemoteDevice());
                        Message message = Message.obtain();
                        message.what = STATE_DISCONNECTED;
                        mStateMachine.sendMessage(message);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the connect socket", e);
                    Toast toast=makeText(getApplicationContext(),"Could not close the connect socket",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }//end of Send thread

        //Receive thread reads data from the input stream
        public class ReceiveThread extends Thread {

            private static final String TAG="BluetoothTxRxApp Receive Thread";
            private final BluetoothSocket mmSocket;
            public final InputStream mmInStream;
            private byte[] mmBuffer; // mmBuffer store for the stream

            public ReceiveThread(BluetoothSocket socket) {
                Log.d(TAG, "Inside Receive thread");
                mmSocket = socket;
                InputStream tmpIn = null;

                try {
                    tmpIn = socket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating input stream", e);
                    Toast toast=makeText(getApplicationContext(),"Error occurred when creating input stream",Toast.LENGTH_SHORT);
                    toast.show();
                }
                mmInStream = tmpIn;
            }

            public void run() {
                mmBuffer = new byte[1024];
                Log.d(TAG,"running");
                int numBytes; // bytes returned from read()
                int totalBytes = 0; //totalBytes received

                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    try {
                        // Read from the InputStream
                        numBytes = mmInStream.read(mmBuffer);
                        totalBytes = totalBytes + numBytes;
                        String incomingMsg = new String(mmBuffer, 0, numBytes);

                        //If incoming packet contains "Start"
                        //Print start time
                        if(incomingMsg.contains("Start")){
                            rx_start_time=SystemClock.elapsedRealtime();
                            Log.d(TAG,"start_time: "+rx_start_time);
                        }
                        //If incoming packet contains "end"
                        //Print end time
                        if(incomingMsg.contains("end")){
                            rx_end_time=SystemClock.elapsedRealtime();
                            Log.d(TAG, "end_time: "+rx_end_time);
                            Log.d(TAG,"end_time - start_time = "+((rx_end_time-rx_start_time)/1000));
                            Log.d(TAG,"totalBytes = "+totalBytes);
                            Log.d(TAG,"totalBits =  "+(totalBytes*8));
                            //throughput calculation
                            float RxTput=((float)totalBytes*8*1000)/(rx_end_time-rx_start_time);
                            float RxTputk=RxTput/1000;
                            Log.d(TAG, "write: Through put (receive) is approximately(in kbps): "+RxTputk);
                            //once transaction is completed move
                            //to "Connected" state
                            Message message = Message.obtain();
                            message.what = STATE_MESSAGE_RECEIVED;
                            mStateMachine.sendMessage(message);
                            break;
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "Input stream was disconnected", e);
                        Message message = Message.obtain();
                        message.what = STATE_MESSAGE_RECEIVE_FAILED;
                        mStateMachine.sendMessage(message);
                        break;
                    }

                }
            }


            // Call this method from the main activity to shut down the connection.
            public void cancel() {
                try {
                    if(mmInStream!=null)
                        try {
                            mmInStream.close();
                        } catch (Exception e) {}
                        mmSocket.close();
                        mStateMachine=pairedDevices.get(mmSocket.getRemoteDevice());
                        Message message = Message.obtain();
                        message.what = STATE_DISCONNECTED;
                        mStateMachine.sendMessage(message);
                        // make socket value associated with corresponding bluetooth device as null
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the connect socket", e);
                    Toast toast=makeText(getApplicationContext(),"Could not close connect socket",Toast.LENGTH_SHORT);
                    toast.show();

                }
            }
        }
    }//end of Receive Thread

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(myReceiver!=null)
            unregisterReceiver(myReceiver);

    }
    private boolean canAccessLocation() {
        return(hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));
    }

    private boolean hasPermission(String perm) {
        return(PackageManager.PERMISSION_GRANTED==checkSelfPermission(perm));
    }

}
