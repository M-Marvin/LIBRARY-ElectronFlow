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

typedef struct {
	char name[NODE_NAME_LENGTH];
	double potential;
} NODE_t;

#define NODE NODE_t*

class Element {

public:
	Element(const char* name, const char* node1name, const char* node2name);
	~Element();
	bool linkNodes(NODE* nodeArray, size_t nodesLen);

	char* name;
	char* node1name;
	char* node2name;
	NODE node1;
	NODE node2;

};

class Resistor : public Element {

public:
	Resistor(const char* name, const char* node1name, const char* node2name, double value);
	~Resistor();

	double resistance;

};

class VoltageSource : public Element {

public:
	VoltageSource(const char* name, const char* node1name, const char* node2name, double value);
	~VoltageSource();

	double voltage;

};

class CurrentSource : public Element {

public:
	CurrentSource(const char* name, const char* node1name, const char* node2name, double value);
	~CurrentSource();

	double current;

};

#endif /* CIRCUIT_H_ */
