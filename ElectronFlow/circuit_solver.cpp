/*
 * circuit_solver.cpp
 *
 *  Created on: 21.05.2024
 *      Author: Marvin K.
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
	SourceSolver::step_callback = 0;
	SourceSolver::simtime = 0;
	SourceSolver::profile.nodeCapacity = DEFAULT_NODE_CAP;
	SourceSolver::profile.enableSourceLimits = DEFAULT_ENABLE_SOURCE_LIMITS;
	SourceSolver::profile.fixedStepsize = DEFAULT_ENABLE_FIXED_TIMESTEP;

	equations::set_fmap_default(&funcmap);
	equations::set_vmap_default(&varmap);
	funcmap.emplace(string("P"), make_pair([this](double* args) {
		return args[0] / SourceSolver::profile.nodeCapacity;
	}, 1));
	funcmap.emplace(string("V"), make_pair([this](double* args) {
		return (args[0] - args[1]) / SourceSolver::profile.nodeCapacity;
	}, 2));
	funcmap.emplace(string("I"), make_pair([this](double* args) {
		return (args[0] / SourceSolver::lastTimestep);
	}, 1));
	SourceSolver::circuit->setvfmaps(&varmap, &funcmap);
}

SourceSolver::~SourceSolver() {}

void SourceSolver::setCallback(function<void(double, NODE*, size_t, Element**, size_t, double, double)> step_callback) {
	SourceSolver::step_callback = step_callback;
}

void SourceSolver::reset() {
	for (NODE node : SourceSolver::circuit->nodes) {
		SourceSolver::varmap[string(node->name)] = 2.0;
	}
	for (Element* element : SourceSolver::circuit->elements) {
		element->cTlast = element->cTnow = 0;
	}
}

void SourceSolver::init() {
	SourceSolver::lastTimestep = profile.initialStepsize;
	SourceSolver::simtime = 0;
}

bool SourceSolver::step() {

	// Step elements
	SourceSolver::lastCtChange = 0;
	for (Element* element : SourceSolver::circuit->elements) {

		double change_timestep = element->step(SourceSolver::lastTimestep, &(SourceSolver::profile));

		if (change_timestep != SourceSolver::lastTimestep) {

			if (change_timestep <= 0) {
				printf("timestep == 0, unable to continue!\n");
				return false;
			}

			// Reset last node values
			for (NODE node : SourceSolver::circuit->nodes) {
				node->charge = SourceSolver::varmap[string(node->name)];
			}

			SourceSolver::lastTimestep = change_timestep;
			printf("change timestep: %1.16lf s\n", SourceSolver::lastTimestep);
			return true;
		}

		SourceSolver::lastCtChange += abs(element->cTlast - element->cTnow);
	}
	SourceSolver::lastCtChange /= SourceSolver::circuit->elements.size();

	// Copy node charges
	for (NODE node : SourceSolver::circuit->nodes) {
		if (!isfinite(node->charge)) {
			printf("non-finite detected in node %s after %1.16lf s!\n", node->name, SourceSolver::simtime);
			return false;
		}
		SourceSolver::varmap[string(node->name)] = node->charge;
	}
	// Copy element charges
	for (Element* element : SourceSolver::circuit->elements) {
		if (!isfinite(element->cTnow)) {
			printf("non-finite detected in element %s after %1.16lf s!\n", element->name, SourceSolver::simtime);
			return false;
		}
		SourceSolver::varmap[string(element->name)] = element->cTnow;
	}

	// Solve equations
	if (SourceSolver::lastCtChange < SourceSolver::profile.nodeCapacity * CT_STABLE_LIMIT) {
		for (Element* element : SourceSolver::circuit->elements) {
			if (!element->calc()) {
				printf("failed to calculate component: %s\n", element->name);
				return false;
			}
		}
		SourceSolver::simtime += SourceSolver::lastTimestep;

		if (SourceSolver::step_callback != 0)
			SourceSolver::step_callback(
					SourceSolver::simtime,
					SourceSolver::circuit->nodes.data(), SourceSolver::circuit->nodes.size(),
					SourceSolver::circuit->elements.data(), SourceSolver::circuit->elements.size(),
					SourceSolver::profile.nodeCapacity, SourceSolver::lastTimestep);
	}

	return true;
}
