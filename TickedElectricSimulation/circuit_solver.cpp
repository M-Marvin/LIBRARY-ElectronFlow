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
	vector<Element*> lastElementCandidates = vector<Element*>();
	NODE tempNode = startNode;
	while (true) {

		for (Element* e : elementsList) {
			if (e->node1 == tempNode || e->node2 == tempNode) {
				lastElementCandidates.push_back(e);
				elementsList.erase(std::remove(elementsList.begin(), elementsList.end(), e), elementsList.end());
			}
		}

		if (lastElementCandidates.size() == 0) return 0;

		// Get next node to check
		tempNode = lastElementCandidates[0]->node1 == tempNode ? lastElementCandidates[0]->node2 : lastElementCandidates[0]->node1;

		// End if last element was found
		if (tempNode == endNode) break;

		// Remove the element to continue with the next one
		lastElementCandidates.erase(lastElementCandidates.begin());

	}

	return lastElementCandidates.size() > 0 ? lastElementCandidates[0] : 0;

}

NODE SubSolver::findSeriesSequence(const vector<Element*>* allElements, NODE startNode, NODE endNode, vector<Element*>* outVec) {

	if (startNode == endNode) return endNode;

	NODE tempNode = startNode;
	while (true) {

		Element* seriesSequenceCandidates = 0;

		for (Element* e : *allElements) {
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

bool SubSolver::structorize(vector<Element*>* elements) {

	// Select first element from node1
	Element* firstElement = 0;
	for (size_t i = 0; i < elements->size(); i++) {
		if ((*elements)[i]->node1 == node1 || (*elements)[i]->node2 == node1) {
			firstElement = (*elements)[i];
			elements->erase(elements->begin());
			break;
		}
	}
	if (firstElement == 0) {
		printf("[!] invalid start nodes, no element on node1 %s\n", node1->name);
		return false;
	}

	// Determine next node in series sequence after node1
	NODE nextNode1 = firstElement->node1 == node1 ? firstElement->node2 : firstElement->node1;
	printf("start first series sequence from node %s to %s, element %s\n", node1->name, nextNode1->name, firstElement->name);

	// Select last element for node2
	Element* lastElement = nextNode1 == SubSolver::node2 ? firstElement : findOtherEnd(elements, nextNode1, SubSolver::node2);
	if (lastElement == 0) {
		printf("[!] invalid series sequence no connection between node1 %s and node2 %s\n", nextNode1->name, SubSolver::node2->name);
		return false;
	}
	elements->erase(std::remove(elements->begin(), elements->end(), lastElement), elements->end());

	// Determine next node in series sequence after node2
	NODE nextNode2 = lastElement == firstElement ? node2 : lastElement->node1 == node2 ? lastElement->node2 : lastElement->node1;
	printf("start end series sequence from node %s to %s, element %s\n", node2->name, nextNode2->name, lastElement->name);

	// Find series sequence from node 1
	vector<Element*> seriesSequence1 = vector<Element*>();
	SubSolver::seriesEndNode1 = findSeriesSequence(elements, nextNode1, nextNode2, &seriesSequence1);
	if (SubSolver::seriesEndNode1 == 0) {
		printf("[!] could not find series connection 1, interrupted sequence between %s and %s\n", nextNode1->name, nextNode2->name);
		return false;
	}
	seriesSequence1.push_back(firstElement);

	// Remove all elements packed in sequence one from all elements list
	elements->erase(std::remove_if(elements->begin(), elements->end(), [seriesSequence1](Element* e) {
		return std::find(seriesSequence1.begin(), seriesSequence1.end(), e) != seriesSequence1.end();
	}), elements->end());

	// Find series sequence from node 2
	vector<Element*> seriesSequence2 = vector<Element*>();
	SubSolver::seriesEndNode2 = node2;
	if (SubSolver::seriesEndNode1 != node2) {
		SubSolver::seriesEndNode2 = findSeriesSequence(elements, nextNode2, SubSolver::seriesEndNode1, &seriesSequence2);
		if (SubSolver::seriesEndNode2 == 0) {
			printf("[!] could not find series connection 2, interrupted sequence between %s and end of first sequence %s\n", nextNode2->name, SubSolver::seriesEndNode1->name);
			return false;
		}
	}
	seriesSequence2.push_back(lastElement);

	// Remove all elements packed in sequence two from all elements list
	elements->erase(std::remove_if(elements->begin(), elements->end(), [seriesSequence2](Element* e) {
		return std::find(seriesSequence2.begin(), seriesSequence2.end(), e) != seriesSequence2.end();
	}), elements->end());

	if (SubSolver::seriesEndNode1 != SubSolver::seriesEndNode2) {

		// Find all paralell elements for the sub solvers
		vector<Element*> parallelElements = vector<Element*>();

		for (Element* element : *elements) {
			if (
					element->node1 == SubSolver::seriesEndNode1 ||
					element->node2 == SubSolver::seriesEndNode1 ||
					element->node1 == SubSolver::seriesEndNode2 ||
					element->node2 == SubSolver::seriesEndNode2) {
				parallelElements.push_back(element);
			}
		}

		// Create parallel solvers
		for (Element* element : parallelElements) {
			if (
					element->node1 == SubSolver::seriesEndNode1 ||
					element->node2 == SubSolver::seriesEndNode1 ||
					element->node1 == SubSolver::seriesEndNode2 ||
					element->node2 == SubSolver::seriesEndNode2) {

				SubSolver* parallelSolver = new SubSolver(SubSolver::seriesEndNode1, SubSolver::seriesEndNode2);
				SubSolver::parallelSolvers.push_back(parallelSolver);

				if (!parallelSolver->structorize(elements)) {
					printf("[!] failed to construct parallel sub solver between nodes %s and %s\n", SubSolver::seriesEndNode1->name, SubSolver::seriesEndNode2->name);
					return false;
				}

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
