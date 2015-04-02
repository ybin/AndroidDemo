package com.ybin.calc.calcservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.ybin.calc.calculator.Calc;

public class CalcService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return new Calc();
    }
}
