/*
 * electron_flow.h
 *
 *  Created on: 30.05.2024
 *      Author: marvi
 */

#ifndef ELECTRON_FLOW_H_
#define ELECTRON_FLOW_H_

#include "circuit_solver.h"

class ElectronFlow {

public:
	ElectronFlow();
	~ElectronFlow();

	bool loadNetList(char* netList);
	bool loadAndRunNetList(char* netList);
	void stepSimulation(double nodeCapacity, double timestep, int steps);
	void printNodeVoltages(const char* refNodeName);
	void controllCommand(int argc, char** argv);

private:
	CircuitContainer* circuit;
	SourceSolver* solver;

};

#endif /* ELECTRON_FLOW_H_ */