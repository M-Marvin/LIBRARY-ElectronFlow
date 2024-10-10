
#include "nglink.h"
#include "de_m_marvin_nglink_NGLink.h"
#include <map>
#include <mutex>

/* Helper methods to convert to JNI values */
#define JSTRING env->FindClass("java/lang/String")

using namespace ngspice;

jobjectArray stringVectorToStringArray(std::vector<std::string>* stringList, JNIEnv* env) {
    jobjectArray stringArr = env->NewObjectArray(stringList->size(), JSTRING, 0);
    for (size_t i = 0; i < stringList->size(); i++) {
        jstring javaString = env->NewStringUTF((*stringList)[i].c_str());
        env->SetObjectArrayElement(stringArr, i, javaString);
    }
    return stringArr;
}
/* End of helper methods */

static std::map<jlong, void*> jclass2cclass;
static std::mutex mapMutex;

JNIEXPORT jint JNICALL Java_de_m_1marvin_nglink_NGLink_initNGLink(JNIEnv* env, jobject obj, jlong classid, jobject icallback) {

	std::unique_lock<std::mutex> lck (mapMutex);

	setvbuf(stdout, NULL, _IONBF, 0);

	if (jclass2cclass[classid] != NULL) {
		nglink* nglinki = (nglink*) jclass2cclass[classid];
		nglinki->logPrinter("for this class an native instance is already created!");
		return 0;
	}

	nglink* nglinki = new nglink();
	jclass2cclass[classid] = nglinki;

    jclass callbackClass = env->FindClass("de/m_marvin/nglink/NGLink$INGCallback");
    if (callbackClass == NULL) {
        nglinki->logPrinter("Failed to find NGCallback class!");
        return -3;
    }
    JNIEnv* javaEnv = env;
    jobject javaCallback = env->NewGlobalRef(icallback);

    jmethodID javaLogFuncID = env->GetMethodID(callbackClass, "log", "(Ljava/lang/String;)V");
    if (javaLogFuncID == NULL) {
    	nglinki->logPrinter("Failed to find log callback method!");
    }
    jmethodID javaDetacheFuncID = env->GetMethodID(callbackClass, "detacheNGSpice", "()V");
    if (javaDetacheFuncID == NULL) {
    	nglinki->logPrinter("Failed to find detacgeNGSpice callback method!");
    }
    jmethodID javaReceiveVecFuncID = env->GetMethodID(callbackClass, "reciveVecData", "(Lde/m_marvin/nglink/NGLink$VectorValuesAll;I)V");
    if (javaReceiveVecFuncID == NULL) {
    	nglinki->logPrinter("Failed to find reciveVecData callback method!");
    }
    jmethodID javaReceiveInitFuncID = env->GetMethodID(callbackClass, "reciveInitData", "(Lde/m_marvin/nglink/NGLink$PlotDescription;)V");
    if (javaReceiveInitFuncID == NULL) {
    	nglinki->logPrinter("Failed to find reciveInitData callback method!");
    }

    if (javaLogFuncID == NULL || javaDetacheFuncID == NULL || javaReceiveVecFuncID == NULL || javaReceiveInitFuncID == NULL) {
        return -3;
    }

    return nglinki->initNGLinkFull(
        [javaEnv, javaCallback, javaLogFuncID](std::string print) {
            javaEnv->CallVoidMethod(javaCallback, javaLogFuncID, javaEnv->NewStringUTF(print.c_str()));
        },
        [javaEnv, javaCallback, javaDetacheFuncID]() {
            javaEnv->CallVoidMethod(javaCallback, javaDetacheFuncID);
        },
        [nglinki, javaEnv, javaCallback, javaReceiveVecFuncID](spice_pvecvaluesall data) {
            jclass vectorValuesClass = javaEnv->FindClass("de/m_marvin/nglink/NGLink$VectorValue");
            if (vectorValuesClass == NULL) {
            	nglinki->logPrinter("Failed to find VectorValue class!");
                return;
            }
            jmethodID vectorValuesConstructor = javaEnv->GetMethodID(vectorValuesClass, "<init>", "(Ljava/lang/String;DDZZ)V");
            if (vectorValuesConstructor == NULL) {
            	nglinki->logPrinter("Failed to find VectorValue constructor!");
                return;
            }
            jclass vectorValuesAllClass = javaEnv->FindClass("de/m_marvin/nglink/NGLink$VectorValuesAll");
            if (vectorValuesAllClass == NULL) {
            	nglinki->logPrinter("Failed to find VectorValuesAll class!");
                return;
            }
            jmethodID vectorValuesAllConstructor = javaEnv->GetMethodID(vectorValuesAllClass, "<init>", "(II[Lde/m_marvin/nglink/NGLink$VectorValue;)V");
            if (vectorValuesAllConstructor == NULL) {
            	nglinki->logPrinter("Failed to find VectorValuesAll constructor!");
                return;
            }

            jint vectorCount = data->veccount;

            jobjectArray vectorValuesArr = javaEnv->NewObjectArray(vectorCount, vectorValuesClass, 0);
            for (int i = 0; i < vectorCount; i++) {
                spice_pvecvalues vectorValueNative = data->vecsa[i];
                jobject vectorValue = javaEnv->NewObject(vectorValuesClass, vectorValuesConstructor,
                    javaEnv->NewStringUTF(vectorValueNative->name),
                    vectorValueNative->creal,
                    vectorValueNative->cimag,
                    vectorValueNative->is_scale,
                    vectorValueNative->is_complex
                );
                javaEnv->SetObjectArrayElement(vectorValuesArr, i, vectorValue);
            }

            jobject vectorValuesAll = javaEnv->NewObject(vectorValuesAllClass, vectorValuesAllConstructor,
                data->veccount,
                data->veccount,
                vectorValuesArr
            );

            javaEnv->CallVoidMethod(javaCallback, javaReceiveVecFuncID, vectorValuesAll, vectorCount);
        },
        [nglinki, javaEnv, javaCallback, javaReceiveInitFuncID](spice_pvecinfoall data) {
            jclass plotDescClass = javaEnv->FindClass("de/m_marvin/nglink/NGLink$PlotDescription");
            if (plotDescClass == NULL) {
            	nglinki->logPrinter("Failed to find PlotDescription class!");
                return;
            }
            jmethodID plotDescConstructor = javaEnv->GetMethodID(plotDescClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I[Lde/m_marvin/nglink/NGLink$VectorDescription;)V");
            if (plotDescConstructor == NULL) {
            	nglinki->logPrinter("Failed to find PlotDescription constructor!");
                return;
            }
            jclass vectorDescClass = javaEnv->FindClass("de/m_marvin/nglink/NGLink$VectorDescription");
            if (vectorDescClass == NULL) {
            	nglinki->logPrinter("Failed to find VectorDescription class!");
                return;
            }
            jmethodID vectorDescConstructor = javaEnv->GetMethodID(vectorDescClass, "<init>", "(ILjava/lang/String;Z)V");
            if (vectorDescConstructor == NULL) {
            	nglinki->logPrinter("Failed to find VectorDescription constructor!");
                return;
            }

            jstring name = javaEnv->NewStringUTF(data->name);
            jstring title = javaEnv->NewStringUTF(data->title);
            //jstring date = javaEnv->NewStringUTF(data->date);
            jstring type = javaEnv->NewStringUTF(data->type);
            jint vectorCount = data->veccount;

            jobjectArray vectorDescArr = javaEnv->NewObjectArray(vectorCount, vectorDescClass, 0);
            for (int i = 0; i < vectorCount; i++) {
            	spice_pvecinfo vectorInfoNative = data->vecs[i];

                jobject vectorInfo = javaEnv->NewObject(vectorDescClass, vectorDescConstructor,
                    vectorInfoNative->number,
                    javaEnv->NewStringUTF(vectorInfoNative->vecname),
                    vectorInfoNative->is_real
                );
                javaEnv->SetObjectArrayElement(vectorDescArr, i, vectorInfo);
            }

            jobject plotDescriptor = javaEnv->NewObject(plotDescClass, plotDescConstructor,
                name,
                title,
                data,
                type,
                vectorCount,
                vectorDescArr
            );

            javaEnv->CallVoidMethod(javaCallback, javaReceiveInitFuncID, plotDescriptor);
        }
    ) ? 0 : 1;

}

JNIEXPORT jint JNICALL Java_de_m_1marvin_nglink_NGLink_detachNGLink(JNIEnv* env, jobject obj, jlong classid) {

	std::unique_lock<std::mutex> lck (mapMutex);

	nglink* nglinki = (nglink*) jclass2cclass[classid];
	if (nglinki == NULL) return -3;
	if (nglinki->isNGSpiceAttached()) {
		nglinki->detachNGSpice();
	}
	nglinki->logPrinter("detaching nglink!");
	delete nglinki;
	jclass2cclass[classid] = NULL;
	return 1;
}

JNIEXPORT jboolean JNICALL Java_de_m_1marvin_nglink_NGLink_isInitialized(JNIEnv* env, jobject obj, jlong classid) {

	std::unique_lock<std::mutex> lck (mapMutex);

	nglink* nglinki = (nglink*) jclass2cclass[classid];
    if (nglinki == NULL) return false;
    return nglinki->isInitialisated();
}

JNIEXPORT jint JNICALL Java_de_m_1marvin_nglink_NGLink_initNGSpice(JNIEnv* env, jobject obj, jlong classid, jstring libName) {

	std::unique_lock<std::mutex> lck (mapMutex);

	nglink* nglinki = (nglink*) jclass2cclass[classid];
    if (nglinki == NULL) return -3;
	if (libName == NULL) return 0;
    return nglinki->initNGSpice(env->GetStringUTFChars(libName, 0)) ? 0 : 1;
}

JNIEXPORT jint JNICALL Java_de_m_1marvin_nglink_NGLink_detachNGSpice(JNIEnv* env, jobject obj, jlong classid) {

	std::unique_lock<std::mutex> lck (mapMutex);

	nglink* nglinki = (nglink*) jclass2cclass[classid];
    if (nglinki == NULL) return -3;
    return nglinki->detachNGSpice() ? 0 : 1;
}

jboolean Java_de_m_1marvin_nglink_NGLink_isNGSpiceAttached(JNIEnv* env, jobject obj, jlong classid) {

	std::unique_lock<std::mutex> lck (mapMutex);

	nglink* nglinki = (nglink*) jclass2cclass[classid];
	if (nglinki == NULL) return false;
	return nglinki->isNGSpiceAttached();
}

JNIEXPORT jint JNICALL Java_de_m_1marvin_nglink_NGLink_execCommand(JNIEnv* env, jobject obj, jlong classid, jstring command) {

	std::unique_lock<std::mutex> lck (mapMutex);

	nglink* nglinki = (nglink*) jclass2cclass[classid];
    if (nglinki == NULL) return -3;
    std::string commandNative = std::string(env->GetStringUTFChars(command, 0));
    return nglinki->execCommand(commandNative) ? 0 : 1;
}

JNIEXPORT jint JNICALL Java_de_m_1marvin_nglink_NGLink_loadCircuit(JNIEnv* env, jobject obj, jlong classid, jstring commandList) {

	std::unique_lock<std::mutex> lck (mapMutex);

	nglink* nglinki = (nglink*) jclass2cclass[classid];
    if (nglinki == NULL) return -3;
    if (commandList == NULL) return 0;
    std::string commandListNative = std::string(env->GetStringUTFChars(commandList, 0));
    return nglinki->loadCircuit(commandListNative) ? 0 : 1;
}

JNIEXPORT jboolean JNICALL Java_de_m_1marvin_nglink_NGLink_isRunning(JNIEnv* env, jobject obj, jlong classid) {

	std::unique_lock<std::mutex> lck (mapMutex);

	nglink* nglinki = (nglink*) jclass2cclass[classid];
    if (nglinki == NULL) return false;
    return nglinki->isRunning();
}

JNIEXPORT jobjectArray JNICALL Java_de_m_1marvin_nglink_NGLink_listPlots(JNIEnv* env, jobject obj, jlong classid) {

	std::unique_lock<std::mutex> lck (mapMutex);

	nglink* nglinki = (nglink*) jclass2cclass[classid];
    if (nglinki == NULL) return NULL;
	vector<string> plots = nglinki->listPlots();
    return stringVectorToStringArray(&plots, env);
}

JNIEXPORT jstring JNICALL Java_de_m_1marvin_nglink_NGLink_getCurrentPlot(JNIEnv* env, jobject obj, jlong classid) {

	std::unique_lock<std::mutex> lck (mapMutex);

	nglink* nglinki = (nglink*) jclass2cclass[classid];
    if (nglinki == NULL) return NULL;
	std::string plot = nglinki->getCurrentPlot();
    return env->NewStringUTF(plot.c_str());
}

JNIEXPORT jobjectArray JNICALL Java_de_m_1marvin_nglink_NGLink_getVecs(JNIEnv* env, jobject obj, jlong classid, jstring plotName) {

	std::unique_lock<std::mutex> lck (mapMutex);

	nglink* nglinki = (nglink*) jclass2cclass[classid];
    if (nglinki == NULL) return NULL;

    if (plotName == NULL) return NULL;
    std::string plotNameNative = string(env->GetStringUTFChars(plotName, 0));
    std::vector<std::string> vecNamesNative = nglinki->listVecs(plotNameNative);
    return stringVectorToStringArray(&vecNamesNative, env);
}
