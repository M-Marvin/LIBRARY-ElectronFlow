# Electron Flow and NGLink

**Electron Flow**
*ElectronFlow* is (currently) a colletion of different projets.
Endgoal is to create an stand alone solver for electric networks, with some additional features.
Right now it its more of an wrapper around the *NG-SPICE* solver, adding some additional features using an preprocessor for the netlists.

**NGLink**
*NGLink* is a library that simplifies the loading and interaction with the *NG-SPICE* library.
It also provides the *JNI* interface for using *NG-SPICE* directly in Java.
It is used by all *ElectronFlow* sub-projects that need to interface with *NG-SPICE*

# Building

Before trying to build the projects, the existence of the ngspice42-native *ngspice42_x64.dll* and its header file *ngspcice.h* in the *External* folder have to be ensured.

The natives are written in *C++* and compiled using an **C++23** compatible compiler.
Before the java projects can be build, both, the *NGLink* and the *ElectronFlow* projects have to be build for all required platforms.
The C projects use simple *Makefile*'s, they can be run from a terminal using `make all` or within the Eclipse IDE (the projects are configured as Eclipse projects, but would probably also work with other IDEs).

After that, simply run the *build* gradle task in the two projects *JNGLink* and *JElectornFlow*
Natives from the *Bin* folder will automatically be copied into the required folder in the java projects.
