/*
 * ngspice.cpp
 *
 *  Created on: 08.10.2024
 *      Author: M_Marvin
 */

#include "electronflowimpl.h"

using namespace ngspice;
using namespace electronflow;
using namespace std;

#ifndef EF_VERSION
#define EF_VERSION "[test-build]"
#endif

#ifndef DEF_NGSPICE
#define DEF_NGSPICE "undefined.lib"
#endif

solver_impl::solver_impl(function<void(string)> logout) {
	solver_impl::logout = logout;
	solver_impl::setlibname(DEF_NGSPICE);

	if (!solver_impl::nglspice.initNGLinkFull([this](string s) { solver_impl::logout(s); }, [](){}, [this](spice_pvecvaluesall d) { solver_impl::dataReceive(d); }, [](spice_pvecinfoall){})) {
		logout("\tfailed to initialize nglink");
	}
}

void solver_impl::setlogger(function<void(string)> logout) {
	solver_impl::logout = logout;
}

void solver_impl::setlibname(string libname) {
	solver_impl::spicelibname = libname;

	// print version info
	string spiceName = solver_impl::spicelibname.substr(solver_impl::spicelibname.find_last_of("\\/") + 1);
	spiceName = spiceName.substr(0, spiceName.rfind("."));
	solver_impl::logout(format("######################################################################"));
	solver_impl::logout(format("\telectron flow ver{} + SPICE {}", EF_VERSION, spiceName));
	solver_impl::logout(format("######################################################################"));
}

solver_impl::~solver_impl() {
	if (solver_impl::nglspice.isNGSpiceAttached())
		solver_impl::nglspice.execCommand("quit");
}

bool solver_impl::checkSPICE() {
	if (!solver_impl::nglspice.isNGSpiceAttached() && !solver_impl::nglspice.initNGSpice(solver_impl::spicelibname))
		return false;
	return true;
}

bool solver_impl::executeList() {
	logout(format("\t{} > run netlist commands", solver_impl::netname));
	for (string command : solver_impl::commands) {
		solver_impl::execute(command);
	}
	return true;
}

bool solver_impl::execute(string command) {
	logout(format("\t{} > run {}", solver_impl::netname, command));
	if (solver_impl::components == 0 && command != "quit") {
		logout(format("\t{} > no components, skip command", solver_impl::netname));
		return true;
	}
	return solver_impl::nglspice.execCommand(command);
}
