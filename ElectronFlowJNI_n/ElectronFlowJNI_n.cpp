
#include <de_m_marvin_electronflow_NativeElectronFlow.h>
#include <electron_flow.h>
#include <iostream>

using namespace electronflow;

int main(int argc, char **argv) {
	std::cout << "Hello World" << std::endl;



	return 0;
}


jlong JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_makeInstance_1n(JNIEnv *env, jclass clazz) {



}

void JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_destroyInstance_1n(JNIEnv *env, jclass clazz, jlong handle) {

}

jboolean JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_loadNetList_1n(JNIEnv *env, jclass clazz, jlong handle, jstring netList) {

}

jboolean JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_executeCommand_1n(JNIEnv *env, jclass clazz, jlong handle, jstring command) {

}
