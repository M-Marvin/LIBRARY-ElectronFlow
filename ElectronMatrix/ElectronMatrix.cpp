#include <iostream>
#include "netanalysis.h"
#include <fstream>

using namespace netanalysis;

int main(int argc, char **argv) {

	std::cout << "Hello World2" << std::endl;

	const char* file = "E:/GitHub/LIBRARY-ElectronFlow/ElectronMatrix/test/circuit_0b.net";

	ifstream files = ifstream(file, ios::binary | ios::ate);

	streampos end = files.tellg();
	files.seekg(0, ios::beg);
	size_t len = (size_t) (end - files.tellg());

	char netlist[len] = {0};
	files.read(netlist, len);

	files.close();

	network_t network;

	parseNetlist(netlist, len, &network);

	findBranches(&network);

}
