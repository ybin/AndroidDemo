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
        // server端用于提供服务，需要一个thread pool来接受请求，
        // 相比之下，客户端只是请求服务，就用不到什么thread pool了。
        Calc* c = new Calc();
        status_t err = defaultServiceManager()->addService(String16("Calc"), c);
        INFO(">>>>>>>>>> strong ref: %d <<<<<<<<<<<<<<<<", c->getStrongCount());
        INFO(">>>>>>>>>> weak ref: %d <<<<<<<<<<<<<<<<", c->getWeakRefs()->getWeakCount());
        if(err) {
            INFO("add Calc service failed...");
            return -1;
        } else {
            INFO("Calc service now ready.");
            // start thread pool，其实就是创建一个线程，然后让线程一直在那里(无限循环)
            // 尝试从driver读取数据并处理(driver提供的数据里面有具体对象指针，如Calc()对象指针，
            // 实际是这些对象来处理请求。
            ProcessState::self()->startThreadPool();
            // 当前线程(进程其实也是一个线程而已)加入线程池，它把环境启动起来之后就没啥事儿了，
            // 所以，加入线程池来处理客户端的请求吧，^_^!
            IPCThreadState::self()->joinThreadPool();
            return 0;
        }
    } else {
        // start calc client
        INFO("start a calc client. please input math express.");
        
        sp<ICalc> calc = getCalcService();
        
        INFO("1 + 1 = %d", calc->add(1, 1));
        INFO("1 - 1 = %d", calc->sub(1, 1));
        sleep(30);
    }

    return 0;
}