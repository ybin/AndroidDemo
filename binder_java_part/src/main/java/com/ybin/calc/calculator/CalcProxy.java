package com.ybin.calc.calculator;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

public class CalcProxy implements ICalc {
    
    private IBinder mRemote;
    
    public CalcProxy(IBinder remote) {
        mRemote = remote;
    }

    @Override
    public IBinder asBinder() {
        return mRemote;
    }

    @Override
    public int add(int a, int b) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        int result = 0;
        
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(a);
            data.writeInt(b);
            mRemote.transact(TRANSACTION_add, data, reply, 0);
            reply.readException();
            result = reply.readInt();
        } finally {
            data.recycle();
            reply.recycle();
        }
        
        return result;
    }
}
