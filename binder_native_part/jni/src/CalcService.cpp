#include "Util.h"
#include "CalcService.h"

using namespace android;

status_t BnCalc::onTransact(uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags) {
    INFO("BnCalc::onTransact, code=%i, flags=%i", code, flags);
    data.checkInterface(this);
    INFO("calc service strong refs: %d", getStrongCount());
    INFO("calc service weak refs: %d", getWeakRefs()->getWeakCount());
    
    switch(code) {
        case CALC_ADD: {
            int32_t a = data.readInt32();
            int32_t b = data.readInt32();
            int32_t result = add(a, b);
            INFO("BnCalc::onTransact add(%i, %i) = %i", a, b, result);
            ASSERT(reply != 0);
            reply->writeInt32(result);
            return NO_ERROR;
        }
        
        case CALC_SUB: {
            int32_t a = data.readInt32();
            int32_t b = data.readInt32();
            int32_t result = sub(a, b);
            INFO("BnCalc::onTransact sub(%i, %i) = %i", a, b, result);
            ASSERT(reply != 0);
            reply->writeInt32(result);
            return NO_ERROR;
        }
        
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}