/*
 * parser.cpp
 *
 *  Created on: 08.10.2024
 *      Author: marvi
 */

#include "electronflow.h"
#include <iostream>
#include <sstream>

using namespace ngspice;
using namespace std;
using namespace electronflow;

bool solver::upload(string netlist) {

	if (!solver::checkSPICE()) {
		logout("\tfailed to load spice library");
		return false;
	}

	solver::netname.clear();
	solver::timepoint.clear();
	solver::nodes.clear();
//	solver::components.clear();

	stringstream netstream(netlist);
	stringstream netout;
	string line;

	while (getline(netstream, line)) {
		line.erase(remove(line.begin(), line.end(), '\r'), line.end());

		if (solver::netname.length() == 0) {
			solver::netname = line;
			netout << line << "\n";

			logout(format("\t{} > parse components ...", solver::netname));
			continue;
		}

		if (line[0] == '*')
			continue;

		if (line[0] == '.') {
			logout(format("control command: {}", line));
			line.erase(line.begin());
			if (line.rfind("end", 0) != 0) solver::commands.push_back(line);
			continue;
		}

		if (line.length() <= 3)
			continue;

		if (line.rfind("ZT", 0) == 0) {
			logout(format("zweitor component: {}", line));
			if (!formatZweitor(&line, &netout))
				return false;
			continue;
		}

		logout(format("network component: {}", line));
		netout << line << "\n";

	}

	solver::filtered = netout.str();
	logout(format("\t{} > upload to SPICE ...", solver::netname));

	if (!solver::nglspice.loadCircuit(solver::filtered)) {
		logout("failed to upload netlist");
		return false;
	}

	logout(format("\t{} > upload complete, initialized and ready for execution", solver::netname));
	return true;
}

string solver::netfiltered() {
	return solver::filtered;
}
