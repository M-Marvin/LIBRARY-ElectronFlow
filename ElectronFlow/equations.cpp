/*
 * equations.cpp
 *
 *  Created on: 31.05.2024
 *      Author: Marvin K.
 */

#include "equations.h"
#include <string.h>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

using namespace equations;

equation::equation(const char* equationStr, bool* success) {
	eq_vec infix = eq_vec();
	if (!strtoeq(equationStr, &infix)) {
		printf("failed to parse equation: %s\n", equationStr);
		if (success != 0) *success = false;
		return;
	}

	equation::eqvec = new eq_vec();
	if (!infix2rpn(&infix, equation::eqvec)) {
		printf("could not convert equation!\n");
		if (success != 0) *success = false;
		return;
	}

	equation::variables = new var_map();
	equation::functions = new func_map();
	equation::ext_variables = false;
	equation::ext_functions = false;

	setvf_default();
	if (success != 0) *success = true;
}

equation::equation(eq_vec* equation, bool infix) {
	if (infix) {
		equation::eqvec = new eq_vec();
		if (!infix2rpn(equation, equation::eqvec)) {
			printf("could convert equation to rpn!\n");
			return;
		}
	} else {
		equation::eqvec = new eq_vec(*equation);
	}

	equation::variables = new var_map();
	equation::functions = new func_map();
	equation::ext_variables = false;
	equation::ext_functions = false;

	setvf_default();
}

equation::~equation() {
	delete equation::eqvec;
	if (!equation::ext_functions) delete equation::functions;
	if (!equation::ext_variables) delete equation::variables;
}

void equation::setv(const char* vname, double value) {
	(*equation::variables)[string(vname)] = value;
}

void equation::setf(const char* fname, function<double(double*)> func, int argc) {
	(*equation::functions)[string(fname)] = make_pair(func, argc);
}

void equation::ext_fmap(func_map* fmap) {
	if (fmap == 0) {
		if (!equation::ext_functions) return;
		equation::functions = new func_map();
		equation::ext_functions = false;
	} else {
		if (!ext_functions) delete equation::functions;
		equation::functions = fmap;
		equation::ext_functions = true;
	}
}

void equation::ext_vmap(var_map* vmap) {
	if (vmap == 0) {
		if (!equation::ext_variables) return;
		equation::variables = new var_map();
		equation::ext_variables = false;
	} else {
		if (!ext_variables) delete equation::variables;
		equation::variables = vmap;
		equation::ext_variables = true;
	}
}

void equation::setvf_default() {
	set_vmap_default(variables);
	set_fmap_default(functions);
}

