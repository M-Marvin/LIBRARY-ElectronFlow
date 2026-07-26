package tvnlnna.solver;

import java.util.function.Consumer;
import org.netlib.util.intW;
import com.github.fommil.netlib.LAPACK;
import de.m_marvin.unimat.impl.MatrixNd;
import tvnlnna.NetworkSolverException;
import tvnlnna.NodalMatrixStampException;
import tvnlnna.nodal.NodalNetwork;
import tvnlnna.nodal.NodalNetwork.StampingContext.StampingMode;

public class NodalNetworkSolver_LAPACK extends NodalNetworkSolver {

	private final LAPACK lapack;
	
	private Consumer<String> debugOut;
	
	private NodalNetwork network;
	private double simtime = 0.0;
	private boolean initflag = false;
	private int nethash;
	
	private int limIter = 100;
	//private double limSingular = 1E-8;
	private double limIdRel = 0.001;
	private double limIdAbs = 0.0001;
	private double limUdRel = 0.001;
	private double limUdAbs = 0.0001;

	public NodalNetworkSolver_LAPACK(LAPACK lapack) {
		this.lapack = lapack;
	}
	
	public static NodalNetworkSolver_LAPACK standard() {
		return new NodalNetworkSolver_LAPACK(LAPACK.getInstance());
	}
	
	@Override
	public NodalNetworkSolver_LAPACK absUtol(double lim) {
		this.limUdAbs = lim;
		return this;
	}

	@Override
	public NodalNetworkSolver_LAPACK absItol(double lim) {
		this.limIdAbs = lim;
		return this;
	}

	@Override
	public NodalNetworkSolver_LAPACK relUtol(double lim) {
		this.limUdRel = lim;
		return this;
	}

	@Override
	public NodalNetworkSolver_LAPACK relItol(double lim) {
		this.limIdRel = lim;
		return this;
	}

//	@Override
//	public NodalNetworkSolver_LAPACK limSingular(double lim) {
//		this.limSingular = lim;
//		return this;
//	}

	@Override
	public NodalNetworkSolver_LAPACK iterLim(int lim) {
		this.limIter = lim;
		return this;
	}

	@Override
	public NodalNetworkSolver_LAPACK debug(Consumer<String> logOut) {
		this.debugOut = logOut;
		return this;
	}

	@Override
	public NodalNetworkSolver_LAPACK setNetwork(NodalNetwork network) {
		this.network = network;
		return this;
	}

	@Override
	public String toString() {
		return String.format("NetworkUniversalSolver{ ITERLIM = %d, UDABS = %f, UDREL = %f, IDABS = %f, IDREL = %f }", this.limIter, this.limUdAbs, this.limUdRel, this.limIdAbs, this.limIdRel);
	}
	
	@Override
	public NodalNetworkSolver_LAPACK resetAndClearSimulation() {
		this.network.setSystemMatrix_x(null);
		this.initflag = false;
		return this;
	}

	@Override
	public NodalNetworkSolver_LAPACK resetAndInitSimulation() {
		this.network.setSystemMatrix_x(null);
		this.initflag = true;
		return this;
	}

	@Override
	public NodalNetworkSolver_LAPACK setSimulationTime(double time) {
		this.simtime = time;
		return this;
	}

	@Override
	public double getSimulationTime() {
		return this.simtime;
	}
	
	private void log(String msg, Object... args) {
		if (this.debugOut != null)
			this.debugOut.accept(String.format(msg, args));
	}
	
	private void qz(MatrixNd A, MatrixNd B, MatrixNd T, MatrixNd S, MatrixNd Q, MatrixNd Z) throws NetworkSolverException {
		
		T.setI(A);
		S.setI(B);
		
		int N = A.height();
		double[] Aarr = A.getArray(false);
		double[] Barr = B.getArray(false);
		double[] Qarr = new double[N*N];
		double[] Zarr = new double[N*N];
		double[] work = new double[8*N+16];
		double[] alphaR = new double[N];
		double[] alphaI = new double[N];
		double[] beta = new double[N];
		
		intW info = new intW(0);
		lapack.dgges(
				"V", 
				"V", 
				"N", 
				new Object(), 
				N, 
				Aarr, 
				A.width(), 
				Barr, 
				B.width(), 
				new intW(0), 
				alphaR, 
				alphaI, 
				beta, 
				Qarr, 
				Q.width(), 
				Zarr, 
				Z.width(), 
				work, 
				work.length, 
				null, 
				info);
		
		T.setArray(Aarr, false);
		S.setArray(Barr, false);
		Q.setArray(Qarr, true); // this effectively transposes the Q matrix, for some weird reason it comes out transposed, even tough VSR/Z should be the transposed one ... and idk why ...
		Z.setArray(Zarr, false);
		
		if (info.val != 0)
			throw new NetworkSolverException("qz transformation of matrices failed: LAPACK INFO = " + info.val);
		
	}
	
