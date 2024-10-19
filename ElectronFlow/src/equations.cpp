/*
 * equations.cpp
 *
 *  Created on: 15.10.2024
 *      Author: marvi
 */

#include "equations.h"
#include <regex>
#include <iostream>
#include <sstream>

using namespace equations;

const static regex node_regex("([\\d]+)|([\\w]+)|([\\/\\-+\\*\\(\\)∆^,=])|\\s+");

equation::equation() {}

equation::equation(const string &str) {

	sregex_iterator rbegin = sregex_iterator(str.begin(), str.end(), node_regex);
	sregex_iterator rend = sregex_iterator();

	for (sregex_iterator m = rbegin; m != rend; m++) {
		smatch match = *m;
		string val;
		node_type type = NDF;
		if (!(val = static_cast<string>(match.str(1))).empty()) {
			type = VAL;
		} else if (!(val = static_cast<string>(match.str(2))).empty()) {
			type = VAR;
		} else if (!(val = static_cast<string>(match.str(3))).empty()) {
			type = OP;
		}

		if (type != NDF) {
			equation::nodes.push_back({ val, type});
		}
	}

}

equation::equation(const equation &eq) {
	equation::nodes = vector<node>(eq.nodes);
}

equation::equation(vector<node>::iterator first, vector<node>::iterator last) {
	equation::nodes = vector<node>(first, last);
}

string equation::str() {
	stringstream s;
	for (node n : equation::nodes) s << n.val;
	return s.str();
}

equation equation::substitute(const equation &eq, const equation &sub) {

	auto seqlen = sub.nodes.size();
	auto eqlen = eq.nodes.size();
	auto replace = min(seqlen, eqlen);

	vector<node>::iterator match;
	auto last = equation::nodes.begin();

	while ((match = search(last, equation::nodes.end(), eq.nodes.begin(), eq.nodes.end())) != equation::nodes.end()) {

		for (unsigned int i = 0; i < replace; i++)
			match[i] = sub.nodes[i];

		if (replace < seqlen) {
			last = equation::nodes.insert(match + replace, sub.nodes.begin() + replace, sub.nodes.end());
		} else if (replace < eqlen) {
			last = equation::nodes.erase(match + replace, match + eqlen);
		}

	}

	return *this;
}

equation equation::substitute_func(string func, int nparam, function<equation(vector<equation>)> sub) {

	equation f = equation(func + "(");

	if (f.nodes.size() != 2) {
		cout << "invalid function name: " << func << "\n";
		return *this;
	}

	vector<node>::iterator match;
	auto last = equation::nodes.begin();

	while ((match = search(last, equation::nodes.end(), f.nodes.begin(), f.nodes.end())) != equation::nodes.end()) {

		vector<equation> params;
		vector<node>::iterator flast = match + 2;

		unsigned int bc = 0;
		for (auto ni = match + 2; ni != equation::nodes.end(); ni++) {
			node n = *ni;
			if (n.type != OP) continue;
			if (n.val == "," && bc == 0) {
				params.emplace_back(flast, ni);
				flast = ni + 1;
			} else if (n.val == ")") {
				if (bc > 0) {
					bc--;
				} else {
					params.emplace_back(flast, ni);
					flast = ni + 1;
					break;
				}
			} else if (n.val == "(") {
				bc++;
			}
		}

		if (nparam >= 0 && (int) params.size() != nparam) continue;

		equation substitute = sub(params);
		size_t seqlen = substitute.nodes.size();
		size_t eqlen = distance(match, flast);
		auto replace = min(seqlen, eqlen);

		for (unsigned int i = 0; i < replace; i++)
			match[i] = substitute.nodes[i];

		if (replace < seqlen) {
			last = equation::nodes.insert(match + replace, substitute.nodes.begin() + replace, substitute.nodes.end());
		} else if (replace < eqlen) {
			last = equation::nodes.erase(match + replace, match + eqlen);
		}

	}

	return *this;
}

void equation::expand() {

	vector<node>::iterator b = equation::nodes.begin();
	vector<node>::iterator e;

	for (auto ni = equation::nodes.begin(); ni != equation::nodes.end(); ni++) {
		node n = *ni;

		if (n.type != OP) continue;

		if (n.val == "-" || n.val == "+") {
			b = ni;
		} else if (n.val == "(") {
			e = ni;
		}

	}

}

void equation::insert(index_t index, const equation &eq) {
	equation::nodes.insert(equation::nodes.begin() + index, eq.nodes.begin(), eq.nodes.end());
}

void equation::insert_ate(index_t index, const equation &eq) {
	equation::nodes.insert(equation::nodes.end() - index, eq.nodes.begin(), eq.nodes.end());
}

bool equations::operator==(const node &a, const node &b) {
	return a.type == b.type && a.val == b.val;
}

equation equations::operator""_f(const char* str, size_t) {
	return equation(string(str));
}

equation equations::operator+(const equation &a, const equation &b) {
	equation r = equation(a);
	r.insert_ate(0, b);
	return r;
}

void equations::operator+=(equation &a, const equation &b) {
	a.insert_ate(0, b);
}


