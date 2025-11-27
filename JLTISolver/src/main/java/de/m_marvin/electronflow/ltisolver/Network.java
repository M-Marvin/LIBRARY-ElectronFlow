package de.m_marvin.electronflow.ltisolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import de.m_marvin.unimat.impl.MatrixNd;

public class Network {
	
	private final Map<String, Component> components = new HashMap<>();
	private final int nNodes;
	private final int nVSources;
	
	// E*x' + A*x = z
	private MatrixNd systemMatrix_A;
	private MatrixNd systemMatrix_x;
	private MatrixNd systemMatrix_z;
	
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
	
	public MatrixNd getSystemMatrix_A() {
		return systemMatrix_A;
	}
	
	public MatrixNd getSystemMatrix_z() {
		return systemMatrix_z;
	}
	
	public void setSystemMatrix_x(MatrixNd systemMatrix_x) {
		this.systemMatrix_x = systemMatrix_x;
	}
	
	public class StampingContext {
		
		private boolean onlyZ;
		private int voltageSourceId = 0;
		
		public StampingContext(boolean onlyZ) {
			this.onlyZ = onlyZ;
		}
		
		public boolean stampOnlyZ() {
			return this.onlyZ;
		}
		
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
		if (this.systemMatrix_x == null)
			throw new IllegalStateException("solution matrix has net yet been computed");
		if (nodeId > this.nNodes)
			throw new IndexOutOfBoundsException("node id does not exist: " + nodeId);
		
		if (nodeId == 0)  return 0.0;
		return this.systemMatrix_x.m(0, nodeId - 1);
	}
	
	public double getVSourceCurrent(int vsourceId) {
		if (this.systemMatrix_x == null)
			throw new IllegalStateException("solution matrix has net yet been computed");
		if (vsourceId >= this.nVSources)
			throw new IndexOutOfBoundsException("voltage source id does not exist: " + vsourceId);
		
		return this.systemMatrix_x.m(0, this.nNodes + vsourceId);
	}
	
	public double[] getVSourceCurrent(String vsourceName) {
		if (this.systemMatrix_x == null)
			throw new IllegalStateException("solution matrix has net yet been computed");
		Component comp = this.components.get(vsourceName);
		if (comp == null)
			throw new IllegalArgumentException("component does not exist: " + vsourceName);
		
		int[] vids = comp.vsourceIds();
		if (vids.length == 0)
			throw new IllegalArgumentException("component does not define a voltage source: " + vsourceName);
		
		return IntStream.of(vids).mapToDouble(this::getVSourceCurrent).toArray();
	}
	
	public void stampMatrices(boolean onlyZ) {
		
		int matrixSize = this.nNodes + this.nVSources;
		boolean makeSparse = matrixSize > 6;
		if (!onlyZ)
			this.systemMatrix_A = new MatrixNd(matrixSize, makeSparse);
		this.systemMatrix_z = new MatrixNd(1, matrixSize, makeSparse);
		
		StampingContext ctx = new StampingContext(onlyZ);
		
		for (var comp : this.components.values())
			comp.stampMatricies(ctx, 
					this.systemMatrix_A,
					this.systemMatrix_z
				);
		
		this.systemMatrix_x = null;
		
	}
	
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		for (var comp : this.components.values())
			buff.append(comp.toString()).append('\n');
		return buff.toString();
	}
	
}
