package tvnlnna.solver;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.ejml.interfaces.linsol.LinearSolverSparse;
import org.ejml.sparse.FillReducing;
import org.ejml.sparse.csc.factory.LinearSolverFactory_DSCC;

import de.m_marvin.unimat.impl.MatrixNd;
import de.m_marvin.univec.impl.Vec2i;
import tvnlnna.NetworkSolverException;
import tvnlnna.NodalMatrixStampException;
import tvnlnna.nodal.NodalNetwork;
import tvnlnna.nodal.NodalNetwork.StampingContext.StampingMode;

public class NodalNetworkSolver {

	private Consumer<String> debugOut;
	
	private NodalNetwork network;
	private double simtime;
	private int stampHash;
	private MatrixNd M;
	private MatrixNd v;
	
	private final LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj> sparseLinearSolver;
	private final LinearSolverDense<DMatrixRMaj> denseLinearSolver;
	
	private int limIter = 100;
	private double limSingular = 1E-8;
	private double limIdRel = 0.001;
	private double limIdAbs = 0.0001;
	private double limUdRel = 0.001;
	private double limUdAbs = 0.0001;

	public NodalNetworkSolver() {
		this.sparseLinearSolver = LinearSolverFactory_DSCC.lu(FillReducing.NONE);
		this.denseLinearSolver = LinearSolverFactory_DDRM.pseudoInverse(true);
	}
	
	/**
	 * Configures a new network solver instance with default settings.
	 */
	public static NodalNetworkSolver standard() {
		return new NodalNetworkSolver();
	}
	
	/**
	 * Configures the absolute node potential tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for potential U is computed trough: Utol = absUtol + relUtol * U
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public NodalNetworkSolver absUtol(double lim) {
		this.limUdAbs = lim;
		return this;
	}

	/**
	 * Configures the absolute branch current tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for current I is computed trough: Itol = absItol + relItol * I
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public NodalNetworkSolver absItol(double lim) {
		this.limIdAbs = lim;
		return this;
	}

	/**
	 * Configures the relative node potential tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for potential U is computed trough: Utol = absUtol + relUtol * U
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public NodalNetworkSolver relUtol(double lim) {
		this.limUdRel = lim;
		return this;
	}

	/**
	 * Configures the relative branch current tolerance setting of the solver.<br>
	 * The tolerance is used to determine convergence when solving nonlinear networks iteratively.<br>
	 * The tolerance for current I is computed trough: Itol = absItol + relItol * I
	 * @param lim The tolerance limit (highest value accepted for convergence)
	 */
	public NodalNetworkSolver relItol(double lim) {
		this.limIdRel = lim;
		return this;
	}
	
	/**
	 * Configures the singularity limit.
	 * The singularity value determines how close the network system of equation matrix is to singularity, the smaller the value, the close it is to singularity.
	 * If the network is singular, this indicates a malformed network, and a solution may not be computed reliably.
	 * @param lim The singularity limit, values below this will be counted as singular and the simulation may abort the simulation
	 */
	public NodalNetworkSolver limSingular(double lim) {
		this.limSingular = lim;
		return this;
	}
	
	/**
	 * Iteration limit for iterative non linear solver.
	 * If the iterative solver exceeds this limit, the simulation is aborted an an convergence error is thrown.
	 * @param lim The max number of iterations to execute before terminating
	 */
	public NodalNetworkSolver iterLim(int lim) {
		this.limIter = lim;
		return this;
	}
	
	/**
	 * Configures an output for debug information produced during each simulation step.
	 * @param logOut A string consumer receiving individual debug output lines
	 */
	public NodalNetworkSolver debug(Consumer<String> logOut) {
		this.debugOut = logOut;
		return this;
	}

	@Override
	public String toString() {
		return String.format("NetworkUniversalSolver{ ITERLIM = %d, UDABS = %f, UDREL = %f, IDABS = %f, IDREL = %f }", this.limIter, this.limUdAbs, this.limUdRel, this.limIdAbs, this.limIdRel);
	}
	
	protected void log(String msg, Object... args) {
		if (this.debugOut != null)
			this.debugOut.accept(String.format(msg, args));
	}
	
