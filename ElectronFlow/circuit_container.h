/*
 * circuit_container.h
 *
 *  Created on: 21.05.2024
 *      Author: Marvin K.
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
	void setvfmaps(var_map* varmap, func_map* funcmap);

	vector<NODE_t*> nodes;
	vector<Element*> elements;
	vector<char*> commands;
private:
	NODE_t* parseNode(const char* nodeName);

};

}

#endif /* CIRCUIT_CONTAINER_H_ */
