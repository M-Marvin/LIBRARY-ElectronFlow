/*
 * electronflow.cpp
 *
 *  Created on: 06.10.2024
 *      Author: marvi
 */
#include "electronflow.h"
#include "netanalysis.h"
#include "zweitor.h"

using namespace netanalysis;
using namespace nglink;

solver::solver() {
	if (!solver::nglspice.initNGLinkFull([](string s) { printf("%s\n", s); }, [](){}, [this](pvecvaluesall v, int n) { this->processData(v, n); }, NULL)) {
		printf("failed to initialize nglink\n");
	}

	if (!solver::nglspice.initNGSpice(solver::libspicename)) {
		printf("failed to load spice: %s\n", solver::libspicename);
	}
}

solver::~solver() {
	if (solver::nglspice.isNGSpiceAttached())
		solver::nglspice.execCommand("quit");
}

bool solver::runNetlist() {
	printf("%s > execute command queue from netlist ...\n", solver::network.name);
	for (string command : this->commands) {
		if (!solver::execute(command.c_str())) return false;
	}
	this->commands.clear();
	return false;
}

bool solver::execute(const char* command) {
	printf("%s > execute command %s\n", solver::network.name, command);
	if (!solver::nglspice.execCommand(command)) {
		return false;
	}
	return true;
}
