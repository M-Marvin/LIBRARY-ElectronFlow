/*
 * circuit2.h
 *
 *  Created on: 08.09.2024
 *      Author: marvi
 */

#ifndef NETANALYSIS_H_
#define NETANALYSIS_H_

#include "equations.h"
#include <map>

using namespace equations;

namespace netanalysis {

#define NODE_NAME_LEN 64
#define COMPONENT_NAME_LEN 64
#define NETWORK_NAME_LEN 256
#define MAX_GATE_PARAMETERS 8

typedef enum component_type {
	G = 0,
	V = 1,
	I = 2
} component_type_t;

typedef struct node {
	char name[NODE_NAME_LEN];
	double voltage;
} node_t;

union node_link {
	node_t* ptr;
	char name[NODE_NAME_LEN];
};

typedef struct eq_param {
	equation* equation;
	double value;
} eq_param_t;

typedef struct vsource_param {
	eq_param u_source;
	eq_param r_series;
	bool is_ideal;
} vsource_param_t;

typedef struct isource_param {
	eq_param i_source;
	eq_param r_parallel;
	bool is_ideal;
} isource_param_t;

typedef struct gate_param {
	unsigned int param_count;
	eq_param parameters[MAX_GATE_PARAMETERS];
} gate_param_t;

union component_param {
	vsource_param_t v;
	isource_param_t i;
	gate_param_t g;
};

typedef struct component {
	char name[COMPONENT_NAME_LEN];
	node_link node_a;
	node_link node_b;
	component_type_t type;
	component_param parameters;
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

typedef struct network {
	char name[NETWORK_NAME_LEN];
	vector<equation> component_equations;
	vector<component_t> components;
	vector<node_t> nodes;
	vector<branch_t> branches;
	vector<branch_t> tendoms;
	vector<mesh_t> meshes;
} network_t;

int parseNetlist(char* netlist, size_t len, network_t* network);
void freeNetwork(network_t* network);

void findBranches(network_t* network);

}

#endif /* NETANALYSIS_H_ */
