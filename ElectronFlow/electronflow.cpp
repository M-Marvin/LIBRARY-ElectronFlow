/*
 * electronflow.cpp
 *
 *  Created on: 10.10.2024
 *      Author: M_Marvin
 */

#include "electronflowimpl.h"
#include "electronflow.h"

using namespace electronflow;
using namespace std;

solver::solver(function<void(string)> logout) {
	solver::impl = new solver_impl(logout);
}

solver::~solver() {
	delete solver::impl;
}

void solver::setlibname(string libname) {
	solver::impl->setlibname(libname);
}

void solver::setlogger(function<void(string)> logout) {
	solver::impl->setlogger(logout);
}

bool solver::upload(istream* in) {
	return solver::impl->upload(in);
}

bool solver::executeList() {
	return solver::impl->executeList();
}

bool solver::execute(string command) {
	return solver::impl->execute(command);
}

void solver::printdata(ostream* out) {
	solver::impl->printdata(out);
}

string solver::netfiltered() {
	return solver::impl->netfiltered();
}
