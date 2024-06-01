/*
 * circuit_solver.h
 *
 *  Created on: 21.05.2024
 *      Author: marvi
 */

#ifndef CIRCUIT_SOLVER_H_
#define CIRCUIT_SOLVER_H_

#include "circuit_container.h"
#include "equations.h"

namespace electronflow {

class SourceSolver {

public:
	SourceSolver(CircuitContainer* circuit);
	~SourceSolver();

	void reset();
	void step(double timestep);
	double nodeCapacity = 0;

private:
	var_map varmap;
	CircuitContainer* circuit;

};

}

#endif /* CIRCUIT_SOLVER_H_ */
