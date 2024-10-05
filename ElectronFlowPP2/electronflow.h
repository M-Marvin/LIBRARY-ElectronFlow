/*
 * electronflow.h
 *
 *  Created on: 06.10.2024
 *      Author: marvi
 */

#ifndef ELECTRONFLOW_H_
#define ELECTRONFLOW_H_

#include <nglink.h>
#include <vector>
#include "netanalysis.h"

#define DEFAULT_LIB_SPCIE_NAME "ngspice42_x64.dll"

using namespace std;
using namespace nglink;

namespace netanalysis {

class solver {

private:
	const char* libspicename = DEFAULT_LIB_SPCIE_NAME;
	NGLink nglspice;
	network network;
	vector<string> commands;

	void processData(pvecvaluesall values, int count);

public:
	solver();
	~solver();
	bool upload(const char* netlist);
	bool runNetlist();
	bool execute(const char* command);

};

}



#endif /* ELECTRONFLOW_H_ */
