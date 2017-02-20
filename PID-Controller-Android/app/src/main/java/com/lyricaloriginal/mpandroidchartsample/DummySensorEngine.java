package com.lyricaloriginal.mpandroidchartsample;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.Date;

/**
 * ダミーのセンサーデータを取得し通知するクラスです。
 * <p/>
 * Created by LyricalOriginal on 2015/12/29.
 */
class DummySensorEngine {

    private final int mInterval;
    private final HandlerThread mThread;
    private final Handler mHandler;
    private final Handler mUiHandler;
    private boolean mRunning = false;
    private Listener mListener = null;
    private int mIndex = 0;

    /**
     * コンストラクタ
     *
     * @param interval センサー値取得感覚。単位はms。
     */
    DummySensorEngine(int interval) {
        if (interval < 10) {
            throw new IllegalArgumentException("intervalは10ms以上の整数を指定してください");
        }
        mInterval = interval;
        mUiHandler = new Handler();
        mThread = new HandlerThread("DummySensor");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
    }

    /**
     * ダミーデータ取得中かどうかを取得します。
     *
     * @return ダミーデータ取得中か
     */
    boolean isRunning() {
        return mRunning;
    }

    /**
     * ダミーデータ取得リスナーを設定します。
     *
     * @param l ダミーデータ取得リスナー
     */
    void setListener(Listener l) {
        if (!mRunning) {
            mListener = l;
        }
    }

    /**
     * ダミーデータ取得処理を行います。<BR>
     * このメソッドはUIスレッドから実行してください。
     */
    void start() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new RuntimeException("このメソッドはUIスレッドから呼び出してください。");
        } else if (mRunning) {
            return;
        }
        mRunning = true;
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (!mRunning) {
                    return;
                }
                final Date currentDate = new Date();
                final double value = getValue();
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mListener != null) {
                            mListener.onValueMonitored(currentDate, value);
                        }
                    }
                });
                mIndex++;
                mHandler.postDelayed(this, mInterval);
            }
        });
    }

    /**
     * ダミーデータ取得処理を停止します。<BR>
     * このメソッドはUIスレッドから実行してください。
     */
    void stop() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new RuntimeException("このメソッドはUIスレッドから呼び出してください。");
        }
        if (mRunning) {
            mRunning = false;
        }
    }

    /**
     * オブジェクトの破棄処理を行います。<BR>
     * オブジェクトないでワーカースレッドを使っているためその処理をするために使う。
     */
    void destroy() {
        if (mRunning) {
            return;
        }
        mThread.quit();
    }

    private double getValue() {
        return Math.cos(Math.PI * mIndex / 31) + Math.sin(Math.PI * (mIndex + 1) / 15);
    }

    public interface Listener {
        void onValueMonitored(Date date, double value);
    }
}
