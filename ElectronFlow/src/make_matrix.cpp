/*
 * make_matrix.cpp
 *
 *  Created on: 15.10.2024
 *      Author: marvi
 */

#include "netanalysis.h"
#include "equations.h"
#include <algorithm>

using namespace netanalysis;

bool network::eqsys(vector<equations::equation> &eqsys) {

	for (index_t ni = 0; ni < network::nodes.size(); ni++) {
		eqsys.push_back("=0"_f);
		for (index_t ei : network::nodes[ni].elements) {
			element* e = &network::elements[ei];
			equation sign = ni == e->node_b ? "+"_f : "-"_f;
			eqsys.back().insert(0, sign + equation("i_" + e->name));
		}
	}

	// DEBUG print
	cout << "equation system: stage 1\n";
	for (equations::equation eq : eqsys) {
		cout << eq.str() << "\n";
	}

	for (element &e : network::elements) {
		if (!e.insert_bce(eqsys, *this)) {
			cout << "failed to insert component bce: " << e.name << ", most likely a missing parameter!\n";
			return false;
		}

	}

	// DEBUG print
	cout << "equation system: stage 2\n";
	for (equations::equation eq : eqsys) {
		cout << eq.str() << "\n";
	}

	for (equations::equation &eq : eqsys) {
		eq.substitute_func("V", 2, [this](vector<equations::equation> params) {
			string nodeA = params[0].str();
			string nodeB = params[1].str();
			if (!network::name2node.contains(nodeA)) {
				cout << "unknown node in equation: " << nodeA << "\n";
				return "0"_f;
			}
			if (!network::name2node.contains(nodeB)) {
				cout << "unknown node in equation: " << nodeB << "\n";
				return "0"_f;
			}
			return equation("(p_" + nodeA + "-p_" + nodeB + ")");
		});
		eq.substitute_func("V", 1, [this](vector<equations::equation> params) {
			string ename = params[0].str();
			if (!network::name2element.contains(ename)) {
				cout << "unknown element in equation: " << ename << "\n";
				return "0"_f;
			}
			index_t ei = network::name2element[ename];
			element* e = &network::elements[ei];
			string nodeA = network::nodes[e->node_a].name;
			string nodeB = network::nodes[e->node_b].name;
			return equation("(p_" + nodeA + "-p_" + nodeB + ")");
		});
		eq.substitute_func("I", 1, [this](vector<equations::equation> params) {
			string ename = params[0].str();
			if (!network::name2element.contains(ename)) {
				cout << "unknown element in equation: " << ename << "\n";
				return "0"_f;
			}
			return equation("i_" + ename);
		});
	}

	// DEBUG print
	cout << "equation system: stage 3\n";
	for (equations::equation eq : eqsys) {
		cout << eq.str() << "\n";
	}

	for (equations::equation &eq : eqsys) {
		eq.expand();
	}

	// DEBUG print
	cout << "equation system: stage 3\n";
	for (equations::equation eq : eqsys) {
		cout << eq.str() << "\n";
	}

	return true;

}