	/**
	 * Sets the network the simulator should simulate each step.
	 * @param network The network to simulate
	 */
	public NodalNetworkSolver setNetwork(NodalNetwork network) {
		if (network == null)
			throw new NullPointerException("network can not be null");
		if (network.getElements().size() == 0)
			throw new NullPointerException("network can not be empty");
		this.network = network;
		return this;
	}
	
	/**
	 * Resets the simulation by clearing the current solution vector of the last simulation step.
	 * The vector is stored inside the network, this call has the same effect as {@code NetworkUniversalSolver#setSystemMatrix_x(null)}
	 */
	public NodalNetworkSolver resetSimulation() {
		this.network.setSystemMatrix_x(null);
		return this;
	}
	
	public NodalNetworkSolver setSimulationTime(double time) {
		this.simtime = time;
		return this;
	}
	
	/**
	 * Internal helper method, solves the system of linear equations: A * x = z for solution vector x
	 * @param A The coefficient matrix of the system of equations
	 * @param z The constant vector of the system
	 * @return The solution vector x
	 * @throws NetworkSolverException
	 */
	private MatrixNd solveLinear(MatrixNd A, MatrixNd z) throws NetworkSolverException {
		log("solve linear / initialize solver with network matrix:\n%s", A);
		
		MatrixNd res;
		if (A.isSparse()) {
			
			log("solve linear / choose sparse solver for sparse matrix system ...");
			
			Map<Vec2i, Double> entries = A.getNonZeroes();
			DMatrixSparseCSC Asol = new DMatrixSparseCSC(A.width(), A.height(), entries.size());
			entries.forEach((pos, val) -> Asol.set(pos.y, pos.x, val));
			
			if (!this.sparseLinearSolver.setA(Asol)) {
				log("solve linear / error: matrix singular");
				throw new NetworkSolverException("unabel to initialize linear solver: system matrix is singular");
			} else {
				double qual = this.sparseLinearSolver.quality();
				log("solve linear / matrix non-singularity: %f", qual);
				if (qual < this.limSingular) {
					log("error: matrix nearly singular: %s < %s", Double.toString(qual), Double.toString(this.limSingular));
					throw  new NetworkSolverException("unabel to initialize linear solver: system matrix nearly singular: QUAL = " + qual);
				}
			}
			log("solve linear / solver initialized");
			
			log("solve linear / solve equation system for vector:\n%s", z);
			DMatrixRMaj x = new DMatrixRMaj(A.height(), 1);
			this.sparseLinearSolver.solve(new DMatrixRMaj(z.get2DArray()), x);
			res = new MatrixNd(x.get2DData());
			
		} else {
			
			log("solve linear / choose dense solver for dense matrix system ...");
			
			DMatrixRMaj Asol = new DMatrixRMaj(A.get2DArray());
			
			this.denseLinearSolver.setA(Asol);
			// the dense solver uses the pseudo inverse, since it also processes the time variant matrices, thus it will never fail because of singularity
//			if (!this.denseLinearSolver.setA(Asol)) {
//				log("error: matrix singular");
//				throw new NetworkSolverException("unabel to initialize linear solver: system matrix is singular");
//			} else {
//				double qual = this.denseLinearSolver.quality();
//				log("matrix non-singularity: %f", qual);
//				if (qual < this.limSingular) {
//					log("error: matrix nearly singular: %s < %s", Double.toString(qual), Double.toString(this.limSingular));
//					throw  new NetworkSolverException("unabel to initialize linear solver: system matrix nearly singular: QUAL = " + qual);
//				}
//			}
			log("solve linear / solver initialized");

			log("solve linear / solve equation system for vector:\n%s", z);
			DMatrixRMaj x = new DMatrixRMaj(A.height(), 1);
			this.denseLinearSolver.solve(new DMatrixRMaj(z.get2DArray()), x);
			res = new MatrixNd(x.get2DData());
			
		}
		
		log("solve linear / solution found:\n%s", res);
		return res;
		
	}
	
