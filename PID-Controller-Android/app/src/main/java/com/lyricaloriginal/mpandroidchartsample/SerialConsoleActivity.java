/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.lyricaloriginal.mpandroidchartsample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;


import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.RadioButton;
import android.widget.RadioGroup;

/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialConsoleActivity extends Activity {

    private final String TAG = SerialConsoleActivity.class.getSimpleName();

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialPort)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialPort sPort = null;

    private TextView mTitleTextView;
    private TextView mDumpTextView;
    private ScrollView mScrollView;
    private CheckBox chkDTR;
    private CheckBox chkRTS;
    private LineChart mChart;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private SerialInputOutputManager mSerialIoManager;

    private Pid mPid;


    private boolean flgUpdateGraph = true;
    private boolean flgPidCont = false;
    private boolean flgStep = false;
    private byte[] dataSend = {0x00};
    private char dataPID = 0x00;

    private int valTarget = 0;
    private double kp = 0;
    private double ki = 0;
    private double kd = 0;
    private char valCnt = 0;

    private int numGraph = 0;

    final double rateParam = 1000;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SerialConsoleActivity.this.updateReceivedData(data);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_console);
        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        chkDTR = (CheckBox) findViewById(R.id.checkBoxDTR);
        chkRTS = (CheckBox) findViewById(R.id.checkBoxRTS);

        final Button btnGraph = (Button)findViewById(R.id.btnDisp);
        final Button btnBack = (Button)findViewById(R.id.btnBack);
        final Button btnPid  = (Button)findViewById(R.id.btnPid);
        final EditText pGainText = (EditText)findViewById(R.id.pGain);
        final EditText iGainText = (EditText)findViewById(R.id.iGain);
        final EditText dGainText = (EditText)findViewById(R.id.dGain);
        final EditText refValText = (EditText)findViewById(R.id.targetT);
        final Button btnStep = (Button)findViewById(R.id.btnStep);
        final Button btnSave = (Button)findViewById(R.id.btnSave);

        final Button btnMonit= (Button)findViewById(R.id.moni_btn);
        btnMonit.setVisibility(View.GONE);
        mChart = (LineChart) findViewById(R.id.chart);
        mChart.setVisibility(View.GONE);
        initChart();

        TextView textViewStart = (TextView) findViewById(R.id.btnPid);
        textViewStart.setText("Start Pid");
        TextView textViewStep = (TextView) findViewById(R.id.btnStep);
        textViewStep.setText("STEP");

        mPid = new Pid(kp, ki, kd, valTarget);


        chkDTR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    sPort.setDTR(isChecked);
                }catch (IOException x){}
            }
        });

        chkRTS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    sPort.setRTS(isChecked);
                } catch (IOException x) {
                }
            }
        });


        //グラフを表示、シリアルコンソールを非表示
        btnGraph.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // Sub 画面を起動
                //Intent intent = new Intent(getApplication(), Activity.class);
                //startActivity(intent);
                chkDTR.setVisibility(View.GONE);
                chkRTS.setVisibility(View.GONE);
                btnGraph.setVisibility(View.GONE);
                mScrollView.setVisibility(View.GONE);


                btnBack.setVisibility(View.VISIBLE);
                btnMonit.setVisibility(View.VISIBLE);
                mChart.setVisibility(View.VISIBLE);
                btnStep.setVisibility(View.VISIBLE);
                btnSave.setVisibility(View.VISIBLE);
            }
        });

        btnMonit.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                //グラフ描画の停止・再開
                if(flgUpdateGraph == true){
                    flgUpdateGraph = false;
                    TextView textView = (TextView) findViewById(R.id.moni_btn);
                    textView.setText("モニター開始");
                }
                else if(flgUpdateGraph==false){
                    flgUpdateGraph = true;
                    TextView textView = (TextView) findViewById(R.id.moni_btn);
                    textView.setText("モニター停止");
                }
            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // Sub 画面を起動
                //Intent intent = new Intent(getApplication(), Activity.class);
                //startActivity(intent);
                chkDTR.setVisibility(View.VISIBLE);
                chkRTS.setVisibility(View.VISIBLE);
                btnGraph.setVisibility(View.VISIBLE);
                mScrollView.setVisibility(View.VISIBLE);
                btnStep.setVisibility(View.VISIBLE);

                btnMonit.setVisibility(View.GONE);
                mChart.setVisibility(View.GONE);
                btnBack.setVisibility(View.GONE);
                btnSave.setVisibility(View.GONE);
            }
        });

        btnPid.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                YAxis leftAxis = mChart.getAxisLeft();
                LimitLine ll = new LimitLine(0);

                //pid制御の開始・パラメータ更新
                if(flgPidCont == true){
                    flgPidCont = false;
                    TextView textView = (TextView) findViewById(R.id.btnPid);
                    textView.setText("PID Standby");
                    leftAxis.removeAllLimitLines();
                    mPid.setPidParam(0, 0, 0, 0);
                }
                else if(flgPidCont==false){
                    flgPidCont = true;

                    Editable p = pGainText.getText();
                    Editable i = iGainText.getText();
                    Editable d = dGainText.getText();
                    Editable r = refValText.getText();

                    double valP, valI, valD;
                    int valR;

                    try{
                        valP = Double.parseDouble(p.toString()) /rateParam;
                    }
                    catch (NumberFormatException e){
                        valP = 0;
                        pGainText.setText("0");
                    }

                    try{
                        valI = Double.parseDouble(i.toString()) /rateParam;
                    }
                    catch (NumberFormatException e){
                        valI = 0;
                        iGainText.setText("0");
                    }

                    try{
                        valD = Double.parseDouble(d.toString()) /rateParam;
                    }
                    catch (NumberFormatException e){
                        valD = 0;
                        dGainText.setText("0");
                    }

                    try{
                        valR = Integer.parseInt(r.toString());
                    }
                    catch (NumberFormatException e){
                        valR = 0;
                        refValText.setText("0");
                    }

                    ll = new LimitLine(valR);
                    ll.setLineColor(Color.RED);
                    leftAxis.addLimitLine(ll);

                    //mPid = new Pid(valP, valI, valD, valR);
                    mPid.setPidParam(valP, valI, valD, valR);

                    TextView textSend = (TextView) findViewById(R.id.sendText);
                    String sendDataText = String.valueOf(valP*rateParam) + " : " + String.valueOf(valI*rateParam) + " : " + String.valueOf(valD*rateParam);
                    textSend.setText(sendDataText);

                    TextView textView = (TextView) findViewById(R.id.btnPid);
                    textView.setText("PID Running...");
                }
            }
        });
        btnStep.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                if(flgStep==false){
                    flgStep = true;
                }
                else{
                    flgStep = false;
                }
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                mChart.saveToGallery("Chart" + String.valueOf(numGraph), 100);
                //mChart.saveToPath("Chart" + String.valueOf(numGraph), "/storage/emulated/0/Download/" + String.valueOf(numGraph)+".png");
                numGraph++;
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        finish();
    }

    void showStatus(TextView theTextView, String theLabel, boolean theValue){
        String msg = theLabel + ": " + (theValue ? "enabled" : "disabled") + "\n";
        theTextView.append(msg);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
            mTitleTextView.setText("No serial device.");
        } else {
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                mTitleTextView.setText("Opening device failed");
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                showStatus(mDumpTextView, "CD  - Carrier Detect", sPort.getCD());
                showStatus(mDumpTextView, "CTS - Clear To Send", sPort.getCTS());
                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(mDumpTextView, "DTR - Data Terminal Ready", sPort.getDTR());
                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(mDumpTextView, "RI  - Ring Indicator", sPort.getRI());
                showStatus(mDumpTextView, "RTS - Request To Send", sPort.getRTS());

            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                mTitleTextView.setText("Error opening device: " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            mTitleTextView.setText("Serial device: " + sPort.getClass().getSimpleName());
        }
        onDeviceStateChange();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data) {

        //シリアルコンソール表示用文字列
        final String message = "Read " + data.length + " bytes: \n"
                + HexDump.dumpHexString(data) + "\n\n";

        //受信データを16進数の文字列として取得
        String tmp = HexDump.toHexString(data);

        //受信データを10進数の数値として取得
        int val = Integer.parseInt(tmp.substring(0,2), 16);
        String logRcv = String.valueOf(val);
        //Log.i("pid", logRcv);

        //上の方に取得データ表示
        mTitleTextView.setText(String.valueOf(val));

        //フラグがonならグラフ描画
        if(flgUpdateGraph==true){
            final Date currentDate = new Date();
            onValueMonitored(currentDate, val);
        }

        //PIDの計算
        if(flgPidCont==true){
            final EditText refValText = (EditText)findViewById(R.id.targetT);
            final Button btnStep = (Button)findViewById(R.id.btnStep);

            if(flgStep== true) {
                Editable r = refValText.getText();
                valCnt =(char)(Integer.parseInt(r.toString())/4);
            }
            else {
                valCnt = (char)(mPid.pid(val));
            }

        }
        else if(flgPidCont==false){
            valCnt = 0;
        }


        //dataPID = (char)(dataPID + 0x01);
        dataSend[0] = (byte)valCnt; //(byte)dataPID;
        //Log.i("pid", String.valueOf(valCnt));
        int send = (int)mPid.pid(val);
        mTitleTextView.setText("Rcv: "+String.valueOf(logRcv) + "  Send: "+ String.valueOf((send)) );
        mSerialIoManager.writeAsync(dataSend);


        //シリアルコンソールを更新
        mDumpTextView.append(message);
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
    }

    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialPort port) {
        sPort = port;
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    public void onValueMonitored(Date date, double value) {
        LineData data = mChart.getData();
        if (data == null) {
            return;
        }

        LineDataSet set = data.getDataSetByIndex(0);
        if (set == null) {
            set = new LineDataSet(null,"水位[mm]");
            set.setColor(Color.BLUE);
            set.setDrawValues(false);
            data.addDataSet(set);
        }

        //  追加描画するデータを追加
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        data.addXValue(format.format(date));
        data.addEntry(new Entry((float) value, set.getEntryCount()), 0);

        //  データを追加したら必ずよばないといけない
        mChart.notifyDataSetChanged();

        mChart.setVisibleXRangeMaximum(60);

        mChart.moveViewToX(data.getXValCount() - 61);   //  移動する

        YAxis leftAxis = mChart.getAxisLeft();
        if(leftAxis.getAxisMaxValue() < value){
            leftAxis.setAxisMaxValue((float)value +20);
        }

    }

    private void initChart() {
        // no description text
        mChart.setDescription("");
        mChart.setNoDataTextDescription("You need to provide data for the chart.");

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);

        // add empty data
        mChart.setData(data);

        //  ラインの凡例の設定
        Legend l = mChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setLabelsToSkip(9);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setAxisMaxValue(10.0f);
        leftAxis.setAxisMinValue(0.0f);
        leftAxis.setStartAtZero(false);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
    }
}
