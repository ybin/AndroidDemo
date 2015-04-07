#include <stdio.h>
#include "RefBase.h"

using namespace android;

class A : public RefBase {
public: 
    A(){
        extendObjectLifetime(OBJECT_LIFETIME_STRONG);
    }
    ~A(){
        printf("delete strong A\n");
    } 
};

static void print(char* name, RefBase* ref) {
    printf("%s: strong ref count: %d\n", name, ref->getStrongCount()); 
    printf("%s: weak ref count: %d\n", name, ref->getWeakRefs()->getWeakCount());
}

int main()
{ 
    sp<A> spa = new A();
    print("A", spa.get());
    /* Êä³ö£º
        A: strong ref count: 1
        A: weak ref count: 1
        delete strong A
     */
    
    return 0;
}