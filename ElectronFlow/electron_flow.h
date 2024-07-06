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

	void setCallbacks(function<void(double, NODE*, size_t, double)> step_callback, function<void(NODE*, size_t, double)> final_callback);
	bool loadNetList(char* netList);
	bool loadAndRunNetList(char* netList);
	bool stepSimulation(double nodeCapacity, double timestep, double simulateTime);
	void printNodeVoltages(const char* refNodeName);
	void controllCommand(int argc, const char** argv);
	void printVersionInfo();

private:
	CircuitContainer* circuit;
	SourceSolver* solver;
	std::function<void(double, NODE*, size_t, double)> step_callback;
	std::function<void(NODE*, size_t, double)> final_callback;

};

}

#endif /* ELECTRON_FLOW_H_ */
