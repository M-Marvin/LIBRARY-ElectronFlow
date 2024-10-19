/*
 * equations.h
 *
 *  Created on: 15.10.2024
 *      Author: marvi
 */

#ifndef EQUATIONS_H_
#define EQUATIONS_H_

#include <string>
#include <vector>
#include <functional>

namespace equations {

typedef size_t index_t;

using namespace std;

enum node_type {
	NDF = 0,VAR,VAL,OP
};

typedef struct {
	string val;
	node_type type;
} node;

bool operator==(const node &a, const node &b);

class equation {

private:
	vector<node> nodes;

public:
	equation();
	equation(const string &str);
	equation(const equation &e);
	equation(vector<node>::iterator first, vector<node>::iterator last);
	string str();
	equation substitute(const equation &eq, const equation &sub);
	equation substitute_func(string func, int nparam, function<equation(vector<equation>)> sub);
	void insert(index_t index, const equation &eq);
	void insert_ate(index_t index, const equation &eq);
	void expand();
};

equation operator""_f(const char* str, size_t);
equation operator+(const equation &a, const equation &b);
void operator+=(equation &a, const equation &b);

}

#endif /* EQUATIONS_H_ */
