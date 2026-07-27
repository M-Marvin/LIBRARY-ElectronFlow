/*
 * dataparsing.cpp
 *
 *  Created on: 08.10.2024
 *      Author: M_Marvin
 */

#include <string>
#include "electronflowimpl.h"

using namespace ngspice;
using namespace electronflow;
using namespace std;

void solver_impl::dataReceive(spice_pvecvaluesall data) {

	for (spice_pvecvalues* pvv = data->vecsa; pvv < data->vecsa + data->veccount; pvv++) {

		string name = string((*pvv)->name);
		double value = (*pvv)->creal;

		if (name == "time") {
			solver_impl::timepoint.push_back(value);
		} else if (!name.ends_with("#branch")) {
			if (name.starts_with("V(") && name.ends_with(""))
				name = name.substr(2, name.length() - 3);
			solver_impl::nodes[name].push_back(value);
		}

	}

}

void solver_impl::printFrame(ostream* out, int frame) {
	for (auto & [name, values] : solver_impl::nodes) {
		*out << format("{}\t{} V\n", name, values[frame]);
	}
}

void solver_impl::printdata(ostream* out) {
	if (solver_impl::timepoint.size() > 0) {
		for (size_t i = 0; i < solver_impl::timepoint.size(); i++) {
			*out << format("{} s\n", solver_impl::timepoint[i]);
			printFrame(out, i);
		}
	} else {
		printFrame(out, 0);
	}
}
