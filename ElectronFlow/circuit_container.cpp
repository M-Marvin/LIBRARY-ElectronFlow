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
using namespace equations;
using namespace electronflow;

CircuitContainer::CircuitContainer() {
	CircuitContainer::nodes = vector<NODE_t*>();
	CircuitContainer::elements = vector<Element*>();
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

void CircuitContainer::setvfmaps(var_map* varmap, func_map* funcmap) {
	for (Element* element : CircuitContainer::elements) {
		element->setvfmaps(varmap, funcmap);
	}
}

NODE_t* CircuitContainer::parseNode(const char* nodeName) {
	if (nodeName == 0) {
		printf("invalid nodename, node == null!\n");
		return 0;
	}
	if (strlen(nodeName) > NODE_NAME_LENGTH) {
		printf("invalid nodename, %s len > %d!\n", nodeName, NODE_NAME_LENGTH);
		return 0;
	}

	if (strlen(nodeName) == 0) return {0};
	for (int i = 0; i < nodes.size(); i++) {
		if (strcmp(CircuitContainer::nodes[i]->name, nodeName) == 0)  {
			return CircuitContainer::nodes[i];
		}
	}
	CircuitContainer::nodes.push_back(new NODE_t());
	strcpy(CircuitContainer::nodes.back()->name, nodeName);
	CircuitContainer::nodes.back()->charge = 0.0;

	return CircuitContainer::nodes.back();
}

bool CircuitContainer::parseCircuit(char* netList) {

	// parse circuit name

	char* tokptr;
	char* circuitName = strtok_r(netList, "\n", &tokptr);

	printf("load circuit '%s'\n", circuitName);

	// parse nodes and elements

	bool errorFlag = false;
	char* line = strtok_r(NULL, "\n", &tokptr);
	int lineNr = 1;
	while (line != 0) {

		// parse line, read element name

		char* tokptrl;

		switch (*line) {
		case '#':
		case '*': break; // # and * = Comment
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
			NODE_t* node1 = parseNode(strtok_r(NULL, " ", &tokptrl));
			NODE_t* node2 = parseNode(strtok_r(NULL, " ", &tokptrl));

			if (strlen(node1->name) == 0) {
				printf("empty node1 on element '%s', line %d", elementName, lineNr);
				errorFlag = true; break;
			}
			if (strlen(node2->name) == 0) {
				printf("empty node2 on element '%s', line %d", elementName, lineNr);
				errorFlag = true; break;
			}

			char* valueStr = strtok_r(NULL, " ", &tokptrl);
			equation* value = new equation(valueStr);

			switch (*elementName) {
			case 'R': {
				CircuitContainer::elements.push_back(new Resistor(elementName, node1->name, node2->name, value));
				break;
			}
			case 'V': {
				CircuitContainer::elements.push_back(new VoltageSource(elementName, node1->name, node2->name, value));
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
		printf("loaded elements: %d\n", CircuitContainer::elements.size());
		printf("loaded nodes: %d\n", CircuitContainer::nodes.size());
	} else {
		printf("[!] error while parsing circuit, not all entries loaded!\n");
		return false;
	}

	return true;

}


