#ifndef _CALC_SERVICE_H_
#define _CALC_SERVICE_H_

#include <binder/IInterface.h>
#include <binder/IBinder.h>
#include <binder/Parcel.h>
#include "ICalc.h"

using namespace android;

class BnCalc : public BnInterface<ICalc> {
    virtual status_t onTransact(uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags);
};

#endif