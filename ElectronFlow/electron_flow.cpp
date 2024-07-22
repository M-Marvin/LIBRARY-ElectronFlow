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

bool ElectronFlow::setProfile(double nodeCapacity, bool enableLimits, bool fixedTimestep) {
	printf("[>] set profile: node_cap = %1.16lf F source_limits = %s fixed_timestep = %s\n", nodeCapacity, enableLimits ? "true" : "false", fixedTimestep ? "true" : "false");

	if (ElectronFlow::solver == 0) {
		printf("no netlist loaded yet!\n");
		return false;
	}

	if (nodeCapacity <= 0) {
		printf("[!] node_cap must be larget than zero!\n");
		return false;
	}

	ElectronFlow::solver->profile.nodeCapacity = nodeCapacity;
	ElectronFlow::solver->profile.enableSourceLimits = enableLimits;
	ElectronFlow::solver->profile.fixedStepsize = fixedTimestep;
	return true;
}

bool ElectronFlow::stepSimulation(double timestep, double simulateTime) {
	printf("[>] run %1.16lf s at %1.16lf s steps ...\n", simulateTime, timestep);

	if (ElectronFlow::solver == 0) {
		printf("no netlist loaded yet!\n");
		return false;
	}

	// Prepare solver and print profile info
	ElectronFlow::solver->profile.initialStepsize = timestep;
	printf("node_capacity set to %1.16lf F\n", ElectronFlow::solver->profile.nodeCapacity);
	if (ElectronFlow::solver->profile.enableSourceLimits) printf("source limits enabled\n");
	if (ElectronFlow::solver->profile.fixedStepsize) {
		printf("fixed stepsize enabled, prioritize simulation speed\n");
	} else {
		printf("fixed stepsize disabled, prioritize simulation precision\n");
	}

	// Reset simulation variables (does not affect data of previous simulation)
	ElectronFlow::solver->init();

	// Run simulation
	while (ElectronFlow::solver->simtime < simulateTime) {
		if (!ElectronFlow::solver->step()) {
			printf("simulation interrupted!\n");
			return false;
		}
	}

	if (ElectronFlow::final_callback != 0)
		ElectronFlow::final_callback(
				ElectronFlow::circuit->nodes.data(), ElectronFlow::circuit->nodes.size(),
				ElectronFlow::circuit->elements.data(), ElectronFlow::circuit->elements.size(),
				ElectronFlow::solver->profile.nodeCapacity, ElectronFlow::solver->lastTimestep);

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
			ground = node->charge / ElectronFlow::solver->profile.nodeCapacity;
			break;
		}
	}

	for (NODE node : ElectronFlow::circuit->nodes) {
		double v = node->charge / ElectronFlow::solver->profile.nodeCapacity - ground;
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

	if (strcmp(argv[0], "profile") == 0) {
		if (argc < 4) {
			printf("[i] profile [node capacity F] [enable source limits true/false] [enable fixed timestep true/false]");
			return;
		}

		double nodeCapacity;
		if (!equations::strtonum(argv[1], &nodeCapacity)) {
			printf("invalid number format: %s\n", argv[2]);
			return;
		}
		bool enableLimits = strcmp(argv[2], "true") == 0;
		bool enableFixedStepsize = strcmp(argv[3], "true") == 0;

		setProfile(nodeCapacity, enableLimits, enableFixedStepsize);
	} else if (strcmp(argv[0], "reset") == 0) {
		resetSimulation();
	} else if (strcmp(argv[0], "step") == 0) {
		if (argc < 3) {
			printf("[i] step [initial timestep s] [simulate time s]\n");
			return;
		}

		double timestep;
		if (!equations::strtonum(argv[1], &timestep)) {
			printf("invalid number format: %s\n", argv[1]);
			return;
		}

		double simulateTime;
		if (!equations::strtonum(argv[2], &simulateTime)) {
			printf("invalid number format: %s\n", argv[2]);
			return;
		}

		stepSimulation(timestep, simulateTime);
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
