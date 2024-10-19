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
using namespace equations;

class matrixset {

public:
	vector<vector<equation>> E_mat;
	vector<vector<equation>> A_mat;
	vector<equation> f_vec;
	vector<equation> x_vec;

};



#endif /* NETMATH_H_ */
