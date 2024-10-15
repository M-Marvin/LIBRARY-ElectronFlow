/*
 * equations.cpp
 *
 *  Created on: 15.10.2024
 *      Author: marvi
 */

#include "equations.h"

equation operator""_f(const char* str, size_t) {
	return equation(string(str));
}

equation operator+(equation a, equation b) {
	return equation(a.value + b.value);
}

void operator+=(equation a, equation b) {
	a.value += b.value;
}


