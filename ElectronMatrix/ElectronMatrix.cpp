#include <iostream>
#include "netanalysis.h"
#include <fstream>
#include <string.h>
#include <tomsolver.hpp>

using namespace tomsolver;
using namespace netanalysis;

void testTomsolver() {
    /*
    Translate from Matlab code:

    root2d.m:
        function F = root2d(x)
            F(1) = exp(-exp(-(x(1)+x(2)))) - x(2)*(1+x(1)^2);
            F(2) = x(1)*cos(x(2)) + x(2)*sin(x(1)) - 0.5;
        end

    root2d_solve.m:
        format long
        fun = @root2d;
        x0 = [0,0];
        x = fsolve(fun,x0)

    result:
        x =

            0.353246561920553   0.606082026502285
     */

    // create equations
    SymVec f = {
        "exp(-exp(-(x1 + x2))) - x2 * (1 + x1 ^ 2) ^ 2"_f,
        "x1 * cos(x2) + x2 * sin(x1) - 0.5"_f,
    };

    try {

        auto v = "1+2*V[n1]"_f;

    	auto vs = Subs(v, VarsTable{{"V(n1)", 10}});

        printf("test2 %s -> %s = %s\n", v->ToString().c_str(), vs->ToString().c_str(), vs->Calc().ToString().c_str());
    } catch (...) {
    	printf("e\n");
    }

    // set initial value 0.0
    Config::Get().initialValue = 0.0;

    // solve, results to ans
    VarsTable ans = Solve(f);

    // print ans
    std::cout << ans << std::endl;

    // get variables' value
    std::cout << "x1 = " << ans["x1"] << std::endl;
    std::cout << "x2 = " << ans["x2"] << std::endl;
}

int main(int argc, char **argv) {

	setvbuf(stdout, NULL, _IONBF, 0);

	std::cout << "ElectronFlow 2.0 test" << std::endl;

    testTomsolver();

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
