#include <jni.h>
#include <string>
#include <iostream>
#include <assert.h>
#include "DtmfDetector.hpp"
#include "DtmfGenerator.hpp"

const INT32 FRAME_SIZE = 25000;
DtmfDetector* detector = nullptr;

void detectorInit() {
    if (!detector) detector = new DtmfDetector(FRAME_SIZE);
}

std::string decode(short* data, int size) {
    detectorInit();
    //detector->zerosIndexDialButton();
    detector->dtmfDetecting(data);
    return std::string(detector->getDialButtonsArray(), detector->getIndexDialButtons());
}


DtmfGenerator* generator = nullptr;

void generatorInit() {
    if (!generator) {
        generator = new DtmfGenerator(FRAME_SIZE, 1000, 200);

        char buttons[] = {'1','2','3','4','5','6','7','8','9','0'};
        generator->dtmfGeneratorReset();
        generator->transmitNewDialButtonsArray(buttons, 10);
    }
}

void encode(short* data) {
    generatorInit();
    if(!generator->getReadyFlag()) {
        generator->dtmfGenerating(data);
    }

    //detector->zerosIndexDialButton();
    //detector->dtmfDetecting(data);
    //return std::string(detector->getDialButtonsArray(), detector->getIndexDialButtons());
}


extern "C" {
    JNIEXPORT jstring JNICALL Java_rjdgtn_csms_MainActivity_stringFromJNI(
            JNIEnv *env,
            jobject /* this */) {
        std::string hello = "Hello from C++";
        return env->NewStringUTF(hello.c_str());
    }

    JNIEXPORT void JNICALL Java_rjdgtn_csms_TransportTask_destroy(JNIEnv *env, jobject) {
        delete detector;
        detector = nullptr;
        delete generator;
        generator = nullptr;
    }

    JNIEXPORT jstring JNICALL Java_rjdgtn_csms_TransportTask_decode(JNIEnv *env, jobject, jshortArray jdata) {
       // std::cerr << "Java_rjdgtn_csms_TransportTask_decode\n";
        //
        jsize len = env->GetArrayLength(jdata);
        jshort* data = env->GetShortArrayElements(jdata, 0);

        assert(len == FRAME_SIZE);
        std::string res = decode(data, len);

        env->ReleaseShortArrayElements(jdata, data, 0);

        return env->NewStringUTF(res.c_str());
    }

    JNIEXPORT void JNICALL Java_rjdgtn_csms_TransportTask_encode(JNIEnv *env, jobject, jshortArray jdata) {
        // std::cerr << "Java_rjdgtn_csms_TransportTask_decode\n";
        //
        jsize len = env->GetArrayLength(jdata);
        jshort* data = env->GetShortArrayElements(jdata, 0);

        assert(len == FRAME_SIZE);
        encode(data);

        env->ReleaseShortArrayElements(jdata, data, 0);

    }

}