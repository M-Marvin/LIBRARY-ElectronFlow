#pragma once

/*
The main methods and constants of the lib.
Contains declarations for callback, constants and methods.
Does not contain any JNI related stuff, this is handled by the
machine-generated de_m_marvin_nglink_NativeNGLink.h file!
*/

#include "ngspice.h"
#include <string>
#include <functional>
#include <windows.h>

using namespace std;

namespace ngspice {

/* Titles from the console output sources */
//#define NG_SPICE string("[Simmulator] ")
//#define NG_CONSOLE string("[Console] ")
//#define NG_LINK string("[Linker] ")

/* Callback functions from nglink */
// Called for printing log information
//typedef void(_stdcall *LogFunc)(const char*);
typedef std::function<void(string)> log_func;
// Called if ngspice wants to detache (MUST be executed (with detacheNGSpice) to prevent unwanted behavior)
//typedef void(_stdcall*DetacheFunc)(void);
typedef std::function<void(void)> detach_func;
// Called to output the simmulation data, contains the actual data after the simmulation has finished
//typedef void(_stdcall*ReciveVecDataFunc)(pvecvaluesall, int);
typedef std::function<void(pvecvaluesall)> receive_vec_data_func;
// Called before a new simmulation is started, contains information abbout the used vectors (name, size, count etc)
//typedef void(_stdcall*ReciveInitDataFunc)(pvecinfoall);
typedef std::function<void(pvecinfoall)> receive_init_data_func;

class nglink {

private:
	HMODULE ngSpiceModule = 0;
	bool initialised = false;
	// All callback functions provided from the application
	log_func logPrinterCallback;
	detach_func detachCallback;
	receive_vec_data_func reciveVecData;
	receive_init_data_func reciveInitData;
	// All methods exported from ngspice used by the lib
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
	// Checks if nglink ist inistialisted
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
	// Lists all aviable plots of the current spice instance
	vector<string> listPlots();
	// Gets the current active plot
	string getCurrentPlot();
	// Lists all aviable vectors of the given plot
	vector<string> listVecs(string plotName);
	// Get a specific vector info
	pvector_info getVecInfo(string vecName);

};

}
