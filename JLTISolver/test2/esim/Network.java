package test.esim;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import de.m_marvin.unimat.impl.MatrixNd;

public class Network {
	
	protected final Map<String, Component> elements = new HashMap<>();
	protected final int nNodes;
	protected final int nVSources;
	
	protected MatrixNd systemMatrix_A;
	protected MatrixNd systemMatrix_E;
	protected MatrixNd systemMatrix_x;
	protected MatrixNd systemMatrix_z;
	
	public Network(Collection<Component> elements) {
		if (elements.isEmpty())
			throw new IllegalArgumentException("elements list can not be empty");
		elements.forEach(c -> this.elements.put(c.name(), c));
		
		this.nVSources = this.elements.values().stream()
				.mapToInt(c -> c.vsourceIds().length)
				.sum();
		this.nNodes = this.elements.values().stream()
				.flatMapToInt(c -> IntStream.of(c.nodes()))
				.max().orElseGet(() -> 0) + 1;
	}
	
	public Collection<Component> getComponents() {
		return elements.values();
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
		if (nodeId >= this.nNodes)
			throw new IndexOutOfBoundsException("node id does not exist: " + nodeId);
		
		return this.systemMatrix_x.m(0, nodeId);
	}
	
	public double getVSourceCurrent(int vsourceId) {
		if (vsourceId >= this.nVSources)
			throw new IndexOutOfBoundsException("voltage source id does not exist: " + vsourceId);
		
		return this.systemMatrix_x.m(0, this.nNodes + vsourceId);
	}
	
	public double getVSourceCurrent(String vsourceName) {
		Component comp = this.elements.get(vsourceName);
		if (comp == null)
			throw new IllegalArgumentException("element does not exist: " + vsourceName);
		
		int[] vids = comp.vsourceIds();
		if (vids.length == 0)
			throw new IllegalArgumentException("element does not define a voltage source: " + vsourceName);
		if (vids.length > 1)
			throw new IllegalArgumentException("element does defnie more than one voltage sources: " + vsourceName);
		return getVSourceCurrent(vids[0]);
	}
	
	protected void fillMatrices() {
		
		int matrixSize = this.nNodes + this.nVSources;
		this.systemMatrix_A = new MatrixNd(matrixSize);
		this.systemMatrix_E = new MatrixNd(matrixSize);
		this.systemMatrix_z = new MatrixNd(1, matrixSize);
		
		Context ctx = new Context();
		
		for (var comp : this.elements.values())
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
		for (var comp : this.elements.values())
			buff.append(comp.toString()).append('\n');
		return buff.toString();
	}
	
}
