# Electron Flow and NGLink

**Electron Flow**
*ElectronFlow* is (currently) a colletion of different projects.
Currently it mainly contains a wrapper around the *NG-SPICE* solver, adding some additional features using an preprocessor for the netlists and an first simple standalone simulator utilizing the netlib project for its solver.

**NGLink**
*NGLink* is a library that simplifies the loading and interaction with the *NG-SPICE* library.
It also provides the *JNI* interface for using *NG-SPICE* directly in Java.
It is used by all *ElectronFlow* sub-projects that need to interface with *NG-SPICE*

**NGSAdditon**
A wrapper around NGspice using NGLink which adds a few additonal capabilities regard "zweitor" components.
This is an lagacy project and most likely developement is not continued.

**TVNL-NNA**
A universal *T*ime *V*ariant *N*on *L*inear *N*odal *N*etwork *A*nalyzer.
This is the currently developed standalone circuit simulation program.
It implements an netlist and element definition file parser and non linear DAE solver using the netlib project.
It does not implement the actual simulation loop, the application utilizing it has full controll of the invocation of the solver with every time step, thus, variable time steps are also supported in theroy, tough this has not yet been tested extensively.
> The NetLib Project: https://www.netlib.org

# Building NGLinik/NGSAdditon

Before trying to build the projects NGLink and NGSAdditon, the existence of the ngspice42-native *ngspice42_x64.dll* and its header file *ngspcice.h* in the *External* folder have to be ensured.

The natives are written in *C++* and compiled using an **C++23** compatible compiler.
Before the java projects can be build, both, the *NGLink* and the *ElectronFlow* projects have to be build for all required platforms.
The C projects use simple *Makefile*'s, they can be run from a terminal using `make all` or within the Eclipse IDE (the projects are configured as Eclipse projects, but would probably also work with other IDEs).

After that, simply run the *build* gradle task in the two projects *JNGLink* and *JElectornFlow*
Natives from the *Bin* folder will automatically be copied into the required folder in the java projects.

# Building TVNL-NNA

This project currently is a pure java project, compillation should therefore be streight forward.
Tough, it utelizes a custom build system which can be seen as a variation to gradle.
The equivalent to .\gradlew build would be .\metaw build
