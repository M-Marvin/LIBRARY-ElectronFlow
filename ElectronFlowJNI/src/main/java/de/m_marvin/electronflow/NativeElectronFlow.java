package de.m_marvin.electronflow;

public class NativeElectronFlow {
	
	static {
		NativeLoader.setTempLibFolder(System.getProperty("java.io.tmpdir") + "/electronflow");
		NativeLoader.setLibLoadConfig("/libload_electronflow.cfg");
		NativeLoader.loadNative("electronflow");
		NativeLoader.loadNative("electronflowjni");
	}
	
	protected native void makeInstance_n();
	protected native void destroyInstance_n();
	
	public static class Node {
		
		public Node(String name, double charge) {
			this.name = name;
			this.charge = charge;
		}
		
		public final String name;
		public double charge;
	}
	
	public static class Element {
		
		public Element(String name, double transferCharge) {
			this.name = name;
			this.transferCharge = transferCharge;
		}
		
		public final String name;
		public double transferCharge;
	}
	
	@FunctionalInterface
	public static interface StepCallback {
		public void stepData(double simtime, Node[] nodes, Element[] elements, double nodecharge, double timestep);
	}

	@FunctionalInterface
	public static interface FinalCallback {
		public void finalData(Node[] nodes, Element[] elements, double nodecharge, double timestep);
	}

	protected native void printVersionInfo_n();
	protected native void setCallbacks_n(StepCallback stepCallback, FinalCallback finalCallback);
	protected native boolean loadNetList_n(String netList);
	protected native boolean loadAndRunNetList_n(String netList);
	protected native boolean stepSimulation_n(double nodeCapacity, double timestep, double simulateTime);
	protected native void printNodeVoltages_n(String refNodeName);
	protected native void controllCommand_n(String... args);
	
}
