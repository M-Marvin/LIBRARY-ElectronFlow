/*
 * circuit_solver.cpp
 *
 *  Created on: 21.05.2024
 *      Author: marvi
 */

#include "circuit_solver.h"

#include <stdio.h>
#include <string.h>

SourceSolver::SourceSolver(CircuitContainer* circuit, Element* source) {
	SourceSolver::circuit = circuit;
	SourceSolver::source = source;
}

SourceSolver::~SourceSolver() {}

bool SourceSolver::compileStructure() {

	printf("compile circuit structure for source '%s'\n", SourceSolver::source->name);

	NODE node1 = SourceSolver::source->node1;
	NODE node2 = SourceSolver::source->node2;

	printf("operate structurizer on nodes '%s'-'%s'\n", node1->name, node2->name);
	


	return false;

}
