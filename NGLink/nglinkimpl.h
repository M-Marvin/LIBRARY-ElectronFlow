#pragma once

/*
Contains the declaration of the actual implementation functions for nglink
Including private data types and internal variables
THIS SHOULD NOT BE INCLUDED IN PROJECTS USING NGLINK!
*/

#include <windows.h>
#include "nglink.h"
#include <ngspice.h>
#include <string>
#include <functional>

using namespace std;

namespace ngspice {

class nglink_impl {

private:
	bool initialised = false;
	// All callback functions provided from the application
	log_func logPrinterCallback;
	detach_func detachCallback;
	receive_vec_data_func reciveVecData;
	receive_init_data_func reciveInitData;
	// Internal implementation variables
	HMODULE ngSpiceModule = 0;
	ngSpice_Init ngSpiceInit;
	ngSpice_Command ngSpiceCommand;
	ngSpice_Circ ngSpiceCirc;
	ngSpice_running ngSpiceRunning;
	ngGet_Vec_Info ngSpiceGetVectorInfo;
	ngSpice_CurPlot ngSpiceGetCurPlot;
	ngSpice_AllPlots ngSpiceListPlots;
	ngSpice_AllVecs ngSpiceListVectors;

public:
	void logPrinter(std::string log);
	bool checkNGState();

	// Initialise NGLink with all callback functions
	bool initNGLink(log_func printFunc, detach_func detacheFunc);
	bool initNGLinkFull(log_func printFunc, detach_func detacheFunc, receive_vec_data_func reciveVecFunc, receive_init_data_func reciveInitFunc);
	// Checks if nglink ist initialized
	bool isInitialisated();
	// Try to load and attach the ngspice lib
	bool initNGSpice(string libName);
	// Checks if ngspice is attached
	bool isNGSpiceAttached();
	// Detaches the ngspice lib
	bool detachNGSpice();
	// Executes a standard spice command
	bool execCommand(string command);
	// Executes a list of commands as if loaded as file
	bool loadCircuit(string circListString);
	// Checks if spice is running in its background thread
	bool isRunning();
	// Lists all available plots of the current spice instance
	vector<string> listPlots();
	// Gets the current active plot
	string getCurrentPlot();
	// Lists all available vectors of the given plot
	vector<string> listVecs(string plotName);

};

}
