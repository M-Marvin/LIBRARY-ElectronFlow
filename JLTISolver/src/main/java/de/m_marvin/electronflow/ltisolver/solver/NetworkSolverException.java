package de.m_marvin.electronflow.ltisolver.solver;

public class NetworkSolverException extends Exception {
	
	private static final long serialVersionUID = -3915155568621626485L;

	public NetworkSolverException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public NetworkSolverException(String msg) {
		super(msg);
	}

	public NetworkSolverException(Throwable cause) {
		super(cause);
	}
	
}
