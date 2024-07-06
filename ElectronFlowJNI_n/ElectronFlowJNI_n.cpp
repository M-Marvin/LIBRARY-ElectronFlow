
#include <de_m_marvin_electronflow_NativeElectronFlow.h>
#include <jni.h>
#include <electron_flow.h>
#include <string.h>
#include <stdio.h>

#define JNI_VERSION JNI_VERSION_1_1
#define J_NODE_CLASS "de/m_marvin/electronflow/NativeElectronFlow$Node"
#define J_NODE_CONSTRUCTOR "<init>"
#define J_NODE_CONSTRUCTOR_SIG "(Ljava/lang/String;D)V"
#define J_NODE_CHARGE_FIELD "charge"
#define J_NODE_CHARGE_FIELD_SIG "D"
#define J_STEP_CALLBACK_INTERFACE "de/m_marvin/electronflow/NativeElectronFlow$StepCallback"
#define J_STEP_CALLBACK_METHOD "stepData"
#define J_STEP_CALLBACK_METHOD_SIG "(D[Lde/m_marvin/electronflow/NativeElectronFlow$Node;D)V"
#define J_FINAL_CALLBACK_INTERFACE "de/m_marvin/electronflow/NativeElectronFlow$FinalCallback"
#define J_FINAL_CALLBACK_METHOD "finalData"
#define J_FINAL_CALLBACK_METHOD_SIG "([Lde/m_marvin/electronflow/NativeElectronFlow$Node;D)V"

using namespace electronflow;

map<jint, ElectronFlow*> instance_map = map<jint, ElectronFlow*>();
map<jint, jobject> step_callback_map = map<jint, jobject>();
map<jint, jobject> final_callback_map = map<jint, jobject>();
map<jint, jobjectArray> node_array_cache_map = map<jint, jobjectArray>();

jclass j_node_class = 0;
jmethodID j_node_constructor = 0;
jfieldID j_node_charge_field = 0;
jclass j_step_callback_interface = 0;
jmethodID j_step_callback_method = 0;
jclass j_final_callback_interface = 0;
jmethodID j_final_callback_method = 0;
jmethodID j_hash_method = 0;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {

	JNIEnv* env;
	if (vm->GetEnv((void**) &env, JNI_VERSION) != 0 || env == NULL) {
		printf("could not get JNI env!\n");
		return -1;
	}

	// Disable output buffer, to keep Java and Native output synchronized
	setvbuf(stdout, NULL, _IONBF, 0);

	// This should never fail, right ?
	j_hash_method = env->GetMethodID(env->FindClass("java/lang/Object"), "hashCode", "()I");

	// Get Node class, constructor and charge field
	j_node_class = (jclass) env->NewGlobalRef(env->FindClass(J_NODE_CLASS));
	if (j_node_class == NULL) {
		printf("failed to locate node class: %s\n", J_NODE_CLASS);
		return -1;
	}
	j_node_constructor = env->GetMethodID(j_node_class, J_NODE_CONSTRUCTOR, J_NODE_CONSTRUCTOR_SIG);
	if (j_node_constructor == NULL) {
		printf("failed to locate node constructor: %s %s\n", J_NODE_CONSTRUCTOR, J_NODE_CONSTRUCTOR_SIG);
		return -1;
	}
	j_node_charge_field = env->GetFieldID(j_node_class, J_NODE_CHARGE_FIELD, J_NODE_CHARGE_FIELD_SIG);
	if (j_node_constructor == NULL) {
		printf("failed to locate node charge field: %s %s\n", J_NODE_CHARGE_FIELD, J_NODE_CHARGE_FIELD_SIG);
		return -1;
	}

	// Get callback methods from objects
	j_step_callback_interface = (jclass) env->NewGlobalRef(env->FindClass(J_STEP_CALLBACK_INTERFACE));
	if (j_step_callback_interface == NULL) {
		printf("failed to locate callback interface: %s\n", J_STEP_CALLBACK_INTERFACE);
		return -1;
	}
	j_final_callback_interface = (jclass) env->NewGlobalRef(env->FindClass(J_FINAL_CALLBACK_INTERFACE));
	if (j_final_callback_interface == NULL) {
		printf("failed to locate callback interface: %s\n", J_FINAL_CALLBACK_INTERFACE);
		return -1;
	}
	j_step_callback_method = env->GetMethodID(j_step_callback_interface, J_STEP_CALLBACK_METHOD, J_STEP_CALLBACK_METHOD_SIG);
	j_final_callback_method = env->GetMethodID(j_final_callback_interface, J_FINAL_CALLBACK_METHOD, J_FINAL_CALLBACK_METHOD_SIG);
	if (j_step_callback_method == NULL) {
		printf("failed to locate callback method: %s %s\n", J_STEP_CALLBACK_METHOD, J_STEP_CALLBACK_METHOD_SIG);
		return -1;
	}
	if (j_final_callback_method == NULL) {
		printf("failed to locate callback method: %s %s\n", J_FINAL_CALLBACK_METHOD, J_FINAL_CALLBACK_METHOD_SIG);
		return -1;
	}

	return JNI_VERSION;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved) {

	JNIEnv* env;
	if (vm->GetEnv((void**) &env, JNI_VERSION) != 0 || env == NULL) {
		printf("could not get JNI env!\n");
		return;
	}

	env->DeleteGlobalRef(j_node_class);
	env->DeleteGlobalRef(j_step_callback_interface);
	env->DeleteGlobalRef(j_final_callback_interface);

}

