/*
 * nglink.cpp
 *
 *  Created on: 10.10.2024
 *      Author: M_Marvin
 */

#include "nglinkimpl.h"
#include "nglink.h"
#include <ngspice.h>
#include <iostream>
#include <functional>
#include <format>
#include <string>
#include <vector>
#include <print>

using namespace std;
using namespace ngspice;

nglink::nglink() {
	nglink::impl = new nglink_impl();
}

nglink::~nglink() {
	delete nglink::impl;
}

/* Helper function for handling log prints */
void nglink::logPrinter(string log)
{
	nglink::impl->logPrinter(log);
}

/* Helper function checks if nglinker and ngspice are initialized */
bool nglink::checkNGState() {
	return nglink::impl->checkNGState();
}

/* Initialize NGLink with all callback functions */
bool nglink::initNGLink(log_func printFunc, detach_func detacheFunc)
{
	return nglink::impl->initNGLink(printFunc, detacheFunc);
}
bool nglink::initNGLinkFull(log_func printFunc, detach_func detacheFunc, receive_vec_data_func reciveVecFunc, receive_init_data_func reciveInitFunc)
{
	return nglink::impl->initNGLinkFull(printFunc, detacheFunc, reciveVecFunc, reciveInitFunc);
}


/* Checks if nglink is initialized */
bool nglink::isInitialisated() {
	return nglink::impl->isInitialisated();
}

/* Try to load and attach the ngspice lib */
bool nglink::initNGSpice(string libName)
{
	return nglink::impl->initNGSpice(libName);
}

/* Checks if ngspice is attached */
bool nglink::isNGSpiceAttached()
{
	return nglink::impl->isNGSpiceAttached();
}

/* Detaches the ngspice lib */
bool nglink::detachNGSpice()
{
	return nglink::impl->detachNGSpice();
}

/* Executes a standard spice command */
bool nglink::execCommand(string command) {
	return nglink::impl->execCommand(command);
}

/* Executes a list of commands as if loaded as file */
bool nglink::loadCircuit(string circListString) {
	return nglink::impl->loadCircuit(circListString);
}

/* Checks if ngspice is running in its background thread */
bool nglink::isRunning() {
	return nglink::impl->isRunning();
}

/* Lists all aviable plots of the current spice instance */
vector<string> nglink::listPlots() {
	return nglink::impl->listPlots();
}

/* Gets the current active plot */
string nglink::getCurrentPlot() {
	return nglink::impl->getCurrentPlot();
}

/* Lists all available vectors of the given plot */
vector<string> nglink::listVecs(string plotName) {
	return nglink::impl->listVecs(plotName);
}