bool equation::evaluate(double* result) {
	vector<double> stack = vector<double>();

	for (eqitem_t eqitem : *equation::eqvec) {

		if (eqitem.type == OPERATOR_NUM) {
			stack.push_back(eqitem.value.number_value);
		} else if (eqitem.type == OPERATOR_VAR) {
			string vname = string(eqitem.value.str_name);
			if (equation::variables->count(vname) == 0) {
				printf("undefined variable %s!\n", vname.c_str());
				return false;
			}
			double v = (*equation::variables)[vname];
			stack.push_back(v);
		} else if (eqitem.type == OPERATOR_FNC) {
			string fname = string(eqitem.value.str_name);
			if (equation::functions->count(fname) == 0) {
				printf("undefined function %s\n", fname.c_str());
				return false;
			} else {
				pair<function<double(double*)>, int> funcpair = (*equation::functions)[fname];
				if (stack.size() < funcpair.second) {
					printf("not enough arguments to function %s!\n", fname.c_str());
					return false;
				}
				double argv[funcpair.second] = {0};
				for (int i = funcpair.second; i > 0; i--) {
					argv[i - 1] = stack.back();
					stack.pop_back();
				}
				double result = funcpair.first(argv);
				stack.push_back(result);
			}
		} else if (eqitem.type == OPERATOR_ADD) {
			if (stack.size() < 2) {
				printf("not enough arguments to operator %c!\n", eqitem.type);
				return false;
			}
			double op2 = stack.back();
			stack.pop_back();
			double op1 = stack.back();
			stack.pop_back();
			stack.push_back(op1 + op2);
		} else if (eqitem.type == OPERATOR_SUB) {
			if (stack.size() < 2) {
				printf("not enough arguments to operator %c!\n", eqitem.type);
				return false;
			}
			double op2 = stack.back();
			stack.pop_back();
			double op1 = stack.back();
			stack.pop_back();
			stack.push_back(op1 - op2);
		} else if (eqitem.type == OPERATOR_MUL) {
			if (stack.size() < 2) {
				printf("not enough arguments to operator %c!\n", eqitem.type);
				return false;
			}
			double op2 = stack.back();
			stack.pop_back();
			double op1 = stack.back();
			stack.pop_back();
			stack.push_back(op1 * op2);
		} else if (eqitem.type == OPERATOR_DIV) {
			if (stack.size() < 2) {
				printf("not enough arguments to operator %c!\n", eqitem.type);
				return false;
			}
			double op2 = stack.back();
			stack.pop_back();
			double op1 = stack.back();
			stack.pop_back();
			stack.push_back(op1 / op2);
		} else if (eqitem.type == OPERATOR_MOD) {
			if (stack.size() < 2) {
				printf("not enough arguments to operator %c!\n", eqitem.type);
				return false;
			}
			double op2 = stack.back();
			stack.pop_back();
			double op1 = stack.back();
			stack.pop_back();
			stack.push_back(fmod(op1, op2));
		} else if (eqitem.type == OPERATOR_POW) {
			if (stack.size() < 2) {
				printf("not enough arguments to operator %c!\n", eqitem.type);
				return false;
			}
			double op2 = stack.back();
			stack.pop_back();
			double op1 = stack.back();
			stack.pop_back();
			stack.push_back(pow(op1, op2));
		} else {
			printf("invalid operator %c in rpn equation vec!\n", eqitem.type);
			return false;
		}

	}

	if (stack.size() == 1) {
		*result = stack.back();
		return true;
	}

	printf("invalid equation syntax, stack count %d != 1!\n", stack.size());
	return false;
}



double equations::mfunc_1_sqrt(double* argv) {
	return sqrt(argv[0]);
}
double equations::mfunc_2_pow(double* argv) {
	return pow(argv[0], argv[1]);
}
double equations::mfunc_2_max(double* argv) {
	return max(argv[0], argv[1]);
}
double equations::mfunc_2_min(double* argv) {
	return min(argv[0], argv[1]);
}
double equations::mfunc_1_sin(double* argv) {
	return sin(argv[0]);
}
double equations::mfunc_1_cos(double* argv) {
	return cos(argv[0]);
}
double equations::mfunc_1_tan(double* argv) {
	return tan(argv[0]);
}
double equations::mfunc_1_asin(double* argv) {
	return asin(argv[0]);
}
double equations::mfunc_1_acos(double* argv) {
	return acos(argv[0]);
}
double equations::mfunc_1_atan(double* argv) {
	return atan(argv[0]);
}

void equations::set_vmap_default(var_map* varmap) {
	varmap->emplace(string("pi"), mvar_pi);
	varmap->emplace(string("e"), mvar_e);
}

void equations::set_fmap_default(func_map* funcmap) {
	funcmap->emplace(string("sqrt"), make_pair(mfunc_1_sqrt, 1));
	funcmap->emplace(string("pow"), make_pair(mfunc_2_pow, 2));
	funcmap->emplace(string("max"), make_pair(mfunc_2_max, 2));
	funcmap->emplace(string("min"), make_pair(mfunc_2_min, 2));
	funcmap->emplace(string("sin"), make_pair(mfunc_1_sin, 1));
	funcmap->emplace(string("cos"), make_pair(mfunc_1_cos, 1));
	funcmap->emplace(string("tan"), make_pair(mfunc_1_tan, 1));
	funcmap->emplace(string("asin"), make_pair(mfunc_1_asin, 1));
	funcmap->emplace(string("acos"), make_pair(mfunc_1_acos, 1));
	funcmap->emplace(string("atan"), make_pair(mfunc_1_atan, 1));
}

