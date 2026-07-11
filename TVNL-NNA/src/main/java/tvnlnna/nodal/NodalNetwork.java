package tvnlnna.nodal;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.IntStream;

import de.m_marvin.unimat.impl.MatrixNd;
import tvnlnna.NodalMatrixStampException;
import tvnlnna.nodal.NodalNetwork.StampingContext.StampingMode;

/**
 * A network contains all the {@link NodalElementState} instances forming a network to simulate.
 * The {@link NodalNetwork} will generate the system matrices which can be solved by the DAE solver.
 * The resulting x vector will be passed back to the network and results can be retrieved trough the element instances. 
 */
public class NodalNetwork {
	
	private final Map<String, NodalElementState> elements = new LinkedHashMap<>();
	private final Map<String, Integer> nodes = new HashMap<String, Integer>();
	private String zeroNode;
	private int nNodes;
	private int nLocal;
	private int nNonLinear;
	private int nTimeVariant;
	
	// E*x' + A*x = z
	private MatrixNd systemMatrix_E;
	private MatrixNd systemMatrix_A;
	private MatrixNd systemMatrix_z;
	private MatrixNd systemMatrix_x;
	
	/**
	 * Creates a new network with the supplied elements and zero variable.
	 * @param elements The list of elements to add to this network
	 * @param zeroNode The name of the zero node, or null if it should be disabled
	 */
	public NodalNetwork(Collection<NodalElementState> elements, String zeroNode) {
		elements.forEach(this::addElement);
		this.zeroNode = zeroNode;
	}
	
	/**
	 * Creates a new empty network.
	 */
	public NodalNetwork() {}
	
	/**
	 * Adds the element to the network.
	 * @param element The element instance to add to the network
	 * @return true if the element was not in the network before, otherwise false
	 */
	public NodalElementState addElement(NodalElementState element) {
		markElementChange();
		element.setNetwork(this);
		NodalElementState replaced = this.elements.put(element.name(), element);
		if ((replaced != null && replaced.isNonLinear()) != element.isNonLinear())
			this.nNonLinear += element.isNonLinear() ? +1 : -1;
		if ((replaced != null && replaced.isTimeVariant()) != element.isTimeVariant())
			this.nTimeVariant += element.isTimeVariant() ? +1 : -1;
		return replaced;
	}
	
	/**
	 * Removes the element from this network,
	 * @param name The name of the element to remove
	 * @return true if the element was in the network before, false otherwise
	 */
	public NodalElementState removeElement(String name) {
		markElementChange();
		NodalElementState removed = this.elements.remove(name);
		if ((removed != null && removed.isNonLinear()))
			this.nNonLinear -= 1;
		if ((removed != null && removed.isTimeVariant()))
			this.nTimeVariant -= 1;
		return removed;
	}
	
	/**
	 * Removes the element from this network,
	 * @param element The element instance to remove
	 * @return true if the element was in the network before, false otherwise
	 */
	public boolean removeElement(NodalElementState element) {
		markElementChange();
		boolean removed = this.elements.remove(element.name(), element);
		if ((removed && element.isNonLinear()))
			this.nNonLinear -= 1;
		if ((removed && element.isTimeVariant()))
			this.nTimeVariant -= 1;
		return removed;
	}
	
	/**
	 * Notifies the network that some elements have been changed externally, this will cause the necessary variables to be recomputed during the next iteration.
	 */
	public void markElementChange() {
		this.nLocal = this.nNodes = 0;
	}
	
	/**
	 * Returns a collection of all elements defined in the network.
	 * @return a collection of all element instances.
	 */
	public Collection<NodalElementState> getElements() {
		return this.elements.values();
	}
	
	/**
	 * Returns the element instance with the supplied name.
	 * @param name the name of the element instance to return
	 * @return the element instance with the supplied name, or null if it does not exist
	 */
	public NodalElementState getElement(String name) {
		return this.elements.get(name);
	}
	
	/**
	 * Returns a list of all node names defined in the network.
	 * @return a list of all node names
	 */
	public List<String> getNodeNames() {
		return nodes.entrySet().stream().sorted((a, b) -> Integer.compare(a.getValue(), b.getValue())).map(Entry::getKey).toList();
	}

	/**
	 * Returns the number of nodes in this network, excluding the zero node.
	 * @return the number of nodes excluding the zero node
	 */
	public int nodeCount() {
		return this.nNodes;
	}
	
	/**
	 * The number of element local variables in this network
	 * @return the number of element local variables
	 */
	public int localCount() {
		return this.nLocal;
	}
	
	/**
	 * Counts the number of not linear elements in this network.
	 * @return the number of not linear elements
	 */
	public int countNonLinear() {
		return nNonLinear;
	}

