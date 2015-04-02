package com.ybin.calc.calcclient;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

import com.ybin.calc.R;
import com.ybin.calc.calculator.CalcNative;
import com.ybin.calc.calculator.ICalc;

public class CalcClient extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.calcclient_main);


        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ICalc calc = CalcNative.asInterface(service);
                int r = 0;
                try {
                    r = calc.add(1, 2);
                } catch (RemoteException e) {
                    Log.e("CalcClient", "call service interface error");
                    e.printStackTrace();
                }
                TextView tv = (TextView) findViewById(R.id.text);
                tv.setText("1 + 2 = " + r);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent intent = new Intent("com.ybin.calc.service");

        bindService(intent, conn, BIND_AUTO_CREATE);
    }
}
