/*
 * netmath.h
 *
 *  Created on: 15.10.2024
 *      Author: marvi
 */

#ifndef NETMATH_H_
#define NETMATH_H_

#include "netanalysis.h"
#include "equations.h"
#include <string>
#include <vector>

using namespace std;

class matrixset {

public:
	vector<vector<equation>> g_mat;
	vector<equation> p_vec;
	vector<equation> i_vec;

};



#endif /* NETMATH_H_ */