JNIEXPORT void JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_makeInstance_1n
  (JNIEnv *env, jobject obj) {

	jint objid = env->CallIntMethod(obj, j_hash_method);
	if (instance_map[objid] != 0) return;
	ElectronFlow* ef_instance = new ElectronFlow();
	instance_map[objid] = ef_instance;
}

JNIEXPORT void JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_destroyInstance_1n
  (JNIEnv *env, jobject obj) {

	jint objid = env->CallIntMethod(obj, j_hash_method);
	if (instance_map[objid] == 0) return;
	delete instance_map[objid];
	instance_map.erase(objid);

	if (step_callback_map[objid] != 0) {
		env->DeleteGlobalRef(step_callback_map[objid]);
		step_callback_map.erase(objid);
	}

	if (final_callback_map[objid] != 0) {
		env->DeleteGlobalRef(final_callback_map[objid]);
		step_callback_map.erase(objid);
	}

	if (node_array_cache_map[objid] != 0) {
		env->DeleteGlobalRef(node_array_cache_map[objid]);
		node_array_cache_map.erase(objid);
	}
}

JNIEXPORT void JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_printVersionInfo_1n
  (JNIEnv *env, jobject obj) {

	jint objid = env->CallIntMethod(obj, j_hash_method);
	ElectronFlow* ef_instance = instance_map[objid];

	if (ef_instance == 0) return;
	ef_instance->printVersionInfo();
}

JNIEXPORT void JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_setCallbacks_1n
  (JNIEnv *env, jobject obj, jobject stepCallbackObj, jobject finalCallbackObj) {

	jint objid = env->CallIntMethod(obj, j_hash_method);
	ElectronFlow* ef_instance = instance_map[objid];
	if (ef_instance == 0) return;

	// Tell the JVM that the native code holds an reference to the two callback objects, so they don't get garbage collected
	jobject j_step_callback = env->NewGlobalRef(stepCallbackObj);
	jobject j_final_callback = env->NewGlobalRef(finalCallbackObj);

	// If this instance already holds callback objects, tell the JVM that the native code no longer holds a reference to them
	if (step_callback_map[objid] != NULL) {
		env->DeleteGlobalRef(step_callback_map[objid]);
	}
	step_callback_map[objid] = j_step_callback;
	if (final_callback_map[objid] != NULL) {
		env->DeleteGlobalRef(final_callback_map[objid]);
	}
	final_callback_map[objid] = j_final_callback;

	// Create and set new callback lambdas using the callback objects
	ef_instance->setCallbacks(
			(stepCallbackObj == NULL) ? (function<void(double, NODE*, size_t, double)>) 0 :
				[env, objid, j_step_callback](double simtime, NODE* nodes, size_t nodec, double nodecharge) {

					// Get and update or create Node arrays from the nodes in the callback
					jobjectArray j_nodes = node_array_cache_map[objid];
					if (j_nodes == 0) {
						j_nodes = (jobjectArray) env->NewGlobalRef(env->NewObjectArray(nodec, j_node_class, NULL));
						for (unsigned int i = 0; i < nodec; i++) {
							jstring j_node_name = env->NewStringUTF(nodes[i]->name);
							jobject j_node = env->NewObject(j_node_class, j_node_constructor, j_node_name, nodes[i]->charge);
							env->SetObjectArrayElement(j_nodes, i, j_node);
						}
						node_array_cache_map[objid] = j_nodes;
					} else {
						// We can assume here that order and length of the nodes is correct, since it does not change without loading a new circuit
						for (unsigned int i = 0; i < nodec; i++) {
							jobject j_node = env->GetObjectArrayElement(j_nodes, i);
							env->SetDoubleField(j_node, j_node_charge_field, nodes[i]->charge);
						}
					}

					env->CallVoidMethod(j_step_callback, j_step_callback_method, simtime, j_nodes, nodecharge);

				},
			(finalCallbackObj == NULL) ? (function<void(NODE*, size_t, double)>) 0 :
				[env, objid, j_final_callback](NODE* nodes, size_t nodec, double nodecharge) {

					// Get and update or create Node arrays from the nodes in the callback
					jobjectArray j_nodes = node_array_cache_map[objid];
					if (j_nodes == 0) {
						j_nodes = (jobjectArray) env->NewGlobalRef(env->NewObjectArray(nodec, j_node_class, NULL));
						for (unsigned int i = 0; i < nodec; i++) {
							jstring j_node_name = env->NewStringUTF(nodes[i]->name);
							jobject j_node = env->NewObject(j_node_class, j_node_constructor, j_node_name, nodes[i]->charge);
							env->SetObjectArrayElement(j_nodes, i, j_node);
						}
						node_array_cache_map[objid] = j_nodes;
					} else {
						// We can assume here that order and length of the nodes is correct, since it does not change without loading a new circuit
						for (unsigned int i = 0; i < nodec; i++) {
							jobject j_node = env->GetObjectArrayElement(j_nodes, i);
							env->SetDoubleField(j_node, j_node_charge_field, nodes[i]->charge);
						}
					}

					env->CallVoidMethod(j_final_callback, j_final_callback_method, j_nodes, nodecharge);

				});

}

