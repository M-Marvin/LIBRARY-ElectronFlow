/*
 * electron_flow.cpp
 *
 *  Created on: 30.05.2024
 *      Author: marvi
 */

#include <stdio.h>
#include "electron_flow.h"
#include <string.h>
#include <stdlib.h>

using namespace electronflow;

ElectronFlow::ElectronFlow() {
	ElectronFlow::circuit = 0;
	ElectronFlow::solver = 0;
	ElectronFlow::final_callback = 0;
	ElectronFlow::step_callback = 0;
}

ElectronFlow::~ElectronFlow() {
	if (ElectronFlow::circuit != 0) delete ElectronFlow::circuit;
	ElectronFlow::circuit = 0;
	if (ElectronFlow::solver != 0) delete ElectronFlow::solver;
	ElectronFlow::solver = 0;
}

void ElectronFlow::setCallbacks(void (*step_callback) (double, NODE*, size_t, double), void (*final_callback) (NODE*, size_t, double)) {
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
		vector<char*> args = vector<char*>();
		char* arg = strtok(line, " ");
		do {
			args.push_back(arg);
		} while ((arg = strtok(NULL, " ")) != 0);

		controllCommand(args.size(), args.data());

	}
	return true;
}

bool ElectronFlow::stepSimulation(double nodeCapacity, double timestep, double simulateTime) {
	printf("[>] run %f s at %f s with %f F node capacity ...\n", simulateTime, timestep, nodeCapacity);

	if (ElectronFlow::solver == 0) {
		printf("no netlist loaded yet!\n");
		return false;
	}

	ElectronFlow::solver->nodeCapacity = nodeCapacity;
	ElectronFlow::solver->reset();
	while (ElectronFlow::solver->simtime < simulateTime) {
		if (!ElectronFlow::solver->step(&timestep)) {
			printf("simulation interrupted!\n");
			return false;
		}
	}

	if (ElectronFlow::final_callback != 0) ElectronFlow::final_callback(ElectronFlow::circuit->nodes.data(), ElectronFlow::circuit->nodes.size(), ElectronFlow::solver->nodeCapacity);

	printf("done\n");
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

void ElectronFlow::controllCommand(int argc, char** argv) {
	if (argc == 0) return;

	printf("[>] run %s\n", argv[0]);

	if (strcmp(argv[0], "step") == 0) {
		if (argc < 4) {
			printf("[i] step [initial timestep s] [node capacity F] [simulate time s]\n");
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

		stepSimulation(nodeCapacity, timestep, simulateTime);
	} else if (strcmp(argv[0], "printv") == 0) {
		if (argc < 2) {
			printf("[i] printv [ref node]\n");
			return;
		}

		char* refNodeName = argv[1];

		printNodeVoltages(refNodeName);
	} else {
		printf("[!] unknown command '%s'\n", argv[0]);
	}

}
