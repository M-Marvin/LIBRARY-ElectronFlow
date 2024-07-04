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

	void setCallback(void (*step_callback) (double, NODE*, size_t, double));
	void reset();
	bool step(double* timestep);
	double nodeCapacity = 0;
	double lastCtChange = 0;
	double simtime;

private:
	var_map varmap;
	func_map funcmap;
	void (*step_callback) (double, NODE*, size_t, double);
	CircuitContainer* circuit;

};

}

#endif /* CIRCUIT_SOLVER_H_ */
