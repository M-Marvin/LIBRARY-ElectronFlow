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
	SourceSolver(CircuitContainer* circuit, Element* source);
	~SourceSolver();
	bool compileStructure();

private:
	const CircuitContainer* circuit;
	const Element* source;

};

class ElementGroup {



};



#endif /* CIRCUIT_SOLVER_H_ */
