/*
 * electron_flow.h
 *
 *  Created on: 30.05.2024
 *      Author: Marvin K.
 */

#ifndef ELECTRON_FLOW_H_
#define ELECTRON_FLOW_H_

#include "circuit_solver.h"

namespace electronflow {

class ElectronFlow {

public:
	ElectronFlow();
	~ElectronFlow();

	void setCallbacks(function<void(double, NODE*, size_t, Element**, size_t, double, double)> step_callback, function<void(NODE*, size_t, Element**, size_t, double, double)> final_callback);
	bool loadNetList(char* netList);
	bool loadAndRunNetList(char* netList);
	void resetSimulation();
	bool setProfile(double nodeCapacity, bool enableLimits, bool fixedTimestep);
	bool stepSimulation(double timestep, double simulateTime);
	void printNodeVoltages(const char* refNodeName);
	void printElementCurrents();
	void controllCommand(int argc, const char** argv);
	void printVersionInfo();

private:
	CircuitContainer* circuit;
	SourceSolver* solver;
	double lastTimestep;
	std::function<void(double, NODE*, size_t, Element**, size_t, double, double)> step_callback;
	std::function<void(NODE*, size_t, Element**, size_t, double, double)> final_callback;

};

}

#endif /* ELECTRON_FLOW_H_ */
