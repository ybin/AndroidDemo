package com.ybin.calc.calculator;

import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface ICalc extends IInterface {
    static final String DESCRIPTOR = "com.example.calcservice";
    static final int TRANSACTION_add = (IBinder.FIRST_CALL_TRANSACTION + 0);

    // 业务接口
    int add(int a, int b) throws RemoteException;
}
