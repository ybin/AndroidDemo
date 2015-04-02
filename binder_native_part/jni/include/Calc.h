#ifndef _CALC_CALC_H_
#define _CALC_CALC_H_

#include "CalcService.h"

class Calc : public BnCalc {
    virtual int32_t add(int32_t a, int32_t b);
    virtual int32_t sub(int32_t a, int32_t b);
};

#endif