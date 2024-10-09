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

solver::solver(function<void(string)> logout) {
	solver::logout = logout;

	if (!solver::nglspice.initNGLinkFull(solver::logout, [](){}, [this](pvecvaluesall d) { solver::dataReceive(d); }, [](pvecinfoall){})) {
		logout("\tfailed to initialize nglink");
	}
}

void solver::setlogger(function<void(string)> logout) {
	solver::logout = logout;
}

void solver::setlibname(string libname) {
	solver::spicelibname = libname;
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
	logout(format("\t{} > run netlist commands", solver::netname));
	for (string command : solver::commands) {
		solver::execute(command);
	}
	return true;
}

bool solver::execute(string command) {
	logout(format("\t{} > run {}", solver::netname, command));
	return solver::nglspice.execCommand(command);
}
