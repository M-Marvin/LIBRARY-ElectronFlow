/*
 * netanalysis.cpp
 *
 *  Created on: 30.09.2024
 *      Author: marvi
 */

#include "electronflow.h"
#include "netanalysis.h"
#include "zweitor.h"
#include <string.h>
#include <string>
#include <map>

using namespace std;
using namespace netanalysis;

int parseComponent(char* line, component_t* component) {

	char* tokptr;
	char* componentName = strtok_r(line, " ", &tokptr);
	char* nodeAname = strtok_r(NULL, " ", &tokptr);
	char* nodeBname = strtok_r(NULL, " ", &tokptr);

	if (strlen(componentName) < 2) return -1;
	if (strlen(nodeAname) < 1) return -1;
	if (strlen(nodeBname) < 1) return -1;

	strcpy_s(component->name, COMPONENT_NAME_LEN, componentName);
	strcpy_s(component->node_a.name, NODE_NAME_LEN, nodeAname);
	strcpy_s(component->node_b.name, NODE_NAME_LEN, nodeBname);

	printf("component: %s - %s > %s\n", nodeAname, nodeBname, componentName);
	return 0;
}

bool solver::upload(const char* netlist) {


	printf("%s > prepare for upload ...\n", network.name);

	// Clear previous network
	solver::commands.clear();
	solver::network.components.clear();
	solver::network.nodes.clear();
	memset(solver::network.name, 0, NETWORK_NAME_LEN);

	// Make SPICE ready for upload
	if (!solver::nglspice.isNGSpiceAttached() && !solver::nglspice.initNGSpice(solver::libspicename)) {
		printf("failed to load SPICE\n");
		return false;
	}

	// Create buffers
	size_t netlen = strlen(netlist) + 1;
	size_t targetCap = netlen * 2;
	char* target = (char*) alloca(targetCap); // TODO
	char* buffer = (char*) alloca(netlen); // TODO
	memset(target, 0, targetCap);
	strcpy(buffer, netlist);
	for (size_t i = 0; i < strlen(buffer); i++) if (buffer[i] == '\r') buffer[i] = '\n';

	// Read first line (title)
	char* tokptr;
	char* line = strtok_r(buffer, "\n", &tokptr);
	if (line == NULL) return false;

	// Write title line
	strcat_s(target, targetCap, line);
	strcat_s(target, targetCap, "\n");
	strcpy_s(solver::network.name, NETWORK_NAME_LEN, line);

	printf("%s > parse components ...\n", solver::network.name);
	while ((line = strtok_r(NULL, "\n", &tokptr)) != 0) {

		// Skip empty lines and comments
		if (strlen(line) < 2) continue;
		if (*line == '*') continue;

		// Parse command line
		if (*line == '.') {
			if (strcmp(line, ".end") == 0) continue; // FIXME
			printf("control command: %s\n", line + 1);
			solver::commands.push_back(string(line + 1));
			continue;
		}

		// Parse zweitor line
		if (line[0] == 'Z' && line[1] == 'T') {

			if (!solveZweitor(line, target, targetCap)) {

			}

			continue;
		}

		// Push component line to target buffer
		strcat_s(target, targetCap, line);
		strcat_s(target, targetCap, "\n");

		// Parse component line
		component_t component = {};
		if (parseComponent(line, &component) != 0) {
			printf("failed to parse component: %s\n", line);
			return false;
		}
		solver::network.components[string(component.name)] = component;

	}

	printf("%s > scan for nodes ...\n", network.name);
	for (const auto & [name, component] : network.components) {
		string nodeAname = component.node_a_name;
		string nodeBname = component.node_b_name;

		if (!network.nodes[nodeAname])
			network.nodes[nodeAname] = {nodeAname};
		if (!network.nodes[nodeBname])
			network.nodes[nodeBname] = {nodeAname};

//		long long nodeAindex = -1;
//		long long nodeBindex = -1;
//		for (const auto & [name, node] : network.nodes) {
//			char* nodeName = name.c_str();
//			if (strcmp(nodeName, nodeAname) == 0) nodeAindex = n;
//			if (strcmp(nodeName, nodeBname) == 0) nodeBindex = n;
//			if (nodeAindex != -1 && nodeBindex != -1) break;
//		}
//		if (nodeAindex == -1) {
//			printf("create node: %s\n", nodeAname);
//
//			network.nodes.push_back({});
//			strcpy_s(network.nodes.back().name, NODE_NAME_LEN, nodeAname);
////			node_t* node = &network.nodes.back();
//		}
//		if (nodeBindex == -1) {
//			printf("create node: %s\n", nodeBname);
//
//			network.nodes.push_back({});
//			strcpy_s(network.nodes.back().name, NODE_NAME_LEN, nodeBname);
////			node_t* node = &network.nodes.back();
//		}
	}

	printf("%s > linking nodes ...\n", network.name);
	for (unsigned int i = 0; i < network.components.size(); i++) {
		char* nodeAname = network.components[i].node_a.name;
		char* nodeBname = network.components[i].node_b.name;

		node_t* nodeA = 0;
		node_t* nodeB = 0;
		for (unsigned int n = 0; n < network.nodes.size(); n++) {
			char* nodeName = network.nodes[n].name;
			if (strcmp(nodeName, nodeAname) == 0) nodeA = network.nodes.data() + n;
			if (strcmp(nodeName, nodeBname) == 0) nodeB = network.nodes.data() + n;
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

		network.components[i].node_a.ptr = nodeA;
		network.components[i].node_b.ptr = nodeB;
		nodeA->component_ptr.push_back(network.components.data() + i);
		nodeB->component_ptr.push_back(network.components.data() + i);
	}

	// TODO DEBUG ONLY
	printf(".\nprocessed network: \n%s\n.\n", target);

	printf("%s > upload to SPICE ...\n", network.name);
	if (!solver::nglspice.loadCircuit(target)) {
		printf("failed to parse circuit network\n");
		return false;
	}

	printf("%s > network initialized and ready\n", network.name);
	return true;

}
