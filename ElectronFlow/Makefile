PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

CLI_NAME = electronflow
LIB_NAME = libelectonflow.dll

OBJS = circuit.o circuit_container.o circuit_solver.o electron_flow.o equations.o

ifeq ($(BUILD_MODE),debug)
	CFLAGS += -g
else
	CFLAGS += -O2
endif

all:	cli lib

cli:	ElectronFlowCLI

lib:	ElectronFlowLIB

ElectronFlowCLI:	ElectronFlowCLI.o $(OBJS)
	$(CXX) $(LDFLAGS) -o $(CLI_NAME) $^

ElectronFlowLIB:	$(OBJS)
	$(CXX) $(LDFLAGS) -shared -o $(LIB_NAME) $^

%.o:	$(PROJECT_ROOT)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

%.o:	$(PROJECT_ROOT)%.c
	$(CC) -c $(CFLAGS) $(CPPFLAGS) -o $@ $<

clean:
	rm -fr ElectronFlowCLI ElectronFlowLIB $(OBJS)
