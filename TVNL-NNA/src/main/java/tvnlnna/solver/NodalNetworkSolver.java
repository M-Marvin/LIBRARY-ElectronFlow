package tvnlnna.solver;

import java.util.function.Consumer;

import tvnlnna.NetworkSolverException;
import tvnlnna.nodal.NodalNetwork;

public abstract class NodalNetworkSolver {
	
	/**
	 * Configures the absolute node potential tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for potential U is computed trough: Utol = absUtol + relUtol * U
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public abstract NodalNetworkSolver absUtol(double lim);

	/**
	 * Configures the absolute branch current tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for current I is computed trough: Itol = absItol + relItol * I
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public abstract NodalNetworkSolver absItol(double lim);

	/**
	 * Configures the relative node potential tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for potential U is computed trough: Utol = absUtol + relUtol * U
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public abstract NodalNetworkSolver relUtol(double lim);

	/**
	 * Configures the relative branch current tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for current I is computed trough: Itol = absItol + relItol * I
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public abstract NodalNetworkSolver relItol(double lim);

//	/**
//	 * Configures the singularity limit.
//	 * The singularity value determines how close the network system of equation matrix is to singularity, the smaller the value, the close it is to singularity.
//	 * If the network is singular, this indicates a malformed network, and a solution may not be computed reliably.
//	 * @param lim The singularity limit, values below this will be counted as singular and the simulation may abort the simulation
//	 */
//	public abstract NodalNetworkSolver limSingular(double lim);

	/**
	 * Iteration limit for iterative non linear solver.
	 * If the iterative solver exceeds this limit, the simulation is aborted an an convergence error is thrown.
	 * @param lim The max number of iterations to execute before terminating
	 */
	public abstract NodalNetworkSolver iterLim(int lim);

	/**
	 * Configures an output for debug information produced during each simulation step.
	 * @param logOut A string consumer receiving individual debug output lines
	 */
	public abstract NodalNetworkSolver debug(Consumer<String> logOut);

	/**
	 * Sets the network the simulator should simulate each step.
	 * @param network The network to simulate
	 */
	public abstract NodalNetworkSolver setNetwork(NodalNetwork network);
	
	/**
	 * Resets the simulation by clearing the current solution vector of the last simulation step.
	 */
	public abstract NodalNetworkSolver resetAndClearSimulation();
	
	/**
	 * Resets the simulation by clearing the current solution vector of the last simulation step, and also tells the solver to use the current element parmaters as the starting state for the simulation.
	 */
	public abstract NodalNetworkSolver resetAndInitSimulation();
	
	/**
	 * Sets the current simulation time.
	 * The simulation time is incremented with the timestep every step, and passed to the element models.
	 * @param time The time value to set, the unit is defined by the application, but should usually be in seconds
	 */
	public abstract NodalNetworkSolver setSimulationTime(double time);
	
	/**
	 * Configures a ramp up period, in which the forcing vector parameters are ramped up from zero to nominal linearly.
	 * This ramp up does account for change over time in the variables, thus the final result might not be linear if the target value changed during ramp up.
	 * @param start the starting time of the ramp
	 * @param end the end time of the ramp
	 */
	public abstract NodalNetworkSolver setRampup(double start, double end);
	
	/**
	 * Returns the current simulation time.
	 * The simulation time is incremented with the timestep every step, and passed to the element models.
	 * @return the current simulation time which would be applied for the next step
	 */
	public abstract double getSimulationTime();
	
	/**
	 * Updates the simulation of the network by stepping forward in time and computing the next solution vector.
	 * If the network is time invariant, the function will indicate a steady state by returning true, further calls will not have any effect.
	 * If the network is time variant, the function will always return false and continue to update the simulation.
	 * @param timestep The timestep increment between the last step and the next one
	 * @throws NetworkSolverException
	 */
	public abstract void step(double timestep) throws NetworkSolverException;
	
}
