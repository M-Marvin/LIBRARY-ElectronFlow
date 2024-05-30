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

ElectronFlow::ElectronFlow() {
	ElectronFlow::circuit = 0;
	ElectronFlow::solver = 0;
}

ElectronFlow::~ElectronFlow() {
	if (ElectronFlow::circuit != 0) delete ElectronFlow::circuit;
	ElectronFlow::circuit = 0;
	if (ElectronFlow::solver != 0) delete ElectronFlow::solver;
	ElectronFlow::solver = 0;
}

bool ElectronFlow::loadNetList(char* netList) {
	printf("[>] load net list\n");
	ElectronFlow::circuit = new CircuitContainer(netList);
	if (ElectronFlow::circuit->linkNodes()) {
		ElectronFlow::solver = new SourceSolver(ElectronFlow::circuit);
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

void ElectronFlow::stepSimulation(double nodeCapacity, double timestep, int steps) {
	printf("[>] run %d steps at %f s with %f F node capacity ...\n", steps, timestep, nodeCapacity);

	if (ElectronFlow::solver == 0) {
		printf("no netlist loaded yet!\n");
		return;
	}

	ElectronFlow::solver->nodeCapacity = nodeCapacity;
	for (int i = 0; i < steps; i++) {
		ElectronFlow::solver->step(timestep);
	}

	printf("done\n");

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
			printf("[i] step [timestep] [node capacity] [steps]\n");
			return;
		}

		double timestep = atof(argv[1]);
		double nodeCapacity = atof(argv[2]);
		int steps = atoi(argv[3]);

		stepSimulation(nodeCapacity, timestep, steps);
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
