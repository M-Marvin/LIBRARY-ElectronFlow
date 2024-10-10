PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

BINDIR = $(PROJECT_ROOT)/../Bin
OBJS = nglink.o nglinkimpl.o
INCL = -I$(PROJECT_ROOT)/../External
LIBS = -lstdc++exp
CFLAGS = -std=c++23
LDFLAGS = -shared

ifeq ($(BUILD_MODE),debug)
	CFLAGS += -g
else
	CFLAGS += -O2
endif

all:	nglink_x64.dll

nglink_x64.dll:	$(OBJS)
	$(CXX) $(LDFLAGS) -o $(BINDIR)/$@ $^ $(LIBS)
	$(EXTRA_CMDS)

%.o:	$(PROJECT_ROOT)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) $(INCL) -o $@ $<

clean:
	rm -fr $(BINDIR)/nglink_x64.dll $(OBJS) $(EXTRA_CLEAN)
