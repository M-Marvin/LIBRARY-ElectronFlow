package de.m_marvin.electronflow;

public class NativeElectronFlow {
	
	public static native long makeInstance_n();
	public static native void destroyInstance_n(long handle);
	
	public static native boolean loadNetList_n(long handle, String netList);
	public static native boolean executeCommand_n(long handle, String command);
	
}
