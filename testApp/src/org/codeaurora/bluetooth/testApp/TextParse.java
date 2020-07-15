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

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;
import static android.content.ContentValues.TAG;


public class TextParse {

    static Queue<Object> objects = new LinkedList<Object> ();

    public  TextParse() throws Exception
    {
        //Queue used to store objects of connect ,scan and adv type
        Queue<String> Parameters = new LinkedList<String>();
         int [] arr=new int[1000000];

         int i=0;
        Log.d("Reached", "TextParse:1");
        String file="res/raw/conf.txt";
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(file);
        //store parameters in a temporary queue Parameters
        try {
            int f=1;
            BufferedReader br= new BufferedReader(new InputStreamReader(inputStream));
            //BufferedReader br = new BufferedReader(new FileReader(file));
            Log.d("reached", "TextParse:2 ");
            String data;
            while ((data = br.readLine())!=null){

                String[] a = data.split("\n");

                        if (a[0].equals("*****************************************************"))
                            continue;
                        String[] b = a[0].split(":");
                        if(b[0].equals("adv"))
                        {Log.d("Visited", "adv");arr[i]=1;i++;MainActivity.advPresentflag=true;}
                        else if(b[0].equals("scan"))
                        {Log.d("Visited", "scan");arr[i]=2;i++;MainActivity.scanPresentflag=true;}
                        else if(b[0].equals("Connect"))
                        {Log.d("Visited", "conn");arr[i]=3;i++;}
                        else if(b[0].equals("TxPowerTest"))
                        {Log.d("Visited", "tx");arr[i]=4;i++;}
                        else if(b[0].equals("LatencyTest"))
                        {Log.d("Visited", "lat");arr[i]=5;i++;}
                        else if(b[0].equals("DataTx"))
                        {Log.d("Visited", "datatx");arr[i]=6;i++;}
                        else if(b[0].equals("DataRx"))
                        {Log.d("Visited", "datarx");arr[i]=7;i++;}
                        else if(b[0].equals("ConnUpdate"))
                        {Log.d("Visited", "connup");arr[i]=8;i++;}
                        else if(b[0].equals("Disconnect"))
                        {Log.d("Visited", "dis");arr[i]=9;i++;}
                        else if(b[0].equals("PhyUpdate"))
                        {Log.d("Visited", "phy update");arr[i]=10;i++;}
                        else if(b[0].equals("ReadPhy"))
                        {Log.d("Visited", "read phy");arr[i]=11;i++;}
                        else if(b[0].equals("Pair"))
                        {Log.d("Visited", "Pair");arr[i]=12;i++;}

                        else
                        {
                            if(b.length==2)
                            {
                                Parameters.add(b[1]);
                                Log.d(TAG, " "+b[0]+":"+b[1]);
                            }
                        }
            }
            br.close();
        }catch (Exception e){
            Log.e("main", " error is "+e.toString());
        }
            int j=0;
            String s=null;
            //divide the contents of text file into separate objects in queue
            while(j<i)
            {

                if(arr[j]==1)
                {
                    Adv adv=new Adv();
                    s=Parameters.poll();
                    Log.d(TAG, "TextParse: "+s);
                    adv.TxPower=Integer.parseInt(s);
                    s=Parameters.poll();
                    Log.d(TAG, "TextParse: "+s);
                    adv.Legacy=Boolean.parseBoolean(s);
                    s=Parameters.poll();
                    Log.d(TAG, "TextParse: "+s);
                    adv.Periodic=Boolean.parseBoolean(s);
                    s=Parameters.poll();
                    Log.d(TAG, "TextParse: "+s);
                    adv.Connectable=Boolean.parseBoolean(s);
                    s=Parameters.poll();
                    Log.d(TAG, "TextParse: "+s);
                    adv.Scannable=Boolean.parseBoolean(s);
                    s=Parameters.poll();
                    Log.d(TAG, "TextParse: "+s);
                    adv.Anonymous=Boolean.parseBoolean(s);
                    s=Parameters.poll();
                    adv.IncludePower=Boolean.parseBoolean(s);
                    s=Parameters.poll();
                    adv.PrimaryPhy=Integer.parseInt(s);
                    s=Parameters.poll();
                    adv.SecondaryPhy=Integer.parseInt(s);
                    s=Parameters.poll();
                    adv.Interval=Integer.parseInt(s);
                    s=Parameters.poll();
                    adv.TimeOutLegacy=Integer.parseInt(s);
                    s=Parameters.poll();
                    adv.AdvertiseMode=Integer.parseInt(s);
                    s=Parameters.poll();
                    adv.ServiceUuid=s;
                    s=Parameters.poll();
                    adv.ManufacturerId=Integer.parseInt(s);
                    s=Parameters.poll();
                    adv.ManufacturerData=s;
                    s=Parameters.poll();
                    adv.ServiceDataUuid=s;
                    s=Parameters.poll();
                    adv.ServiceData=s;
                    s=Parameters.poll();
                    adv.AdvTO=Integer.parseInt(s);
                    objects.add("advflag");
                    Log.d(TAG, "TextParse:adv flag");
                    objects.add(adv);
                    Log.d(TAG, "TextParse:adv");
                }

                else if(arr[j]==2)
                {
                    Scan scn=new Scan();
                    //s=Parameters.poll();
                    //scn.scanflag=Boolean.parseBoolean(s);
                    s=Parameters.poll();
                    scn.DeviceName=s;
                    s=Parameters.poll();
                    scn.DeviceAddress=s;
                    s=Parameters.poll();
                    scn.ServiceUuid=s;
                    s=Parameters.poll();
                    scn.SvcMaskUuid=s;
                    s=Parameters.poll();
                    scn.ManufacturerId=s;
                    s=Parameters.poll();
                    scn.ManufacturerData=s;
                    s=Parameters.poll();
                    scn.ManuMaskData=s;
                    s=Parameters.poll();
                    scn.ServiceDataUuid=s;
                    s=Parameters.poll();
                    scn.ServiceData=s;
                    s=Parameters.poll();
                    scn.SvcDataMask=s;
                    s=Parameters.poll();
                    scn.ScanMode=Integer.parseInt(s);
                    s=Parameters.poll();
                    scn.CallbackType=Integer.parseInt(s);
                    s=Parameters.poll();
                    scn.ResultType=Integer.parseInt(s);
                    s=Parameters.poll();
                    scn.NumOfAdvMatches=Integer.parseInt(s);
                    s=Parameters.poll();
                    scn.MatchMode=Integer.parseInt(s);
                    s=Parameters.poll();
                    scn.ReportDelay=Integer.parseInt(s);
                    s=Parameters.poll();
                    scn.ScanTO=Integer.parseInt(s);
                    objects.add("scanflag");
                    Log.d(TAG, "TextParse:scan flag");
                    objects.add(scn);
                    Log.d(TAG, "TextParse:scan");
                }
                else if(arr[j]==3) {
                    objects.add("Connectflag");
                    Log.d(TAG, "TextParse:connect flag");
                }
                else if(arr[j]==4) {
                    objects.add("TxPowerTestflag");
                    Log.d(TAG, "TextParse:txpwrtest flag");
                }

                else if(arr[j]==5) {
                    objects.add("LatencyTestflag");
                    Log.d(TAG, "TextParse:latencytest flag");
                    LatencyTest mLat=new LatencyTest();
                    s=Parameters.poll();
                    mLat.latService=s;
                    s=Parameters.poll();
                    mLat.latChar=s;
                    objects.add(mLat);
                }
                else if(arr[j]==6) {
                    objects.add("DataTxflag");
                    DataTx mdataTx=new DataTx();
                    s=Parameters.poll();
                    mdataTx.charData=s;
                    s=Parameters.poll();
                    mdataTx.txService=s;
                    s=Parameters.poll();
                    mdataTx.txChar=s;
                    objects.add(mdataTx);
                }
                else if(arr[j]==7) {
                    objects.add("DataRxflag");
                    DataRx mdataRx =new DataRx();
                    s=Parameters.poll();
                    mdataRx.rxService=s;
                    s=Parameters.poll();
                    mdataRx.rxChar=s;
                    s=Parameters.poll();
                    mdataRx.numNotifications=Integer.parseInt(s);
                    objects.add(mdataRx);
                }
                else if(arr[j]==8) {
                    objects.add("ConnUpdateflag");
                    ConnUpdate conn=new ConnUpdate();
                    s=Parameters.poll();
                    conn.ConnIntervalMin=Integer.parseInt(s);
                    s=Parameters.poll();
                    conn.ConnIntervalMax=Integer.parseInt(s);
                    s=Parameters.poll();
                    conn.ConnSlaveLatency=Integer.parseInt(s);
                    s=Parameters.poll();
                    conn.ConnSupTO=Integer.parseInt(s);
                    objects.add(conn);
                }
                else if(arr[j]==9) {
                    objects.add("Disconnectflag");
                }

                else if(arr[j]==10) {
                    objects.add("PhyUpdateflag");
                    PhyUpdate phy=new PhyUpdate();
                    s=Parameters.poll();
                    phy.txPhy=Integer.parseInt(s);
                    s=Parameters.poll();
                    phy.rxPhy=Integer.parseInt(s);
                    s=Parameters.poll();
                    phy.phyOpt=Integer.parseInt(s);
                    objects.add(phy);
                }
                else if(arr[j]==11) {
                    objects.add("ReadPhyflag");
                }
                else if(arr[j]==12) {
                    objects.add("Pairflag");
                }

                j++;
            }
        }
}

