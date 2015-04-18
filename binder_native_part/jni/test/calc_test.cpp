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
        // server�������ṩ������Ҫһ��thread pool����������
        // ���֮�£��ͻ���ֻ��������񣬾��ò���ʲôthread pool�ˡ�
        Calc* c = new Calc();
        status_t err = defaultServiceManager()->addService(String16("Calc"), c);
        INFO(">>>>>>>>>> strong ref: %d <<<<<<<<<<<<<<<<", c->getStrongCount());
        INFO(">>>>>>>>>> weak ref: %d <<<<<<<<<<<<<<<<", c->getWeakRefs()->getWeakCount());
        if(err) {
            INFO("add Calc service failed...");
            return -1;
        } else {
            INFO("Calc service now ready.");
            // start thread pool����ʵ���Ǵ���һ���̣߳�Ȼ�����߳�һֱ������(����ѭ��)
            // ���Դ�driver��ȡ���ݲ�����(driver�ṩ�����������о������ָ�룬��Calc()����ָ�룬
            // ʵ������Щ��������������
            ProcessState::self()->startThreadPool();
            // ��ǰ�߳�(������ʵҲ��һ���̶߳���)�����̳߳أ����ѻ�����������֮���ûɶ�¶��ˣ�
            // ���ԣ������̳߳�������ͻ��˵�����ɣ�^_^!
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