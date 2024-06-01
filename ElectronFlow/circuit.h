 /*
 * circuit.h
 *
 *  Created on: 20.05.2024
 *      Author: marvi
 */

#ifndef CIRCUIT_H_
#define CIRCUIT_H_

#define NODE_NAME_LENGTH 16

#include <stdint.h>
#include "equations.h"

using namespace equations;

namespace electronflow {

typedef struct {
	char name[NODE_NAME_LENGTH];
	double charge;
} NODE_t;

#define NODE NODE_t*

class Element {

public:
	Element(const char* name, const char* node1name, const char* node2name);
	virtual ~Element();

	bool linkNodes(NODE* nodeArray, size_t nodesLen);
	virtual void step(double nodeCapacity, double timestep);
	virtual bool calc();

	char* name;
	char* node1name;
	char* node2name;
	NODE node1;
	NODE node2;

};

class Resistor : public Element {

public:
	Resistor(const char* name, const char* node1name, const char* node2name, equation* value);
	~Resistor();

	void step(double nodeCapacity, double timestep);
	bool calc();

	equation* resistanceEq;
	double resistance;

};

class VoltageSource : public Element {

public:
	VoltageSource(const char* name, const char* node1name, const char* node2name, equation* value);
	~VoltageSource();

	void step(double nodeCapacity, double timestep);
	bool calc();

	equation* voltageEq;
	double voltage;

};

}

#endif /* CIRCUIT_H_ */
