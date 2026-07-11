package tvnlnna.nodal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import de.m_marvin.unimat.impl.MatrixNd;
import tvnlnna.NodalMatrixStampException;
import tvnlnna.nodal.NodalElement.ElementVariable;
import tvnlnna.nodal.NodalElement.SystemVariablePair;
import tvnlnna.nodal.NodalNetwork.StampingContext;

/**
 * Represents an single instance of an element in the network.
 * Each instance stores name, type of element, connected node names, node and local ids, and parameters for the current simulation step of the element.
 * The parameters are the collection of values for element variables, node variables, and local variables.
 * Except element variables, all of them may change between simulation steps.
 */
public class NodalElementState {
	
	/** The unique name of this element instance in the network **/
	private final String name;
	/** The element definition of this instance **/
	private final NodalElement element;
	/** The names of the nodes this element is connected to **/
	private final String[] nodes;
	/** The parameters of the element **/
	private final Map<String, Double> parameters = new HashMap<String, Double>();
	/** If this element is disabled and should not contribute to the stamp of the circuit **/
	private boolean disabled = false;
	/** The network instance this element belongs to **/
	private NodalNetwork network;
	
	/** The ids of the locals of this element, they refer to their location in the system matrices **/
	private final int[] localIds;
	/** The ids of the nodes the element is connected to, they refer to their location in the system matrices **/
	private final int[] nodeIds;
	
	private static final Pattern NAME_FILTER = Pattern.compile("^\\S+");
	
	public NodalElementState(String name, NodalElement element) {
		if (name.isBlank() || !NAME_FILTER.matcher(name).matches())
			throw new IllegalArgumentException("element name is blank or contains and/or starts with an white space character");
		this.name = name;
		this.nodes = new String[element.nodes().size()];
		this.nodeIds = new int[element.nodes().size()];
		this.localIds = new int[element.locals().size()];
		this.element = element;
		
		resetParameters();
	}

	/**
	 * Resets all parameters of this element instance to zero, excluding the element variables.
	 */
	public void resetParameters() {
		for (var e : this.element.nodes())
			setParameter(e.name, 0.0);
		for (var e : this.element.locals())
			setParameter(e.name, 0.0);
	}
	
	public void setNetwork(NodalNetwork network) {
		this.network = network;
	}
	
	public NodalElement type() {
		return element;
	}
	
	/**
	 * The network unique name of this element with the element type prefix.
	 * @return the unique name of this element with the element type prefix
	 */
	public String name() {
		return this.element.name() + this.name;
	}

	/**
	 * Checks weather this element has non linear behavior.
	 * @return true if the element definition is marked as non linear
	 */
	public boolean isNonLinear() {
		return this.element.isNonLinear();
	}

	/**
	 * Checks weather this element has time variant behavior.
	 * @return true if the element definition is marked as time variant
	 */
	public boolean isTimeVariant() {
		return this.element.isTimeVariant();
	}
	
	/**
	 * Sets weather the element is disabled or not.
	 * If disabled, the element will be left out of the system matrix stamps.
	 * @param disabled true if the element should be disabled
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	/**
	 * Checks weather the element is disabled or not.
	 * If disabled, the element will be left out of the system matrix stamps.
	 * @return true if the element is currently disabled
	 */
	public boolean isDisabled() {
		return disabled;
	}
	
	/**
	 * Sets one of the parameters of this element, this can be an node variable, local variable or element variable defined in the element definition.
	 * @param name The name of the variable to assign a value to
	 * @param value The value to assign
	 */
	public void setParameter(String name, double value) {
		Objects.requireNonNull(name);
		this.parameters.put(name, value);
	}
	
	/**
	 * Gets one of the parameters of this element, this can be an node variable, local variable or element variable defined in the element definition.
	 * @param name The name of the variable to read the value from
	 * @return The value assigned to the variable
	 */
	public double getParamter(String name) {
		Objects.requireNonNull(name);
		return this.parameters.getOrDefault(name, 0.0);
	}
	
	/**
	 * Returns the full map of parameters currently defined for this element instance
	 * @return the map consisting of variable value pairs for all parameters of this element instance
	 */
	public Map<String, Double> parameters() {
		return parameters;
	}

	/**
	 * Sets the node name assigned to an single node variable.
	 * @param indx The node variable index to assign a node name to
	 * @param nodeName The node name to assign
	 * @throws IndexOutOfBoundsException If the index is invalid
	 * 
	 */
	public void setNodeName(int indx, String nodeName) {
		Objects.requireNonNull(nodeName);
		if (nodeName.isBlank() || !NAME_FILTER.matcher(nodeName).matches())
			throw new IllegalArgumentException("node name is blank or contains and/or starts with an white space character");
		this.nodes[indx] = nodeName;
		if (this.network != null)
			this.network.markElementChange();
	}

	/**
	 * Sets the node name assigned to an single node variable.
	 * @param nodeVariable The node variable name to assign a node name to
	 * @param nodeName The node name to assign
	 */
	public void setNodeName(String nodeVariable, String nodeName) {
		Objects.requireNonNull(nodeVariable);
		Objects.requireNonNull(nodeName);
		setNodeName(this.element.getNodeVariableIndex(nodeVariable), nodeName);
	}