	private void sv(MatrixNd A, MatrixNd x, MatrixNd B) throws NetworkSolverException {
		
		int N = A.height();
		double[] Aarr = new double[A.height() * A.width()];
		double[] Barr = new double[B.height()];
		System.arraycopy(A.getArray(false), 0, Aarr, 0, Aarr.length);
		System.arraycopy(B.getArray(false), 0, Barr, 0, Barr.length);
		int[] pivot = new int[N];
		
		intW info = new intW(0);
		lapack.dgesv(
				N,
				x.width(),
				Aarr,
				N,
				pivot,
				Barr,
				N,
				info);
		
		x.setArray(Barr, false);
		
		if (info.val != 0)
			throw new NetworkSolverException("sv linear solver failed: LAPACK INFO = " + info.val + (info.val > 0 ? " system singular" : ""));
		
	}
	
	/**
	 * Checks weather the changes in between two solution vectors for the network are small enough to satisfy the convergence tolerances.
	 * @param x1 The first solution vector
	 * @param x2 The second solution vector
	 * @return true if the conditions for convergence are satisfied
	 */
	private boolean checkConvergence(MatrixNd x1, MatrixNd x2) {
		double[] a = x1.getArray(false);
		double[] b = x2.getArray(false);
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
	
	private void solveTimeVariant(double timestep) throws NetworkSolverException {
		
		if (this.network.isNonLinear()) {

			log("solve time variant / start non linear itterative solver ...");
			
			// working variables
			int N = this.network.getSystemMatrix_x().height();
			MatrixNd T = new MatrixNd(N, N);
			MatrixNd S = new MatrixNd(N, N);
			MatrixNd Q = new MatrixNd(N, N);
			MatrixNd Z = new MatrixNd(N, N);
			
			// load last result as starting point for non linear approximation
			MatrixNd x0 = this.network.getSystemMatrix_x();
			
			// prepare next solution vector
			MatrixNd x = new MatrixNd(1, N);
			this.network.setSystemMatrix_x(x);
			
			// run iterative non linear approximation for next step
			MatrixNd xl = null;
			for (int iter = 0; iter < this.limIter; iter++) {
				
				try {
					
					log("solve time variant / iteration: %d / %d", iter + 1, this.limIter);
					var ctx = this.network.stampMatrices(StampingMode.FULL_MATRICES, this.simtime, iter);
					
					qz(this.network.getSystemMatrix_A(), this.network.getSystemMatrix_E(), T, S, Q, Z);
					MatrixNd M = S.scalarDiv(timestep).addI(T);
					MatrixNd v = S.scalarDiv(timestep).mul(x0).addI(Q.mul(this.network.getSystemMatrix_z()));
					sv(M, x, v);
					x.setI(Z.mul(x));

					this.network.updateElementParameters(ctx);
					
					if (xl != null && checkConvergence(xl, x)) {
						log("solve time variant / convergence detected, solution found");
						this.network.setSystemMatrix_x(x);
						return;
					}
					
					if (xl != null)
						log("solve time variant / no convergence, continue ...");
					if (xl == null)
						xl = x.copy();
					else
						xl.setI(x);
					
				} catch (NodalMatrixStampException | NetworkSolverException e) {
					log("error: non linear time variant solver failed at itteration %d", iter + 1);
					throw new NetworkSolverException("unable to solve iteration: ITER = " + (iter + 1), e);
				}
				
			}

			log("solve time variant / error: iteration limit reached");
			throw new NetworkSolverException("unable to solve non-linearity: no convergence within limits: " + toString());
			
		} else {
			
			try {

				log("solve time variant / start linear solver ...");
				
				// working variables
				int N = this.network.getSystemMatrix_x().height();
				MatrixNd T = new MatrixNd(N, N);
				MatrixNd S = new MatrixNd(N, N);
				MatrixNd Q = new MatrixNd(N, N);
				MatrixNd Z = new MatrixNd(N, N);

				// load last result as starting point for non linear approximation
				MatrixNd x0 = this.network.getSystemMatrix_x();
				
				var ctx = this.network.stampMatrices(StampingMode.FULL_MATRICES, this.simtime, 0);
				
				qz(this.network.getSystemMatrix_A(), this.network.getSystemMatrix_E(), T, S, Q, Z);
				MatrixNd M = S.scalarDiv(timestep).addI(T);
				MatrixNd v = S.scalarDiv(timestep).mul(x0).addI(Q.mul(this.network.getSystemMatrix_z()));
				sv(M, this.network.getSystemMatrix_x(), v);
				this.network.getSystemMatrix_x().setI(Z.mul(this.network.getSystemMatrix_x()));
				
				this.network.updateElementParameters(ctx);
				
				log("solve time variant / solution found");
				
			} catch (NetworkSolverException | NodalMatrixStampException e) {
				log("solve time variant / error: linear solver failed");
				throw new NetworkSolverException("unable to solve linear", e);
			}
			
		}
		
	}
	
	private void solveTimeInvariant() throws NetworkSolverException {
		
		if (this.network.isNonLinear()) {
			
			try {
					
				log("solve time invariant / start non linear itterative solver ...");
				
				// run iterative non linear approximation for next step
				MatrixNd xl = null;
				for (int iter = 0; iter < this.limIter; iter++) {
					
					try {
	
						log("solve time invariant / iteration: %d / %d", iter + 1, this.limIter);
						var ctx = this.network.stampMatrices(StampingMode.TIME_INVARIANT, this.simtime, iter);
						
						sv(this.network.getSystemMatrix_A(), this.network.getSystemMatrix_x(), this.network.getSystemMatrix_z());
						
						System.out.println(this.network.getSystemMatrix_z() + " -> \n" + this.network.getSystemMatrix_x());
						
						this.network.updateElementParameters(ctx);
						
						if (xl != null && checkConvergence(xl, this.network.getSystemMatrix_x())) {
							log("solve time invariant / convergence detected, solution found");
							return;
						}

						if (xl != null)
							log("solve time invariant / no convergence, continue ...");
						if (xl == null)
							xl = this.network.getSystemMatrix_x().copy();
						else
							xl.setI(this.network.getSystemMatrix_x());
						
					} catch (NodalMatrixStampException | NetworkSolverException e) {
						log("solve time invariant / error: non linear solver failed at itteration %d", iter + 1);
						throw new NetworkSolverException("unable to solve iteration: ITER = " + (iter + 1), e);
					}
					
				}
	
				log("solve time invariant / error: iteration limit reached");
				throw new NetworkSolverException("unable to solve non-linearity: no convergence within limits: " + toString());

			} catch (NetworkSolverException e) {
				log("solve time invariant / error: unable to compute non linear solution");
				throw new NetworkSolverException("unable to solve non linear system", e);
			}
			
		} else {
			
			try {

				log("solve time invariant / start linear solver ...");
				
				var ctx =this.network.stampMatrices(StampingMode.TIME_INVARIANT, this.simtime, 0);
				
				sv(this.network.getSystemMatrix_A(), this.network.getSystemMatrix_x(), this.network.getSystemMatrix_z());
				
				this.network.updateElementParameters(ctx);
				
				log("solve time invariant / solution found");
				
			} catch (NodalMatrixStampException | NetworkSolverException e) {
				log("solve time invariant / error: unable to compute linear solution");
				throw new NetworkSolverException("unable to solve linear system", e);
			}
			
		}
		
	}
	
	@Override
	public void step(double timestep) throws NetworkSolverException {
		
		if (this.network.isTimeVariant()) {
			
			if (this.network.getSystemMatrix_x() == null) {
				
				// if no previous state vector exists, compute initial state
				if (this.initflag) {
					this.initflag = false;
					
					try {
						log("solve time invariant init / initialize state from element parameters ...");
						this.network.stampMatrices(StampingMode.INIT_VECTOR, this.simtime, 0);
					} catch (NodalMatrixStampException e) {
						log("solve time invariant init / error: stamping initial state vector failed");
						throw new NetworkSolverException("unable to perform initial state vector stamping", e);
					}
				} else {
					try {
						log("solve time invariant init / initialize state trough time invariant step to find stable state ...");
						solveTimeInvariant();
					} catch (NetworkSolverException e) {
						log("solve time invariant init / error: solving for initial state vector failed");
						throw new NetworkSolverException("unable to perform initial state vector computation", e);
					}
				}
				
			} else {
				
				solveTimeVariant(timestep);
				
			}
			
			// advance simulation time
			this.simtime += timestep;
			
		} else {
			
			// only recompute if the solution is not already known
			if (this.network.getSystemMatrix_x() == null || this.nethash != this.network.hashCode()) {
				this.nethash = this.network.hashCode();
				
				solveTimeInvariant();
				
			}
			
		}
		
	}

}
