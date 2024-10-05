/*
 * ngspice.cpp
 *
 *  Created on: 08.10.2024
 *      Author: marvi
 */

#include "electronflow.h"

using namespace ngspice;
using namespace electronflow;
using namespace std;

solver::solver() {
	if (!solver::nglspice.initNGLinkFull(solver::logout, [](){}, [this](pvecvaluesall d) { solver::dataReceive(d); }, [](pvecinfoall){})) {
		logout("failed to initialize nglink");
	}
}

solver::~solver() {
	if (solver::nglspice.isNGSpiceAttached())
		solver::nglspice.execCommand("quit");
}

bool solver::checkSPICE() {
	if (!solver::nglspice.isNGSpiceAttached() && !solver::nglspice.initNGSpice(solver::spicelibname))
		return false;
	return true;
}

bool solver::executeList() {
	logout(format("{} > run netlist commands", solver::netname));
	for (string command : solver::commands) {
		solver::execute(command);
	}
	return true;
}

bool solver::execute(string command) {
	logout(format("{} > run {}", solver::netname, command));
	return solver::nglspice.execCommand(command);
}
