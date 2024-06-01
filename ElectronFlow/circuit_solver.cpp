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

using namespace electronflow;

SourceSolver::SourceSolver(CircuitContainer* circuit) {
	SourceSolver::circuit = circuit;
}

SourceSolver::~SourceSolver() {}

void SourceSolver::reset() {
	for (NODE node : SourceSolver::circuit->nodes) {
		SourceSolver::varmap.emplace(string(node->name), 0.0);
	}
}

void SourceSolver::step(double timestep) {
	// Solve equations
	for (Element* element : SourceSolver::circuit->elements) {
		element->calc();
	}

	// Step elements
	for (Element* element : SourceSolver::circuit->elements) {
		element->step(SourceSolver::nodeCapacity, timestep);
	}

	// Copy node charges
	for (NODE node : SourceSolver::circuit->nodes) {
		SourceSolver::varmap.emplace(string(node->name), node->charge);
	}
}
