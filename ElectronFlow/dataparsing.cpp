/*
 * dataparsing.cpp
 *
 *  Created on: 08.10.2024
 *      Author: marvi
 */

#include "electronflow.h"
#include <string>

using namespace ngspice;
using namespace electronflow;
using namespace std;

void solver::dataReceive(pvecvaluesall data) {

	for (pvecvalues* pvv = data->vecsa; pvv < data->vecsa + data->veccount; pvv++) {

		string name = string((*pvv)->name);
		double value = (*pvv)->creal;

		if (name == "time") {
			solver::timepoint.push_back(value);
		} else if (!name.ends_with("#branch")) {
			if (name.starts_with("V(") && name.ends_with(""))
				name = name.substr(2, name.length() - 3);
			solver::nodes[name].push_back(value);
		}

	}

}

void solver::printFrame(stringstream* out, int frame) {
	for (auto & [name, values] : solver::nodes) {
		*out << format("{}\t{} V\n", name, values[frame]);
	}
}

void solver::printdata(stringstream* out) {
	if (solver::timepoint.size() > 0) {
		for (size_t i = 0; i < solver::timepoint.size(); i++) {
			*out << format("{} s\n", solver::timepoint[i]);
			printFrame(out, i);
		}
	} else {
		printFrame(out, 0);
	}
}
