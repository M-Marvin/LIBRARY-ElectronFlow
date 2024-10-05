
/*
This is a library to simplify the use of ngspice.dll
This library does mainly 3 thing:
[A] - Simplify instantiating ngspice and handling of its callback's
[B] - Make it compatible with java via a JNI interface
[C] - !WORK IN PROGRESS! Make it possible to bind a single file to ngspice and auto detect changes to cause a auto-reload
*/

#include "nglink.h"
#include <iostream>
#include <functional>
#include <format>
#include <string>
#include <vector>
#include <print>

using namespace std;
using namespace ngspice;

/* Helper function for handling log prints */
void nglink::logPrinter(string log)
{
	if (logPrinterCallback != NULL)
	{
		logPrinterCallback(log);
	}
	else
	{
		print(std::cout, "[>] {}", log);
	}
}

/* Helper function checks if nglinker and ngspice are initialized */
bool nglink::checkNGState() {
	if (!initialised)
	{
		logPrinter("nglinker is not initialised, use initNGLink first!");
		return false;
	}

	if (ngSpiceModule != 0)
	{
		return true;
	}
	else
	{
		logPrinter("ngspice is not attached!");
		return false;
	}
}

/* Initialize NGLink with all callback functions */
bool nglink::initNGLink(log_func printFunc, detach_func detacheFunc)
{
	return initNGLinkFull(printFunc, detacheFunc, NULL, NULL);
}
bool nglink::initNGLinkFull(log_func printFunc, detach_func detacheFunc, receive_vec_data_func reciveVecFunc, receive_init_data_func reciveInitFunc)
{
	detachCallback = detacheFunc;
	logPrinterCallback = printFunc;
	reciveVecData = reciveVecFunc;
	reciveInitData = reciveInitFunc;

	initialised = logPrinterCallback != NULL && detachCallback != NULL;
	if (initialised)
	{
		logPrinter("nglink initialized!");
		return 1;
	}
	else
	{
		logPrinter("failed to initialize nglink, logPrinter and detach callback's are null!");
		return 0;
	}
}


/* Checks if nglink is initialized */
bool nglink::isInitialisated() {
	return initialised;
}

/* Try to load and attach the ngspice lib */
bool nglink::initNGSpice(string libName)
{
	if (!initialised)
	{
		logPrinter("nglinker is not initialized, use initNGLink first!");
		return -1;
	}

	if (ngSpiceModule == 0)
	{

		ngSpiceModule = LoadLibraryA(libName.c_str());

		if (ngSpiceModule == 0)
		{
			logPrinter(format("failed to load ngspice dll {}!", libName));
			return 0;
		}
		else
		{

			ngSpiceInit = (ngSpice_Init)GetProcAddress(ngSpiceModule, "ngSpice_Init");
			ngSpiceCommand = (ngSpice_Command)GetProcAddress(ngSpiceModule, "ngSpice_Command");
			ngSpiceCirc = (ngSpice_Circ)GetProcAddress(ngSpiceModule, "ngSpice_Circ");
			ngSpiceRunning = (ngSpice_running)GetProcAddress(ngSpiceModule, "ngSpice_running");
			ngSpiceGetVectorInfo = (ngGet_Vec_Info)GetProcAddress(ngSpiceModule, "ngGet_Vec_Info");
			ngSpiceGetCurPlot = (ngSpice_CurPlot)GetProcAddress(ngSpiceModule, "ngSpice_CurPlot");
			ngSpiceListPlots = (ngSpice_AllPlots)GetProcAddress(ngSpiceModule, "ngSpice_AllPlots");
			ngSpiceListVectors = (ngSpice_AllVecs)GetProcAddress(ngSpiceModule, "ngSpice_AllVecs");

			if (ngSpiceInit == NULL) logPrinter("failed to load ngspice ngSpice_Init func!");
			if (ngSpiceCommand == NULL) logPrinter("failed to load ngspice ngSpice_Command func!");
			if (ngSpiceCirc == NULL) logPrinter("failed to load ngspice ngSpice_Circ func!");
			if (ngSpiceRunning == NULL) logPrinter("failed to load ngspice ngSpice_running func!");
			if (ngSpiceGetVectorInfo == NULL) logPrinter("failed to load ngspice ngGet_Vec_Info func!");
			if (ngSpiceGetCurPlot == NULL) logPrinter("failed to load ngspice ngSpice_CurPlot func!");
			if (ngSpiceListPlots == NULL) logPrinter("failed to load ngspice ngSpice_AllPlots func!");
			if (ngSpiceListVectors == NULL) logPrinter("failed to load ngspice ngSpice_AllVecs func!");

			if (ngSpiceInit == NULL || ngSpiceCommand == NULL || ngSpiceCirc == NULL || ngSpiceRunning == NULL || ngSpiceGetVectorInfo == NULL || ngSpiceGetCurPlot == NULL || ngSpiceListPlots == NULL || ngSpiceListVectors == NULL) {
				logPrinter("ngspice already attached!");
				return 0;
			}

			int status = ngSpiceInit(
				[](char* print, int id, void* caller) -> int {
					nglink* nglinki = (nglink*) caller;
					nglinki->logPrinter(string(print));
					return 1;
				},
				[](char* print, int id, void* caller) -> int {
					nglink* nglinki = (nglink*) caller;
					nglinki->logPrinter(string(print));
					return 1;
				},
				[](int exitStatus, NG_BOOL detachDll, NG_BOOL normalExit, int id, void* caller) -> int {
					nglink* nglinki = (nglink*) caller;
					if (!normalExit)
					{
						nglinki->logPrinter("crash of ngspice simmulator!");
					}
					else
					{
						nglinki->logPrinter("ngspice wants to quit!");
					}
					if (detachDll)
					{
						nglinki->logPrinter("detaching ngspice ...");
						nglinki->detachNGSpice();
					} else {
						nglinki->ngSpiceModule = NULL;
						nglinki->ngSpiceInit = NULL;
						nglinki->ngSpiceCommand = NULL;
						nglinki->ngSpiceCirc = NULL;
						nglinki->ngSpiceRunning = NULL;
						nglinki->ngSpiceGetVectorInfo = NULL;
						nglinki->ngSpiceGetCurPlot = NULL;
						nglinki->ngSpiceListPlots = NULL;
						nglinki->ngSpiceListVectors = NULL;
						nglinki->logPrinter("ngspice detaching by itself!");
					}
					nglinki->detachCallback();
					return 1;
				},
				[](pvecvaluesall data, int count, int id, void* caller) -> int {
					nglink* nglinki = (nglink*) caller;
					if (nglinki->reciveVecData != NULL) nglinki->reciveVecData(data);
					return 1;
				},
				[](pvecinfoall data, int id, void* caller) -> int {
					nglink* nglinki = (nglink*) caller;
					if (nglinki->reciveInitData != NULL) nglinki->reciveInitData(data);
					return 1;
				},
				NULL, this
			);

			if (status == 0)
			{
				logPrinter("ngspice attached!");
				return 1;
			}
			logPrinter("failed to attach ngspice!");
			return 0;
		}
	}
	else
	{
		logPrinter("ngspice is already attached!");
		return 0;
	}
}

