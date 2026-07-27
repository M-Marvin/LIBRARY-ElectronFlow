
/*
Contains the declaration of the actual implementation functions for electronflow
Including private data types and internal variables
THIS SHOULD NOT BE INCLUDED IN PROJECTS USING ELECTRONFLOW!
*/

#ifndef ELECTRONFLOWIMPL_H_
#define ELECTRONFLOWIMPL_H_

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

class solver_impl {

private:
	function<void(string)> logout;
	string netname;
	string filtered;
	unsigned int components;
	vector<string> commands;
	vector<double> timepoint;
	map<string, vector<double>> nodes;
	string spicelibname;
	nglink nglspice;

	bool formatZweitor(string* line, ostream* out);
	void dataReceive(spice_pvecvaluesall data);
	bool checkSPICE();
	void printFrame(ostream* out, int frame);

public:
	solver_impl(function<void(string)> logout = [](string s){ print(cout, "{}\n", s); });
	~solver_impl();
	void setlibname(string libname);
	void setlogger(function<void(string)> logout);
	bool upload(istream* in);
	bool executeList();
	bool execute(string command);
	void printdata(ostream* out);
	string netfiltered();

};

}

#endif /* ELECTRONFLOWIMPL_H_ */
