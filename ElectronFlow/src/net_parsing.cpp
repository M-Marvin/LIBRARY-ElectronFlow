/*
 * net_parsing.cpp
 *
 *  Created on: 15.10.2024
 *      Author: marvi
 */

#include "netanalysis.h"
#include <iostream>
#include <string>
#include <regex>

using namespace std;
using namespace netanalysis;

const static regex element_two_port_regex("([A-Z][\\w]+)\\s(\\w+)\\s(\\w+)\\s(.+)");

bool network::parse(istream &netlist) {

	network::nodes.clear();
	network::elements.clear();
	network::name2node.clear();
	network::name2element.clear();

	for (string line; std::getline(netlist, line); ) {

		smatch m;
		if (regex_match(line, m, element_two_port_regex)) {

			string name = m.str(1);
			string nodeA = m.str(2);
			string nodeB = m.str(3);
			string param = m.str(4);

			if (!network::name2node.contains(nodeA)) {
				network::nodes.emplace_back(nodeA);
				network::name2node[nodeA] = network::nodes.size() - 1;
			}
			if (!network::name2node.contains(nodeB)) {
				network::nodes.emplace_back(nodeB);
				network::name2node[nodeB] = network::nodes.size() - 1;
			}

			index_t nodeAi = network::name2node[nodeA];
			index_t nodeBi = network::name2node[nodeB];

			switch (name[0]) {
				case 'V': network::elements.emplace_back(VSOURCE, name, nodeAi, nodeBi); break;
				case 'I': network::elements.emplace_back(ISOURCE, name, nodeAi, nodeBi); break;
				case 'R': network::elements.emplace_back(RESISTOR, name, nodeAi, nodeBi); break;
				case 'C': network::elements.emplace_back(CAPACITOR, name, nodeAi, nodeBi); break;
				default: {
					cout << "unknown component type: " << name << "\n";
					return false;
				}
			}

			network::elements.back().parameters[(char) param[0]] = equation(param.substr(2));

			index_t ei = network::elements.size() - 1;
			network::name2element[name] = ei;
			network::nodes[nodeAi].elements.push_back(ei);
			network::nodes[nodeBi].elements.push_back(ei);

		}

	}

	return true;

}


