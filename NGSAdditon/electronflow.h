
/*
This is a library wrapping NGSPICE and NGLink and adding additonal features to them.
*/

#ifndef ELECTRONFLOW_H_
#define ELECTRONFLOW_H_

#include <iostream>
#include <functional>
#include <string>
#include <print>

using namespace std;

namespace electronflow {

class solver_impl;
class solver {

private:
	solver_impl* impl;

public:
	solver(function<void(string)> logout = [](string s){ print(cout, "{}\n", s); });
	~solver();
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
