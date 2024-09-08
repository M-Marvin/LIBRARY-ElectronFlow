package testing;

import de.m_marvin.unimat.api.IMatrix;

public class IllegalMatrixOperationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public IllegalMatrixOperationException(String message) {
		super(message);
	}
	
	public IllegalMatrixOperationException(int indexX, int indexY) {
		this(String.format("Matrix index out of bounds: %d %d", indexX, indexY));
	}
	
	public IllegalMatrixOperationException(IMatrix<?> matA, IMatrix<?> matB) {
		this(String.format("Dimensional Mismatch: Matrix dimenstion don't match: %d x %d <-> %d x %d", matA.getColumns(), matA.getRows(), matB.getColumns(), matB.getRows()));
	}
	
}
