PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

OBJS = ElectronFlowJNI_n.o
CFLAGS = -I "$(PROJECT_ROOT)..\ElectronFlowJNI\build\generated\sources\headers\java\main" -I "$(PROJECT_ROOT)..\ElectronFlow\" -I "C:\Program Files\Java\jdk-17.0.10+7\include" -I "C:\Program Files\Java\jdk-17.0.10+7\include\win32"

ifeq ($(BUILD_MODE),debug)
	CFLAGS += -g
else ifeq ($(BUILD_MODE),run)
	CFLAGS += -O2
else ifeq ($(BUILD_MODE),linuxtools)
	CFLAGS += -g -pg -fprofile-arcs -ftest-coverage
	LDFLAGS += -pg -fprofile-arcs -ftest-coverage
	EXTRA_CLEAN += ElectronFlowJNI_n.gcda ElectronFlowJNI_n.gcno $(PROJECT_ROOT)gmon.out
	EXTRA_CMDS = rm -rf ElectronFlowJNI_n.gcda
else
    $(error Build mode $(BUILD_MODE) not supported by this Makefile)
endif

all:	ElectronFlowJNI_n

ElectronFlowJNI_n:	$(OBJS)
	$(CXX) -shared $(LDFLAGS) -o $@ $^
	$(EXTRA_CMDS)

%.o:	$(PROJECT_ROOT)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

%.o:	$(PROJECT_ROOT)%.c
	$(CC) -c $(CFLAGS) $(CPPFLAGS) -o $@ $<

clean:
	rm -fr ElectronFlowJNI_n $(OBJS) $(EXTRA_CLEAN)
