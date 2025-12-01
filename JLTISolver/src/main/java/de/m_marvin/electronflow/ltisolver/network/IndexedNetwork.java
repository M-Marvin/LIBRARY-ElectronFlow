package de.m_marvin.electronflow.ltisolver.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import de.m_marvin.electronflow.ltisolver.elements.Element;
import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork.StampingContext.StampingMode;
import de.m_marvin.unimat.impl.MatrixNd;

public class IndexedNetwork {
	
	private Map<String, Element> elements = new HashMap<>();
	private Map<String, Integer> nodes = new HashMap<String, Integer>();
	private int nNodes;
	private int nVSources;
	
	// E*x' + A*x = z
	private MatrixNd systemMatrix_A;
	private MatrixNd systemMatrix_x;
	private MatrixNd systemMatrix_z;
	
	public IndexedNetwork(Collection<Element> elements) {
		if (elements.isEmpty())
			throw new IllegalArgumentException("element list can not be empty");
		elements.forEach(c -> {
			this.elements.put(c.name(), c);
			c.setNetwork(this);
		});
	}
	
	public IndexedNetwork() {}
	
	public Element addElement(Element element) {
		this.nVSources = this.nNodes = 0;
		element.setNetwork(this);
		return this.elements.put(element.name(), element);
	}
	
	public Element removeElement(String name) {
		this.nVSources = this.nNodes = 0;
		return this.elements.remove(name);
	}
	
	public boolean removeElement(Element element) {
		this.nVSources = this.nNodes = 0;
		return this.elements.remove(element.name(), element);
	}
	
	public Collection<Element> getElements() {
		return this.elements.values();
	}
	
	public Element getElement(String name) {
		return this.elements.get(name);
	}
	
	public Set<String> getNodes() {
		return nodes.keySet();
	}
	
	public int getnNodes() {
		return nNodes;
	}
	
	public int getNVSources() {
		return nVSources;
	}
	
	public MatrixNd getSystemMatrix_A() {
		return this.systemMatrix_A;
	}
	
	public MatrixNd getSystemMatrix_z() {
		return this.systemMatrix_z;
	}
	
	public void setSystemMatrix_x(MatrixNd systemMatrix_x) {
		this.systemMatrix_x = systemMatrix_x;
	}
	
	public double getNodePotential(int nodeId) {
		if (this.systemMatrix_x == null)
			throw new IllegalStateException("solution matrix has net yet been computed");
		if (nodeId > this.nNodes)
			throw new IndexOutOfBoundsException("node id does not exist: " + nodeId);
		
		if (nodeId == 0)  return 0.0;
		return this.systemMatrix_x.m(0, nodeId - 1);
	}

	public double getNodePotential(String name) {
		Integer nodeId = this.nodes.get(name);
		return nodeId == null ? 0.0 : getNodePotential(nodeId);
	}
	
	public double getVSourceCurrent(int vsourceId) {
		if (this.systemMatrix_x == null)
			throw new IllegalStateException("solution matrix has net yet been computed");
		if (vsourceId >= this.nVSources)
			throw new IndexOutOfBoundsException("voltage source id does not exist: " + vsourceId);
		
		return this.systemMatrix_x.m(0, this.nNodes + vsourceId);
	}
	
	public double[] getElementCurrents(String name) {
		if (this.systemMatrix_x == null)
			throw new IllegalStateException("solution matrix has net yet been computed");
		Element element = this.elements.get(name);
		if (element != null)
			return element.currents();
		return new double[0];
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
		
		public int nodeId(String node) {
			if (!IndexedNetwork.this.nodes.containsKey(node))
				IndexedNetwork.this.nodes.put(node, IndexedNetwork.this.nodes.size());
			return IndexedNetwork.this.nodes.get(node);
		}
		
	}
	
	public void stampMatrices(StampingMode mode) {

		StampingContext ctx = new StampingContext(mode);
		
		if (nVSources == 0 || nNodes == 0) {
			this.elements.values().forEach(c -> c.index(ctx));
			this.nVSources = this.elements.values().stream()
					.mapToInt(c -> c.vsourceIds().length)
					.sum();
			this.nNodes = this.elements.values().stream()
					.flatMapToInt(c -> IntStream.of(c.nodes()))
					.max().orElseGet(() -> 0);
		}
		
		int matrixSize = this.nNodes + this.nVSources;
		boolean makeSparse = matrixSize > 6;
		if (mode.stampsA())
			this.systemMatrix_A = new MatrixNd(matrixSize, makeSparse);
		if (mode.stampsZ())
			this.systemMatrix_z = new MatrixNd(1, matrixSize, makeSparse);
		
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
