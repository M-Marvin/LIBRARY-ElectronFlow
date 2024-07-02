/*
 * circuit_solver.cpp
 *
 *  Created on: 21.05.2024
 *      Author: marvi
 */

#include "circuit_solver.h"

#include <stdio.h>
#include <string.h>
#include <algorithm>
#include <math.h>

using namespace std;
using namespace electronflow;

/* the percentage of the node capacity under which the average change
 * in charge transfer value per element has to be,
 * for the state to be considered stable */
#define CT_STABLE_LIMIT 0.001

SourceSolver::SourceSolver(CircuitContainer* circuit) {
	SourceSolver::circuit = circuit;
	SourceSolver::varmap = var_map();
	SourceSolver::funcmap = func_map();
	SourceSolver::simtime = 0;

	equations::set_fmap_default(&funcmap);
	equations::set_vmap_default(&varmap);
	funcmap.emplace(string("P"), make_pair([this](double* args) {
		return args[0] / SourceSolver::nodeCapacity;
	}, 1));
	funcmap.emplace(string("V"), make_pair([this](double* args) {
		return (args[0] - args[1]) / SourceSolver::nodeCapacity;
	}, 2));
	SourceSolver::circuit->setvfmaps(&varmap, &funcmap);
}

SourceSolver::~SourceSolver() {}

void SourceSolver::reset() {
	for (NODE node : SourceSolver::circuit->nodes) {
		SourceSolver::varmap[string(node->name)] = 2.0;
	}
	SourceSolver::simtime = 0;
	SourceSolver::lastCtChange = 0;
}

bool SourceSolver::step(double* timestep) {

	// Solve equations
	if (SourceSolver::lastCtChange < SourceSolver::nodeCapacity * CT_STABLE_LIMIT) {
		for (Element* element : SourceSolver::circuit->elements) {
			if (!element->calc()) {
				printf("failed to calculate component: %s\n", element->name);
				return false;
			}
		}
		SourceSolver::simtime += *timestep;
	}

	// Step elements
	SourceSolver::lastCtChange = 0;
	for (Element* element : SourceSolver::circuit->elements) {

		double change_timestep = element->step(SourceSolver::nodeCapacity, *timestep);

		if (change_timestep != *timestep) {

			if (change_timestep <= 0) {
				printf("timestep == 0, unable to continue!\n");
				return false;
			}

			// Reset last node values
			for (NODE node : SourceSolver::circuit->nodes) {
				node->charge = SourceSolver::varmap[string(node->name)];
			}

			*timestep = change_timestep;
			printf("change timestep: %f s\n", *timestep);
			return true;
		}

		SourceSolver::lastCtChange += abs(element->cTlast - element->cTnow);
	}
	SourceSolver::lastCtChange /= SourceSolver::circuit->elements.size();

	// Copy node charges
	for (NODE node : SourceSolver::circuit->nodes) {
		if (!isfinite(node->charge)) {
			printf("non-finite detected in node %s after %f s!\n", node->name, SourceSolver::simtime);
			return false;
		}
		SourceSolver::varmap[string(node->name)] = node->charge;
	}

	return true;
}
