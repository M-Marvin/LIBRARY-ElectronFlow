#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <vector>
#include <fstream>
#include "electron_flow.h"

using namespace std;

int main(int argc, char **argv) {

	char* filePath = 0;
	char** controllCommand = 0;
	int controllCommandN = 0;
	bool runFileCommand = false;

	// Parse arguments
	for (int i = 0; i < argc; i++) {
		if (strcmp(argv[i], "-f") == 0) {
			filePath = argv[i + 1];
		} else if (strcmp(argv[i], "-r") == 0) {
			controllCommand = argv + i + 1;
			controllCommandN = argc - i - 1;
		} else if (strcmp(argv[i], "-e") == 0) {
			runFileCommand = true;
		}
	}

	ElectronFlow simulator = ElectronFlow();

	if (filePath != 0) {

		printf("input file: '%s'\n", filePath);

		ifstream netListFile(filePath);

		if (!netListFile.is_open()) {
			printf("could not open file!\n");
			return -1;
		}

		netListFile.seekg(0, ios::end);
		streampos length = netListFile.tellg();
		netListFile.seekg(0, ios::beg);
		char netList[(size_t) length + 1];
		memset(netList, 0, sizeof(netList));
		netListFile.read(netList, length);
		netListFile.close();

		if (runFileCommand) {
			if (!simulator.loadAndRunNetList(netList)) {
				printf("failed to load and run file!\n");
				return -1;
			}
		} else {
			if (!simulator.loadNetList(netList)) {
				printf("failed to load file!\n");
				return -1;
			}
		}

	}

	if (controllCommand != 0) {

		printf("run cli command\n");

		simulator.controllCommand(controllCommandN, controllCommand);

	}

	return 0;
}
