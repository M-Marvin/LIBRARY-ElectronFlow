/*
 * circuit_solver.h
 *
 *  Created on: 21.05.2024
 *      Author: marvi
 */

#ifndef CIRCUIT_SOLVER_H_
#define CIRCUIT_SOLVER_H_

#include "circuit_container.h"

class SubSolver {

public:
	SubSolver(NODE node1, NODE node2);
	~SubSolver();

	bool structorize(vector<Element*>* elements, Element* startElement = 0);

private:
	NODE node1;
	NODE node2;
	NODE seriesEndNode1;
	NODE seriesEndNode2;
	vector<Element*> seriesElements1;
	vector<Element*> seriesElements2;
	vector<SubSolver*> parallelSolvers;

	Element* findOtherEnd(const vector<Element*>* allElements, NODE startNode, NODE endNode);
	NODE findSeriesSequence(const vector<Element*>* allElements, NODE startNode, NODE endNode, vector<Element*>* outVec);

};

class SourceSolver {

public:
	SourceSolver(CircuitContainer* circuit, Element* source);
	~SourceSolver();
	bool compileStructure();

private:
	CircuitContainer* circuit;
	Element* source;
	SubSolver* rootSolver;

};

#endif /* CIRCUIT_SOLVER_H_ */