	/**
	 * Returns the name of the node assigned to the node variable.
	 * @param nodeVariable The name of the node variable
	 * @return the name of the node assigned to the node variable or null if none is assigned
	 */
	public String getNodeName(String nodeVariable) {
		Objects.requireNonNull(nodeVariable);
		return getNodeName(this.element.getNodeVariableIndex(nodeVariable));
	}

	/**
	 * Returns the name of the node assigned to the node variable.
	 * @param indx The index of the node variable
	 * @return the name of the node assigned to the node variable or null if none is assigned
	 * @throws IndexOutOfBoundsException If the index is invalid
	 */
	public String getNodeName(int indx) {
		if (indx < 0 || indx >= this.nodes.length)
			throw new IndexOutOfBoundsException("node index " + indx + " is out of bounds for this element: " + this.name);
		return this.nodes[indx];
	}

	/**
	 * Assigns the node names to the node variables.
	 * @param nodeNames The node names to assign
	 * @Throws {@link IllegalArgumentException} if the node name list does not match the node variable count or contains invalid node names
	 */
	public void setNodeNames(String... nodeNames) {
		Objects.requireNonNull(nodeNames);
		if (nodeNames.length != this.nodes.length)
			throw new IllegalArgumentException("number of node names does not match number of nodes for this element");
		if (this.network != null)
			this.network.markElementChange();
		for (int i = 0; i < this.nodes.length; i++) {
			if (nodeNames[i].isBlank() || !NAME_FILTER.matcher(nodeNames[i]).matches())
				throw new IllegalArgumentException("node name is blank or contains and/or starts with an white space character");
			this.nodes[i] = nodeNames[i];
		}
	}
	
	/**
	 * The list of currently assigned nodes for each node variable.
	 * @return the list of node names
	 */
	public String[] getNodeNames() {
		return this.nodes;
	}

	/**
	 * The list of currently assigned node variable ids.
	 * @return the list of variable ids
	 */
	public int[] nodes() {
		return this.nodeIds;
	}
	
	/**
	 * The list of currently assigned local variable ids.
	 * @return the list of variable ids
	 */
	public int[] localIds() {
		return this.localIds;
	}

	/**
	 * Invoking this function will assign new node and local variable ids to this element instance, taken from the stamping context provided.
	 * This will invalidate all parameters of this element, including element variables.
	 * @param ctx The stamp context to use for indexing
	 */
	public void index(StampingContext ctx) {
		for (int i = 0; i < this.nodes.length; i++)
			this.nodeIds[i] = ctx.nodeId(this.nodes[i]);
		for (int i = 0; i < this.localIds.length; i++)
			this.localIds[i] = ctx.nextLocalId();
		resetParameters();
	}
	
	/**
	 * Invoking this function will apply the elements matrix stamp to the system matrices provided.
	 * If required, it will also compute the linearization of the element on the current operation point, which will be taken from the parameters in the element state (usually the results of the previous iteration, or zero if this is the first iteration).
	 * This function will influence the parameters currently assigned for this element instance.
	 * @param ctx The stamping context, providing additional information for this step
	 * @param A The system A matrix (time invariant part)
	 * @param E The system E matrix (time variant part)
	 * @param z The forcing vector (constants)
	 * @param x The solution vector (node and local variable values) from the previous simulation step
	 * @throws NodalMatrixStampException 
	 */
	public void stampMatricies(NodalNetwork.StampingContext ctx, MatrixNd A, MatrixNd E, MatrixNd z, MatrixNd x) throws NodalMatrixStampException {
		if (this.disabled) return;
		
		// we don't override the parameters if stamping of the X vector is requested, as this would defeat the purpose
		if (!ctx.stampsX()) {
			// copy solution vector results from previous step into parameters for node potentials ...
			for (var e : this.element.locals())
				setParameter(e.name, x.m(0, this.localIds[this.element.getLocalVariableIndex(e.name)] + ctx.nodeCount()));
			// ... and element variables
			for (var e : this.element.nodes()) {
				int i = this.nodeIds[this.element.getNodeVariableIndex(e.name)] - 1;
				if (i >= 0)
					setParameter(e.name, x.m(0, i));
			}
		}
		
		this.element.stampMatricies(ctx, A, E, z, x, this);
	}
	
	public String shortString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.element.name()).append(" ").append(this.name);
		for (SystemVariablePair variable : this.element.nodes())
			sb.append(" ").append(variable.name).append("=").append(getNodeName(variable.name));;
		for (ElementVariable variable : this.element.variables())
			sb.append(" ").append(variable.name).append("=").append(getParamter(variable.name));
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return "NodalElementState{ name = " + this.name + ", element = " + this.element.toString() + ", disabled = " + this.disabled + " }";
	}
	
	@Override
	public int hashCode() {
		double[] param = this.element.variables().stream().map(v -> v.name).mapToDouble(this.parameters::get).toArray();
		return Objects.hash(this.element, this.name, Arrays.hashCode(param));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodalElementState other) {
			double[] param1 = this.element.variables().stream().map(v -> v.name).mapToDouble(this.parameters::get).toArray();
			double[] param2 = other.element.variables().stream().map(v -> v.name).mapToDouble(other.parameters::get).toArray();
			return	Objects.equals(this.element, other.element) &&
					Objects.equals(this.name, other.name) &&
					Arrays.compare(param1, param2) == 0;
		}
		return false;
	}
	
}
