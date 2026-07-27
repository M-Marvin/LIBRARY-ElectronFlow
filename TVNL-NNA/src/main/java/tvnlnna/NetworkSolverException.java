package tvnlnna;

/**
 * Signals that a problem occurred during solving for the solution vector of an nodal network.
 */
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
