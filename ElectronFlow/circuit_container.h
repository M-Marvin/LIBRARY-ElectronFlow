/*
 * circuit_container.h
 *
 *  Created on: 21.05.2024
 *      Author: marvi
 */

#ifndef CIRCUIT_CONTAINER_H_
#define CIRCUIT_CONTAINER_H_

#include "circuit.h"
#include <vector>

using namespace std;

class CircuitContainer {

public:
	CircuitContainer(char* netList);
	~CircuitContainer();
	bool linkNodes();

	vector<NODE_t*> nodes;
	vector<Element*> elements;
	vector<char*> commands;
private:
	double parseNumberValue(const char* valueStr);
	NODE_t* parseNode(const char* nodeName, vector<NODE_t*>* nodes);
	pair<vector<NODE_t*>, vector<Element*>> parseCircuit(char* netList);

};

#endif /* CIRCUIT_CONTAINER_H_ */
