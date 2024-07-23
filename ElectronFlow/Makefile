PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

CLI_NAME = electronflow
LIB_NAME = electronflow_x64.dll
TARGET_DIR = "$(PROJECT_ROOT)..\ElectronFlowJNI\src\main\resources\natives\$(LIB_NAME)"

OBJS = circuit.o circuit_container.o circuit_solver.o electron_flow.o equations.o
CFLAGS = -DVERSION_STR=\"0.3_alpha_2\"

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
	copy $(LIB_NAME) $(TARGET_DIR)

%.o:	$(PROJECT_ROOT)%.cpp
	$(CXX) -c $(CFLAGS) -o $@ $<

clean:
	rm -fr ElectronFlowCLI ElectronFlowLIB $(OBJS)
	rm -fr $(LIB_NAME)
	rm -fr $(CLI_NAME)
	rm -fr $(TARGET_DIR)