bool equations::infix2rpn(eq_vec* infix, eq_vec* rpnbuf) {

	eq_vec stack = eq_vec();

	for (eqitem_t eqitem : *infix) {

		// If number or variable
		if (eqitem.type == OPERATOR_NUM || eqitem.type == OPERATOR_VAR) {
			// Push to output
			rpnbuf->push_back(eqitem);
		// If function
		} else if (eqitem.type == OPERATOR_FNC) {
			// Push to stack
			stack.push_back(eqitem);
		// If comma (separator)
		} else if (eqitem.type == OPERATOR_SEP) {
			// While the operator on the stack is not an open braked
			while (stack.size() > 0 && stack.back().type != OPERATOR_BRO) {
				// Push stack to output
				rpnbuf->push_back(stack.back());
				stack.erase(stack.end());
			}
		// If open braked
		} else if (eqitem.type == OPERATOR_BRO) {
			// Push to stack
			stack.push_back(eqitem);
		// If close braked
		} else if (eqitem.type == OPERATOR_BRC) {
			// While the the operator on the stack is not a open braked
			while (stack.size() > 0 && stack.back().type != OPERATOR_BRO) {
				// Pop from stack and push to output
				rpnbuf->push_back(stack.back());
				stack.erase(stack.end());
			}
			if (stack.size() == 0 || stack.back().type != OPERATOR_BRO) {
				printf("mismatching parenthesis !\n");
				return false;
			}
			// Pop open braked from stack and discard
			stack.erase(stack.end());
			// If operator on stack is function
			if (stack.size() > 0 && stack.back().type == OPERATOR_FNC) {
				// Pop from stack and push to output
				rpnbuf->push_back(stack.back());
				stack.erase(stack.end());
			}
		// Else (an normal operator)
		} else {
			// While there is an operator o2 on the stack and o2 is not a open braked
			// and (o2 has greater precedence than o1 or (o1 has same precendence as o2 and o1 is left-associative (non pow operator)))
			while (
					stack.size() > 0 && (stack.back().type != OPERATOR_BRO &&
					(stack.back().precedence > eqitem.precedence || (stack.back().precedence == eqitem.precedence && eqitem.type != OPERATOR_POW))
					)) {

				// Pop o2 from the stack and push to output
				rpnbuf->push_back(stack.back());
				stack.erase(stack.end());
			}
			// Push to output
			stack.push_back(eqitem);
		}

	}
	// Pop from stack and push to output for remaining operators
	while (stack.size() > 0) {
		if (stack.back().type == OPERATOR_BRO || stack.back().type == OPERATOR_BRC) {
			printf("mismatching parenthesis !\n");
			return false;
		}
		rpnbuf->push_back(stack.back());
		stack.erase(stack.end());
	}

	return true;

}

char* equations::eqtostr(eq_vec* equation, char* strbuf, size_t len) {

	size_t ptr = 0;
	memset(strbuf, 0, len);

	for (eqitem_t eqitem : *equation) {

		if (eqitem.type == OPERATOR_NUM) {
			ptr += snprintf(strbuf + ptr, len - ptr, "%f", eqitem.value.number_value);
		} else if (
				eqitem.type == OPERATOR_FNC ||
				eqitem.type == OPERATOR_VAR) {
			ptr += snprintf(strbuf + ptr, len - ptr, "%s", eqitem.value.str_name);
		} else {
			strbuf[ptr] = eqitem.type;
			ptr++;
		}

		if (ptr >= len) return strbuf + len - 1;
	}

	return strbuf + ptr;
}

