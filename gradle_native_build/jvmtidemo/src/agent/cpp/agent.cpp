#include <iostream>
#include <jvmti.h>
#include <jni.h>

using namespace std;

typedef struct {
    jvmtiEnv *jvmti;
} GlobalAgentData;

static GlobalAgentData *gdata;

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    cout << "A message from my super agent!" << endl;
    cout << "options: " << options << endl;
    
    jvmtiEnv *jvmti = NULL;
    jvmtiCapabilities capabilities;
    jvmtiError error;
    
    jint result = jvm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1);
    if(result != JNI_OK) {
        cout << "Error: Unable to access JVMTI!" << endl;
    }
    
    memset(&capabilities, 0, sizeof(jvmtiCapabilities));
    capabilities.can_tag_objects = 1;
    error = (jvmti)->AddCapabilities(&capabilities);
    
    gdata = (GlobalAgentData*) malloc(sizeof(GlobalAgentData));
    gdata -> jvmti = jvmti;
    
    return JNI_OK;
}

jint JNICALL objectCountingCallback(jlong class_tag, jlong size, jlong* tag_ptr, jint length, void* user_data) {
    int* count = (int*) user_data;
    *count += 1;
    return JVMTI_VISIT_OBJECTS;
}
 
extern "C"
JNIEXPORT jint JNICALL Java_com_example_jvmti_Test_countInstances(JNIEnv *env, jclass thisClass, jclass klass) {
    int count = 0;
    jvmtiHeapCallbacks callbacks;
    (void)memset(&callbacks, 0, sizeof(callbacks));
    callbacks.heap_iteration_callback = (jvmtiHeapIterationCallback)&objectCountingCallback;
    jvmtiError error = gdata->jvmti->IterateThroughHeap(0, klass, &callbacks, &count);
    return count;
}

extern "C"
JNIEXPORT void JNICALL Java_com_example_jvmti_Test_printSysteProperties(JNIEnv *env, jclass thisClass) {
    cout << "print syste properties:" << endl;
    jvmtiError err;
    jint count_ptr;
    char **property_ptr;
    char *value_ptr;
    
    err = gdata->jvmti->GetSystemProperties(&count_ptr, &property_ptr);
    if(err != JVMTI_ERROR_NONE) {
        cout << "error occured!!!" << endl;
    }
    cout << "There are " << count_ptr << " system properties." << endl;
    jvmtiError err2;
    for(int i = 0; i < count_ptr; i++) {
        cout << property_ptr[i] << " = ";
        err2 = gdata->jvmti->GetSystemProperty(property_ptr[i], &value_ptr);
        gdata->jvmti->Deallocate((unsigned char*)property_ptr[i]);
        if(err2 != JVMTI_ERROR_NONE || value_ptr == NULL) continue;
        cout << value_ptr << endl;
    }
}
