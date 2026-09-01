package tvnlnna.solver;

import java.util.function.Consumer;

import tvnlnna.NetworkSolverException;
import tvnlnna.nodal.NodalNetwork;

/**
 * The network solver implements the algorithm which implements the non linear and the differential algebraic equation solver to run the simulation.
 * There are different implementations possible, tough currently only {@link NodalNetworkSolver<N, E>_LAPACK} is supplied by default.
 */
public abstract class NodalNetworkSolver<N, E> {
	
	/**
	 * Configures the absolute node potential tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for potential U is computed trough: Utol = absUtol + relUtol * U
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public abstract NodalNetworkSolver<N, E> absUtol(double lim);

	/**
	 * Configures the absolute branch current tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for current I is computed trough: Itol = absItol + relItol * I
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public abstract NodalNetworkSolver<N, E> absItol(double lim);

	/**
	 * Configures the relative node potential tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for potential U is computed trough: Utol = absUtol + relUtol * U
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public abstract NodalNetworkSolver<N, E> relUtol(double lim);

	/**
	 * Configures the relative branch current tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for current I is computed trough: Itol = absItol + relItol * I
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public abstract NodalNetworkSolver<N, E> relItol(double lim);

	/**
	 * Iteration limit for iterative non linear solver.
	 * If the iterative solver exceeds this limit, the simulation is aborted an an convergence error is thrown.
	 * @param lim The max number of iterations to execute before terminating
	 */
	public abstract NodalNetworkSolver<N, E> iterLim(int lim);

	/**
	 * Configures an output for debug information produced during each simulation step.
	 * @param logOut A string consumer receiving individual debug output lines
	 */
	public abstract NodalNetworkSolver<N, E> debug(Consumer<String> logOut);

	/**
	 * Sets the network the simulator should simulate each step.
	 * @param network The network to simulate
	 */
	public abstract NodalNetworkSolver<N, E> setNetwork(NodalNetwork<N, E> network);
	
	/**
	 * Resets the simulation by clearing the current solution vector of the last simulation step.
	 */
	public abstract NodalNetworkSolver<N, E> resetAndClearSimulation();
	
	/**
	 * Resets the simulation by clearing the current solution vector of the last simulation step, and also tells the solver to use the current element parmaters as the starting state for the simulation.
	 */
	public abstract NodalNetworkSolver<N, E> resetAndInitSimulation();
	
	/**
	 * Sets the current simulation time.
	 * The simulation time is incremented with the timestep every step, and passed to the element models.
	 * @param time The time value to set, the unit is defined by the application, but should usually be in seconds
	 */
	public abstract NodalNetworkSolver<N, E> setSimulationTime(double time);
	
	/**
	 * Configures a ramp up period, in which the forcing vector parameters are ramped up from zero to nominal linearly.
	 * This ramp up does account for change over time in the variables, thus the final result might not be linear if the target value changed during ramp up.
	 * @param start the starting time of the ramp
	 * @param end the end time of the ramp
	 */
	public abstract NodalNetworkSolver<N, E> setRampup(double start, double end);
	
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
	 * @param timestep The time step increment between the last step and the next one
	 * @throws NetworkSolverException
	 */
	public abstract void step(double timestep) throws NetworkSolverException;
	
}
