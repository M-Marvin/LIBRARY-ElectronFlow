/*
 * vectorprocessing.cpp
 *
 *  Created on: 08.10.2024
 *      Author: marvi
 */

#include "electronflow.h"

using namespace netanalysis;

void solver::processData(pvecvaluesall values, int count) {

	printf("vecindex %d\n", values->vecindex);
	for (int i = 0; i < values->veccount; i++) {

		pvecvalues v = values->vecsa[i];
		if (!strrchr(v->name, '#')) {
			printf("%f %f %d %d '%s'\n", values->vecsa[i]->cimag, values->vecsa[i]->creal, values->vecsa[i]->is_complex, values->vecsa[i]->is_scale, values->vecsa[i]->name);

			node_t* n = &solver::network.nodes[string(v->name)];

			printf("-> %p\n", n);
		}

	}

}
