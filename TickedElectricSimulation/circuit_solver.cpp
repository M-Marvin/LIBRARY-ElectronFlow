/*
 * circuit_solver.cpp
 *
 *  Created on: 21.05.2024
 *      Author: marvi
 */

#include "circuit_solver.h"

#include <stdio.h>
#include <string.h>
#include <algorithm>

SourceSolver::SourceSolver(CircuitContainer* circuit, Element* source) {
	SourceSolver::circuit = circuit;
	SourceSolver::source = source;
	SourceSolver::rootSolver = 0;
}

SourceSolver::~SourceSolver() {
	delete SourceSolver::rootSolver;
}

bool SourceSolver::compileStructure() {

	printf("compile circuit structure for source '%s'\n", SourceSolver::source->name);

	NODE node1 = SourceSolver::source->node1;
	NODE node2 = SourceSolver::source->node2;
	SourceSolver::rootSolver = new SubSolver(node1, node2);

	printf("operate structurizer on nodes '%s'-'%s'\n", node1->name, node2->name);
	vector<Element*> tempElements = vector<Element*>(SourceSolver::circuit->elements);
	tempElements.erase(std::remove(tempElements.begin(), tempElements.end(), SourceSolver::source), tempElements.end());
	if (!SourceSolver::rootSolver->structorize(&tempElements)) {
		printf("[!] failed to run structorizer!\n");
		return false;
	}
	printf("structorizer completed\n");
	
	return true;

}

/**
 * Searches for an valid connection between startNode and endNode (in direction of firstElement from startNode) and returns the last element before endNode.
 */
Element* SubSolver::findOtherEnd(const vector<Element*>* allElements, NODE startNode, NODE endNode) {

	vector<Element*> elementsList = vector<Element*>(*allElements);
	vector<pair<Element*, NODE>> lastElementCandidates = vector<pair<Element*, NODE>>();
	NODE tempNode = startNode;
	while (true) {

		for (Element* e : elementsList) {
			if (e->node1 == tempNode || e->node2 == tempNode) {
				lastElementCandidates.push_back(make_pair(e, tempNode));
				elementsList.erase(std::remove(elementsList.begin(), elementsList.end(), e), elementsList.end());
			}
		}

//		printf("- %d\n", lastElementCandidates.size());
//		for (pair<Element*, NODE> e : lastElementCandidates) {
//			printf("-- %s\n", e.first->name);
//		}

		if (lastElementCandidates.size() == 0) return 0;

		// Get next node to check
		NODE lastNode = lastElementCandidates[0].second;
		tempNode = lastElementCandidates[0].first->node1 == lastNode ? lastElementCandidates[0].first->node2 : lastElementCandidates[0].first->node1;

		// End if last element was found
		if (tempNode == endNode) break;

		// Remove the element to continue with the next one
		lastElementCandidates.erase(lastElementCandidates.begin());

	}

	return lastElementCandidates.size() > 0 ? lastElementCandidates[0].first : 0;

}

NODE SubSolver::findSeriesSequence(const vector<Element*>* allElements, NODE startNode, NODE endNode, vector<Element*>* outVec) {

	if (startNode == endNode) return endNode;

	NODE tempNode = startNode;
	while (true) {

		Element* seriesSequenceCandidates = 0;

		for (Element* e : *allElements) {
			if (find(outVec->begin(), outVec->end(), e) != outVec->end()) continue;

			if (e->node1 == tempNode || e->node2 == tempNode) {

				if (seriesSequenceCandidates != 0) {
					// End of series sequence
					return tempNode;
				}

				seriesSequenceCandidates = e;
			}
		}

		if (seriesSequenceCandidates == 0) {
			// Interrupted sequence
			break;
		}

		// Next node
		outVec->push_back(seriesSequenceCandidates);
		tempNode = seriesSequenceCandidates->node1 == tempNode ? seriesSequenceCandidates->node2 : seriesSequenceCandidates->node1;

		if (tempNode == endNode) {
			// End reached, start to end is one series connection
			return tempNode;
		}

	}

	return 0;
}

