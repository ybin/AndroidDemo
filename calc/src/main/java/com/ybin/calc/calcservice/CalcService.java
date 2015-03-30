package com.ybin.calc.calcservice;

import com.ybin.calc.calculator.CalcNative;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class CalcService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return new CalcNative();
    }

}