JNIEXPORT jboolean JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_loadNetList_1n
  (JNIEnv *env, jobject obj, jstring netlist) {

	jint objid = env->CallIntMethod(obj, j_hash_method);
	ElectronFlow* ef_instance = instance_map[objid];
	if (ef_instance == 0) return false;

	// Delete cached node array. since the new circuit probably has different nodes
	if (node_array_cache_map[objid] != 0) {
		env->DeleteGlobalRef(node_array_cache_map[objid]);
		node_array_cache_map.erase(objid);
	}

	const char* netlist_str = env->GetStringUTFChars(netlist, NULL);
	char* netlist_dup = strdup(netlist_str);
	bool result = ef_instance->loadNetList(netlist_dup);
	free(netlist_dup);
	return result;
}

JNIEXPORT jboolean JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_loadAndRunNetList_1n
  (JNIEnv *env, jobject obj, jstring netlist) {

	jint objid = env->CallIntMethod(obj, j_hash_method);
	ElectronFlow* ef_instance = instance_map[objid];
	if (ef_instance == 0) return false;

	// Delete cached node array. since the new circuit probably has different nodes
	if (node_array_cache_map[objid] != 0) {
		env->DeleteGlobalRef(node_array_cache_map[objid]);
		node_array_cache_map.erase(objid);
	}

	const char* netlist_str = env->GetStringUTFChars(netlist, NULL);
	char* netlist_dup = strdup(netlist_str);
	bool result = ef_instance->loadAndRunNetList(netlist_dup);
	free(netlist_dup);
	return result;
}

JNIEXPORT jboolean JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_stepSimulation_1n
  (JNIEnv *env, jobject obj, jdouble nodeCapacity, jdouble timestep, jdouble simulateTime) {

	jint objid = env->CallIntMethod(obj, j_hash_method);
	ElectronFlow* ef_instance = instance_map[objid];
	if (ef_instance == 0) return false;

	return ef_instance->stepSimulation(nodeCapacity, timestep, simulateTime);
}

JNIEXPORT void JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_printNodeVoltages_1n
  (JNIEnv *env, jobject obj, jstring refNode) {

	jint objid = env->CallIntMethod(obj, j_hash_method);
	ElectronFlow* ef_instance = instance_map[objid];
	if (ef_instance == 0) return;

	ef_instance->printNodeVoltages(env->GetStringUTFChars(refNode, NULL));
}

JNIEXPORT void JNICALL Java_de_m_1marvin_electronflow_NativeElectronFlow_controllCommand_1n
  (JNIEnv *env, jobject obj, jobjectArray cmdArgs) {

	jint objid = env->CallIntMethod(obj, j_hash_method);
	ElectronFlow* ef_instance = instance_map[objid];
	if (ef_instance == 0) return;

	jclass j_string = env->FindClass("java/lang/String");

	int argc = env->GetArrayLength(cmdArgs);
	const char* argv[argc];
	for (int i = 0; i < argc; i++) {
		jobject j_arg = env->GetObjectArrayElement(cmdArgs, i);
		if (!env->IsInstanceOf(j_arg, j_string) || j_arg == NULL) return;
		argv[i] = env->GetStringUTFChars((jstring) j_arg, NULL);
	}

	ef_instance->controllCommand(argc, argv);
}
