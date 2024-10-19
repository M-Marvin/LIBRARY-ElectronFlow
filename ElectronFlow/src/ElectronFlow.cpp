//============================================================================
// Name        : ElectronFlow.cpp
// Author      : 
// Version     :
// Copyright   : Your copyright notice
// Description : Hello World in C++, Ansi-style
//============================================================================

#include <iostream>
#include <fstream>
#include "netanalysis.h"
#include "equations.h"

using namespace netanalysis;
using namespace std;

int main(int argc, const char** argv) {

	setbuf(stdout, NULL);

	cout << "!!!Hello World!!!" << endl; // prints !!!Hello World!!!

	string file = string(argv[1]);
	cout << "File: " << file << "\n";
	ifstream netfile(file);
	if (!netfile.is_open())
		return -1;

	network net;
	net.parse(netfile);
	netfile.close();


	matrixset ms;
	net.matrix(ms);

//
//	equation a = "1+2"_f;
//	equation b = "23"_f;
//	equation c = "5*V(A,B)=V(A,B)"_f;
//
//	cout << a.str() << "|\n" << b.str() << "|\n" << c.str() << "|\n";
//
//	equation sub = "("_f + a + "-"_f + b + ")"_f;
//
//	cout << sub.str() << "\n";
//
//	c.substitute("V(A,B)"_f, sub);
//
//	cout << c.str() << "\n";

	flush(cout);

	return 0;
}
