/*
 * make_matrix.cpp
 *
 *  Created on: 15.10.2024
 *      Author: marvi
 */

#include "netanalysis.h"
#include "equations.h"
#include <algorithm>

bool network::matrix(matrixset* matrixset) {

	int nNodes = network::nodes.size();

	matrixset->g_mat.reserve(nNodes);
	for (int i = 0; i < nNodes; i++)
		matrixset->g_mat[i].reserve(nNodes);

	matrixset->i_vec.reserve(nNodes);
	matrixset->p_vec.reserve(nNodes);

	for (int n = 0; n < nNodes; n++) {
		vector<index_t>* iev1 = &network::nodes[n].elements;

		for (int m = 0; m < nNodes; m++) {
			equation* eq = &matrixset->g_mat[n][m];
			*eq = "-"_f;

			if (n == m) {
				for (index_t ie : network::nodes[n].elements) {
					element* e = &network::elements[ie];
					if (e->type == GATE)
						*eq += "+"_f + e->g_val();
				}
			} else {
				vector<index_t>* iev2 = &network::nodes[m].elements;
				for (index_t ie : *iev1) {
					element* e = &network::elements[ie];
					if (find(iev2->begin(), iev2->end(), ie) != iev2->end())
						*eq += "+"_f + e->g_val();
				}

			}
		}

		equation* eq = &matrixset->i_vec[n];
		for (int ie : network::nodes[n].elements) {
			element* e = &network::elements[ie];
			if (e->type == ISOURCE)
				eq += "+"_f + e->i_val();
		}

	}

	return false;

}



