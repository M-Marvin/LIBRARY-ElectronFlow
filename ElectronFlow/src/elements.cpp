/*
 * elements.cpp
 *
 *  Created on: 16.10.2024
 *      Author: marvi
 */

#include "netanalysis.h"

using namespace netanalysis;

element::element(etype type, string name, index_t nodeA, index_t nodeB) {
	element::node_a = nodeA;
	element::node_b = nodeB;
	element::name = name;
	element::type = type;
}

bool element::insert_bce(vector<equations::equation> &eqsys, network &network) {
	switch (element::type) {
		case VSOURCE: return true;
		case ISOURCE:
			if (!element::parameters.contains('I')) return false;
			eqsys.push_back(equation("I-i_" + element::name + "=0").substitute("I"_f, element::parameters['I']));
			return true;
		case RESISTOR:
			if (element::parameters.contains('R')) {
				eqsys.push_back(equation("V(" + network.nodes[element::node_a].name + "," + network.nodes[element::node_b].name + ")/R-i_" + element::name + "=0").substitute("R"_f, element::parameters['R']));
				return true;
			} else if (element::parameters.contains('G')) {
				eqsys.push_back(equation("G*V(" + network.nodes[element::node_a].name + "," + network.nodes[element::node_b].name + ")-i_" + element::name + "=0").substitute("G"_f, element::parameters['G']));
				return true;
			} else {
				return false;
			}
		case CAPACITOR:
			if (!element::parameters.contains('C')) return false;
			eqsys.push_back(equation("C*(∆V(" + network.nodes[element::node_a].name + "," + network.nodes[element::node_b].name + ")/∆t)-i_" + element::name + "=0").substitute("C"_f, element::parameters['C']));
			return true;
	}
	return false;
}
