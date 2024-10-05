/*
 * advmath.h
 *
 *  Created on: 06.10.2024
 *      Author: marvi
 */

#ifndef NETMATH_H_
#define NETMATH_H_

#include "tomsolver.hpp"

namespace netmath {

typedef tomsolver::Node mfunction_t;
typedef tomsolver::SymVec mfunction_set_t;
typedef tomsolver::VarsTable mvarstable_t;

class mtemplate {

public:
	void clear();
	mtemplate insert_start(mtemplate mtemp);
	mtemplate insert_end(mtemplate mtemp);
	mtemplate insert_at(size_t pos, mtemplate mtemp);

};

mtemplate parsemt(const char* exp);

inline mtemplate operator""_mt(const char* exp, size_t) {
	return parsemt(exp);
}

}

#endif /* NETMATH_H_ */
