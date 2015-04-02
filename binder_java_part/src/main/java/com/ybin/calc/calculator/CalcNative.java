package com.ybin.calc.calculator;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public abstract class CalcNative extends Binder implements ICalc {
    // 工具方法，应该放到单独的工具类里面，
    // 该方法只是做一个转换，与binder没有实质性的关联
    public static ICalc asInterface(IBinder obj) {
        IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
        if(iin != null) {
            return (ICalc)iin;
        }
        return new CalcProxy(obj);
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        switch (code) {
        case INTERFACE_TRANSACTION: {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        case TRANSACTION_add: {
            data.enforceInterface(DESCRIPTOR);
            int a = data.readInt();
            int b = data.readInt();
            int result = this.add(a, b);
            reply.writeNoException();
            reply.writeInt(result);
            return true;
        }
        }
        return super.onTransact(code, data, reply, flags);
    }
}
