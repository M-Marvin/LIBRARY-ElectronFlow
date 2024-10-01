#include <iostream>
#include "netanalysis.h"
#include <fstream>
#include <string.h>

using namespace netanalysis;

int main(int argc, char **argv) {

	setvbuf(stdout, NULL, _IONBF, 0);

	std::cout << "ElectronFlow 2.0 test" << std::endl;

	const char* rfile = "/../../../test/circuit_0b.net";
	char file[strlen(argv[0]) + strlen(rfile) + 1] = {0};
	strcat(file, argv[0]);
	strcat(file, rfile);

	printf("file: %s\n", file);

	ifstream files = ifstream(file, ios::binary | ios::ate);

	if (!files.is_open()) printf("not found!\n");

	streampos end = files.tellg();
	files.seekg(0, ios::beg);
	size_t len = (size_t) (end - files.tellg());

	char netlist[len + 1] = {0};
	files.read(netlist, len);

	files.close();

	network_t network;

	for (size_t i = 0; i < len; i++) {
		if (netlist[i] == '\r') netlist[i] = '\n';
	}

	if (parseNetlist(netlist, &network) != 0) {
		printf("failed to parse netlist\n");
		return -1;
	}

	if (findBranches(&network) != 0) {
		printf("failed to apply mesh algorithm\n");
		return -1;
	}



}
