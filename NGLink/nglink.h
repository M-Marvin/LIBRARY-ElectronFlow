#pragma once

/*
This is a library to simplify the use of ngspice.dll
This library does mainly 2 things:
[A] - Simplify instantiating ngspice and handling of its callback's
[B] - Make it compatible with java via a JNI interface
*/

#include <string>
#include <functional>

using namespace std;

namespace ngspice {

/* copy of SPICE data types */

typedef struct spice_vecvalues {
	char* name;		/* name of a specific vector */
	double creal;	  /* actual data value */
	double cimag;	  /* actual data value */
	bool is_scale;	 /* if 'name' is the scale vector */
	bool is_complex;   /* if the data are complex numbers */
} spice_vecvalues, * spice_pvecvalues;

/* info for a specific vector */
typedef struct spice_vecinfo
{
	int number;	 /* number of vector, as postion in the linked list of vectors, starts with 0 */
	char* vecname;  /* name of the actual vector */
	bool is_real;   /* TRUE if the actual vector has real data */
	void* pdvec;	/* a void pointer to struct dvec *d, the actual vector */
	void* pdvecscale; /* a void pointer to struct dvec *ds, the scale vector */
} spice_vecinfo, * spice_pvecinfo;

/* info for the current plot */
typedef struct spice_vecinfoall
{
	/* the plot */
	char* name;
	char* title;
	char* date;
	char* type;
	int veccount;

	/* the data as an array of vecinfo with length equal to the number of vectors in the plot */
	spice_pvecinfo* vecs;

} spice_vecinfoall, * spice_pvecinfoall;

typedef struct spice_vecvaluesall {
	int veccount;	  /* number of vectors in plot */
	int vecindex;	  /* index of actual set of vectors. i.e. the number of accepted data points */
	spice_pvecvalues* vecsa; /* values of actual set of vectors, indexed from 0 to veccount - 1 */
} spice_vecvaluesall, * spice_pvecvaluesall;

/* end of SPICE data types */

/* Callback functions from nglink */
// Called for printing log information
typedef std::function<void(string)> log_func;
// Called if ngspice wants to detach (MUST be executed (with detacheNGSpice) to prevent unwanted behavior)
typedef std::function<void(void)> detach_func;
// Called to output the simulation data, contains the actual data after the simmulation has finished
typedef std::function<void(spice_pvecvaluesall)> receive_vec_data_func;
// Called before a new simulation is started, contains information abbout the used vectors (name, size, count etc)
typedef std::function<void(spice_pvecinfoall)> receive_init_data_func;

class nglink_impl;
class nglink {

private:
	nglink_impl* impl;

public:
	nglink();
	~nglink();

	void logPrinter(std::string log);
	bool checkNGState();

	// Initialise NGLink with all callback functions
	bool initNGLink(log_func printFunc, detach_func detacheFunc);
	bool initNGLinkFull(log_func printFunc, detach_func detacheFunc, receive_vec_data_func reciveVecFunc, receive_init_data_func reciveInitFunc);
	// Checks if nglink is initialized
	bool isInitialisated();
	// Try to load and attach the ngspice lib
	bool initNGSpice(string libName);
	// Checks if ngspice is attached
	bool isNGSpiceAttached();
	// Detaches the ngspice lib
	bool detachNGSpice();
	// Executes a standard spice command
	bool execCommand(string command);
	// Executes a list of commands as if loaded as file
	bool loadCircuit(string circListString);
	// Checks if spice is running in its background thread
	bool isRunning();
	// Lists all aviable plots of the current spice instance
	vector<string> listPlots();
	// Gets the current active plot
	string getCurrentPlot();
	// Lists all available vectors of the given plot
	vector<string> listVecs(string plotName);
	// Get a specific vector info
	//pvector_info getVecInfo(string vecName);

};

}
