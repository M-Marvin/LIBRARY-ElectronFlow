/*
 * jni.cpp
 *
 *  Created on: 10.10.2024
 *      Author: M_Marvin
 */

#include "de_m_marvin_electronflow_Solver.h"
#include <map>
#include <mutex>
#include <sstream>
#include "electronflow.h"

using namespace electronflow;

static std::map<jlong, void*> jclass2cclass;
static std::mutex mapMutex;

JNIEXPORT jint JNICALL Java_de_m_1marvin_electronflow_Solver_initElectronFlow(JNIEnv *env, jobject obj, jlong classid, jobject logout) {

	std::unique_lock<std::mutex> lck (mapMutex);

	if (jclass2cclass[classid] != NULL) {
		solver* slv = (solver*) jclass2cclass[classid];
		return 0;
	}

	setvbuf(stdout, NULL, _IONBF, 0);

	solver* slv;
    if (logout != NULL) {
    	jclass callbackClass = env->FindClass("java/util/function/Consumer");
		if (callbackClass == NULL) {
			return -3;
		}
		JNIEnv* javaEnv = env;
		jobject javaCallback = env->NewGlobalRef(logout);

		jmethodID javaLogFuncID = env->GetMethodID(callbackClass, "accept", "(Ljava/lang/Object;)V");
		if (javaLogFuncID == NULL) {
			return -3;
		}

		slv = new solver([javaEnv, javaCallback, javaLogFuncID](std::string print) {
			javaEnv->CallVoidMethod(javaCallback, javaLogFuncID, javaEnv->NewStringUTF(print.c_str()));
		});
    } else {
    	slv = new solver();
    }
	jclass2cclass[classid] = slv;
	return 0;
}

JNIEXPORT jint JNICALL Java_de_m_1marvin_electronflow_Solver_detachElectronFlow(JNIEnv *env, jobject obj, jlong classid) {

	std::unique_lock<std::mutex> lck (mapMutex);

	solver* slv = (solver*) jclass2cclass[classid];
	if (slv == NULL) return -3;
	delete slv;
	jclass2cclass[classid] = NULL;
	return 1;
}

JNIEXPORT jint JNICALL Java_de_m_1marvin_electronflow_Solver_setLibName(JNIEnv *env, jobject obj, jlong classid, jstring libname) {

	std::unique_lock<std::mutex> lck (mapMutex);

	solver* slv = (solver*) jclass2cclass[classid];
    if (slv == NULL) return -3;
    std::string libnameNative = std::string(env->GetStringUTFChars(libname, 0));
    slv->setlibname(libnameNative);
    return 0;
}

JNIEXPORT jint JNICALL Java_de_m_1marvin_electronflow_Solver_setLogger(JNIEnv *env, jobject obj, jlong classid, jobject logout) {

	std::unique_lock<std::mutex> lck (mapMutex);

	solver* slv = (solver*) jclass2cclass[classid];
    if (slv == NULL) return -3;

    if (logout != NULL) {
    	jclass callbackClass = env->FindClass("java/util/function/Consumer");
		if (callbackClass == NULL) {
			return -3;
		}
		JNIEnv* javaEnv = env;
		jobject javaCallback = env->NewGlobalRef(logout);

		jmethodID javaLogFuncID = env->GetMethodID(callbackClass, "accept", "(Ljava/lang/Object;)V");
		if (javaLogFuncID == NULL) {
			return -3;
		}

		slv->setlogger([javaEnv, javaCallback, javaLogFuncID](std::string print) {
			javaEnv->CallVoidMethod(javaCallback, javaLogFuncID, javaEnv->NewStringUTF(print.c_str()));
		});
    } else {
    	slv->setlogger([](std::string s) { print(std::cout, "{}\n", s); });
    }
    return 0;
}

JNIEXPORT jboolean JNICALL Java_de_m_1marvin_electronflow_Solver_upload(JNIEnv *env, jobject obj, jlong classid, jstring netlist) {

	std::unique_lock<std::mutex> lck (mapMutex);

	solver* slv = (solver*) jclass2cclass[classid];
    if (slv == NULL) return -3;
    std::string netlistNative = std::string(env->GetStringUTFChars(netlist, 0));
    std::stringstream netlistStream(netlistNative);
    return slv->upload(&netlistStream) ? 0 : 1;
}

JNIEXPORT jboolean JNICALL Java_de_m_1marvin_electronflow_Solver_executeList(JNIEnv *env, jobject obj, jlong classid) {

	std::unique_lock<std::mutex> lck (mapMutex);

	solver* slv = (solver*) jclass2cclass[classid];
    if (slv == NULL) return -3;
    return slv->executeList() ? 0 : 1;
}

JNIEXPORT jboolean JNICALL Java_de_m_1marvin_electronflow_Solver_execute(JNIEnv *env, jobject obj, jlong classid, jstring command) {

	std::unique_lock<std::mutex> lck (mapMutex);

	solver* slv = (solver*) jclass2cclass[classid];
    if (slv == NULL) return -3;
    std::string commandNative = std::string(env->GetStringUTFChars(command, 0));
    return slv->execute(commandNative) ? 0 : 1;
}

JNIEXPORT jstring JNICALL Java_de_m_1marvin_electronflow_Solver_printData(JNIEnv *env, jobject obj, jlong classid) {

	std::unique_lock<std::mutex> lck (mapMutex);

	solver* slv = (solver*) jclass2cclass[classid];
    if (slv == NULL) return NULL;
    std::stringstream dataBuffer;
	slv->printdata(&dataBuffer);
    return env->NewStringUTF(dataBuffer.str().c_str());
}

JNIEXPORT jstring JNICALL Java_de_m_1marvin_electronflow_Solver_netFiltered(JNIEnv *env, jobject obj, jlong classid) {

	std::unique_lock<std::mutex> lck (mapMutex);

	solver* slv = (solver*) jclass2cclass[classid];
    if (slv == NULL) return NULL;
	std::string plot = slv->netfiltered();
    return env->NewStringUTF(plot.c_str());
}
