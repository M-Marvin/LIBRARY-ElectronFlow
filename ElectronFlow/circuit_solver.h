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
#include <functional>

namespace electronflow {

class SourceSolver {

public:
	SourceSolver(CircuitContainer* circuit);
	~SourceSolver();

	void setCallback(std::function<void(double, NODE*, size_t, Element**, size_t, double, double)> step_callback);
	void reset();
	bool step(double* timestep);
	double nodeCapacity = 0;
	double lastCtChange = 0;
	double simtime;

private:
	var_map varmap;
	func_map funcmap;
	std::function<void(double, NODE*, size_t, Element**, size_t, double, double)> step_callback;
	CircuitContainer* circuit;

};

}

#endif /* CIRCUIT_SOLVER_H_ */
