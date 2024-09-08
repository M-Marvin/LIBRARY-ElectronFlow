package testing;

import de.m_marvin.unimat.api.IMatrix;
import de.m_marvin.unimat.api.IMatrixMath;

public class MatrixDf implements IMatrix<MatrixDf>, IMatrixMath<MatrixDf> {
	
	protected final float m[][];
	
	public MatrixDf(int columns, int rows) {
		this.m = new float[columns][rows];
	}
	
	@Override
	public MatrixDf copy() {
		return new MatrixDf(getColumns(), getRows());
	}

	@Override
	public float getField(int x, int y) {
		if (x < 0 || x >= this.m.length || y < 0 || y >= this.m[0].length)
			throw new IllegalMatrixOperationException(x, y);
		return this.m[x][y];
	}

	@Override
	public void setField(int x, int y, float f) {
		if (x < 0 || x >= this.m.length || y < 0 || y >= this.m[0].length)
			throw new IllegalMatrixOperationException(x, y);
		this.m[x][y] = f;
	}

	@Override
	public int getColumns() {
		return this.m.length;
	}

	@Override
	public int getRows() {
		return this.m[0].length;
	}

	@Override
	public float[] toFloatArr() {
		float[] arr = new float[this.m.length * this.m[0].length];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = this.m[i % getColumns()][(i / getColumns()) % getRows()];
		}
		return arr;
	}

	@Override
	public void loadFloatArr(float[] arr) {
		if (arr.length != getRows() * getColumns())
			throw new IllegalMatrixOperationException(String.format("Array length %d does not match %d * %d", arr.length, getRows(), getColumns()));
		for (int i = 0; i < arr.length; i++) {
			this.m[i % getColumns()][(i / getColumns()) % getRows()] = arr[i];
		}
	}

	@Override
	public MatrixDf setI(MatrixDf mat) {
		if (mat.getRows() != getRows() || mat.getColumns() != getColumns())
			throw new IllegalMatrixOperationException(this, mat);
		for (int x = 0; x < getColumns(); x++) {
			for (int y = 0; y < getRows(); y++) {
				this.m[x][y] = mat.m[x][y];
			}
		}
		return this;
	}

	@Override
	public MatrixDf add(MatrixDf mat) {
		if (mat.getRows() != getRows() || mat.getColumns() != getColumns())
			throw new IllegalMatrixOperationException(this, mat);
		MatrixDf res = new MatrixDf(getColumns(), getRows());
		for (int x = 0; x < getColumns(); x++) {
			for (int y = 0; y < getRows(); y++) {
				res.m[x][y] = this.m[x][y] + mat.m[x][y];
			}
		}
		return res;
	}

	@Override
	public MatrixDf sub(MatrixDf mat) {
		if (mat.getRows() != getRows() || mat.getColumns() != getColumns())
			throw new IllegalMatrixOperationException(this, mat);
		MatrixDf res = new MatrixDf(getColumns(), getRows());
		for (int x = 0; x < getColumns(); x++) {
			for (int y = 0; y < getRows(); y++) {
				res.m[x][y] = this.m[x][y] - mat.m[x][y];
			}
		}
		return res;
	}

	@Override
	public MatrixDf mul(MatrixDf mat) {
		if (this.getColumns() != mat.getRows())
			throw new IllegalMatrixOperationException(this, mat);
		MatrixDf res = new MatrixDf(mat.getColumns(), this.getRows());
		for (int x = 0; x < res.getColumns(); x++) {
			for (int y = 0; y < res.getRows(); y++) {
				float s1 = 0;
				for (int j = 0; j < getColumns(); j++)
					s1 += this.m[j][y] * mat.m[x][j];
				res.m[x][y] = s1;
			}
		}
		return res;
	}

	@Override
	public float determinant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean invertI() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public MatrixDf tryInvertI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MatrixDf identity() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float adjugateAndDet() {
		// TODO Auto-generated method stub
		return 0;
	}

}
