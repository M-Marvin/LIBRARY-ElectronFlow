package de.m_marvin.electronflow;

import java.util.Map;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.interfaces.linsol.LinearSolverSparse;
import org.ejml.sparse.FillReducing;
import org.ejml.sparse.csc.factory.LinearSolverFactory_DSCC;

import de.m_marvin.unimat.impl.MatrixNd;
import de.m_marvin.univec.impl.Vec2i;

public class Solver {
	
	private final LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj> linearSolver;
	private Network network;
	
	public Solver() {
		this.linearSolver = LinearSolverFactory_DSCC.lu(FillReducing.NONE);
		
	}
	
	protected void initialize(Network network) {
		this.network = network;
		
		MatrixNd Amat = network.getSystemMatrix_A();
		Map<Vec2i, Double> entries = Amat.getNonZeroes();
		DMatrixSparseCSC Asol = new DMatrixSparseCSC(Amat.width(), Amat.height(), entries.size());
		entries.forEach((pos, val) -> Asol.set(pos.y, pos.x, val));
		
		if (!this.linearSolver.setA(Asol))
			throw new IllegalStateException("unabel to initialize linear solver: system matrix is singular");
		if (this.linearSolver.quality() < 1E-8)
			throw new IllegalStateException("unabel to initialize linear solver: system matrix nearly singular");
		
		
		network.getSystemMatrix_E();
		
	}
	
	protected void solve() {
		DMatrixRMaj x = new DMatrixRMaj(network.getSystemMatrix_A().height(), 1);
		this.linearSolver.solve(new DMatrixRMaj(network.getSystemMatrix_z().get2DArray()), x);
		network.setSystemMatrix_x(new MatrixNd(x.get2DData()));
	}
	
}
