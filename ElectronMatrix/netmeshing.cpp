/*
 * netmeshing.cpp
 *
 *  Created on: 30.09.2024
 *      Author: marvi
 */

#include "netanalysis.h"

using namespace netanalysis;

int netanalysis::findBranches(network_t* network) {

	network->subnets.clear();
	network->branches.clear();
	network->meshes.clear();

	vector<component_t*> components;
	for (size_t i = 0; i < network->components.size(); i++)
		components.push_back(network->components.data() + i);

	printf("%s > scan for branches ...\n", network->name);

	component_t* c1 = 0;
	component_t* c2 = 0;
	while (components.size() > 0) {

		// new branch
		network->branches.push_back({});
		branch_t* branch = &network->branches.back();

		// select first component, add to branch, remove from remainder list and initialize itterator variables
		component_t* ibca = components.back();
		component_t* ibcb = ibca;
		components.pop_back();
		branch->components.push_back(ibca);

		// search for components connected to node_a of first component until end of branch
		node_t* npa = ibca->node_a.ptr;
		node_t* npb = ibcb->node_b.ptr;
		while (branch->node_a != npa || branch->node_b != npb) {

			if (branch->node_a != npa) {
				if (npa->component_ptr.size() > 2) { // Last node of branch found if more than two components connected
					branch->node_a = npa;
				} else if (npa->component_ptr.size() < 2) { // Invalid branch of node has only one connection -> open circuit
					branch->node_a = npa = 0;
				} else { // Continue iteration on this node, add component to branch, remove from remainder list
					ibca = (component_t*) (npa->component_ptr[0] == ibca ? npa->component_ptr[1] : npa->component_ptr[0]);
					npa = ibca->node_a.ptr == npa ? ibca->node_b.ptr : ibca->node_a.ptr;
					branch->components.push_back(ibca);
					components.erase(find(components.begin(), components.end(), ibca));
				}
			}

			if (branch->node_b != npb) {
				if (npb->component_ptr.size() > 2) { // Last node of branch found if more than two components connected
					branch->node_b = npb;
				} else if (npb->component_ptr.size() < 2) { // Invalid branch of node has only one connection -> open circuit
					branch->node_b = npb = 0;
				} else { // Continue iteration on this node, add component to branch, remove from remainder list
					ibcb = (component_t*) (npb->component_ptr[0] == ibcb ? npb->component_ptr[1] : npb->component_ptr[0]);
					npb = ibcb->node_a.ptr == npb ? ibcb->node_b.ptr : ibcb->node_a.ptr;
					branch->components.push_back(ibcb);
					components.erase(find(components.begin(), components.end(), ibcb));
				}
			}

			// Detect singular branch closed loop
			if (ibca == ibcb) {
				// Even number of components, just select one of the nodes as "knot" for both branch ends
				branch->node_b = npb = branch->node_a = npa = ibca->node_a.ptr;
			} else if (c1 == ibcb && c2 == ibca) {
				// Uneven number of components, remove the last two components (have been inserted twice) and select one of the nodes as "knot" for both branch ends
				branch->components.pop_back();
				branch->components.pop_back();
				branch->node_b = npb = branch->node_a = npa = ibca->node_a.ptr;
			}
			c1 = ibca;
			c2 = ibcb;

		}

		// If end nodes are null, an open circuit was detected -> branch invalid
		if (branch->node_a == 0 || branch->node_b == 0) {
			network->branches.pop_back();
			continue;
		}

		printf("found branch: [%d] ", network->branches.size() - 1);
		for (component_t* c : branch->components) printf("%s ", c->name);
		printf("\n");

	}

	printf("%s > found %d branches\n", network->name, network->branches.size());

	printf("%s > scan for sub-networks ...\n", network->name);

	vector<branch_t*> branches;
	for (size_t i = 0; i < network->branches.size(); i++)
		branches.push_back(network->branches.data() + i);

	vector<branch_t*> ic;
	while (branches.size() > 0) {

		network->subnets.push_back({});
		subnet_t* subnet = &network->subnets.back();

		// Select first branch and add to subnet, remove from remainder list
		branch_t* b = branches.back();
		subnet->branches.push_back(b);
		subnet->knots.push_back(b->node_a);
		if (b->node_a == b->node_b) {
			// Special case, singular loop branch, skip to increase efficiency.
			branches.pop_back();

			printf("found subnet: singular branch: (%d) [%d]\n", network->subnets.size() - 1, ((size_t) b - (size_t) network->branches.data()) / sizeof(branch_t));
			continue;
		} else {
			subnet->knots.push_back(b->node_b);
			branches.pop_back();
		}

		// Search for connected branches and knots and add to subnet
		ic.push_back(b);
		while (ic.size() > 0) {

			branch_t* icb = ic.back();
			ic.pop_back();

			// Find connected branches
			for (size_t i = 0; i < branches.size(); i++) {
				branch_t* ib = branches[i];
				if (ib->node_a == icb->node_a || ib->node_b == icb->node_a || ib->node_a == icb->node_b || ib->node_b == icb->node_b) {
					subnet->branches.push_back(ib);
					ic.push_back(ib);
					branches.erase(branches.begin() + i);

					// Test for new knots on branch
					if (find(subnet->knots.begin(), subnet->knots.end(), ib->node_a) == subnet->knots.end())
						subnet->knots.push_back(ib->node_a);
					if (find(subnet->knots.begin(), subnet->knots.end(), ib->node_b) == subnet->knots.end())
						subnet->knots.push_back(ib->node_b);

				}
			}

		}

		printf("found subnet: (%d) %d branches  %d knots: ", network->subnets.size() - 1, subnet->branches.size(), subnet->knots.size());
		for (branch_t* b : subnet->branches) printf("[%d] ", ((size_t) b - (size_t) network->branches.data()) / sizeof(branch_t));
		printf("\n");

	}

	printf("%s > found %d sub-networks\n", network->name, network->subnets.size());

	printf("%s > scan for meshes ...\n", network->name);

	struct scan_entry {
		node_t* node = 0x0;
		branch_t* branch = 0x0;
		vector<branch_t*> skip_list = vector<branch_t*>();
	};

	for (size_t i = 0; i < network->subnets.size(); i++) {

		subnet_t* subnet = network->subnets.data() + i;
		int m = subnet->branches.size() - (subnet->knots.size() - 1);

		// Initialize last node as starting point
		vector<node_t*> knots = vector<node_t*>(subnet->knots);
		vector<branch_t*> tendoms = vector<branch_t*>(subnet->branches);
		vector<struct scan_entry> stack;
		stack.push_back({});
		stack.back().node = knots.back();
		knots.pop_back();

		tendom_itr_start:
		while (m != tendoms.size()) {

			// Get last element on path stack
			node_t* in = stack.back().node;

			// Search for next possible element
			for (size_t i = 0; i < tendoms.size(); i++) {
				branch_t* b = tendoms[i];
				if (
						(b->node_a == in || b->node_b == in)// &&
						//find(stack.back().skip_list.begin(), stack.back().skip_list.end(), b) == stack.back().skip_list.end()
					) {

					// Select the next node to iterate
					node_t* n = b->node_a == in ? b->node_b : b->node_a;

					// Check if this component does not lead back to an already "mapped" branch
					auto vp = find(knots.begin(), knots.end(), n);
					if (vp == knots.end()) continue;

					// Add this entry to the end of the current path stack
					stack.push_back({ n, b });
					knots.erase(vp);
					tendoms.erase(tendoms.begin() + i);

					// Move control back to outer loop for next iteration
					goto tendom_itr_start;

				}

			}

			// Check if number of remaining branches (tendoms) matches expected count, this means the optimal path has ben reached
			// The branch stack now contains a list of valid tendoms for the meshes
			//if (m == tendoms.size()) break;

			// No more branches for this path, step back one node
			node_t* k = stack.back().node;
			branch_t* b = stack.back().branch;
			stack.pop_back();
			if (stack.empty()) break;
			//stack.back().skip_list.push_back(b);
			tendoms.push_back(b);
			knots.push_back(k);

		}

		if (tendoms.size() != m) {
			printf("subnet (%d): could not find valid tree structure in sub-network containing node %s\n", ((size_t) subnet - (size_t) network->subnets.data()) / sizeof(subnet_t), subnet->knots[0]->name);
			return -1;
		}

		printf("valid tendoms for subnet: (%d) %d\n", ((size_t) subnet - (size_t) network->subnets.data()) / sizeof(subnet_t), tendoms.size());

		// Make list of all branches that are not tendoms
		knots = vector<node_t*>(subnet->knots);
		vector<branch_t*> branches = vector<branch_t*>(subnet->branches);
		branches.erase(remove_if(branches.begin(), branches.end(), [tendoms](branch_t* b) { return find(tendoms.begin(), tendoms.end(), b) != tendoms.end(); }), branches.end());

		for (branch_t* tendom : tendoms) {

			// Create new mesh
			subnet->meshes.push_back({});
			mesh_t* mesh = &subnet->meshes.back();
			mesh->branchs.push_back(tendom);

			// Search for closed loop trough branches
			node_t* in = tendom->node_a;
			mesh_itr_start:
			while (in != tendom->node_b) {

				for (size_t bi = 0; bi < branches.size(); bi++) {
					branch_t* ib = branches[bi];
					if (	ib->node_a == in || ib->node_b == in
							) {

						// Select next node to iterate
						node_t* n = in == ib->node_a ? ib->node_b : ib->node_a;

						// Check if this component does not lead back to an already "mapped" branch
						auto vp = find(knots.begin(), knots.end(), n);
						if (vp == knots.end()) continue;

						mesh->branchs.push_back(ib);
						branches.erase(branches.begin() + bi);
						knots.erase(vp);
						in = n;

						// Move control back to outer loop for next iteration
						goto mesh_itr_start;

					}
				}

				// Check if mesh is closed
				//if (in == tendom->node_b) break;

				// No more branches for this path, step back one node
				branch_t* b = mesh->branchs.back();
				mesh->branchs.pop_back();
				branches.push_back(b);
				knots.push_back(in);
				in = b->node_a == in ? b->node_b : b->node_a;

			}

			if (in != tendom->node_b) {
				printf("failed to find closed loop for tendom between '%s' and '%s'\n", tendom->node_a->name, tendom->node_b->name);
				return -1;
			}

			printf("mesh completed: tendom (%d) branches: %d\n", subnet->meshes.size() - 1, mesh->branchs.size());

		}

		// Register meshes in network
		for (size_t i = 0; i < subnet->meshes.size(); i++)
			network->meshes.push_back(subnet->meshes.data() + i);

	}

	printf("%s > meshing completed\n", network->name);

	return 0;

}