	/**
	 * Counts the number of time variant elements in this network.
	 * @return the number of time variant elements
	 */
	public int countTimeVariant() {
		return nTimeVariant;
	}
	
	/**
	 * Checks weather this network is not linear
	 * @return true if the network is not linear, false otherwise
	 */
	public boolean isNonLinear() {
		return nNonLinear > 0;
	}
	
	/**
	 * Checks weather this network is time variant.
	 * @return true if the network is time variant, false otherwise
	 */
	public boolean isTimeVariant() {
		return nTimeVariant > 0;
	}
	
	/**
	 * The time invariant coefficient matrix of the network system, will be configured during the matrix stamping.
	 * @return the A matrix
	 */
	public MatrixNd getSystemMatrix_A() {
		return this.systemMatrix_A;
	}
	
	/**
	 * The differential coefficiency matrix of the network system, will be configured during the matrix stamping.
	 * @return the E matrix
	 */
	public MatrixNd getSystemMatrix_E() {
		return systemMatrix_E;
	}
	
	/**
	 * The forcing vector of the network system, will contain source-constants to "excite" the network, with only zero values, nothing would happen, is configured during matrix stamping.
	 * @return the Z vector as column vector
	 */
	public MatrixNd getSystemMatrix_z() {
		return this.systemMatrix_z;
	}
	
	/**
	 * The solution vector of the network system, will be computed and set by the DAE solver.
	 * @return the X vector as column vector
	 */
	public MatrixNd getSystemMatrix_x() {
		return systemMatrix_x;
	}
	
	public boolean hasValidSolutionVector() {
		if (this.systemMatrix_x == null || this.systemMatrix_A == null)
			return false;
		return this.systemMatrix_x.height() == this.systemMatrix_A.height() && this.systemMatrix_x.width() == 1;
	}
	
	/**
	 * Sets the name of the zero node, can be set to null to disable the zero node.
	 * @param zeroNode The name of the zero node or null to disable it
	 */
	public void setZeroNode(String zeroNode) {
		this.zeroNode = zeroNode;
	}
	
	/**
	 * Returns the name of the zero node.
	 * @return the name of the zero node, or null if no zero node is configured
	 */
	public String getZeroNode() {
		return zeroNode;
	}
	
	/**
	 * Checks if the node name is the zero node.
	 * @param node The node name to check
	 * @return true if and only if the node is the zero node
	 */
	public boolean isZeroNode(String node) {
		if (this.zeroNode == null)
			return false;
		return this.zeroNode.equals(node);
	}
	
	/**
	 * Sets the x solution vector of the network.
	 * This method is invoked by the solver.
	 * @param systemMatrix_x the x column vector
	 */
	public void setSystemMatrix_x(MatrixNd systemMatrix_x) {
		this.systemMatrix_x = systemMatrix_x;
	}
	
	public class StampingContext {
		
		public static enum StampingMode {
			INIT_VECTOR(false, false, false, true),
			TIME_INVARIANT(true, false, true, false),
			FULL_MATRICES(true, true, true, false);
			
			private final boolean stampA;
			private final boolean stampE;
			private final boolean stampZ;
			private final boolean stampX;
			
			private StampingMode(boolean stampA, boolean stampE, boolean stampZ, boolean stampX) {
				this.stampA = stampA;
				this.stampE = stampE;
				this.stampZ = stampZ;
				this.stampX = stampX;
			}
			
			public boolean stampsA() {
				return stampA;
			}

			public boolean stampsE() {
				return stampE;
			}
			
			public boolean stampsZ() {
				return stampZ;
			}
			
			public boolean stampsX() {
				return stampX;
			}
			
		}
		
		private final StampingMode mode;
		private final double simtime;
		private final int iter;
		private int nextLocalId = 0;
		private int nextNodeId = 0;
		
		public StampingContext(StampingMode mode, double simtime, int iter) {
			this.mode = mode;
			this.simtime = simtime;
			this.iter = iter;
		}
		
		public StampingMode mode() {
			return mode;
		}
		
		/**
		 * The current simulation time.
		 * @return the current simulation time, the unit will be defined by the application, tough it should usually be in seconds
		 */
		public double simtime() {
			return this.simtime;
		}
		
		/**
		 * The current iteration number during non linear network solving
		 * @return the number of the current iteration, starting with zero
		 */
		public int iter() {
			return this.iter;
		}
		
		/**
		 * Checks if this is the first iteration in non linear networks during solving.
		 * @return true if this is the first iteration
		 */
		public boolean nlInit() {
			return iter() == 0;
		}
		
