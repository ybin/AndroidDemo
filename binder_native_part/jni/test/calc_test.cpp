#include <binder/IBinder.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <binder/IPCThreadState.h>
#include "Util.h"
#include "ICalc.h"
#include "Calc.h"

using namespace android;

sp<ICalc> getCalcService() {
    sp<IServiceManager> sm = defaultServiceManager();
    ASSERT(sm != 0);
    sp<IBinder> binder = sm->getService(String16("Calc"));
    ASSERT(binder != 0);
    sp<ICalc> calc = interface_cast<ICalc>(binder);
    ASSERT(calc != 0);
    return calc;
}

int main(int argc, char** argv) {
    if (argc == 1) {
        // start calc service
        defaultServiceManager()->addService(String16("Calc"), new Calc());
        ProcessState::self()->startThreadPool();
        INFO("Calc service now ready.");
        IPCThreadState::self()->joinThreadPool();
    } else {
        // start calc client
        INFO("start a calc client. please input math express.");
        
        sp<ICalc> calc = getCalcService();
        
        INFO("1 + 1 = %d", calc->add(1, 1));
        INFO("1 - 1 = %d", calc->sub(1, 1));
    }

    return 0;
}