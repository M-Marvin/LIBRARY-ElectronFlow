/*
 * parser.cpp
 *
 *  Created on: 08.10.2024
 *      Author: M_Marvin
 */

#include <iostream>
#include <sstream>
#include "electronflowimpl.h"

using namespace ngspice;
using namespace std;
using namespace electronflow;

bool solver_impl::upload(istream* in) {

	if (!solver_impl::checkSPICE()) {
		logout("\tfailed to load spice library");
		return false;
	}

	solver_impl::netname.clear();
	solver_impl::timepoint.clear();
	solver_impl::nodes.clear();
	solver_impl::components = 0;

	stringstream netout;
	string line;

	while (getline(*in, line)) {
		line.erase(remove(line.begin(), line.end(), '\r'), line.end());

		if (solver_impl::netname.length() == 0) {
			solver_impl::netname = line;
			netout << line << "\n";

			logout(format("\t{} > parse components ...", solver_impl::netname));
			continue;
		}

		if (line[0] == '*')
			continue;

		if (line[0] == '.') {
			logout(format("control command: {}", line));
			line.erase(line.begin());
			if (line.rfind("end", 0) != 0) solver_impl::commands.push_back(line);
			continue;
		}

		if (line.length() <= 3)
			continue;

		if (line.rfind("ZT", 0) == 0) {
			logout(format("zweitor component: {}", line));
			if (!formatZweitor(&line, &netout))
				return false;
			solver_impl::components++;
			continue;
		}

		logout(format("network component: {}", line));
		solver_impl::components++;
		netout << line << "\n";

	}

	solver_impl::filtered = netout.str();
	logout(format("\t{} > upload to SPICE ...", solver_impl::netname));

	if (!solver_impl::nglspice.loadCircuit(solver_impl::filtered)) {
		logout("failed to upload netlist");
		return false;
	}

	logout(format("\t{} > upload complete, initialized and ready for execution", solver_impl::netname));
	return true;
}

string solver_impl::netfiltered() {
	return solver_impl::filtered;
}
