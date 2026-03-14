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
	private int nonLinear;
	
	// E*x' + A*x = z
	private MatrixNd systemMatrix_A;
	private MatrixNd systemMatrix_x;
	private MatrixNd systemMatrix_z;
	
	public IndexedNetwork(Collection<Element> elements) {
		if (elements.isEmpty())
			throw new IllegalArgumentException("element list can not be empty");
		elements.forEach(this::addElement);
	}
	
	public IndexedNetwork() {}
	
	public Element addElement(Element element) {
		this.nVSources = this.nNodes = 0;
		element.setNetwork(this);
		Element replaced = this.elements.put(element.name(), element);
		if ((replaced != null && replaced.isNonLinear()) != element.isNonLinear())
			this.nonLinear += element.isNonLinear() ? +1 : -1;
		return replaced;
	}
	
	public Element removeElement(String name) {
		this.nVSources = this.nNodes = 0;
		Element removed = this.elements.remove(name);
		if ((removed != null && removed.isNonLinear()))
			this.nonLinear -= 1;
		return removed;
	}
	
	public boolean removeElement(Element element) {
		this.nVSources = this.nNodes = 0;
		boolean removed = this.elements.remove(element.name(), element);
		if ((removed && element.isNonLinear()))
			this.nonLinear -= 1;
		return removed;
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
	
	public int getnNonLinear() {
		return nonLinear;
	}
	
	public boolean isNonLinear() {
		return nonLinear > 0;
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
	
	public MatrixNd getSystemMatrix_x() {
		return systemMatrix_x;
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
		
		private final StampingMode mode;
		private final int iter;
		private int voltageSourceId = 0;
		
		public StampingContext(StampingMode mode, int iter) {
			this.mode = mode;
			this.iter = iter;
		}
		
		public StampingMode mode() {
			return mode;
		}
		
		public int iter() {
			return this.iter;
		}
		
		public boolean nlInit() {
			return iter() == 0;
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
		if (isNonLinear() && mode != StampingMode.FULL_MATRICES)
			throw new IllegalStateException("can't partialy stamp matrix of non-linear network");
		stampMatrices(mode, 0);
	}

	public void stampMatrices(int iter) {
		stampMatrices(StampingMode.FULL_MATRICES, iter);
	}
	
	private void stampMatrices(StampingMode mode, int iter) {
		
		StampingContext ctx = new StampingContext(mode, iter);
		
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
		
	}
	
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		for (var comp : this.elements.values())
			buff.append(comp.toString()).append('\n');
		return buff.toString();
	}
	
}
