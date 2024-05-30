/*
 * circuit_container.cpp
 *
 *  Created on: 21.05.2024
 *      Author: marvi
 */

#include "circuit_container.h"
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <vector>

using namespace std;

CircuitContainer::CircuitContainer(char* netList) {
	pair<vector<NODE_t*>, vector<Element*>> vecp = parseCircuit(netList);
	CircuitContainer::nodes = vector<NODE_t*>(vecp.first); // TODO error when member vectors constant
	CircuitContainer::elements = vector<Element*>(vecp.second);
}

CircuitContainer::~CircuitContainer() {
	for (NODE_t* n : CircuitContainer::nodes) delete n;
	CircuitContainer::nodes.clear();
	for (Element* e : CircuitContainer::elements) delete e;
	CircuitContainer::elements.clear();
	for (char* l : CircuitContainer::commands) free(l);
	CircuitContainer::commands.clear();
}

bool CircuitContainer::linkNodes() {
	printf("linking circuit nodes\n");
	size_t nodeCount = CircuitContainer::nodes.size();
	NODE* nodes = CircuitContainer::nodes.data();
	for (Element* e : CircuitContainer::elements) {
		if (!e->linkNodes(nodes, nodeCount)) {
			printf("[!] failed to link nodes of element '%s'!\n", e->name);
			return false;
		}
	}
	return true;
}

double CircuitContainer::parseNumberValue(const char* valueStr) {
	if (valueStr == 0) {
		printf("invalid component, value == null!\n");
		return 0.0;
	}

	const char* lastChar = valueStr + strlen(valueStr) - 1;

	double multiplier;
	switch (*lastChar) {
	case 'p': multiplier = 1E-9; break;
	case 'n': multiplier = 1E-6; break;
	case 'm': multiplier = 1E-3; break;
	case 'k': multiplier = 1E+3; break;
	case 'M': multiplier = 1E+6; break;
	case 'G': multiplier = 1E+9; break;
	default: multiplier = 1; break;
	}

	double value = atof(valueStr);
	return value * multiplier;
}

NODE_t* CircuitContainer::parseNode(const char* nodeName, vector<NODE_t*>* nodes) {
	if (nodeName == 0) {
		printf("invalid component, node == null!\n");
		return 0;
	}

	if (strlen(nodeName) == 0) return {0};
	for (int i = 0; i < nodes->size(); i++) {
		if (strcmp((*nodes)[i]->name, nodeName) == 0)  {
			return (*nodes)[i];
		}
	}
	nodes->push_back(new NODE_t());
	strcpy(nodes->back()->name, nodeName);
	nodes->back()->charge = 0.0;
	return nodes->back();
}

pair<vector<NODE_t*>, vector<Element*>> CircuitContainer::parseCircuit(char* netList) {

	// parse circuit name

	char* tokptr;
	char* circuitName = strtok_r(netList, "\n", &tokptr);

	printf("load circuit '%s'\n", circuitName);

	// parse nodes and elements

	vector<NODE_t*> nodesVec = vector<NODE_t*>();
	vector<Element*> elementsVec = vector<Element*>();
	bool errorFlag = false;

	char* line = strtok_r(NULL, "\n", &tokptr);
	int lineNr = 1;
	while (line != 0) {

		// parse line, read element name

		char* tokptrl;

		switch (*line) {
		case '#': break; // # = Comment
		case '.': {

			// Copy command line into command vector
			size_t lineLen = strlen(line);
			char* commandLine = (char*) malloc(lineLen);
			memset(commandLine, 0, lineLen);
			strcpy(commandLine, line + 1);
			CircuitContainer::commands.push_back(commandLine);

			break;
		}

		case 'R':
		case 'V':
		case 'I': {

			// parse element specific arguments and construct

			char* elementName = strtok_r(line, " ", &tokptrl);
			NODE_t* node1 = parseNode(strtok_r(NULL, " ", &tokptrl), &nodesVec);
			NODE_t* node2 = parseNode(strtok_r(NULL, " ", &tokptrl), &nodesVec);

			if (strlen(node1->name) == 0) {
				printf("empty node1 on element '%s', line %d", elementName, lineNr);
				errorFlag = true; break;
			}
			if (strlen(node2->name) == 0) {
				printf("empty node2 on element '%s', line %d", elementName, lineNr);
				errorFlag = true; break;
			}

			char* valueStr = strtok_r(NULL, " ", &tokptrl);
			double value = parseNumberValue(valueStr);

			switch (*elementName) {
			case 'R': {
				//printf("resistor between '%s' and '%s' with value '%fOhm'\n", node1->name, node2->name, value);
				elementsVec.push_back(new Resistor(elementName, node1->name, node2->name, value));
				break;
			}
			case 'V': {
				//printf("voltage source between '%s' and '%s' with value '%fV'\n", node1->name, node2->name, value);
				elementsVec.push_back(new VoltageSource(elementName, node1->name, node2->name, value));
				break;
			}
//			case 'I': {
//				printf("current source between '%s' and '%s' with value '%fA'\n", node1->name, node2->name, value);
//				elementsVec.push_back(new CurrentSource(elementName, node1->name, node2->name, value));
//				break;
//			}
			}

			break;
		}
		default: {
			printf("[!] unknown component type '%s'!\n", line);
			break;
		}
		}

		// next line

		line = strtok_r(NULL, "\n", &tokptr);
		lineNr++;
	}

	if (!errorFlag) {
		printf("loading of circuit successful\n");
		printf("loaded elements: %d\n", elementsVec.size());
		printf("loaded nodes: %d\n", nodesVec.size());
	} else {
		printf("[!] error while parsing circuit, not all entries loaded!\n");
	}

	return make_pair(nodesVec, elementsVec);

}


