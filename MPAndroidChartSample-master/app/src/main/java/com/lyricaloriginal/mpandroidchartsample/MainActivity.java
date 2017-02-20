package com.lyricaloriginal.mpandroidchartsample;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * メインアクティビティ
 */
public class MainActivity extends AppCompatActivity implements DummySensorEngine.Listener {

    private LineChart mChart;
    private DummySensorEngine mSensorEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  0.5秒おきにデータを追加していくため
        mSensorEngine = new DummySensorEngine(500);
        mSensorEngine.setListener(this);
        mChart = (LineChart) findViewById(R.id.chart);
        initChart();

        findViewById(R.id.monitor_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSensorEngine.isRunning()) {
                    stopMonitoring();
                } else {
                    startMonitoring();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mSensorEngine.isRunning()) {
            stopMonitoring();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorEngine.destroy();
    }

    @Override
    public void onValueMonitored(Date date, double value) {
        LineData data = mChart.getData();
        if (data == null) {
            return;

        }

        LineDataSet set = data.getDataSetByIndex(0);
        if (set == null) {
            set = new LineDataSet(null, "サンプルデータ");
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
        leftAxis.setAxisMaxValue(3.0f);
        leftAxis.setAxisMinValue(-3.0f);
        leftAxis.setStartAtZero(false);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void startMonitoring() {
        mSensorEngine.start();
        TextView textView = (TextView) findViewById(R.id.monitor_btn);
        textView.setText("モニター停止");
    }

    private void stopMonitoring() {
        mSensorEngine.stop();
        TextView textView = (TextView) findViewById(R.id.monitor_btn);
        textView.setText("モニター開始");
    }
}
