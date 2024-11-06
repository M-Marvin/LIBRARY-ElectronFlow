PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

EF_VERSION = "1.0_beta"
DEF_NGSPICE = "ngspice42_x64.dll"

BINDIR = $(PROJECT_ROOT)/../Binaries
OBJS = electronflow.o netparser.o dataparser.o ngspice.o zweitor.o
OBJS_CLI = cli.o $(OBJS)
OBJS_JNI = jni.o $(OBJS)
INCL = -I$(PROJECT_ROOT)/../NGLink -I$(PROJECT_ROOT)/../JElectronFlow/build/generated/sources/headers/java/main -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32"
LIBS = -L$(BINDIR) -lstdc++exp -l:nglink_x64.dll
CFLAGS = -std=c++23 -DEF_VERSION=\"$(EF_VERSION)\" -DDEF_NGSPICE=\"$(DEF_NGSPICE)\"

BUILD_MODE = prod

ifeq ($(BUILD_MODE),debug)
	CFLAGS += -g
else
	CFLAGS += -O2
	LDFLAGS = -static -static-libgcc -static-libstdc++
endif

all:	electronflow.exe electronflow_x64.dll electronflow_jni_x64.dll

electronflow_x64.dll:	$(OBJS)
	$(CXX) $(LDFLAGS) -o $(BINDIR)/$@ $^ $(LIBS) -shared
	$(EXTRA_CMDS)

electronflow_jni_x64.dll:	$(OBJS_JNI)
	$(CXX) $(LDFLAGS) -o $(BINDIR)/$@ $^ $(LIBS) -shared
	$(EXTRA_CMDS)

electronflow.exe:	$(OBJS_CLI)
	$(CXX) $(LDFLAGS) -o $(BINDIR)/$@ $^ $(LIBS)
	$(EXTRA_CMDS)

%.o:	$(PROJECT_ROOT)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) $(INCL) -o $@ $<

clean:
	rm -fr $(BINDIR)/electronflow.exe $(BINDIR)/electronflow_jni_x64.dll $(BINDIR)/electronflow_x64.dll $(OBJS_CLI) $(OBJS_JNI) $(EXTRA_CLEAN)
