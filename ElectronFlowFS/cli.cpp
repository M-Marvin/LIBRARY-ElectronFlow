/*
 * cli.cpp
 *
 *  Created on: 09.10.2024
 *      Author: marvi
 */

#include "electronflow.h"
#include <iostream>
#include <fstream>
#include <print>
#include <vector>
#include <string>

using namespace std;
using namespace electronflow;

string inputFile;
string outputFile;
vector<string> commands;
bool silent = false;
bool noscript = false;

int main(int argn, const char** argv) {

	print("\n\t\telectron flow ver3.0 - test build\n\n");

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

	solver slv;

	ifstream inputStream;
	inputStream.open(inputFile, ios::ate);
	if (!inputStream.is_open()) {
		print("unable to open file: {}\n", inputFile);
		return -1;
	}
	streampos end = inputStream.tellg();
	inputStream.seekg(0, ios::beg);
	size_t length = (size_t) (end - inputStream.tellg());
	char* buffer = (char*) alloca(length + 1);
	memset(buffer, 0, length + 1);
	inputStream.read(buffer, length);
	inputStream.close();

	if (!slv.upload(string(buffer))) {
		print("failed to upload netlist\n");
		return -1;
	}

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


