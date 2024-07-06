PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

LIB_NAME = electronflow_jni_x64.dll
TARGET_DIR = "$(PROJECT_ROOT)..\ElectronFlowJNI\src\main\resources\natives\"

OBJS = ElectronFlowJNI_n.o
CFLAGS = -I"$(PROJECT_ROOT)..\ElectronFlowJNI\build\generated\sources\headers\java\main" -I"$(PROJECT_ROOT)..\ElectronFlow" -I"C:\Program Files\Java\jdk-17.0.10+7\include" -I"C:\Program Files\Java\jdk-17.0.10+7\include\win32"
LDFLAGS = -L"$(PROJECT_ROOT)..\ElectronFlow\build\default" -lelectronflow_x64

ifeq ($(BUILD_MODE),debug)
	CFLAGS += -g
else ifeq ($(BUILD_MODE),run)
	CFLAGS += -O2
endif

all:	ElectronFlowJNI_n

ElectronFlowJNI_n:	$(OBJS)
	$(CXX) -shared $(LDFLAGS) -o $(LIB_NAME) $^
	copy $(LIB_NAME) $(TARGET_DIR)

%.o:	$(PROJECT_ROOT)%.cpp
	$(CXX) -c $(CFLAGS) -o $@ $<

clean:
	rm -fr ElectronFlowJNI_n $(OBJS)
