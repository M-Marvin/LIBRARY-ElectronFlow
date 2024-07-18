/*
 * electron_flow.cpp
 *
 *  Created on: 30.05.2024
 *      Author: Marvin K.
 */

#include "electron_flow.h"

#include <corecrt.h>
#include <stdio.h>
#include <string.h>
#include <functional>
#include <vector>

#include "circuit.h"
#include "circuit_container.h"
#include "equations.h"

using namespace electronflow;
using namespace std;

ElectronFlow::ElectronFlow() {
	ElectronFlow::circuit = 0;
	ElectronFlow::solver = 0;
	ElectronFlow::final_callback = 0;
	ElectronFlow::step_callback = 0;
	ElectronFlow::lastTimestep = 0;
}

ElectronFlow::~ElectronFlow() {
	if (ElectronFlow::circuit != 0) delete ElectronFlow::circuit;
	ElectronFlow::circuit = 0;
	if (ElectronFlow::solver != 0) delete ElectronFlow::solver;
	ElectronFlow::solver = 0;
}

void ElectronFlow::printVersionInfo() {
	printf("===============\n");
	printf(" electron flow\n");
	printf(" v%s\n", VERSION_STR);
	printf("===============\n");
}

void ElectronFlow::setCallbacks(function<void(double, NODE*, size_t, Element**, size_t, double, double)> step_callback, function<void(NODE*, size_t, Element**, size_t, double, double)> final_callback) {
	ElectronFlow::step_callback = step_callback;
	ElectronFlow::final_callback = final_callback;
}

bool ElectronFlow::loadNetList(char* netList) {
	printf("[>] load net list\n");
	ElectronFlow::circuit = new CircuitContainer();
	if (ElectronFlow::circuit->parseCircuit(netList) && ElectronFlow::circuit->linkNodes()) {
		ElectronFlow::solver = new SourceSolver(ElectronFlow::circuit);
		ElectronFlow::solver->setCallback(step_callback);
		return true;
	}
	return false;
}

bool ElectronFlow::loadAndRunNetList(char* netList) {
	if (!loadNetList(netList)) return false;
	printf("[>] run net list\n");

	for (char* line : ElectronFlow::circuit->commands) {

		// Parse command
		vector<const char*> args = vector<const char*>();
		const char* arg = strtok(line, " ");
		do {
			args.push_back(arg);
		} while ((arg = strtok(NULL, " ")) != 0);

		controllCommand(args.size(), args.data());

	}
	return true;
}

void ElectronFlow::resetSimulation() {
	printf("[>] reset simulation data\n");
	ElectronFlow::solver->reset();
}

bool ElectronFlow::stepSimulation(double nodeCapacity, double timestep, double simulateTime, bool enableLimits) {
	printf("[>] run %1.16lf s at %1.16lf s with %1.16lf F node capacity ...\n", simulateTime, timestep, nodeCapacity);

	if (ElectronFlow::solver == 0) {
		printf("no netlist loaded yet!\n");
		return false;
	}

	// Configure solver
	ElectronFlow::solver->nodeCapacity = nodeCapacity;
	if (enableLimits) printf("source limits enabled\n");
	ElectronFlow::solver->enableLimits = enableLimits;

	// Reset simtime
	ElectronFlow::solver->simtime = 0;
	ElectronFlow::solver->lastCtChange = 0;

	// Run simulation
	while (ElectronFlow::solver->simtime < simulateTime) {
		if (!ElectronFlow::solver->step(&timestep)) {
			printf("simulation interrupted!\n");
			return false;
		}
	}

	if (ElectronFlow::final_callback != 0)
		ElectronFlow::final_callback(
				ElectronFlow::circuit->nodes.data(), ElectronFlow::circuit->nodes.size(),
				ElectronFlow::circuit->elements.data(), ElectronFlow::circuit->elements.size(),
				ElectronFlow::solver->nodeCapacity, timestep);

	printf("done\n");
	ElectronFlow::lastTimestep = timestep;
	return true;
}

void ElectronFlow::printNodeVoltages(const char* refNodeName) {
	printf("[>] print node voltages\n");

	if (ElectronFlow::circuit == 0) {
		printf("no netlist loaded yet!\n");
		return;
	}

	double ground = 0;
	for (NODE node : ElectronFlow::circuit->nodes) {
		if (strcmp(node->name, refNodeName) == 0) {
			ground = node->charge / ElectronFlow::solver->nodeCapacity;
			break;
		}
	}

	for (NODE node : ElectronFlow::circuit->nodes) {
		double v = node->charge / ElectronFlow::solver->nodeCapacity - ground;
		printf("node %s v: %f\n", node->name, v);
	}
}

void ElectronFlow::printElementCurrents() {
	printf("[>] print element currents\n");

	if (ElectronFlow::circuit == 0) {
		printf("no netlist loaded yet!\n");
		return;
	}

	for (Element* element : ElectronFlow::circuit->elements) {
		double i = element->cTnow / ElectronFlow::lastTimestep;
		printf("element %s i: %f\n", element->name, i);
	}
}

void ElectronFlow::controllCommand(int argc, const char** argv) {
	if (argc == 0) return;

	printf("[>] run %s\n", argv[0]);

	if (strcmp(argv[0], "reset") == 0) {
		resetSimulation();
	} else if (strcmp(argv[0], "step") == 0) {
		if (argc < 4) {
			printf("[i] step [initial timestep s] [node capacity F] [simulate time s] (do reset true/false)\n");
			return;
		}

		double timestep;
		if (!equations::strtonum(argv[1], &timestep)) {
			printf("invalid number format: %s\n", argv[1]);
			return;
		}
		double nodeCapacity;
		if (!equations::strtonum(argv[2], &nodeCapacity)) {
			printf("invalid number format: %s\n", argv[2]);
			return;
		}
		double simulateTime;
		if (!equations::strtonum(argv[3], &simulateTime)) {
			printf("invalid number format: %s\n", argv[2]);
			return;
		}
		bool enableLimits = argc < 5 ? false : strcmp(argv[4], "true") == 0;

		stepSimulation(nodeCapacity, timestep, simulateTime, enableLimits);
	} else if (strcmp(argv[0], "printv") == 0) {
		if (argc < 2) {
			printf("[i] printv [ref node]\n");
			return;
		}

		const char* refNodeName = argv[1];

		printNodeVoltages(refNodeName);
	} else if (strcmp(argv[0], "printi") == 0) {
		printElementCurrents();
	} else {
		printf("[!] unknown command '%s'\n", argv[0]);
	}
}
