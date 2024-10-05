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
#include "netmath.h"

using namespace netmath;
using namespace equations;

namespace netanalysis {

#define NODE_NAME_LEN 64
#define COMPONENT_NAME_LEN 64
#define NETWORK_NAME_LEN 256
#define TERM_LEN 256
#define MAX_GATE_PARAMETERS 8

typedef enum component_type {
	G = 0,
	V = 1,
	I = 2
} component_type_t;

typedef struct node {
	char name[NODE_NAME_LEN] = {0};
	vector<void*> component_ptr = vector<void*>();
	double voltage = 0;
} node_t;

union node_link {
	node_t* ptr;
	char name[NODE_NAME_LEN];
};

typedef struct vsource_param {
	mtemplate u_source = {};
	mtemplate r_series = {};
	bool is_ideal = true;
} vsource_param_t;

typedef struct isource_param {
	mtemplate i_source = {};
	mtemplate r_parallel = {};
	bool is_ideal = true;
} isource_param_t;

typedef struct gate_param {
	unsigned int param_count = 0;
	mtemplate parameters[MAX_GATE_PARAMETERS] = {};
} gate_param_t;

union component_param {
	vsource_param_t v;
	isource_param_t i;
	gate_param_t g;
};

typedef struct component {
	char name[COMPONENT_NAME_LEN] = {0};
	node_link node_a = {};
	node_link node_b = {};
	mtemplate gate_equation = {};
	component_type_t type = G;
	component_param parameters = {};
} component_t;

typedef struct branch {
	node_t* node_a = 0x0;
	node_t* node_b = 0x0;
	vector<component_t*> components = vector<component_t*>(); // sorted from node_a to node_b
	double current = 0;
} branch_t;

typedef struct mesh {
	vector<branch_t*> branchs = vector<branch_t*>(); // sorted from tendom node a to tendem node b (tendom last entry in list)
	double current = 0;
} mesh_t;

typedef struct subnet {
	vector<branch_t*> branches = vector<branch_t*>();
	vector<node_t*> knots = vector<node_t*>();
	vector<mesh_t> meshes = vector<mesh_t>();
} subnet_t;

typedef struct network {
	char name[NETWORK_NAME_LEN] = {0};
	//vector<equation> component_equations;
	vector<component_t> components = vector<component_t>();
	vector<node_t> nodes = vector<node_t>();
	vector<branch_t> branches = vector<branch_t>();
	vector<mesh_t*> meshes = vector<mesh_t*>();
	vector<subnet_t> subnets = vector<subnet_t>();
} network_t;

int parseNetlist(char* netlist, network_t* network);
int findBranches(network_t* network);
void freeNetwork(network_t* network);

}

#endif /* NETANALYSIS_H_ */
