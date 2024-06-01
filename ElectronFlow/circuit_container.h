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
using namespace equations;

namespace electronflow {

class CircuitContainer {

public:
	CircuitContainer();
	~CircuitContainer();
	bool parseCircuit(char* netList);
	bool linkNodes();

	vector<NODE_t*> nodes;
	vector<Element*> elements;
	vector<char*> commands;
private:
	NODE_t* parseNode(const char* nodeName);

};

}

#endif /* CIRCUIT_CONTAINER_H_ */
