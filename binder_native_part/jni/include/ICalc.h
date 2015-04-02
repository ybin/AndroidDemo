#ifndef _CALC_ICALC_H_
#define _CALC_ICALC_H_

#include <utils/RefBase.h>
#include <binder/IInterface.h>
#include <binder/IBinder.h>

using namespace android;

/*
 * 业务逻辑接口
 */

class ICalc : public IInterface {
public:
    // transaction code
    enum {
        CALC_ADD = IBinder::FIRST_CALL_TRANSACTION + 0,
        CALC_SUB = IBinder::FIRST_CALL_TRANSACTION + 1,
    };
    
    // 业务接口
    virtual int32_t add(int32_t a, int32_t b) = 0;
    virtual int32_t sub(int32_t a, int32_t b) = 0;
    
    // meta interface info
    DECLARE_META_INTERFACE(Calc);
};

#endif