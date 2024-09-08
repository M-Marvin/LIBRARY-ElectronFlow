/*
 * circuit2.h
 *
 *  Created on: 08.09.2024
 *      Author: marvi
 */

#ifndef CIRCUIT_H_
#define CIRCUIT_H_

#include "equations.h"
#include <map>

using namespace equations;

namespace network_analysis {

#define NODE_NAME_LEN 64

typedef enum component_type {
	R = 0,
	V = 1,
	I = 2
} component_type_t;

typedef struct node {
	char name[NODE_NAME_LEN];
	double voltage;
} node_t;

typedef struct component {
	node_t* node_a;
	node_t* node_b;
	component_type_t type;
	eq_vec* equation;
	double value;
} component_t;

typedef struct branch {
	node_t* node_a;
	node_t* node_b;
	vector<component_t*> components; // sorted from node_a to node_b
	double current;
} branch_t;

typedef struct mesh {
	vector<branch_t*> branchs; // sorted
} mesh_t;

}

// GdE 1 WS 21_22 Foliensatz 3.pdf
// https://de.wikipedia.org/wiki/Knotenpotentialverfahren
// https://de.wikipedia.org/wiki/Maschenstromverfahren
// https://de.wikipedia.org/wiki/Zweitor#π-Ersatzschaltung
// https://de.wikipedia.org/wiki/Netzwerkanalyse_(Elektrotechnik)

#endif /* CIRCUIT_H_ */
