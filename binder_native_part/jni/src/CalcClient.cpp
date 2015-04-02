#include "Util.h"
#include "CalcClient.h"

using namespace android;

int32_t BpCalc::add(int32_t a, int32_t b) {
    int ret;
    Parcel data, reply;
    data.writeInterfaceToken(ICalc::getInterfaceDescriptor());
    data.writeInt32(a);
    data.writeInt32(b);
    INFO("BpCalc::add(%i, %i), before transact", a, b);
    remote()->transact(CALC_ADD, data, &reply);
    status_t status = reply.readInt32(&ret);
    INFO("BpCalc::add(), after transact, status = %i, reply = %i", status, ret);
    return ret;
}

int32_t BpCalc::sub(int32_t a, int32_t b) {
    int ret;
    Parcel data, reply;
    data.writeInterfaceToken(ICalc::getInterfaceDescriptor());
    data.writeInt32(a);
    data.writeInt32(b);
    INFO("BpCalc::sub(%i, %i), before transact", a, b);
    remote()->transact(CALC_SUB, data, &reply);
    status_t status = reply.readInt32(&ret);
    INFO("BpCalc::sub(), after transact, status = %i, reply = %i", status, ret);
    return ret;
}

// IMPLEMENT_META_INTERFACE(Calc, "Calc");
// Macro above expands to code below. Doing it by hand so we can log ctor and destructor calls.

const android::String16 ICalc::descriptor("Calc");
const android::String16& ICalc::getInterfaceDescriptor() const {
    return ICalc::descriptor;
}
android::sp<ICalc> ICalc::asInterface(const android::sp<android::IBinder>& obj) {
    android::sp<ICalc> intr;
    if (obj != NULL) {
        intr = static_cast<ICalc*>(obj->queryLocalInterface(ICalc::descriptor).get());
        if (intr == NULL) {
            intr = new BpCalc(obj);
        }
    }
    return intr;
}
ICalc::ICalc() { INFO("ICalc::ICalc()"); }
ICalc::~ICalc() { INFO("ICalc::~ICalc()"); }
// End of macro expansion