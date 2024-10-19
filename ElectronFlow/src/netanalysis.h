/*
 * netanalysis.h
 *
 *  Created on: 15.10.2024
 *      Author: marvi
 */

#ifndef NETANALYSIS_H_
#define NETANALYSIS_H_

#include "netmath.h"
#include "equations.h"
#include <string>
#include <vector>
#include <map>
#include <iostream>

using namespace std;

namespace netanalysis {

typedef unsigned int index_t;

class element;
class node {

public:
	string name;
	vector<index_t> elements;

	node(string name) {
		node::name = name;
	}

};

enum etype {
	VSOURCE,ISOURCE,RESISTOR,CAPACITOR
};

class network;
class element {

public:
	etype type;
	string name;
	index_t node_a;
	index_t node_b;
	map<char, equations::equation> parameters;

	element(etype type, string name, index_t nodeA, index_t nodeB);
	bool insert_bce(vector<equations::equation> &eqsys, network &network);
//	bool substitute_bce(equations::equation &eq, network &network);

};

class network {

public:
	vector<node> nodes;
	vector<element> elements;
	map<string, index_t> name2node;
	map<string, index_t> name2element;

	bool parse(istream &netlist);
	bool eqsys(vector<equations::equation> &eqsys);

};

}

#endif /* NETANALYSIS_H_ */
