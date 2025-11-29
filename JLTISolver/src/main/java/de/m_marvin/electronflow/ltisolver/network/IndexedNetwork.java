package de.m_marvin.electronflow.ltisolver.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import de.m_marvin.electronflow.ltisolver.elements.Element;
import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork.StampingContext.StampingMode;
import de.m_marvin.unimat.impl.MatrixNd;

public class IndexedNetwork {
	
	private final Map<String, Element> elements = new HashMap<>();
	private final int nNodes;
	private final int nVSources;
	
	// E*x' + A*x = z
	private MatrixNd systemMatrix_A;
	private MatrixNd systemMatrix_x;
	private MatrixNd systemMatrix_z;
	
	public IndexedNetwork(Collection<Element> elements) {
		if (elements.isEmpty())
			throw new IllegalArgumentException("element list can not be empty");
		elements.forEach(c -> this.elements.put(c.name(), c));
		
		this.nVSources = this.elements.values().stream()
				.mapToInt(c -> c.vsourceIds().length)
				.sum();
		this.nNodes = this.elements.values().stream()
				.flatMapToInt(c -> IntStream.of(c.nodes()))
				.max().orElseGet(() -> 0);
	}
	
	public Collection<Element> getComponents() {
		return elements.values();
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
		
		public static enum StampingMode {
			FULL_MATRICES(true, true),
			FORCING_VECTOR(false, true);
			
			private final boolean stampA;
			private final boolean stampZ;
			
			private StampingMode(boolean stampA, boolean stampZ) {
				this.stampA = stampA;
				this.stampZ = stampZ;
			}
			
			public boolean stampsA() {
				return stampA;
			}
			
			public boolean stampsZ() {
				return stampZ;
			}
			
		}
		
		private StampingMode mode;
		private int voltageSourceId = 0;
		
		public StampingContext(StampingMode mode) {
			this.mode = mode;
		}
		
		public StampingMode mode() {
			return mode;
		}
		
		public boolean stampsA() {
			return mode().stampsA();
		}
		
		public boolean stampsZ() {
			return mode().stampsZ();
		}
		
		public int nodeCount() {
			return IndexedNetwork.this.nNodes;
		}
		
		public int voltageSourcesCount() {
			return IndexedNetwork.this.nVSources;
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
		Element comp = this.elements.get(vsourceName);
		if (comp == null)
			throw new IllegalArgumentException("element does not exist: " + vsourceName);
		
		int[] vids = comp.vsourceIds();
		if (vids.length == 0)
			throw new IllegalArgumentException("element does not define a voltage source: " + vsourceName);
		
		return IntStream.of(vids).mapToDouble(this::getVSourceCurrent).toArray();
	}
	
	public void stampMatrices(StampingMode mode) {
		
		int matrixSize = this.nNodes + this.nVSources;
		boolean makeSparse = matrixSize > 6;
		if (mode.stampsA())
			this.systemMatrix_A = new MatrixNd(matrixSize, makeSparse);
		if (mode.stampsZ())
			this.systemMatrix_z = new MatrixNd(1, matrixSize, makeSparse);
		
		StampingContext ctx = new StampingContext(mode);
		
		for (var comp : this.elements.values())
			comp.stampMatricies(ctx, 
					this.systemMatrix_A,
					this.systemMatrix_z
				);
		
		this.systemMatrix_x = null;
		
	}
	
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		for (var comp : this.elements.values())
			buff.append(comp.toString()).append('\n');
		return buff.toString();
	}
	
}
