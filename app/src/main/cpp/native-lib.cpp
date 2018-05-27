#include <jni.h>
#include <string>
#include <iostream>
#include <assert.h>
#include "DtmfDetector.hpp"
#include "DtmfGenerator.hpp"

extern "C" {
#include "dtmf.h"
#include "CRCX.h"
}

int curFrameSize = 0;
DtmfGenerator* generator = nullptr;

//DtmfDetector* detector = nullptr;
//
//static std::string str;
//void detectorInit() {
//    if (!detector) {
//        detector = new DtmfDetector(FRAME_SIZE);
//
//        DTMFSetup(SAMPLING_RATE, BUFFER_SIZE);
//        str.clear();
//    }
//}


//

//void generatorInit() {
//    if (!generator) {
//        generator = new DtmfGenerator(FRAME_SIZE, 1000, 200);
//
//        char buttons[] = {'1','2','3','4','5','6','7','8','9','0'};
//        generator->dtmfGeneratorReset();
//        generator->transmitNewDialButtonsArray(buttons, 10);
//    }
//}

//void encode(short* data) {
//    generatorInit();
//    if(!generator->getReadyFlag()) {
//        generator->dtmfGenerating(data);
//    }
//
//    //detector->zerosIndexDialButton();
//    //detector->dtmfDetecting(data);
//    //return std::string(detector->getDialButtonsArray(), detector->getIndexDialButtons());
//}


extern "C" {
    JNIEXPORT jstring JNICALL Java_rjdgtn_csms_MainActivity_stringFromJNI(
            JNIEnv *env,
            jobject /* this */) {
        std::string hello = "Hello from C++";
        return env->NewStringUTF(hello.c_str());
    }

    JNIEXPORT void JNICALL Java_rjdgtn_csms_TransportTask_destroy(JNIEnv *env, jobject) {
//        delete detector;
//        detector = nullptr;
//        delete generator;
//        generator = nullptr;
    }

    JNIEXPORT jchar JNICALL Java_rjdgtn_csms_ReadTask_decode(JNIEnv *env, jobject, jshortArray jdata) {
       // std::cerr << "Java_rjdgtn_csms_TransportTask_decode\n";

        jsize len = env->GetArrayLength(jdata);
        jshort* data = env->GetShortArrayElements(jdata, 0);

        char code = '\0';
        DTMFDecode(data, len, &code);

        env->ReleaseShortArrayElements(jdata, data, 0);

        return code;
    }

    JNIEXPORT void JNICALL Java_rjdgtn_csms_SendTask_encodeInit(JNIEnv *env, jobject, jint frameSize, jint callDur, jint spaceDur) {
        static int curCallDur = 0;
        static int curSpaceDur = 0;

        if (generator && (curFrameSize != frameSize || curCallDur != callDur || curSpaceDur != spaceDur)) {
            delete generator;
            generator = nullptr;
        }

        if (!generator) {
            generator = new DtmfGenerator(frameSize, callDur, spaceDur);
            curFrameSize = frameSize;
            curCallDur = callDur;
            curSpaceDur = spaceDur;
        }
    }

    JNIEXPORT void JNICALL Java_rjdgtn_csms_SendTask_encode(JNIEnv *env, jobject, jstring jstr) {

        assert(generator);
        jsize len = env->GetStringLength(jstr);
        const jchar* str = env->GetStringChars(jstr, nullptr);

        char* buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = str[i];
        }

        generator->dtmfGeneratorReset();
        generator->transmitNewDialButtonsArray(buf, len);

        env->ReleaseStringChars(jstr, str);
    }

    JNIEXPORT jboolean JNICALL Java_rjdgtn_csms_SendTask_encodeStep(JNIEnv *env, jobject, jshortArray jdata) {
//        // std::cerr << "Java_rjdgtn_csms_TransportTask_decode\n";
//        //
        jsize len = env->GetArrayLength(jdata);
        jshort* data = env->GetShortArrayElements(jdata, 0);

        assert(generator);
        assert(curFrameSize == len);
        generator->dtmfGenerating(data);

        env->ReleaseShortArrayElements(jdata, data, 0);

        return generator->getReadyFlag();
    }

    JNIEXPORT jbyte JNICALL Java_rjdgtn_csms_DtmfPacking_crc80x31(JNIEnv *env, jobject, jbyteArray jdata, jint len) {
        // std::cerr << "Java_rjdgtn_csms_TransportTask_decode\n";

        jbyte* data = env->GetByteArrayElements(jdata, 0);

        jbyte checksum = Crc8((unsigned char*)data, len, Crc8Table_0x31);

        env->ReleaseByteArrayElements(jdata, data, 0);

        return checksum;
    }

    JNIEXPORT jbyte JNICALL Java_rjdgtn_csms_DtmfPacking_crc80x9B(JNIEnv *env, jobject, jbyteArray jdata, jint len) {
        // std::cerr << "Java_rjdgtn_csms_TransportTask_decode\n";

        jbyte* data = env->GetByteArrayElements(jdata, 0);

        jbyte checksum = Crc8((unsigned char*)data, len, Crc8Table_0x9B);

        env->ReleaseByteArrayElements(jdata, data, 0);

        return checksum;
    }

}