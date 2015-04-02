#ifndef _CALC_CLIENT_H_
#define _CALC_CLIENT_H_

#include <binder/IInterface.h>
#include <binder/IBinder.h>
#include <binder/Parcel.h>
#include "ICalc.h"
#include "Util.h"

using namespace android;

class BpCalc : public BpInterface<ICalc> {
public:
    BpCalc(const sp<IBinder>& impl)
        : BpInterface<ICalc>(impl)
    {
        INFO("BpCalc::BpCalc(impl)");
    }
    
    virtual int32_t add(int32_t a, int32_t b);
    virtual int32_t sub(int32_t a, int32_t b);
};

#endif