#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <vector>
#include "circuit_container.h"
#include "circuit_solver.h"

using namespace std;

int main(int argc, char **argv) {

	const char* test = "test circuit\n\n# Comment voltage divider\nV1 vcc gnd 230\nR1 vcc out 1k\nR2 out gnd 1k";
	char* d = (char*) malloc(strlen(test));
	strcpy(d, test);

	CircuitContainer c = CircuitContainer(d);

	for (int i = 0; i < c.elements.size(); i++) {
		printf("DEBUG element %s\n", c.elements[i]->name);
	}

	if (!c.linkNodes()) {
		printf("DEBUG failed to link!");
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