/* Checks if ngspice is attached */
bool nglink::isNGSpiceAttached()
{
	return ngSpiceModule != 0;
}

/* Detaches the ngspice lib */
bool nglink::detachNGSpice()
{
	if (!initialised)
	{
		logPrinter("nglinker is not initialized, use initNGLink first!");
		return false;
	}

	if (ngSpiceModule != 0)
	{
		if (FreeLibrary(ngSpiceModule)) {
			ngSpiceModule = NULL;
			ngSpiceInit = NULL;
			ngSpiceCommand = NULL;
			ngSpiceCirc = NULL;
			ngSpiceRunning = NULL;
			ngSpiceGetVectorInfo = NULL;
			ngSpiceGetCurPlot = NULL;
			ngSpiceListPlots = NULL;
			ngSpiceListVectors = NULL;
			logPrinter("ngspice detached!");
			return true;
		}
		logPrinter("ngspice could not be detached!");
		return false;
	}
	else
	{
		logPrinter("ngspice is not attached!");
		return false;
	}
}

/* Executes a standard spice command */
bool nglink::execCommand(string command) {
	int status = checkNGState();
	if (status != 1) return false;

	const char* cstr = command.c_str();
	char* buffer = (char*) alloca(strlen(cstr) + 1);
	strcpy(buffer, cstr);

	return ngSpiceCommand(buffer) == 0 ? true : false;
}

/* Executes a list of commands as if loaded as file */
bool nglink::loadCircuit(string circListString) {
	int status = checkNGState();
	if (status != 1) return status;

	const char* cstr = circListString.c_str();
	char* buffer = (char*) alloca(strlen(cstr) + 1);
	strcpy(buffer, cstr);

	std::vector<char*> commands = {};
	char *line = strtok(buffer, "\n");
	if (line == NULL) return false;
	commands.push_back(line);
	while ((line = strtok(NULL, "\n")) != NULL) {
		commands.push_back(line);
	}
	if (strcmp(commands.back(), ".end\n") != 0) {
		commands.push_back(_strdup(".end\n"));
	}
	return ngSpiceCirc(commands.data()) == 0 ? 1 : 0;
}

/* Checks if ngspice is running in its background thread */
bool nglink::isRunning() {
	int status = checkNGState();
	if (status != 1) return 0;

	return ngSpiceRunning();
}

/* Lists all aviable plots of the current spice instance */
vector<string> nglink::listPlots() {
	int status = checkNGState();
	if (status != 1) return vector<string>();

	char** plotcstr =  ngSpiceListPlots();

	vector<string> plots;
	size_t i = 0;
	while (plotcstr[i++] != 0)
		plots.push_back(string(plotcstr[i]));

	return plots;
}

/* Gets the current active plot */
string nglink::getCurrentPlot() {
	int status = checkNGState();
	if (status != 1) return "";

	return string(ngSpiceGetCurPlot());
}

/* Lists all available vectors of the given plot */
vector<string> nglink::listVecs(string plotName) {
	int status = checkNGState();
	if (status != 1) return vector<string>();

	const char* cstr = plotName.c_str();
	char* buffer = (char*) alloca(strlen(cstr) + 1);
	strcpy(buffer, cstr);

	char** vectorcstr = ngSpiceListVectors(buffer);

	vector<string> vectors;
	size_t i = 0;
	while (vectorcstr[i++] != 0)
		vectors.push_back(string(vectorcstr[i]));

	return vectors;
}

/* Get a specific vector info */
pvector_info nglink::getVecInfo(string vecName) {
	int status = checkNGState();
	if (status != 1) return NULL;

	const char* cstr = vecName.c_str();
	char* buffer = (char*) alloca(strlen(cstr) + 1);
	strcpy(buffer, cstr);

	return ngSpiceGetVectorInfo(buffer);
}