	/**
	 * Checks weather the changes in between two solution vectors for the network are small enough to satisfy the convergence tolerances.
	 * @param x1 The first solution vector
	 * @param x2 The second solution vector
	 * @return true if the conditions for convergence are satisfied
	 */
	private boolean checkConvergence(MatrixNd x1, MatrixNd x2) {
		double[] a = x1.getArray();
		double[] b = x2.getArray();
		for (int i = 0; i < a.length; i++) {
			double diff = Math.abs(a[i] - b[i]);
			if (i < this.network.nodeCount()) {
				if (diff > diff * limUdRel + limUdAbs) return false;
			} else {
				if (diff > diff * limIdRel + limIdAbs) return false;
			}
		}
		return true;
	}
	
	/**
	 * Solves the network assuming no time variance (ignoring the E differential matrix).
	 * The resulting solution vector represents a stable state solution for the network assuming no change over time.
	 * @throws NetworkSolverException
	 */
	private void solveTimeInvariant() throws NetworkSolverException {
		
		if (this.network.isNonLinear()) {
			
			log("solve time invariant / start non linear itterative solver ...");
			
			MatrixNd x0 = this.network.getSystemMatrix_x();
			for (int iter = 0; iter < this.limIter; iter++) {
				try {
					
					log("solve time invariant / iteration: %d / %d", iter + 1, this.limIter);
					this.network.stampMatrices(StampingMode.TIME_INVARIANT, this.simtime, iter);
					this.network.setSystemMatrix_x(solveLinear(this.network.getSystemMatrix_A(), this.network.getSystemMatrix_z()));
					

					log("solve time invariant / check for convergence ...");
					if (x0 != null) {
						if (checkConvergence(x0, this.network.getSystemMatrix_x())) {
							log("solve time invariant / convergence detected, solution found");
							return;
						}
					}
					log("solve time invariant / no convergence, continue ...");
					
					x0 = this.network.getSystemMatrix_x();

				} catch (NetworkSolverException | NodalMatrixStampException e) {
					log("solve time invariant / error: non linear solver failed at itteration %d", iter + 1);
					throw new NetworkSolverException("unable to solve iteration: ITER = " + (iter + 1), e);
				}
			}
			
			log("solve time invariant / error: iteration limit reached");
			throw new NetworkSolverException("unable to solve non-linearity: no convergenze within limits: " + toString());
			
		} else {
			
			try {

				log("solve time invariant / start linear solver ...");
				
				this.network.stampMatrices(StampingMode.TIME_INVARIANT, this.simtime, 0);
				this.network.setSystemMatrix_x(solveLinear(this.network.getSystemMatrix_A(), this.network.getSystemMatrix_z()));

				log("solve time invariant / linear solution found");
				
			} catch (NetworkSolverException | NodalMatrixStampException e) {
				log("solve time invariant / error: linear solver failed");
				throw new NetworkSolverException("unable to solve linear", e);
			}
			
		}
		
	}