bool SubSolver::structorize(vector<Element*>* elements, Element* startElement) {

	// If start element provided, initialize first sequence with it
	NODE startNode = node1;
	if (startElement != 0) {
		SubSolver::seriesElements1.push_back(startElement);
		startNode = startElement->node1 == node1 ? startElement->node2 : startElement->node1;
	}

	// Find series sequence from node 1
	printf("scan %s - %s\n", startNode->name, node2->name);
	SubSolver::seriesEndNode1 = findSeriesSequence(elements, startNode, node2, &(SubSolver::seriesElements1));
	if (SubSolver::seriesEndNode1 == 0) {
		printf("[!] could not find series connection 1, interrupted sequence between %s and %s\n", startNode->name, node2->name);
		return false;
	}
	printf("[-] first series sequence %s - %s\n", startNode->name, seriesEndNode1->name);

	// Remove all elements packed in sequence one from all elements list
	elements->erase(std::remove_if(elements->begin(), elements->end(), [this](Element* e) {
		return find(SubSolver::seriesElements1.begin(), SubSolver::seriesElements1.end(), e) != SubSolver::seriesElements1.end();
	}), elements->end());

	// Find series sequence from node 2
	SubSolver::seriesEndNode2 = node2;
	if (SubSolver::seriesEndNode1 != node2) {
		SubSolver::seriesEndNode2 = findSeriesSequence(elements, node2, SubSolver::seriesEndNode1, &(SubSolver::seriesElements2));
		if (SubSolver::seriesEndNode2 == 0) {
			printf("[!] could not find series connection 2, interrupted sequence between %s and end of first sequence %s\n", node2->name, SubSolver::seriesEndNode1->name);
			return false;
		}
	}
	printf("[-] second series sequence %s - %s\n", node2->name, seriesEndNode2->name);

	// Remove all elements packed in sequence two from all elements list
	elements->erase(std::remove_if(elements->begin(), elements->end(), [this](Element* e) {
		return find(SubSolver::seriesElements2.begin(), SubSolver::seriesElements2.end(), e) != SubSolver::seriesElements2.end();
	}), elements->end());

	if (SubSolver::seriesEndNode1 != SubSolver::seriesEndNode2) {

		// Find all parallel elements for the sub solvers
		vector<Element*> parallelElements = vector<Element*>();

		for (Element* element : *elements) {
			if (
					element->node1 == SubSolver::seriesEndNode1 ||
					element->node2 == SubSolver::seriesEndNode1) {
				parallelElements.push_back(element);
			}
		}

		// Create parallel solvers
		for (Element* element : parallelElements) {

			printf("start parallel solver on nodes '%s'-'%s'\n", SubSolver::seriesEndNode1->name, SubSolver::seriesEndNode2->name);

			SubSolver* parallelSolver = new SubSolver(SubSolver::seriesEndNode1, SubSolver::seriesEndNode2);
			SubSolver::parallelSolvers.push_back(parallelSolver);

			if (!parallelSolver->structorize(elements, element)) {
				printf("[!] failed to construct parallel sub solver between nodes %s and %s\n", SubSolver::seriesEndNode1->name, SubSolver::seriesEndNode2->name);
				return false;
			}

		}

	}

	return true;

}

SubSolver::SubSolver(NODE node1, NODE node2) {
	SubSolver::node1 = node1;
	SubSolver::node2 = node2;
	SubSolver::seriesEndNode1 = 0;
	SubSolver::seriesEndNode2 = 0;
	SubSolver::seriesElements1 = vector<Element*>();
	SubSolver::seriesElements2 = vector<Element*>();
	SubSolver::parallelSolvers = vector<SubSolver*>();
}

SubSolver::~SubSolver() {
	for (SubSolver* solver : SubSolver::parallelSolvers) delete solver;
	SubSolver::parallelSolvers.clear();
	SubSolver::seriesElements1.clear();
	SubSolver::seriesElements2.clear();
}
