package de.m_marvin.electronflow;

public class ElectronFlow extends NativeElectronFlow {
	
	public ElectronFlow() {
		makeInstance_n();
		printVersionInfo_n();
	}
	
	public void destroy() {
		destroyInstance_n();
	}
	
	public void printVersionInfo() {
		printVersionInfo_n();
	}
	public void setCallbacks(StepCallback stepCallback, FinalCallback finalCallback) {
		setCallbacks_n(stepCallback, finalCallback);
	}
	public boolean loadNetList(String netList) {
		return loadNetList_n(netList);
	}
	public boolean loadAndRunNetList(String netList) {
		return loadAndRunNetList_n(netList);
	}
	public void resetSimulation() {
		resetSimulation_n();
	}
	public boolean stepSimulation(double nodeCapacity, double timestep, double simulateTime, boolean enableLimits) {
		return stepSimulation_n(nodeCapacity, timestep, simulateTime, enableLimits);
	}
	public void printNodeVoltages(String refNodeName) {
		printNodeVoltages_n(refNodeName);
	}
	public void printElementCurrents() {
		printElementCurrents_n();
	}
	public void controllCommand(String... args) {
		controllCommand_n(args);
	}
	
}
