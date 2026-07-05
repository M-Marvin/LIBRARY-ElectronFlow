package de.m_marvin.electronflow.nltisolver.solver;

import java.util.Map;
import java.util.function.Consumer;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.interfaces.linsol.LinearSolverSparse;
import org.ejml.sparse.FillReducing;
import org.ejml.sparse.csc.factory.LinearSolverFactory_DSCC;

import de.m_marvin.electronflow.nltisolver.network.IndexedNetwork;
import de.m_marvin.electronflow.nltisolver.network.IndexedNetwork.StampingContext.StampingMode;
import de.m_marvin.unimat.impl.MatrixNd;
import de.m_marvin.univec.impl.Vec2i;

public class NetworkSolver {
	
	private final LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj> linearSolver;
	private IndexedNetwork network;
	private Consumer<String> debugOut;
	
	private int limIter = 100;
	private double limSingular = 1E-8;
	private double limIdRel = 0.001;
	private double limIdAbs = 0.0001;
	private double limUdRel = 0.001;
	private double limUdAbs = 0.0001;
	
	public NetworkSolver() {
		this.linearSolver = LinearSolverFactory_DSCC.lu(FillReducing.NONE);
	}
	
	public static NetworkSolver standard() {
		return new NetworkSolver();
	}
	
	public NetworkSolver absUtol(double lim) {
		this.limUdAbs = lim;
		return this;
	}

	public NetworkSolver absItol(double lim) {
		this.limIdAbs = lim;
		return this;
	}

	public NetworkSolver relUtol(double lim) {
		this.limUdRel = lim;
		return this;
	}

	public NetworkSolver relItol(double lim) {
		this.limIdRel = lim;
		return this;
	}
	
	public NetworkSolver limSingular(double lim) {
		this.limSingular = lim;
		return this;
	}
	
	public NetworkSolver iterLim(int lim) {
		this.limIter = lim;
		return this;
	}
	
	public NetworkSolver debug(Consumer<String> logOut) {
		this.debugOut = logOut;
		return this;
	}
	
	@Override
	public String toString() {
		return String.format("NetworkSolver{ ITERLIM = %d, UDABS = %f, UDREL = %f, IDABS = %f, IDREL = %f }", this.limIter, this.limUdAbs, this.limUdRel, this.limIdAbs, this.limIdRel);
	}
	
	protected void log(String msg, Object... args) {
		if (this.debugOut != null)
			this.debugOut.accept(String.format(msg, args));
	}
	
	protected void initializeA() throws NetworkSolverException {
		MatrixNd Amat = network.getSystemMatrix_A();
		log("initialize solver with network matrix:\n%s", Amat);
		
		Map<Vec2i, Double> entries = Amat.getNonZeroes();
		DMatrixSparseCSC Asol = new DMatrixSparseCSC(Amat.width(), Amat.height(), entries.size());
		entries.forEach((pos, val) -> Asol.set(pos.y, pos.x, val));
		
		if (!this.linearSolver.setA(Asol)) {
			log("error: matrix singular");
			throw new NetworkSolverException("unabel to initialize linear solver: system matrix is singular");
		} else {
			double qual = this.linearSolver.quality();
			log("matrix non-singularity: %f", qual);
			if (qual < this.limSingular) {
				log("error: matrix nearly singular: %s < %s", Double.toString(qual), Double.toString(this.limSingular));
				throw  new NetworkSolverException("unabel to initialize linear solver: system matrix nearly singular: QUAL = " + qual);
			}
		}
		log("solver initialized");
	}
	
	protected MatrixNd solveX() {
		log("solve equation system for vector:\n%s", network.getSystemMatrix_z());
		DMatrixRMaj x = new DMatrixRMaj(network.getSystemMatrix_A().height(), 1);
		this.linearSolver.solve(new DMatrixRMaj(network.getSystemMatrix_z().get2DArray()), x);
		MatrixNd res = new MatrixNd(x.get2DData());
		log("solution found:\n%s", res);
		return res;
	}
	
	protected boolean checkConvergence(MatrixNd A, MatrixNd B) {
		log("check for convergence ...");
		double[] a = A.getArray();
		double[] b = B.getArray();
		for (int i = 0; i < a.length; i++) {
			double diff = Math.abs(a[i] - b[i]);
			if (i < this.network.getnNodes() - 1) {
				if (diff > diff * limUdRel + limUdAbs) return false;
			} else {
				if (diff > diff * limIdRel + limIdAbs) return false;
			}
		}
		log("convergence detected, solution found");
		return true;
	}
	
	public void solveNonLinear(IndexedNetwork network) throws NetworkSolverException {
		this.network = network;
		log("start non linear solver ...");
		for (int i = 0; i < this.limIter; i++) {
//			log("iteration: %d / %d", i + 1, this.limIter);
			try {
				this.network.stampMatrices(i);
				initializeA();
				MatrixNd X = solveX();
				boolean solved = i == 0 ? false : checkConvergence(X, this.network.getSystemMatrix_x());
				this.network.setSystemMatrix_x(X);
				if (solved) {
					log("non linear solution found");
					return;
				}
			} catch (NetworkSolverException e) {
				log("error: non linear solver failed at itteration %d", i + 1);
				throw new NetworkSolverException("unable to solve iteration: ITER = " + (i + 1), e);
			}
		}
		log("error: iteration limit reached");
		throw new NetworkSolverException("unable to solve non-linearity: no convergenze within limits: " + toString());
	}
	
	public void solveLinear(IndexedNetwork network) throws NetworkSolverException {
		this.network = network;
		log("start linear solver ...");
		initializeA();
		this.network.setSystemMatrix_x(solveX());
		log("non linear solution found");
	}
	
	public void solve(IndexedNetwork network) throws NetworkSolverException {
		if (network.isNonLinear())
			solveNonLinear(network);
		else {
			network.stampMatrices(StampingMode.FULL_MATRICES);
			solveLinear(network);
		}
	}
	
}