bool equations::strtoeq(const char* equation, eq_vec* infixbuf) {

	size_t bufferPtr = 0;
	char buffer[PARSE_ITEM_BUFFER_LEN] = {0};
	size_t len = strlen(equation);

	for (size_t i = 0; i < len; i++) {
		char character = *(equation + i);

		if (	character == OPERATOR_ADD ||
				character == OPERATOR_SUB ||
				character == OPERATOR_DIV ||
				character == OPERATOR_MUL ||
				character == OPERATOR_MOD ||
				character == OPERATOR_POW ||
				character == OPERATOR_BRC ||
				character == OPERATOR_SEP) {

			if (bufferPtr > 0) {

				double number;
				if (strtonum(buffer, &number)) {

					// Put number
					infixbuf->push_back(eqitem_t());
					infixbuf->back().type = OPERATOR_NUM;
					infixbuf->back().precedence = 0;
					infixbuf->back().value.number_value = number;

				} else {

					// Put variable
					infixbuf->push_back(eqitem_t());
					infixbuf->back().type = OPERATOR_VAR;
					infixbuf->back().precedence = 0;
					if (strlen(buffer) > VAR_FUNC_NAME_LEN) {
						printf("variable name '%s' length > %d!\n", buffer, VAR_FUNC_NAME_LEN);
						return false;
					}
					strcpy(infixbuf->back().value.str_name, buffer);

				}
				memset(buffer, 0, 64);
				bufferPtr = 0;

			}

			// Put operator
			infixbuf->push_back(eqitem_t());
			infixbuf->back().type = character;
			infixbuf->back().value.number_value = 0;

			if (character == OPERATOR_ADD || character == OPERATOR_SUB) {
				infixbuf->back().precedence = 2;
			} else if (character == OPERATOR_MUL || character == OPERATOR_DIV || character == OPERATOR_MOD) {
				infixbuf->back().precedence = 3;
			} else if (character == OPERATOR_POW) {
				infixbuf->back().precedence = 4;
			}

		} else if (character == OPERATOR_BRO) {

			if (bufferPtr > 0) {

				// Put function
				infixbuf->push_back(eqitem_t());
				infixbuf->back().type = OPERATOR_FNC;
				if (strlen(buffer) > VAR_FUNC_NAME_LEN) {
					printf("function name '%s' length > %d!\n", buffer, VAR_FUNC_NAME_LEN);
					return false;
				}
				strcpy(infixbuf->back().value.str_name, buffer);
				//memcpy(infixbuf->back().value.str_name, buffer, 4);

				memset(buffer, 0, 64);
				bufferPtr = 0;

			}

			// Put close backed operator
			infixbuf->push_back(eqitem_t());
			infixbuf->back().type = OPERATOR_BRO;
			infixbuf->back().value.number_value = 0;

		} else {
			if (bufferPtr == PARSE_ITEM_BUFFER_LEN) {
				printf("out of buffer space!\n");
				return false;
			}
			buffer[bufferPtr] = character;
			bufferPtr++;
		}
	}

	if (bufferPtr > 0) {

		double number;
		if (strtonum(buffer, &number)) {

			// Put number
			infixbuf->push_back(eqitem_t());
			infixbuf->back().type = OPERATOR_NUM;
			infixbuf->back().value.number_value = number;

		} else {

			// Put function
			infixbuf->push_back(eqitem_t());
			infixbuf->back().type = OPERATOR_VAR;
			memcpy(infixbuf->back().value.str_name, buffer, 4);

		}

	}

	return true;

}

bool equations::strtonum(const char* numberStr, double* value) {
	if (numberStr == 0) {
		return false;
	}

	size_t len = strlen(numberStr);
	char* factorStr;
	double base = strtod(numberStr, &factorStr);

	if (factorStr == numberStr || factorStr - numberStr < len - (*factorStr == 0 ? 0 : 1)) {
		return false;
	}

	double factor;
	switch (*factorStr) {
	case 'p': factor = 1E-12; break;
	case 'n': factor = 1E-9; break;
	case 'u': factor = 1E-6; break;
	case 'm': factor = 1E-3; break;
	case 'k': factor = 1E+3; break;
	case 'M': factor = 1E+6; break;
	case 'G': factor = 1E+9; break;
	default: factor = 1; break;
	}

	*value = base * factor;
	return true;
}
