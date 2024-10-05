/*
 * zweitor.cpp
 *
 *  Created on: 08.10.2024
 *      Author: marvi
 */

#include "electronflow.h"
#include <regex>

using namespace std;
using namespace electronflow;

regex const pattern("(ZT[0-9a-zA-Z_]+) ([0-9a-zA-Z_]+) ([0-9a-zA-Z_]+) ([0-9a-zA-Z_]+) ([0-9a-zA-Z_]+) ([ZYHPAB])\\[ ([0-9a-zA-Z\\/_\\-+\\*\\(\\)\\.]+) ([0-9a-zA-Z\\/_\\-+\\*\\(\\)\\.,]+) ([0-9a-zA-Z\\/_\\-+\\*\\(\\)\\.,]+) ([0-9a-zA-Z\\/_\\-+\\*\\(\\)\\.,]+) \\]\r*");

map<string, pair<string, string>> equations = {
		{"H", make_pair(
				"B1_{0} {1} {2} V=I(B1_{0})*{5}+V({3},{4})*{6}",
				"B2_{0} {4} {3} I=I(B1_{0})*{7}+V({3},{4})*{8}"
		)}
};

bool solver::formatZweitor(string* line, stringstream* out) {

	smatch m;

	if (!regex_match(*line, m, pattern)) {
		logout("failed to parse zweitor, syntax invalid");
		return false;
	}

	string name = m[1];
	string node0 = m[2];
	string node1 = m[3];
	string node2 = m[4];
	string node3 = m[5];
	string type = m[6];
	string m11 = m[7];
	string m12 = m[8];
	string m21 = m[9];
	string m22 = m[10];

	if (!equations.contains(type)) {
		logout(format("invalid zweitor type: {}", type));
		return false;
	}

	pair<string, string> eqf = equations[type];

	string eq1 = vformat(eqf.first, make_format_args(name, node0, node1, node2, node3, m11, m12, m21, m22));
	string eq2 = vformat(eqf.second, make_format_args(name, node0, node1, node2, node3, m11, m12, m21, m22));

	*out << eq1 << "\n" << eq2 << "\n";
	return true;

}


