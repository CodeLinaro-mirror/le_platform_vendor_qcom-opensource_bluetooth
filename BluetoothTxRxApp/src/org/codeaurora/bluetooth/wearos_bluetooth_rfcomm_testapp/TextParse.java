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

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;

import static android.content.ContentValues.TAG;

public class TextParse {
    private static final String TAG="BluetoothTxRxApp TextParse";
    Queue<Object> objects = new LinkedList<Object> ();
    public  TextParse() throws Exception
    {
        //Queue used to store objects of connect ,scan and adv type
        Queue<String> Parameters = new LinkedList<String>();
        int [] arr=new int[10000];
        int i=0;

        Log.d(TAG,"Reading the contents of the config.txt from system/bin file into an InputStream" );
        String file="/system/bin/config.txt";
        try {
            int f=1;
             BufferedInputStream bf = new BufferedInputStream(new FileInputStream(file));
             BufferedReader br= new BufferedReader(new InputStreamReader(bf));
             String data;
            //Read till end of file
            while ((data = br.readLine())!=null) {
                String[] a = data.split("\\r?\\n|\\r");
                String[] strPattern = {"*****************************************************"};
                if (a[0].equals(strPattern[0]))
                    continue;
                String[] b = a[0].split("=");
                Log.d(TAG,""+b[0]+"-"+b[1]);
                if(b[0].equals("Discoverability")) {
                    Log.d(TAG, "Discoverability");
                    arr[i]=1;
                    i++;
                } else if(b[0].equals("Connect")) {
                    Log.d(TAG, "Connect");
                    arr[i]=2;
                    i++;
                } else if(b[0].equals("Send")) {
                    Log.d(TAG, "Send");
                    arr[i]=3;
                    i++;
                } else if(b[0].equals("Receive")) {
                    Log.d(TAG, "Receive");
                    arr[i]=4;
                    i++;
                } else if(b[0].equals("Disconnect")) {
                    Log.d(TAG, "Disconnect");
                    arr[i]=5;
                    i++;
                } else if(b[0].equals("Unpair")) {
                    Log.d(TAG, "Unpair");
                    arr[i]=6;
                    i++;
                } else if(b[0].equals("Scan")) {
                    Log.d(TAG, "Scan");
                    arr[i]=7;
                    i++;
                } else if(b[0].equals("Accept")) {
                    Log.d(TAG, "Accept");
                    arr[i]=8;
                    i++;
                } else {
                    if(b.length==2){
                        Parameters.add(b[1]);
                }
            }
        }
        br.close();
        } catch (Exception e){
            Log.e(TAG, " error is "+e.toString());
        }

        int j=0;
        String s=null;
        while(j<i)
        {
            if(arr[j]==1)
            {
                objects.add("Discoverability");
                s=Parameters.poll();
                Discoverability d=new Discoverability();
                d.time=Integer.parseInt(s);
                objects.add(d);
                Log.d(TAG,"Added Discoverability");
            }

            if(arr[j]==2)
            {
                objects.add("Connect");
                s=Parameters.poll();
                Connect c=new Connect();
                c.BDaddress=s;
                objects.add(c);
                Log.d(TAG,"Added Connect");
            }

            if(arr[j]==3)
            {
                objects.add("Send");
                s=Parameters.poll();
                Send c=new Send();
                c.BDaddress=s;
                objects.add(c);
                Log.d(TAG,"Added Send");
            }

            if(arr[j]==4)
            {
                objects.add("Receive");
                s=Parameters.poll();
                Receive c=new Receive();
                c.BDaddress=s;
                objects.add(c);
                Log.d(TAG,"Added Receive");
            }

            if(arr[j]==5)
            {
                objects.add("Disconnect");
                s=Parameters.poll();
                Disconnect c=new Disconnect();
                c.BDaddress=s;
                objects.add(c);
                Log.d(TAG,"Added Disconnect");
            }

            if(arr[j]==6)
            {
                objects.add("Unpair");
            }

            if(arr[j]==7)
            {
                objects.add("Scan");
                s=Parameters.poll();
                Scan c=new Scan();
                c.BDaddress=s;
                objects.add(c);
                Log.d(TAG,"Added Scan");
            }

            if(arr[j]==8)
            {

                objects.add("Accept");
                Log.d(TAG,"Added Accept");
            }
            j++;
        }
        //divide the contents of text file into separate objects in queue
    }

}
