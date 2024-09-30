PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

OBJS = equations.o netparsing.o netmeshing.o ElectronMatrix.o

CFLAGS += -g -Og
ifeq ($(BUILD_MODE),debug)
	CFLAGS += -g
else ifeq ($(BUILD_MODE),run)
	CFLAGS += -O2
else ifeq ($(BUILD_MODE),linuxtools)
	CFLAGS += -g -pg -fprofile-arcs -ftest-coverage
	LDFLAGS += -pg -fprofile-arcs -ftest-coverage
	EXTRA_CLEAN += ElectronMatrix.gcda ElectronMatrix.gcno $(PROJECT_ROOT)gmon.out
	EXTRA_CMDS = rm -rf ElectronMatrix.gcda
else
    $(error Build mode $(BUILD_MODE) not supported by this Makefile)
endif

all:	ElectronMatrix

ElectronMatrix:	$(OBJS)
	$(CXX) $(LDFLAGS) -o $@ $^
	$(EXTRA_CMDS)

%.o:	$(PROJECT_ROOT)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

%.o:	$(PROJECT_ROOT)%.c
	$(CC) -c $(CFLAGS) $(CPPFLAGS) -o $@ $<

clean:
	rm -fr ElectronMatrix $(OBJS) $(EXTRA_CLEAN)
