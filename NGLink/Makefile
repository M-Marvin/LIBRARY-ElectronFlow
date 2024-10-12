PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

BINDIR = $(PROJECT_ROOT)/../Binaries
OBJS = nglink.o nglinkimpl.o
OBJS_JNI = jni.o $(OBJS)
INCL = -I$(PROJECT_ROOT)/../External -I$(PROJECT_ROOT)/../JNGLink/build/generated/sources/headers/java/main -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32"
LIBS = -lstdc++exp
CFLAGS = -std=c++23

BUILD_MODE = prod

ifeq ($(BUILD_MODE),debug)
	CFLAGS += -g
else
	CFLAGS += -O2
	LDFLAGS = -static -static-libgcc -static-libstdc++
endif

all:	nglink_x64.dll nglink_jni_x64.dll

nglink_x64.dll:	$(OBJS)
	$(CXX) $(LDFLAGS) -o $(BINDIR)/$@ $^ $(LIBS) -shared
	$(EXTRA_CMDS)

nglink_jni_x64.dll:	$(OBJS_JNI)
	$(CXX) $(LDFLAGS) -o $(BINDIR)/$@ $^ $(LIBS) -shared
	$(EXTRA_CMDS)

%.o:	$(PROJECT_ROOT)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) $(INCL) -o $@ $<

clean:
	rm -fr $(BINDIR)/nglink_x64.dll $(BINDIR)/nglink_jni_x64.dll $(OBJS_JNI) $(EXTRA_CLEAN)
