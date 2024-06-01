/*
 * circuit.cpp
 *
 *  Created on: 21.05.2024
 *      Author: marvi
 */

#include <stdlib.h>
#include <string.h>
#include "circuit.h"
#include <stdio.h>

using namespace electronflow;

/* Define base element */

Element::Element(const char* name, const char* node1name, const char* node2name) {
	Element::name = (char*) malloc(strlen(name) + 1);
	strcpy(Element::name, name);
	Element::node1name = (char*) malloc(strlen(node1name) + 1);
	strcpy(Element::node1name, node1name);
	Element::node2name = (char*) malloc(strlen(node2name) + 1);
	strcpy(Element::node2name, node2name);
	Element::node1 = 0;
	Element::node2 = 0;
}

Element::~Element() {
	free(Element::name);
	free(Element::node1name);
	free(Element::node2name);
}

bool Element::linkNodes(NODE* nodes, size_t nodesLen) {
	for (int i = 0; i < nodesLen; i++) {
		if (strcmp(nodes[i]->name, Element::node1name) == 0) {
			Element::node1 = nodes[i];
		}
		if (strcmp(nodes[i]->name, Element::node2name) == 0) {
			Element::node2 = nodes[i];
		}
	}
	return Element::node1 != 0 && Element::node2 != 0;
}

bool Element::calc() {
	return true;
}
void Element::step(double nodeCapacity, double timestep) {}

/* Define resistor element */

Resistor::Resistor(const char* name, const char* node1name, const char* node2name, equation* value) : Element(name, node1name, node2name) {
	Resistor::resistanceEq = value;
	Resistor::resistance = 1.0;
}

Resistor::~Resistor() {
	delete Resistor::resistanceEq;
}

bool Resistor::calc() {
	return Resistor::resistanceEq->evaluate(&(Resistor::resistance));
}

void Resistor::step(double nodeCapacity, double timestep) {
	double vNode1 = Resistor::node1->charge / nodeCapacity;
	double vNode2 = Resistor::node2->charge / nodeCapacity;
	double i = (vNode1 - vNode2) / Resistor::resistance;
	double cTransfer = i * timestep;
//	printf("I %f\n", i);
//	printf("V %f %f\n", vNode1, vNode2);
//	printf("C %f\n\n", cTransfer);
	Resistor::node1->charge -= cTransfer;
	Resistor::node2->charge += cTransfer;
}

/* Define voltage source element */

VoltageSource::VoltageSource(const char* name, const char* node1name, const char* node2name, equation* value) : Element(name, node1name, node2name) {
	VoltageSource::voltageEq = value;
	VoltageSource::voltage = 0.0;
}

VoltageSource::~VoltageSource() {}

bool VoltageSource::calc() {
	return VoltageSource::voltageEq->evaluate(&(VoltageSource::voltage));
}

void VoltageSource::step(double nodeCapacity, double timestep) {
	double vNodes = (VoltageSource::node1->charge / nodeCapacity) - (VoltageSource::node2->charge / nodeCapacity);
	double cTransfer = (VoltageSource::voltage - vNodes) * nodeCapacity * 0.5;
	VoltageSource::node1->charge += cTransfer;
	VoltageSource::node2->charge -= cTransfer;

	double vNode1 = VoltageSource::node1->charge / nodeCapacity;
	double vNode2 = VoltageSource::node2->charge / nodeCapacity;
//	printf("V %f %f\n", vNode1, vNode2);
//	printf("%f - %f\n\n", VoltageSource::node1->charge, VoltageSource::node2->charge);
}
