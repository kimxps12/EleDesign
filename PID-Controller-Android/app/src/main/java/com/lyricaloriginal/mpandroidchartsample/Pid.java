package com.lyricaloriginal.mpandroidchartsample;

import android.util.Log;
/**
 * Created by ユウタロウ on 2017/01/18.
 */
public class Pid {

    protected int     mDiff1;         //入力値と目標値の差
    protected int     mDiff2;         //入力値と目標値の差
    protected double  mIntegral;      //積分値
    protected double  mKp;            //比例パラメータ
    protected double  mKi;            //積分パラメータ
    protected double  mKd;            //微分パラメータ
    protected int     mTargetVal;    //目標値

    private final int DELTA_T = 50;         //微積分に用いる微小時間の設定
    private final int PID_MAX_OUT = 255;    //PID出力の最大値
    private final int PID_MIN_OUT = 0;      //PID出力の最小値


    //コンストラクタ
    Pid(double p_kp, double p_ki ,double p_kd ,int p_target){

        mDiff1 = 0;
        mDiff2 = 0;
        mKp = p_kp;
        mKi = p_ki;
        mKd = p_kd;
        mIntegral = 0;
        mTargetVal = p_target;

    }

    //PIDの計算結果が、最小値・最大値に引っかからないか確認する
    //引数：確認したい値, 既定の最小値, 既定の最大値
    //返り値：確認したい値が最小・最大値の範囲内ならそのまま、範囲外ならそれぞれ最大・最小値を返す
    protected double math_limit(double rtn_val  ,double min_val ,double max_val){
        if(rtn_val > max_val){
            return max_val;
        }
        else if(rtn_val < min_val){
            return min_val;
        }
        else{
            return rtn_val;
        }
    }

    //目標値を変更する
    //引数：変更後の目標値
    //返り値：なし
    void setTargetVal(int val){
        if (mTargetVal != val){
            mIntegral = 0;
        }
        mTargetVal = val;
    }

    //p, i, d の各ゲインを設定
    void setPidParam(double p_kp, double p_ki ,double p_kd ,int p_target){

        mKp = p_kp;
        mKi = p_ki;
        mKd = p_kd;
        //mTargetVal = p_target;
        //mDiff1 = mDiff2;
        //mDiff2 = 0;
        //mIntegral = 0;
        if (mTargetVal != p_target){
            mIntegral = 0;
        }
        mTargetVal = p_target;
    }

    protected double pid(int valSens){

        double p,i,d;

        //diff1が古い値
        //diff2が新しい値

        mDiff1   = mDiff2;
        mDiff2   = mTargetVal - valSens;
        mIntegral +=((mDiff2 + mDiff1) / 2.0 )* DELTA_T;
        //String logI = "I= ("  +String.valueOf(mDiff1) +" + "+ String.valueOf(mDiff2)+ ") /2 ";
        //String logSens = String.valueOf(valSens);
        //Log.i("pid", logSens);

        p = mKp * mDiff2;
        i = mKi * mIntegral;
        d = mKd * (mDiff2 - mDiff1) / DELTA_T;

        String logPid = "P:"+String.valueOf(p) + " I:"+String.valueOf(i)+ " D:"+String.valueOf(d);
        Log.i("pid", logPid);

        return math_limit(p + i + d, PID_MIN_OUT, PID_MAX_OUT);
    }

}
