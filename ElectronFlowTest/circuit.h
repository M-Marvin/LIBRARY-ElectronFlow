 /*
 * circuit.h
 *
 *  Created on: 20.05.2024
 *      Author: Marvin K.
 */

#ifndef CIRCUIT_H_
#define CIRCUIT_H_

#define NODE_NAME_LENGTH 64

#include <stdint.h>
#include "equations.h"

using namespace equations;

namespace electronflow {

typedef struct {
	char name[NODE_NAME_LENGTH];
	double charge;
} NODE_t;

#define NODE NODE_t*

typedef struct step_profile {
	bool enableSourceLimits;
	double initialStepsize;
	double nodeCapacity;
	bool fixedStepsize;
} step_profile_t;

#define DEFAULT_NODE_CAP 0.12
#define DEFAULT_ENABLE_SOURCE_LIMITS true
#define DEFAULT_ENABLE_FIXED_TIMESTEP false

class Element {

public:
	Element(const char* name, const char* node1name, const char* node2name);
	virtual ~Element();

	bool linkNodes(NODE* nodeArray, size_t nodesLen);
	virtual double step(double timestep, step_profile_t* profile);
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

	double step(double timestep, step_profile_t* profile);
	bool calc();
	void setvfmaps(var_map* varmap, func_map* funcmap);

	equation* resistanceEq;
	double resistance;

};

class VoltageSource : public Element {

public:
	VoltageSource(const char* name, const char* node1name, const char* node2name, equation* value, equation* limit);
	~VoltageSource();

	double step(double timestep, step_profile_t* profile);
	bool calc();
	void setvfmaps(var_map* varmap, func_map* funcmap);

	equation* voltageEq;
	double voltage;
	equation* currentLimitEq;
	double currentLimit;

};

}

#endif /* CIRCUIT_H_ */
