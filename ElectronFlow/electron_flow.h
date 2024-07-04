/*
 * electron_flow.h
 *
 *  Created on: 30.05.2024
 *      Author: marvi
 */

#ifndef ELECTRON_FLOW_H_
#define ELECTRON_FLOW_H_

#include "circuit_solver.h"

namespace electronflow {

class ElectronFlow {

public:
	ElectronFlow();
	~ElectronFlow();

	void setCallbacks(void (*step_callback) (double, NODE*, size_t, double), void (*final_callback) (NODE*, size_t, double));
	bool loadNetList(char* netList);
	bool loadAndRunNetList(char* netList);
	bool stepSimulation(double nodeCapacity, double timestep, double simulateTime);
	void printNodeVoltages(const char* refNodeName);
	void controllCommand(int argc, char** argv);

private:
	CircuitContainer* circuit;
	SourceSolver* solver;
	void (*final_callback) (NODE*, size_t, double);
	void (*step_callback) (double, NODE*, size_t, double);

};

}

#endif /* ELECTRON_FLOW_H_ */
