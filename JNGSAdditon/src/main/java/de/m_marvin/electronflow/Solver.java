package de.m_marvin.electronflow;

import java.util.function.Consumer;

public class Solver {

	private static String ngspiceNativeAutodetect;
	private static Error loadException;
	
	static {
		try {
			NativeLoader.setTempLibFolder(System.getProperty("java.io.tmpdir") + "/jelectronflow");
			NativeLoader.setLibLoadConfig("/libload_electronflow.cfg");
			NativeLoader.loadNative("nglink");
			NativeLoader.loadNative("electronflow");
			NativeLoader.loadNative("libomp");
			ngspiceNativeAutodetect = NativeLoader.getNative("ngspice");
		} catch (Error e) {
			loadException = e;
		}
	}
	
	private final long classid;
	private Consumer<String> logfunc;
	
	public Solver() {
		if (loadException != null) throw loadException;
		this.classid = this.hashCode();
	}
	
	protected boolean classidErrorCodes(int code) {
		switch (code) {
		case 1:
			return true;
		case 0:
			return false;
		case -3:
			logfunc.accept("Error-Code returned: jni-error, c-class not bound!");
			return false;
		default:
			logfunc.accept("Error-Code returned: unknown error!");
			return false;
		}
	}
	
	private native int initElectronFlow(long classid, Consumer<String> logout);
	public boolean attachElectronFlow() {
		return attachElectronFlow(System.err::println);
	}
	public boolean attachElectronFlow(Consumer<String> logout) {
		this.logfunc = logout;
		if (!classidErrorCodes(initElectronFlow(this.classid, logout))) {
			setLibName(ngspiceNativeAutodetect);
			return true;
		}
		return false;
	}
	
	private native int detachElectronFlow(long classid);
	public void detachElectronFlow() {
		classidErrorCodes(detachElectronFlow(this.classid));
	}
	
	private native int setLibName(long classid, String libname);
	public void setLibName(String libname) {
		classidErrorCodes(setLibName(this.classid, libname));
	}
	
	private native int setLogger(long classid, Consumer<String> logout);
	public void setLogger(Consumer<String> logout) {
		this.logfunc = logout;
		classidErrorCodes(setLogger(this.classid, logout));
	}
	
	private native boolean upload(long classid, String netlist);
	public boolean upload(String netlist) {
		return upload(this.classid, netlist);
	}
	
	private native boolean executeList(long classid);
	public boolean executeList() {
		return executeList(this.classid);
	}
	
	private native boolean execute(long classid, String command);
	public boolean execute(String command) {
		return execute(this.classid, command);
	}
	
	private native String printData(long classid);
	public String printData() {
		return printData(this.classid);
	}
	
	private native String netFiltered(long classid);
	public String netFiltered() {
		return netFiltered(this.classid);
	}
	
}
