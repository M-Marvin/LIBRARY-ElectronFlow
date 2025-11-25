package de.m_marvin.electronflow;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import de.m_marvin.unimat.impl.MatrixNd;

public class Network {
	
	protected final Map<String, Component> components = new HashMap<>();
	protected final int nNodes;
	protected final int nVSources;
	
	protected MatrixNd systemMatrix_A;
	protected MatrixNd systemMatrix_E;
	protected MatrixNd systemMatrix_x;
	protected MatrixNd systemMatrix_z;
	
	public Network(Collection<Component> components) {
		if (components.isEmpty())
			throw new IllegalArgumentException("components list can not be empty");
		components.forEach(c -> this.components.put(c.name(), c));
		
		this.nVSources = this.components.values().stream()
				.mapToInt(c -> c.vsourceIds().length)
				.sum();
		this.nNodes = this.components.values().stream()
				.flatMapToInt(c -> IntStream.of(c.nodes()))
				.max().orElseGet(() -> 0);
	}
	
	public Collection<Component> getComponents() {
		return components.values();
	}
	
	public class Context {
		
		private int voltageSourceId = 0;
		
		public int nodeCount() {
			return Network.this.nNodes;
		}
		
		public int voltageSourcesCount() {
			return Network.this.nVSources;
		}
		
		public int nextVoltageSourceId() {
			return this.voltageSourceId++;
		}
		
	}
	
	public double getNodePotential(int nodeId) {
		if (nodeId > this.nNodes)
			throw new IndexOutOfBoundsException("node id does not exist: " + nodeId);
		
		if (nodeId == 0)  return 0.0;
		return this.systemMatrix_x.m(0, nodeId - 1);
	}
	
	public double getVSourceCurrent(int vsourceId) {
		if (vsourceId >= this.nVSources)
			throw new IndexOutOfBoundsException("voltage source id does not exist: " + vsourceId);
		
		return this.systemMatrix_x.m(0, this.nNodes + vsourceId);
	}
	
	public double[] getVSourceCurrent(String vsourceName) {
		Component comp = this.components.get(vsourceName);
		if (comp == null)
			throw new IllegalArgumentException("component does not exist: " + vsourceName);
		
		int[] vids = comp.vsourceIds();
		if (vids.length == 0)
			throw new IllegalArgumentException("component does not define a voltage source: " + vsourceName);
		
		return IntStream.of(vids).mapToDouble(this::getVSourceCurrent).toArray();
	}
	
	protected void fillMatrices() {
		
		int matrixSize = this.nNodes + this.nVSources;
		this.systemMatrix_A = new MatrixNd(matrixSize);
		this.systemMatrix_E = new MatrixNd(matrixSize);
		this.systemMatrix_z = new MatrixNd(1, matrixSize);
		
		Context ctx = new Context();
		
		for (var comp : this.components.values())
			comp.stampMatricies(ctx, 
					this.systemMatrix_A, 
					this.systemMatrix_E,
					this.systemMatrix_z
				);
		
	}
	
	protected void solveLinear() {

		// TODO debugging
		
		System.out.println("= Matrix A =");
		System.out.println(this.systemMatrix_A);
		System.out.println("= Source Vector z =");
		System.out.println(this.systemMatrix_z);
		
		System.out.println("- - - - - - -");
		
		System.out.println("det(A) = " + this.systemMatrix_A.determinant());
		
		MatrixNd invA = this.systemMatrix_A.invert();
		this.systemMatrix_x = invA.mul(this.systemMatrix_z);

		System.out.println("= Solution Vector x =");
		System.out.println(this.systemMatrix_x);
		
	}
	
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		for (var comp : this.components.values())
			buff.append(comp.toString()).append('\n');
		return buff.toString();
	}
	
}
