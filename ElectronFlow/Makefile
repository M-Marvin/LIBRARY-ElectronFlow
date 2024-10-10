PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

BINDIR = $(PROJECT_ROOT)/../Bin
OBJS = cli.o electronflow.o netparser.o dataparser.o ngspice.o zweitor.o
INCL = -I$(PROJECT_ROOT)/../NGLink
LIBS = -L$(BINDIR) -lstdc++exp -lnglink_x64
CFLAGS = -std=c++23
LDFLAGS =

ifeq ($(BUILD_MODE),debug)
	CFLAGS += -g
else
	CFLAGS += -O2
endif

all:	electronflow.exe

electronflow.exe:	$(OBJS)
	$(CXX) $(LDFLAGS) -o $(BINDIR)/$@ $^ $(LIBS)
	$(EXTRA_CMDS)

%.o:	$(PROJECT_ROOT)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) $(INCL) -o $@ $<

clean:
	rm -fr $(BINDIR)/electronflow.exe $(OBJS) $(EXTRA_CLEAN)
