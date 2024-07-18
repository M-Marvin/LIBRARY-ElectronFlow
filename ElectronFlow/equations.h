/*
 * equations.h
 *
 *  Created on: 31.05.2024
 *      Author: Marvin K.
 */

#ifndef EQUATIONS_H_
#define EQUATIONS_H_

#include <vector>
#include <map>
#include <string>
#include <functional>

using namespace std;

namespace equations {

#define VAR_FUNC_NAME_LEN 64

union eqdata_t {
	double number_value;
	char str_name[VAR_FUNC_NAME_LEN];
};

typedef struct {
	char type;
	char precedence;
	eqdata_t value;
} eqitem_t;

#define eq_vec vector<eqitem_t>

#define OPERATOR_NUM 1
#define OPERATOR_VAR 2
#define OPERATOR_FNC 3
#define OPERATOR_ADD '+'
#define OPERATOR_SUB '-'
#define OPERATOR_MUL '*'
#define OPERATOR_DIV '/'
#define OPERATOR_MOD '%'
#define OPERATOR_POW '^'
#define OPERATOR_BRO '('
#define OPERATOR_BRC ')'
#define OPERATOR_SEP ','
#define PARSE_ITEM_BUFFER_LEN 256

/**
 * Convert an string representing an infix equation to an equation vector
 */
bool strtoeq(const char* equation, eq_vec* infixbuf);

/**
 * Convert an equation vector to an string representation
 */
char* eqtostr(eq_vec* equation, char* strbuf, size_t len);

/**
 * Convert an equation vector representing an infix notation to an rpn notation vector
 */
bool infix2rpn(eq_vec* infix, eq_vec* rpnbuf);

/**
 * Convert a string representing an number to an double
 */
bool strtonum(const char* numberStr, double* value);

#define mvar_pi M_PI
#define mvar_e M_E

double mfunc_1_sqrt(double* argv);
double mfunc_2_pow(double* argv);
double mfunc_2_max(double* argv);
double mfunc_2_min(double* argv);
double mfunc_1_sin(double* argv);
double mfunc_1_cos(double* argv);
double mfunc_1_tan(double* argv);
double mfunc_1_asin(double* argv);
double mfunc_1_acos(double* argv);
double mfunc_1_atan(double* argv);

#define func_map map<string, pair<function<double(double*)>, int>>
#define var_map map<string, double>

void set_vmap_default(var_map* varmap);
void set_fmap_default(func_map* funcmap);

/**
 * Represents an mathematical equation that can be solved by supplying variable values and functions
 */
class equation {

public:
	/** Construct equation from infix notation string */
	equation(const char* equationStr, bool* success);
	/** Construct equation from infix or rpn notation vec */
	equation(eq_vec* equation, bool infix);
	~equation();

	/** Define default functions and variables */
	void setvf_default();
	/** Define variable */
	void setv(const char* vname, double value);
	/** Define function */
	void setf(const char* fname, function<double(double*)> func, int argc);
	/** Set external map to store functions (can be shared between extensions) */
	void ext_fmap(func_map* fmap);
	/** Set external map to store variables (can be shared between extensions) */
	void ext_vmap(var_map* vmap);
	/** Calculate result of this equation */
	bool evaluate(double* result);

private:
	eq_vec* eqvec;
	bool ext_variables;
	bool ext_functions;
	var_map* variables;
	func_map* functions;

};

}

#endif /* EQUATIONS_H_ */
