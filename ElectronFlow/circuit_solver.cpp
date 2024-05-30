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

SourceSolver::SourceSolver(CircuitContainer* circuit) {
	SourceSolver::circuit = circuit;
}

SourceSolver::~SourceSolver() {}

void SourceSolver::step(double timestep) {

	for (Element* element : SourceSolver::circuit->elements) {

		element->step(SourceSolver::nodeCapacity, timestep);

	}

}
