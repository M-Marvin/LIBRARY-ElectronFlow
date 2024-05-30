/*
 * circuit_solver.h
 *
 *  Created on: 21.05.2024
 *      Author: marvi
 */

#ifndef CIRCUIT_SOLVER_H_
#define CIRCUIT_SOLVER_H_

#include "circuit_container.h"

class SourceSolver {

public:
	SourceSolver(CircuitContainer* circuit);
	~SourceSolver();

	void step(double timestep);
	double nodeCapacity = 0;

private:
	CircuitContainer* circuit;

};

#endif /* CIRCUIT_SOLVER_H_ */
