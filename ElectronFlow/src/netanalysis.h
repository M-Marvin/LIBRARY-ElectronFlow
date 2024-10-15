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

typedef unsigned int index_t;

class element;
class node {
public:
	equation name;
	vector<index_t> elements;
};

enum etype {
	VSOURCE,ISOURCE,GATE
};

class element {

public:
	etype type;
	index_t node_a;
	index_t node_b;

	virtual equation g_val();
	virtual equation i_val();
	virtual equation v_val();

};

class vsource : element {

};

class isource : element {

};

class gate : element {

};

class network {

public:
	vector<node> nodes;
	vector<element> elements;

	bool parse(istream netlist);
	bool matrix(matrixset* matrixset);

};

#endif /* NETANALYSIS_H_ */