		/**
		 * Checks weather the A matrix should be generated during this iteration.
		 * @return true if the matrix should be generated
		 */
		public boolean stampsA() {
			return mode().stampsA();
		}

		/**
		 * Checks weather the E matrix should be generated during this iteration.
		 * @return true if the matrix should be generated
		 */
		public boolean stampsE() {
			return mode().stampsE();
		}

		/**
		 * Checks weather the Z matrix should be generated during this iteration.
		 * @return true if the matrix should be generated
		 */
		public boolean stampsZ() {
			return mode().stampsZ();
		}
		
		/**
		 * Checks weather the X matrix should be generated during this iteration. <br>
		 * NOTE: Generating the X matrix, aka the solution vector, means filling it with the 
		 * values currently stored in the component parameters (such as node potentials).
		 * This is not used during normal simulation, but can be used to initialize the starting 
		 * conditions by setting the parameters in the element states, and then letting the initial vector generate during the first step.
		 * @return true if the matrix should be generated
		 */
		public boolean stampsX() {
			return mode().stampsX();
		}
		
		/**
		 * Returns the number of nodes in this network, excluding the zero node.
		 * @return the number of nodes excluding the zero node
		 */
		public int nodeCount() {
			return NodalNetwork.this.nNodes;
		}
		
		/**
		 * The number of element local variables in this network
		 * @return the number of element local variables
		 */
		public int localCount() {
			return NodalNetwork.this.nLocal;
		}
		
		/**
		 * During indexing, this method will provide ids for the element local variables.
		 * @return the next free id to assign for an element local variable
		 */
		public int nextLocalId() {
			return this.nextLocalId++;
		}
		
		/**
		 * This method will return the id for a node name, during indexing, it will also create new ids for not yet registered nodes.
		 * @param node The node name to get the id for
		 * @return the id of the node
		 */
		public int nodeId(String node) {
			if (!NodalNetwork.this.nodes.containsKey(node))
				NodalNetwork.this.nodes.put(node, NodalNetwork.this.isZeroNode(node) ? 0 : ++nextNodeId);
			return NodalNetwork.this.nodes.get(node);
		}
		
	}
	
	/**
	 * Invoking this function will generate the system matrices representing the DAE-system.
	 * This method is the base method called by {@link NodalNetwork#stampMatrices(StampingMode)} and {@link NodalNetwork#stampMatrices(int)}
	 * The basis for non-linear element's linearization will be the working point computed using the x vector, which will be set to zero for the first iteration, or contain the values of the previous iteration.
	 * @param mode The stamping mode, allows to apply a filter, which determines what matrices are generated
	 * @param time The simulation time, this will be passed to the element definitions and may be used there
	 * @param iter The iteration number
	 * @throws NodalMatrixStampException
	 */
	public void stampMatrices(StampingMode mode, double time, int iter) throws NodalMatrixStampException {
		
		StampingContext ctx = new StampingContext(mode, time, iter);
		
		if (nLocal == 0 || nNodes == 0) {
			this.nodes.clear();
			this.elements.values().forEach(c -> c.index(ctx));
			this.nLocal = this.elements.values().stream()
					.mapToInt(c -> c.localIds().length)
					.sum();
			this.nNodes = this.elements.values().stream()
					.flatMapToInt(c -> IntStream.of(c.nodes()))
					.max().orElseGet(() -> 0);
			this.systemMatrix_x = null;
		}
		
		int matrixSize = this.nNodes + this.nLocal;
		boolean makeSparse = false; // matrixSize > 100;
		if (mode.stampsA())
			this.systemMatrix_A = new MatrixNd(matrixSize, makeSparse);
		if (mode.stampsE())
			this.systemMatrix_E = new MatrixNd(matrixSize, makeSparse);
		if (mode.stampsZ())
			this.systemMatrix_z = new MatrixNd(1, matrixSize);
		if (this.systemMatrix_x == null)
			this.systemMatrix_x = new MatrixNd(1, matrixSize);
		
		for (var comp : this.elements.values())
			try {
				comp.stampMatricies(ctx, 
						this.systemMatrix_A,
						this.systemMatrix_E,
						this.systemMatrix_z,
						this.systemMatrix_x
					);
			} catch (NodalMatrixStampException e) {
				throw new NodalMatrixStampException("unexpected exception occured during system matrices stamping", e);
			}
		
	}
	
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		for (var element : this.elements.values())
			buff.append(element.shortString()).append('\n');
		return buff.toString();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.elements, this.nodes, this.zeroNode);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodalNetwork other) {
			return	Objects.equals(this.elements, other.elements) &&
					Objects.equals(this.nodes, other.nodes) &&
					Objects.equals(this.zeroNode, other.zeroNode);
		}
		return false;
	}
	
}
