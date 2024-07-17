#include <stdio.h>
#include <string.h>
#include <fstream>
#include <iomanip>
#include "electron_flow.h"

using namespace std;
using namespace electronflow;

ofstream dataOut = 0;

// Data callback
void nodeDataCallback(double simtime, NODE* nodes, size_t nodec, Element** elements, size_t elementc, double nodecharge, double timestep) {
	if (dataOut.is_open()) {
		dataOut << "T\t" << simtime << endl;
		for (size_t i = 0; i < nodec; i++) {
			dataOut << "V\t" << nodes[i]->name <<"\t" << (nodes[i]->charge / nodecharge) << endl;
		}
		for (size_t i = 0; i < elementc; i++) {
			dataOut << "I\t" << elements[i]->name << "\t" << (elements[i]->cTnow / timestep) << endl;
		}
	}
}

// Data callback
void nodeDataCallbackFinal(NODE* nodes, size_t nodec, Element** elements, size_t elementc, double nodecharge, double timestep) {
	if (dataOut.is_open()) {
		dataOut << "== final data ==" << endl;
		for (size_t i = 0; i < nodec; i++) {
			dataOut << "V\t" << nodes[i]->name <<"\t" << (nodes[i]->charge / nodecharge) << endl;
		}
		for (size_t i = 0; i < elementc; i++) {
			dataOut << "I\t" << elements[i]->name << "\t" << (elements[i]->cTnow / timestep) << endl;
		}
	}
}

int main(int argc, const char **argv) {

	setvbuf(stdout, NULL, _IONBF, 0);

	if (argc == 1) {
		printf("\t-f\tinput file\n");
		printf("\t-o\toutput file\n");
		printf("\t-e\texecute netlist commands\n");
		printf("\t-h\trecord all data trough simulation\n");
		printf("\t-c\texecute simulation command\n");
		return 0;
	}

	const char* filePath = 0;
	const char* dataFilePath = 0;
	bool runFileCommand = false;
	bool recordNodeHistory = false;

	// Parse arguments
	for (int i = 0; i < argc; i++) {
		if (strcmp(argv[i], "-f") == 0) {
			filePath = argv[i + 1];
		} if (strcmp(argv[i], "-e") == 0) {
			runFileCommand = true;
		} if (strcmp(argv[i], "-h") == 0) {
			recordNodeHistory = true;
		} if (strcmp(argv[i], "-o") == 0) {
			dataFilePath = argv[i + 1];
		}
	}

	// Open output file
	if (dataFilePath != 0) {
		dataOut = ofstream(dataFilePath);
		dataOut << std::fixed << std::setprecision(10);

		if (!dataOut.is_open()) {
			printf("could not open output file!\n");
			return -1;
		}
	}

	// Initialize simulator
	ElectronFlow simulator = ElectronFlow();
	if (recordNodeHistory) {
		simulator.setCallbacks(&nodeDataCallback, &nodeDataCallbackFinal);
	} else {
		simulator.setCallbacks(0, &nodeDataCallbackFinal);
	}

	// Open, load and execute netlist file
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

	printf("run cli commands\n");

	// Run CLI commands
	const char** clicmd = 0;
	int cliargc = 0;
	for (int i = 0; i < argc; i++) {
		cliargc++;

		if (strcmp(argv[i], "-c") == 0 || i == argc - 1) {

			// Run command
			if (clicmd != 0) {
				simulator.controllCommand(cliargc, clicmd);
			}

			clicmd = argv + i + 1;
			cliargc = 0;
		}
	}

	dataOut.close();

	return 0;
}
