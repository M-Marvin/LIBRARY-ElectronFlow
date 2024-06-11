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
	virtual double step(double nodeCapacity, double timestep);
	virtual bool calc();
	virtual void setvfmaps(var_map* varmap, func_map* funcmap);

	char* name;
	char* node1name;
	char* node2name;
	NODE node1;
	NODE node2;
	double cTlast;
	double cTnow;

};

class Resistor : public Element {

public:
	Resistor(const char* name, const char* node1name, const char* node2name, equation* value);
	~Resistor();

	double step(double nodeCapacity, double timestep);
	bool calc();
	void setvfmaps(var_map* varmap, func_map* funcmap);

	equation* resistanceEq;
	double resistance;

};

class VoltageSource : public Element {

public:
	VoltageSource(const char* name, const char* node1name, const char* node2name, equation* value);
	~VoltageSource();

	double step(double nodeCapacity, double timestep);
	bool calc();
	void setvfmaps(var_map* varmap, func_map* funcmap);

	equation* voltageEq;
	double voltage;

};

}

#endif /* CIRCUIT_H_ */
