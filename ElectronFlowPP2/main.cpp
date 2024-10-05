/*
 * main.cpp
 *
 *  Created on: 06.10.2024
 *      Author: marvi
 */

#include "electronflow.h"
#include <fstream>
#include <iostream>

using namespace std;
using namespace netanalysis;

int main(int argc, const char** argv) {

	setvbuf(stdout, NULL, _IONBF, 0);

	printf("ElectnorFlow test 3.0");


	const char* rfile = "/../../../ElectronMatrix/test/zweitor.net";

	size_t len = strlen(argv[0]) + strlen(rfile) + 1;
	char* file = (char*) alloca(len);
	memset(file, 0, len);
	strcat(file, argv[0]);
	strcat(file, rfile);

	printf("file: %s\n", file);

	ifstream files = ifstream(file, ios::binary | ios::ate);

	if (!files.is_open()) printf("not found!\n");

	streampos end = files.tellg();
	files.seekg(0, ios::beg);
	size_t len2 = (size_t) (end - files.tellg());

	char* netlist = (char*) alloca(len + 1);
	memset(netlist, 0, len2 + 1);
	files.read(netlist, len2);

	files.close();

	solver slv = solver();

	slv.upload(netlist);
	slv.runNetlist();

	printf("\n\n\n");

	return 0;

}
