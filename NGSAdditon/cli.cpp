/*
 * cli.cpp
 *
 *  Created on: 09.10.2024
 *      Author: M_Marvin
 */

#include <iostream>
#include <fstream>
#include <print>
#include <vector>
#include <string>
#include "electronflowimpl.h"

using namespace std;
using namespace electronflow;

string inputFile;
string outputFile;
vector<string> commands;
bool silent = false;
bool noscript = false;

int main(int argn, const char** argv) {

	solver_impl slv;

	for (int i = 1; i < argn; i++) {
		string arg = string(argv[i]);

		if (arg == "-o" && i < argn - 1) {
			outputFile = string(argv[i + 1]);
			i++;
		} else if (arg == "-i" && i < argn - 1) {
			inputFile = string(argv[i + 1]);
			i++;
		} else if (arg == "-silent") {
			silent = true;
		} else if (arg == "-noscript") {
			noscript = true;
		} else {
			commands.push_back(arg);
		}
	}

	if (inputFile.empty()) {
		print("electronflow [-i *input file*] (-o *output file*) (-silent) (-noscript) *command1* ... *commandN*");
		return 0;
	}

	ifstream inputStream;
	inputStream.open(inputFile, ios::in);
	if (!inputStream.is_open()) {
		print("unable to open file: {}\n", inputFile);
		return -1;
	}
	if (!slv.upload(&inputStream)) {
		print("failed to upload netlist\n");
		return -1;
	}
	inputStream.close();

	if (!noscript) {
		if (!slv.executeList()) {
			print("failed to run script commands\n");
			return -1;
		}
	}

	for (string command : commands) {
		if (!slv.execute(command)) {
			print("failed to run command: {}\n", command);
			return -1;
		}
	}

	stringstream outputData;
	slv.printdata(&outputData);
	string outputStr = outputData.str();

	if (!outputFile.empty()) {

		ofstream outputStream;
		outputStream.open(outputFile);
		if (!outputStream.is_open()) {
			print("unable to open file: {}\n", outputFile);
			return -1;
		}
		outputStream.write(outputStr.c_str(), outputStr.length());
		outputStream.close();

	}

	if (!silent) {
		print("\nfiltered:\n{}\n", slv.netfiltered());
		print("\noutput data:\n{}\n", outputStr);
	}

	return 0;

}


