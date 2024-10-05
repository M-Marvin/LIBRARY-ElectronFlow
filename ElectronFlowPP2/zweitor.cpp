/*
 * zweitor.cpp
 *
 *  Created on: 06.10.2024
 *      Author: marvi
 */

#include "zweitor.h"
#include <string.h>
#include <format>
#include <iostream>
#include <string>
#include <string_view>

using namespace std;

bool netanalysis::solveZweitor(char* line, char* target, size_t cap) {

	char* tokptr;
	string componentName = string(strtok_r(line, " ", &tokptr));
	string node1A = string(strtok_r(NULL, " ", &tokptr));
	string node1B = string(strtok_r(NULL, " ", &tokptr));
	string node2A = string(strtok_r(NULL, " ", &tokptr));
	string node2B = string(strtok_r(NULL, " ", &tokptr));

	char* matStart = strtok_r(NULL, " ", &tokptr);
	if (matStart[1] != '[') {
		printf("zweitor format invalid\n");
		return false;
	}
	string m11 = string(strtok_r(NULL, " ", &tokptr));
	string m12 = string(strtok_r(NULL, " ", &tokptr));
	string m21 = string(strtok_r(NULL, " ", &tokptr));
	string m22 = string(strtok_r(NULL, " ", &tokptr));
	char* matEnd = strtok_r(NULL, " ", &tokptr);
	if (matEnd[0] != ']') {
		printf("zweitor format invalid\n");
		return false;
	}

	printf("zweitor: %s %c %s %s %s %s\n", componentName.c_str(), matStart[0], m11.c_str(), m12.c_str(), m21.c_str(), m22.c_str());

	string eq1format;
	string eq2format;

	// Convert zweitor matrix to two components with equations
	switch (matStart[0]) {
	case 'Z': { // TODO characteristics
		eq1format = "B1_{0} {1} {2} V=I(B1_{0})*{5}+V({3},{4})*{6}\n";
		eq2format = "B2_{0} {4} {3} I=I(B1_{0})*{7}+V({3},{4})*{8}\n";
		break;
	}
	case 'Y': { //
		eq1format = "B1_{0} {1} {2} V=I(B1_{0})*{5}+V({3},{4})*{6}\n";
		eq2format = "B2_{0} {4} {3} I=I(B1_{0})*{7}+V({3},{4})*{8}\n";
		break;
	}
	case 'H': {
		eq1format = "B1_{0} {1} {2} V=I(B1_{0})*{5}+V({3},{4})*{6}\n";
		eq2format = "B2_{0} {4} {3} I=I(B1_{0})*{7}+V({3},{4})*{8}\n";
		break;
	}
	case 'P': { //
		eq1format = "B1_{0} {1} {2} V=I(B1_{0})*{5}+V({3},{4})*{6}\n";
		eq2format = "B2_{0} {4} {3} I=I(B1_{0})*{7}+V({3},{4})*{8}\n";
		break;
	}
	case 'A': { //
		eq1format = "B1_{0} {1} {2} V=I(B1_{0})*{5}+V({3},{4})*{6}\n";
		eq2format = "B2_{0} {4} {3} I=I(B1_{0})*{7}+V({3},{4})*{8}\n";
		break;
	}
	case 'B': { //
		eq1format = "B1_{0} {1} {2} V=I(B1_{0})*{5}+V({3},{4})*{6}\n";
		eq2format = "B2_{0} {4} {3} I=I(B1_{0})*{7}+V({3},{4})*{8}\n";
		break;
	}
	default:
		printf("invalid matrix characteristic %c\n", matStart[0]);
		return false;
	}

	string s1 = std::vformat(eq1format, std::make_format_args(componentName, node1A, node1B, node2A, node2B, m11, m12, m21, m22));
	string s2 = std::vformat(eq2format, std::make_format_args(componentName, node1A, node1B, node2A, node2B, m11, m12, m21, m22));
	strcat(target, s1.c_str());
	strcat(target, s2.c_str());

	return true;

}
