#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <vector>
#include <fstream>
#include "circuit_container.h"
#include "circuit_solver.h"

using namespace std;

int main(int argc, char **argv) {

	string path(argv[0]);
	string file("\\..\\..\\..\\test\\test_2p.txt");

	ifstream netListFile(path + file);

	if (!netListFile.is_open()) {
		printf("could not open test.txt %s\n", (path + file).c_str());
		return -2;
	}

	netListFile.seekg(0, ios::end);
	size_t length = netListFile.tellg();
	netListFile.seekg(0, ios::beg);
	char netList[length];
	netListFile.read(netList, length);
	netListFile.close();

	printf("loaded net list:\n%s\n", netList);

	CircuitContainer c = CircuitContainer(netList);

	for (int i = 0; i < c.elements.size(); i++) {
		printf("DEBUG element %s\n", c.elements[i]->name);
	}

	if (!c.linkNodes()) {
		printf("DEBUG failed to link!\n");
		return -1;
	}

	if (c.elements.size() == 0) {
		printf("DEBUG no elements\n");
		return -1;
	}

	Element* e = c.elements[0];
	SourceSolver solver = SourceSolver(&c, e);

	if (!solver.compileStructure()) {
		printf("DEBUG error struct!\n");
		return -1;
	}

	return 0;
}
