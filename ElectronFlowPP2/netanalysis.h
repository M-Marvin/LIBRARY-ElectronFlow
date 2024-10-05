/*
 * circuit2.h
 *
 *  Created on: 08.09.2024
 *      Author: marvi
 */

#ifndef NETANALYSIS_H_
#define NETANALYSIS_H_

#include <map>
#include <vector>
#include <string>

using namespace std;

namespace netanalysis {

#define NODE_NAME_LEN 64
#define COMPONENT_NAME_LEN 64
#define NETWORK_NAME_LEN 256

struct component;

typedef struct node {
	string name = "";
	vector<component*> component_ptr = vector<component*>();
	vector<double> voltage = vector<double>();
} node_t;

//union node_link {
//	node_t* ptr;
//	char name[NODE_NAME_LEN];
//};

typedef struct component {
	string name = "";
	string node_a_name = "";
	string node_b_name = "";
	node_t* node_a_ptr = 0x0;
	node_t* node_b_ptr = 0x0;
} component_t;

typedef struct network {
	char name[NETWORK_NAME_LEN] = {0};
	map<string, component_t> components = map<string, component_t>();
	map<string, node_t> nodes = map<string, node_t>();
} network_t;

}

#endif /* NETANALYSIS_H_ */
