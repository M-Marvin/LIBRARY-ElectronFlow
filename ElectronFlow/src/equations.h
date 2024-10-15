/*
 * equations.h
 *
 *  Created on: 15.10.2024
 *      Author: marvi
 */

#ifndef EQUATIONS_H_
#define EQUATIONS_H_

#include <string>

using namespace std;

class equation {

protected:

public:
	string value;
	equation(string str);

};

equation operator""_f(const char* str, size_t);
equation operator+(equation a, equation b);
void operator+=(equation a, equation b);

#endif /* EQUATIONS_H_ */
