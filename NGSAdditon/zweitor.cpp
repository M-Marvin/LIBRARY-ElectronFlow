/*
 * zweitor.cpp
 *
 *  Created on: 08.10.2024
 *      Author: M_Marvin
 */

#include <regex>
#include "electronflowimpl.h"

using namespace std;
using namespace electronflow;

regex const pattern("(ZT[^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([ZYHPAB])\\[ ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) \\]\\r*");

map<string, pair<string, string>> equations = {
		{"Z", make_pair(
				"B1_{0} {1} {2} V=I(B1_{0})*{5}+I(B2_{0})*{6}",
				"B2_{0} {3} {4} V=I(B1_{0})*{7}+I(B2_{0})*{8}"
		)},
		{"Y", make_pair(
				"B1_{0} {2} {1} I=V({1},{2})*{5}+V({3},{4})*{6}",
				"B2_{0} {4} {3} I=V({1},{2})*{7}+V({3},{4})*{8}"
		)},
		{"H", make_pair(
				"B1_{0} {1} {2} V=I(B1_{0})*{5}+V({3},{4})*{6}",
				"B2_{0} {4} {3} I=I(B1_{0})*{7}+V({3},{4})*{8}"
		)},
		{"P", make_pair(
				"B1_{0} {2} {1} I=V({1},{2})*{5}+I(B2_{0})*{6}",
				"B2_{0} {3} {4} V=V({1},{2})*{7}+I(B2_{0})*{8}"
		)}
};

bool solver_impl::formatZweitor(string* line, ostream* out) {

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


