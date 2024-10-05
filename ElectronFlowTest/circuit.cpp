/*
 * circuit.cpp
 *
 *  Created on: 21.05.2024
 *      Author: Marvin K.
 */

#include <stdlib.h>
#include <string.h>
#include "circuit.h"
#include <stdio.h>
#include <math.h>

using namespace std;
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
	Element::cTlast = 0;
	Element::cTnow = 0;
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
void Element::setvfmaps(var_map* varmap, func_map* funcmap) {}
double Element::step(double timestep, step_profile_t* profile) {
	return timestep;
}

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

void Resistor::setvfmaps(var_map* varmap, func_map* funcmap) {
	Resistor::resistanceEq->ext_fmap(funcmap);
	Resistor::resistanceEq->ext_vmap(varmap);
}

double Resistor::step(double timestep, step_profile_t* profile) {
	double cNode1 = Resistor::node1->charge;
	double cNode2 = Resistor::node2->charge;
	double cDiff = (cNode1 - cNode2);
	double i = cDiff / (profile->nodeCapacity * Resistor::resistance);
	Element::cTlast = Element::cTnow;
	Element::cTnow = i * timestep;

	if (abs(Element::cTnow) > abs(cDiff / 2)) {
		if (profile->fixedStepsize) {
			Element::cTnow = cDiff > 0 ? min(cDiff, Element::cTnow) : max(cDiff, Element::cTnow);
		} else {
			return (cDiff / 2) / i;
		}
	}

	Resistor::node1->charge -= Element::cTnow;
	Resistor::node2->charge += Element::cTnow;
	return timestep;
}

/* Define voltage source element */

VoltageSource::VoltageSource(const char* name, const char* node1name, const char* node2name, equation* value, equation* limit) : Element(name, node1name, node2name) {
	VoltageSource::voltageEq = value;
	VoltageSource::voltage = 0.0;
	VoltageSource::currentLimitEq = limit;
	VoltageSource::currentLimit = 0.0;
}

VoltageSource::~VoltageSource() {}

bool VoltageSource::calc() {
	return	VoltageSource::voltageEq->evaluate(&(VoltageSource::voltage)) &&
			VoltageSource::currentLimitEq->evaluate(&(VoltageSource::currentLimit));
}

void VoltageSource::setvfmaps(var_map* varmap, func_map* funcmap) {
	VoltageSource::voltageEq->ext_fmap(funcmap);
	VoltageSource::voltageEq->ext_vmap(varmap);
	VoltageSource::currentLimitEq->ext_fmap(funcmap);
	VoltageSource::currentLimitEq->ext_vmap(varmap);
}

double VoltageSource::step(double timestep, step_profile_t* profile) {
	double vNodes = (VoltageSource::node1->charge / profile->nodeCapacity) - (VoltageSource::node2->charge / profile->nodeCapacity);
	double v = VoltageSource::voltage - vNodes;
	Element::cTlast = Element::cTnow;

	double mct = VoltageSource::currentLimit * timestep;
	Element::cTnow = v * profile->nodeCapacity * 0.5;
	if (mct != 0 && abs(Element::cTnow) > mct && profile->enableSourceLimits) {
		Element::cTnow = signbit(Element::cTnow) == 0 ? mct : -mct;
	}

	if (Element::cTnow >= 0) {
		double diff = max(Element::cTnow - VoltageSource::node2->charge, 0.0);
		VoltageSource::node1->charge += Element::cTnow + diff;
		VoltageSource::node2->charge -= Element::cTnow - diff;
	} else {
		double diff = max(-Element::cTnow - VoltageSource::node1->charge, 0.0);
		VoltageSource::node1->charge += Element::cTnow + diff;
		VoltageSource::node2->charge -= Element::cTnow - diff;
	}

	return timestep;
}
