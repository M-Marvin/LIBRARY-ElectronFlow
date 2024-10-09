/*
 * electronflow.h
 *
 *  Created on: 08.10.2024
 *      Author: marvi
 */

#ifndef ELECTRONFLOW_H_
#define ELECTRONFLOW_H_

#include "nglink.h"
#include <iostream>
#include <sstream>
#include <functional>
#include <string>
#include <vector>
#include <map>
#include <print>

using namespace ngspice;
using namespace std;

namespace electronflow {

typedef struct component {
	string node_a;
	string node_b;
	vector<double> current;
} component_t;

class solver {

private:
	function<void(string)> logout;
	string netname;
	string filtered;
	vector<string> commands;
	vector<double> timepoint;
	map<string, vector<double>> nodes;
//	map<string, component_t> components;
	string spicelibname = "ngspice42_x64.dll";
	nglink nglspice;

	bool formatZweitor(string* line, stringstream* out);
	void dataReceive(pvecvaluesall data);
	bool checkSPICE();
	void printFrame(stringstream* out, int frame);

public:
	solver(function<void(string)> logout = [](string s){ print(cout, "{}\n", s); });
	~solver();
	void setlibname(string libname);
	void setlogger(function<void(string)> logout);
	bool upload(string netlist);
	void printdata(stringstream* out);
	string netfiltered();
	bool executeList();
	bool execute(string command);
	bool voltages();
	bool current();
	bool timeline();

};

}

#endif /* ELECTRONFLOW_H_ */
