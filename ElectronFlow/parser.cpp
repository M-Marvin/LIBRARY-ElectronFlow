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
		logout("failed to load spice library");
		return false;
	}

	solver::netname.clear();
	solver::nodes.clear();
	solver::components.clear();

	stringstream netstream(netlist);
	stringstream netout;
	string line;

	while (getline(netstream, line)) {
		line.erase(remove(line.begin(), line.end(), '\r'), line.end());

		if (solver::netname.length() == 0) {
			solver::netname = line;
			netout << line << "\n";

			logout(format("{} > parse components ...", solver::netname));
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

	// TODO debug print
	print(cout, "filtered:\n{}\n", netout.str());

	logout(format("{} > upload to SPICE ...", solver::netname));

	if (!solver::nglspice.loadCircuit(netout.str())) {
		logout("failed to upload netlist");
		return false;
	}

	logout(format("{} > upload complete, initialized and ready for execution", solver::netname));
	return true;
}