	/**
	 * Solves the network with respect to changes over time.
	 * The solution vector is computed based on the vector of the previous step, and a timestep parameter.
	 * @param xt The solution vector of the previous step
	 * @param dt The timestep increment for the next step
	 * @throws NetworkSolverException
	 */
	private void solveTimeVariant(MatrixNd xt, double dt) throws NetworkSolverException {
		
		if (this.network.isNonLinear()) {
			
			log("solve time variant / start non linear time variant itterative solver ...");
			
			MatrixNd x0 = this.network.getSystemMatrix_x(); // reuse previous step result (xt) as starting point
			MatrixNd I = new MatrixNd(xt.height(), xt.height()).identityI();
			for (int iter = 0; iter < this.limIter; iter++) {
				try {
					
					log("solve time variant / iteration: %d / %d", iter + 1, this.limIter);
					this.network.stampMatrices(StampingMode.FULL_MATRICES, this.simtime, iter);
					
					DMatrixRMaj Esol = new DMatrixRMaj(this.network.getSystemMatrix_E().get2DArray());
					if (!this.denseLinearSolver.setA(Esol)) {
						log("solve time variant / error: pseudo inverse solver failed to initialize");
						throw new NetworkSolverException("unabel to initialize pseudo inverse solver");
					}
					this.denseLinearSolver.invert(Esol);
					MatrixNd Einv = new MatrixNd(Esol.get2DData());
					
					MatrixNd Et = Einv.scalarMul(dt);
					this.M = Et.mul(this.network.getSystemMatrix_A()).addI(I);
					this.v = Et.mul(this.network.getSystemMatrix_z());
					
					this.network.setSystemMatrix_x(solveLinear(M, v.add(xt)));
					
					log("solve time variant / check for convergence ...");
					if (x0 != null) {
						if (checkConvergence(x0, this.network.getSystemMatrix_x())) {
							log("convergence detected, solution found");
							return;
						}
					}
					log("solve time variant / no convergence, continue ...");
					
					x0 = this.network.getSystemMatrix_x();

				} catch (NetworkSolverException | NodalMatrixStampException e) {
					log("error: non linear time variant solver failed at itteration %d", iter + 1);
					throw new NetworkSolverException("unable to solve iteration: ITER = " + (iter + 1), e);
				}
			}
			
			log("solve time variant / error: iteration limit reached");
			throw new NetworkSolverException("unable to solve non-linearity: no convergenze within limits: " + toString());
			
		} else {
			
			try {

				log("solve time variant / start linear time variant solver ...");
				
				int networkHash = this.network.hashCode();
				if (networkHash != this.stampHash) {
					this.stampHash = networkHash;

					log("solve time variant / change in linear network detected, reinitializing ...");
					this.network.stampMatrices(StampingMode.FULL_MATRICES, this.simtime, 0);

					DMatrixRMaj Esol = new DMatrixRMaj(this.network.getSystemMatrix_E().get2DArray());
					if (!this.denseLinearSolver.setA(Esol)) {
						log("solve time variant / error: pseudo inverse solver failed to initialize");
						throw new NetworkSolverException("unabel to initialize pseudo inverse solver");
					}
					this.denseLinearSolver.invert(Esol);
					MatrixNd Einv = new MatrixNd(Esol.get2DData());

					MatrixNd Et = Einv.scalarMul(dt);
					MatrixNd I = new MatrixNd(Et.height(), Et.height()).identityI();
					this.M = Et.mul(this.network.getSystemMatrix_A()).addI(I);
					this.v = Et.mul(this.network.getSystemMatrix_z());
				}
				
				this.network.setSystemMatrix_x(solveLinear(M, v.add(xt)));

				log("solve time variant / linear time variant solution found");
				
			} catch (NetworkSolverException | NodalMatrixStampException e) {
				log("solve time variant / error: linear time variant solver failed");
				throw new NetworkSolverException("unable to solve linear", e);
			}
			
		}
		
	}
	
	/**
	 * Updates the simulation of the network by stepping forward in time and computing the next solution vector.
	 * If the network is time invariant, the function will indicate a steady state by returning true, further calls will not have any effect.
	 * If the network is time variant, the function will always return false and continue to update the simulation.
	 * @param timestep The timestep increment between the last step and the next one
	 * @return true if a time invariant network was detected and the never changing solution vector was computed
	 * @throws NetworkSolverException
	 */
	public boolean step(double timestep) throws NetworkSolverException {
		
		try {

			if (this.network.isTimeVariant()) {
				
				log("perform time variant simulation step: dt = %f", timestep);
				
				if (!this.network.hasValidSolutionVector()) {
					log("perform time invariant simulation for initilazation ...");
					resetSimulation();
					solveTimeInvariant();
				} else {
					solveTimeVariant(this.network.getSystemMatrix_x(), timestep);	
				}

				this.simtime += timestep;
				return false;
				
			} else {
				
				// check if result is already set, no need to recompute
				if (this.network.hasValidSolutionVector())
					return true;
				
				log("perform time invariant simulation");
				resetSimulation();
				solveTimeInvariant();

				this.simtime += timestep;
				return true;
				
			}
			
		} catch (NetworkSolverException e) {
			log("error: simulation step failed");
			throw new NetworkSolverException("unable to perform simulation step", e);
		}
		
	}
	
}
