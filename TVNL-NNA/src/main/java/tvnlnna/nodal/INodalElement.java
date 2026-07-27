package tvnlnna.nodal;

import java.util.List;

import de.m_marvin.unimat.impl.MatrixNd;
import tvnlnna.NodalMatrixStampException;
import tvnlnna.mathematical.term.MathParsingContext;
import tvnlnna.nodal.NodalElement.ElementVariable;
import tvnlnna.nodal.NodalElement.SystemVariablePair;

/**
 * The definition of an element type.
 * Each element used in a network will need an element definition assigned, which can be parsed from an model XML file or defined by implementing this interface.
 * The element defined properties of the element required for simulation, such as what variables it adds to the system of equations.<br>
 * There are three types of variables:<br>
 * - node variables: Will be shared with other elements with's nodes are connected with this elements nodes<br>
 * - local variables: Will be local to each instance of the element, such as currents trough an voltage source<br>
 * - element variables: Will be constant during simulation, and can only be configured before starting the simulation, such as conductivity of an resistor.<br>
 * <br>
 * Additonaly, node and local variables will consist of a pair of two variables, one will be the actual variable, the other the corresponding constant which is used to form the equation of the system of equations.
 * In electrical simulations the node variable will have the unit volt, and correspond to the potential of the node, the constant will have the unit ampere, and will be the sum of current in that node.
 */
public interface INodalElement {
	
	/**
	 * Identifier for this type of element.
	 * @return the identifier string of this element type
	 */
	public String name();

	/**
	 * Checks if this element is marked as non linear.
	 * @return true if and only if it was marked as modified
	 */
	public boolean isNonLinear();
	
	/** 
	 * Checks if this element is marked as time variant.
	 * @return true if and only if it was marked as time variant
	 */
	public boolean isTimeVariant();
	
	/**
	 * The context used to parse math expressions for this element.
	 * The context is defined trough the function-block of the XML file and provides mathematical functions that can be used in other expressions.
	 * @return the math parsing context that can be used to parse math expressions for this element
	 */
	public MathParsingContext mathContext();
	
	/**
	 * The list of node variable pairs (unknown and constant) defined for this element type.
	 * Each entry consists of the pair of the solution vector variable name, and the forcing vector constant name.
	 * @return the list of node variable names in pairs
	 */
	public List<SystemVariablePair> nodes();
	
	/**
	 * Gets the index of the node variable name in this element type.
	 * @param name The name of the node variable defined for this element type
	 * @return the index of the node in this element
	 * @throws IllegalArgumentException if the string does not match any node variable name
	 */
	public int getNodeVariableIndex(String name);
	
	/**
	 * Checks weather this element defines the node variable (either the unknown or the constant)
	 * @param name The name of the variable to check for
	 * @param constant If true, checks weather its defined as a constant, if false it checks for an unknown
	 * @return true if and only if a variable is defined with the name and it is the type (constant/unknown) that was requested
	 */
	public boolean hasNodeVariable(String name, boolean constant);
	
	/**
	 * The list of local variable pairs (unknown and constant) defined for this element type.
	 * These are computed for the system just like node variables, but are local to each instance of this element and not shared like connected nodes between elements.
	 * Each entry consists of the pair of the solution vector variable name, and the forcing vector constant name.
	 * @return the list of local variable names in pairs
	 */
	public List<SystemVariablePair> locals();
	
	/**
	 * Gets the index of the local variable name in this element type.
	 * @param name The name of the local variable defined for this element type
	 * @return the index of the local in this element
	 * @throws IllegalArgumentException if the string does not match any local variable name
	 */
	public int getLocalVariableIndex(String name);

	/**
	 * Checks weather this element defines the local variable (either the unknown or the constant)
	 * @param name The name of the variable to check for
	 * @param constant If true, checks weather its defined as a constant, if false it checks for an unknown
	 * @return true if and only if a variable is defined with the name and it is the type (constant/unknown) that was requested
	 */
	public boolean hasLocalVariable(String name, boolean constant);
	
	/**
	 * The list of element variables, properties which are configured for each instance of this element during creation of the network.
	 * These are constant during the simulation. 
	 * @return the list of element variable names
	 */
	public List<ElementVariable> variables();

	/**
	 * Invoking this function will apply the elements matrix stamp to the system matrices provided.
	 * If required, it will also compute the linearization of the element on the current operation point, which will be taken from the parameters in the element state (usually the results of the previous iteration, or zero if this is the first iteration).
	 * @param ctx The stamping context, providing additional information for this step
	 * @param A The system A matrix (time invariant part)
	 * @param E The system E matrix (time variant part)
	 * @param z The forcing vector (constants)
	 * @param state The element instance/state
	 * @throws NodalMatrixStampException 
	 */
	public void stampMatricies(NodalNetwork.StampingContext ctx, MatrixNd A, MatrixNd E, MatrixNd z, MatrixNd x, NodalElementState state) throws NodalMatrixStampException;

	/**
	 * Create a new state/instance of this element which can be added to an network.
	 * @param name The name of the new instance of this element
	 * @return the state instance for the new element
	 */
	public default NodalElementState newInstance(String name) {
		NodalElementState state = new NodalElementState(name, this);
		for (var variable : this.variables())
			state.setParameter(variable.name, variable.defaultValue);
		return state;
	}
	
}
