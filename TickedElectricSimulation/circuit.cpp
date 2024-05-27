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

Resistor::Resistor(const char* name, const char* node1name, const char* node2name, double value) : Element(name, node1name, node2name) {
	Resistor::resistance = value;
}

Resistor::~Resistor() {}

VoltageSource::VoltageSource(const char* name, const char* node1name, const char* node2name, double value) : Element(name, node1name, node2name) {
	VoltageSource::voltage = value;
}

VoltageSource::~VoltageSource() {}

CurrentSource::CurrentSource(const char* name, const char* node1name, const char* node2name, double value) : Element(name, node1name, node2name) {
	CurrentSource::current = value;
}

CurrentSource::~CurrentSource() {}
