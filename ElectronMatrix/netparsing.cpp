/*
 * netanalysis.cpp
 *
 *  Created on: 30.09.2024
 *      Author: marvi
 */

#include "netanalysis.h"
#include <string.h>
#include <string>
#include <map>

using namespace std;
using namespace equations;
using namespace netanalysis;

int parseComponent(char* line, component_t* component, vector<equation>* equations) {

	char* tokptr;
	char* componentName = strtok_r(line, " ", &tokptr);
	char* nodeAname = strtok_r(NULL, " ", &tokptr);
	char* nodeBname = strtok_r(NULL, " ", &tokptr);

	// Parameters that all components share
	strcpy_s(component->name, COMPONENT_NAME_LEN, componentName);
	strcpy_s(component->node_a.name, COMPONENT_NAME_LEN, nodeAname);
	strcpy_s(component->node_b.name, COMPONENT_NAME_LEN, nodeBname);

	printf("component: %s - %s > %s\n", nodeAname, nodeBname, componentName);

	// Component specific parameters
	switch (*componentName) {
	case 'V': {
		component->type = V;

		char* equationStr = strtok_r(NULL, " ", &tokptr);
		bool eqres;
		equations->emplace_back(equationStr, &eqres);
		if (!eqres) {
			printf("failed to parse v source voltage equation: %s %s\n", componentName, equationStr);
			return -1;
		}
		component->parameters.v.u_source.equation = &equations->back();

		equationStr = strtok_r(NULL, " ", &tokptr);
		if (equationStr != NULL) {
			component->parameters.v.is_ideal = false;
			equations->emplace_back(equationStr, &eqres);
			if (!eqres) {
				printf("failed to parse v source series resistance equation: %s %s\n", componentName, equationStr);
				return -1;
			}
			component->parameters.v.r_series.equation = &equations->back();
		} else {
			component->parameters.v.is_ideal = true;
		}

		return 0;
	}
	case 'I': {
		component->type = I;

		char* equationStr = strtok_r(NULL, " ", &tokptr);
		bool eqres;
		equations->emplace_back(equationStr, &eqres);
		if (!eqres) {
			printf("failed to parse i source current equation: %s %s\n", componentName, equationStr);
			return -1;
		}
		component->parameters.i.i_source.equation = &equations->back();

		equationStr = strtok_r(NULL, " ", &tokptr);
		if (equationStr != NULL) {
			component->parameters.i.is_ideal = false;
			equations->emplace_back(equationStr, &eqres);
			if (!eqres) {
				printf("failed to parse v source parallel resistance equation: %s %s\n", componentName, equationStr);
				return -1;
			}
			component->parameters.i.r_parallel.equation = &equations->back();
		} else {
			component->parameters.i.is_ideal = true;
		}

		return 0;
	}
	case 'R': {
		component->type = G;

		component->parameters.g.param_count = 1;

		char* equationStr = strtok_r(NULL, " ", &tokptr);
		bool eqres;
		equations->emplace_back(equationStr, &eqres);
		if (!eqres) {
			printf("failed to parse resistor value equation: %s %s\n", componentName, equationStr);
			return -1;
		}
		component->parameters.g.parameters[0].equation = &equations->back();

		// TODO Gate equations

		return 0;
	}
	}

	printf("unknown component type: %s\n", componentName);
	return -1;
}

int netanalysis::parseNetlist(char* netlist, network_t* network) {

	network->components.clear();
	network->nodes.clear();
	network->component_equations.clear();

	char* tokptr;
	char* line = strtok_r(netlist, "\n", &tokptr);

	strcpy_s(network->name, NETWORK_NAME_LEN, line);

	printf("%s > parse components ...\n", network->name);
	while ((line = strtok_r(NULL, "\n", &tokptr)) != 0) {

		if (strlen(line) < 2) continue;

		if (*line == '*') continue;

		if (*line == '.') {
			printf("controll command: %s\n", line);
			continue;

		}

		network->components.push_back({});
		if (parseComponent(line, &network->components.back(), &network->component_equations) != 0) {
			printf("failed to parse component: %s\n", line);
			return -1;
		}

	}

	printf("%s > finalize equations ...\n", network->name);
	for (unsigned int i = 0; i < network->component_equations.size(); i++) {
		network->component_equations[i].finalize();
	}

	printf("%s > scan for nodes ...\n", network->name);
	for (unsigned int i = 0; i < network->components.size(); i++) {
		char* nodeAname = network->components[i].node_a.name;
		char* nodeBname = network->components[i].node_b.name;

		unsigned long long nodeAindex = -1;
		unsigned long long nodeBindex = -1;
		for (unsigned int n = 0; n < network->nodes.size(); n++) {
			char* nodeName = network->nodes[n].name;
			if (strcmp(nodeName, nodeAname) == 0) nodeAindex = n;
			if (strcmp(nodeName, nodeBname) == 0) nodeBindex = n;
			if (nodeAindex != -1 && nodeBindex != -1) break;
		}
		if (nodeAindex == -1) {
			printf("create node: %s\n", nodeAname);

			network->nodes.push_back({});
			strcpy_s(network->nodes.back().name, NODE_NAME_LEN, nodeAname);
			node_t* node = &network->nodes.back();
			node->component_ptr = vector<void*>();
			node->component_ptr.clear();
		}
		if (nodeBindex == -1) {
			printf("create node: %s\n", nodeBname);

			network->nodes.push_back({});
			strcpy_s(network->nodes.back().name, NODE_NAME_LEN, nodeBname);
			node_t* node = &network->nodes.back();
			node->component_ptr = vector<void*>();
			node->component_ptr.clear();
		}
	}

	printf("%s > linking nodes ...\n", network->name);
	for (unsigned int i = 0; i < network->components.size(); i++) {
		char* nodeAname = network->components[i].node_a.name;
		char* nodeBname = network->components[i].node_b.name;

		node_t* nodeA = 0;
		node_t* nodeB = 0;
		for (unsigned int n = 0; n < network->nodes.size(); n++) {
			char* nodeName = network->nodes[n].name;
			if (strcmp(nodeName, nodeAname) == 0) nodeA = network->nodes.data() + n;
			if (strcmp(nodeName, nodeBname) == 0) nodeB = network->nodes.data() + n;
			if (nodeA != 0 && nodeB != 0) break;
		}
		if (nodeA == 0) {
			printf("error, not found node %s\n", nodeAname);
			return -1;
		}
		if (nodeB == 0) {
			printf("error, not found node %s\n", nodeBname);
			return -1;
		}

		network->components[i].node_a.ptr = nodeA;
		network->components[i].node_b.ptr = nodeB;
		nodeA->component_ptr.push_back(network->components.data() + i);
		nodeB->component_ptr.push_back(network->components.data() + i);
	}

	return 0;
}
