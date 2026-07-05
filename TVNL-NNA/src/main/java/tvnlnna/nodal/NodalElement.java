package tvnlnna.nodal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import de.m_marvin.basicxml.XMLException;
import de.m_marvin.basicxml.marshaling.adapter.XMLClassFieldAdapter;
import de.m_marvin.basicxml.marshaling.annotations.XMLField;
import de.m_marvin.basicxml.marshaling.annotations.XMLField.FieldType;
import de.m_marvin.basicxml.marshaling.annotations.XMLRootType;
import de.m_marvin.basicxml.marshaling.annotations.XMLType;
import de.m_marvin.basicxml.marshaling.annotations.XMLTypeAdapter;
import de.m_marvin.unimat.impl.MatrixNd;
import tvnlnna.NodalMatrixStampException;
import tvnlnna.mathematical.MathematicalEvaluationException;
import tvnlnna.mathematical.MathematicalExpressionException;
import tvnlnna.mathematical.expression.MathExpression;
import tvnlnna.mathematical.function.MathFunction;
import tvnlnna.mathematical.term.MathParsingContext;
import tvnlnna.nodal.NodalElement.StampPattern.StampPatternAdapter;

/**
 * The definition of an element type.
 * Each element used in a network will need an element definition assigned, which can be parsed from an model XML file.
 * The element defined properties of the element required for simulation, such as what variables it adds to the system of equations.<br>
 * There are three types of variables:<br>
 * - node variables: Will be shared with other elements with's nodes are connected with this elements nodes<br>
 * - local variables: Will be local to each instance of the element, such as currents trough an voltage source<br>
 * - element variables: Will be constant during simulation, and can only be configured before starting the simulation, such as conductivity of an resistor.<br>
 * <br>
 * Additonaly, node and local variables will consist of a pair of two variables, one will be the actual variable, the other the corresponding constant which is used to form the equation of the system of equations.
 * In electrical simulations the node variable will have the unit volt, and correspond to the potential of the node, the constant will have the unit ampere, and will be the sum of current in that node.
 */
@XMLRootType("nodalElement")
@XMLType
public class NodalElement {
	
	/* XML - general element attributes */
	
	@XMLField(FieldType.ATTRIBUTE)
	private String name;
	@XMLField(FieldType.ATTRIBUTE)
	private boolean nonLinear;
	@XMLField(FieldType.ATTRIBUTE)
	private boolean timeVariant;

	/* XML - element variables, nodes, localss */
	
	@XMLType
	public class SystemVariablePair {
		
		@XMLField(FieldType.ATTRIBUTE)
		public String name;
		
		@XMLField(FieldType.ATTRIBUTE)
		public String constant;
		
	}
	
	@XMLField(value = FieldType.ELEMENT_COLLECTION, name = "node", type = SystemVariablePair.class)
	private List<SystemVariablePair> nodes = new ArrayList<NodalElement.SystemVariablePair>();
	@XMLField(value = FieldType.ELEMENT_COLLECTION, name = "local", type = SystemVariablePair.class)
	private List<SystemVariablePair> locals = new ArrayList<NodalElement.SystemVariablePair>();
	
	@XMLType
	public class ElementVariable {
		
		@XMLField(FieldType.ATTRIBUTE)
		public String name;
		
	}
	
	@XMLField(value = FieldType.ELEMENT_COLLECTION, name = "variable", type = ElementVariable.class)
	private List<ElementVariable> variables = new ArrayList<NodalElement.ElementVariable>();
	
	/* XML - element function set */
	
	@XMLType
	public class ElementFunctionSet {
		
		public static class ElementFunctionSetAdapter implements XMLClassFieldAdapter<MathParsingContext, NodalElement> {

			@Override
			public MathParsingContext adaptType(String str, NodalElement parentObject) {
				MathParsingContext ctx = MathParsingContext.standard();
				MathFunction.parseInfixList(str, ctx);
				return ctx;
			}

			@Override
			public String typeString(MathParsingContext value) {
				throw new UnsupportedOperationException();
			}
			
		}
		
		@XMLField(FieldType.TEXT)
		@XMLTypeAdapter(value = ElementFunctionSetAdapter.class, parent = NodalElement.class)
		public MathParsingContext context;
		
	}

	@XMLField(value = FieldType.ELEMENT)
	private ElementFunctionSet functions;
	
	/* XML - element matrix stamps */
	
	@XMLType
	public class StampComputation {

		public static class StampFunctionAdapter implements XMLClassFieldAdapter<MathExpression, NodalElement> {

			@Override
			public MathExpression adaptType(String str, NodalElement parentObject) {
				return MathExpression.parseInfix(str, parentObject.mathContext());
			}

			@Override
			public String typeString(MathExpression value) {
				throw new UnsupportedOperationException();
			}
			
		}
		
		@XMLField(value = FieldType.REMAINING_ATTRIBUTE_MAP, type = MathExpression.class)
		@XMLTypeAdapter(value = StampFunctionAdapter.class, parent = NodalElement.class)
		public Map<String, MathExpression> computations = new HashMap<String, MathExpression>();
		
	}
	
	@XMLType
	public class StampDerivation extends StampComputation {
		
		@XMLField(value = FieldType.ATTRIBUTE, name = "var")
		public String variable;
		
	}
	
	@XMLType
	public class StampPattern {
		
		public static class StampPatternAdapter implements XMLClassFieldAdapter<StampPattern, NodalElement> {

			@Override
			public StampPattern adaptType(String str, NodalElement parentObject) throws XMLException {
				MathParsingContext ctx = parentObject.mathContext();
				
				// split string in rows and columns to form string table
				String[][] table = str.lines().map(s -> Stream.of(s.split("\\t")).filter(s1 -> !s1.isBlank()).map(String::strip).toArray(String[]::new)).toArray(String[][]::new);
				// check if its a single column table by testing the last column for length 2 (label + value)
				boolean singleColumn = table[table.length - 1].length == 2;
				// compute expected element stamp size (node + local count)
				int s = parentObject.nodes().size() + parentObject.locals().size();
				
				StampPattern pattern = parentObject.new StampPattern();
				if (singleColumn) {
					// check if table length matches element stamp size
					if (table.length != s)
						throw new XMLException("unable to parse stamp table, row count does not match node + local count");
					// check if all rows have the same correct size
					for (int i = 0; i < table.length; i++)
						if (table[i].length != 2)
							throw new XMLException("unable to parse stamp table, row has more than one entry");
					
					// initialize stamp expression table
					pattern.stamp = new MathExpression[][] { new MathExpression[table.length] };
					for (int i = 0; i < table.length; i++) {
						String label = table[i][0];
						String exprstr = table[i][1];
						
						// check if the unknown label is valid and determine its place in the stamp expression table
						int indx;
						if (parentObject.hasNodeVariable(label, true))
							indx = parentObject.getNodeVariableIndex(label);
						else if (parentObject.hasLocalVariable(label, true))
							indx = parentObject.getLocalVariableIndex(label) + parentObject.nodes().size();
						else
							throw new XMLException("unable to parse stamp table, row has undefined constant name: " + label);
						
						// parse and place expression in table, use context defined in function block
						try {
							pattern.stamp[0][indx] = MathExpression.parseInfix(exprstr, ctx);
						} catch (MathematicalExpressionException e) {
							throw new XMLException("unable to parse stamp table expressioin: " + exprstr, e);
						}
					}
				} else {
					// check if table length matches element stamp size
					if (table.length - 1 != s)
						throw new XMLException("unable to parse stamp table, row count does not match node + local count");
					// check if first table row width matches element stamp size (first row will be 1 shorter because it only contains labels for columns)
					if (table[0].length != s)
						throw new XMLException("unable to parse stamp table, first column labels dont match node + local count");
					// check if all following rows have the same correct size
					for (int i = 1; i < table.length; i++)
						if (table[i].length - 1 != s)
							throw new XMLException("unable to parse stamp table, row length does not match node + local count");
					
					// initialize stamp expression table
					pattern.stamp = new MathExpression[table.length - 1][table.length - 1];
					for (int i = 0; i < pattern.stamp.length; i++)
						pattern.stamp[i] = new MathExpression[table.length -1];
					for (int i = 0; i < table.length - 1; i++) {
						for (int j = 0; j < table.length - 1; j++) {
							String labelR = table[i + 1][0];
							String labelC = table[0][j];
							String exprstr = table[i + 1][j + 1];
							
							// check if the unknown is valid and determine its row location in the expression table
							int indxR;
							if (parentObject.hasNodeVariable(labelR, true))
								indxR = parentObject.getNodeVariableIndex(labelR);
							else if (parentObject.hasLocalVariable(labelR, true))
								indxR = parentObject.getLocalVariableIndex(labelR) + parentObject.nodes().size();
							else
								throw new XMLException("unable to parse stamp table, row has undefined constant name: " + labelR);
							// check if the unknown is valid and determine its column location in the expression table
							int indxC;
							if (parentObject.hasNodeVariable(labelC, false))
								indxC = parentObject.getNodeVariableIndex(labelC);
							else if (parentObject.hasLocalVariable(labelC, false))
								indxC = parentObject.getLocalVariableIndex(labelC) + parentObject.nodes().size();
							else
								throw new XMLException("unable to parse stamp table, column has undefined variable name: " + labelC);
							
							// parse and place expression in table, use context defined in function block
							try {
								pattern.stamp[indxC][indxR] = MathExpression.parseInfix(exprstr, ctx);
							} catch (MathematicalExpressionException e) {
								throw new XMLException("unable to parse stamp table expressioin: " + exprstr, e);
							}
						}
					}
				}
				return pattern;
			}
			
			@Override
			public String typeString(StampPattern value) {
				throw new UnsupportedOperationException();
			}
			
		}

		public MathExpression[][] stamp;
		
		public double evaluateStampEntry(int i, int j, Map<String, Double> parameters) throws NodalMatrixStampException {
			try {
				return this.stamp[i][j].evaluate(parameters);
			} catch (MathematicalEvaluationException e) {
				throw new NodalMatrixStampException("unable to evaluate stamp expression: " + this.stamp[i][j].str(), e);
			}
		}
		
	}
	
	@XMLType
	public class ElementStamp {
		
		@XMLField(value = FieldType.ELEMENT_COLLECTION, name = "compute", type = StampComputation.class)
		public List<StampComputation> computations = new ArrayList<NodalElement.StampComputation>();
		
		@XMLField(value = FieldType.ELEMENT_COLLECTION, name = "derive", type = StampDerivation.class)
		public List<StampDerivation> derivatives = new ArrayList<NodalElement.StampDerivation>();
		
		@XMLField(FieldType.TEXT)
		@XMLTypeAdapter(StampPatternAdapter.class)
		public StampPattern pattern;
		
		public void updateParameters(NodalElementState state) throws NodalMatrixStampException {
			for (var compute : this.computations)
				for (var entry : compute.computations.entrySet()) {
					try {
						state.setParameter(entry.getKey(), entry.getValue().evaluate(state.parameters()));
					} catch (MathematicalEvaluationException e) {
						throw new NodalMatrixStampException("unable to evalueate stamp computation: " + entry.getKey() + " := " + entry.getValue().str(), e);
					}
				}
			for (var derive : this.derivatives)
				for (var entry : derive.computations.entrySet()) {
					try {
						state.setParameter(entry.getKey(), entry.getValue().evaluateAndDerive(state.parameters(), derive.variable).derivative());
					} catch (MathematicalEvaluationException e) {
						throw new NodalMatrixStampException("unable to evalueate stamp computation: " + entry.getKey() + "' := d/d" + derive.variable + "[ " + entry.getValue().str() + " ]", e);
					}
				}
		}
		
		public double evaluateStampEntry(int i, int j, NodalElementState state) throws NodalMatrixStampException {
			return this.pattern.evaluateStampEntry(i, j, state.parameters());
		}
		
	}

	@XMLField(FieldType.ELEMENT)
	private ElementStamp stampA;
	@XMLField(FieldType.ELEMENT)
	private ElementStamp stampE;
	@XMLField(FieldType.ELEMENT)
	private ElementStamp stampZ;
	
	/* XML - end of parsed data section */
	
	@Override
	public String toString() {
		return "NodalElement{ name = " + this.name + ", nonLinear = " + this.nonLinear + ", timeVariant = " + this.timeVariant + " }";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodalElement other) {
			return other.name.equals(this.name);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
	
	/**
	 * Identifier for this type of element.
	 * @return the identifier string of this element type
	 */
	public String name() {
		return this.name;
	}

	/**
	 * Checks if this element is marked as non linear.
	 * @return true if and only if it was marked as modified
	 */
	public boolean isNonLinear() {
		return this.nonLinear;
	}
	
	/** 
	 * Checks if this element is marked as time variant.
	 * @return true if and only if it was marked as time variant
	 */
	public boolean isTimeVariant() {
		return this.timeVariant;
	}
	
	/**
	 * The context used to parse math expressions for this element.
	 * The context is defined trough the function-block of the XML file and provides mathematical functions that can be used in other expressions.
	 * @return the math parsing context that can be used to parse math expressions for this element
	 */
	public MathParsingContext mathContext() {
		return this.functions != null ? this.functions.context : MathParsingContext.standard();
	}
	
	/**
	 * The list of node variable pairs (unknown and constant) defined for this element type.
	 * Each entry consists of the pair of the solution vector variable name, and the forcing vector constant name.
	 * @return the list of node variable names in pairs
	 */
	public List<SystemVariablePair> nodes() {
		return this.nodes;
	}
	
	/**
	 * Gets the index of the node variable name in this element type.
	 * @param name The name of the node variable defined for this element type
	 * @return the index of the node in this element
	 * @throws IllegalArgumentException if the string does not match any node variable name
	 */
	public int getNodeVariableIndex(String name) {
		for (int i = 0; i < this.nodes.size(); i++) {
			if (this.nodes.get(i).name.equals(name) || this.nodes.get(i).constant.equals(name))
				return i;
		}
		throw new IllegalArgumentException("node not defined for element type: " + name() + "/" +  name);
	}
	
	/**
	 * Checks weather this element defines the node variable (either the unknown or the constant)
	 * @param name The name of the variable to check for
	 * @param constant If true, checks weather its defined as a constant, if false it checks for an unknown
	 * @return true if and only if a variable is defined with the name and it is the type (constant/unknown) that was requested
	 */
	public boolean hasNodeVariable(String name, boolean constant) {
		for (var pair : this.nodes)
			if (constant ? pair.constant.equals(name) : pair.name.equals(name))
				return true;
		return false;
	}
	
	/**
	 * The list of local variable pairs (unknown and constant) defined for this element type.
	 * These are computed for the system just like node variables, but are local to each instance of this element and not shared like connected nodes between elements.
	 * Each entry consists of the pair of the solution vector variable name, and the forcing vector constant name.
	 * @return the list of local variable names in pairs
	 */
	public List<SystemVariablePair> locals() {
		return this.locals;
	}
	
	/**
	 * Gets the index of the local variable name in this element type.
	 * @param name The name of the local variable defined for this element type
	 * @return the index of the local in this element
	 * @throws IllegalArgumentException if the string does not match any local variable name
	 */
	public int getLocalVariableIndex(String name) {
		for (int i = 0; i < this.locals.size(); i++) {
			if (this.locals.get(i).name.equals(name) || this.locals.get(i).constant.equals(name))
				return i;
		}
		throw new IllegalArgumentException("local not defined for element type: " + name() + "/" +  name);
	}

	/**
	 * Checks weather this element defines the local variable (either the unknown or the constant)
	 * @param name The name of the variable to check for
	 * @param constant If true, checks weather its defined as a constant, if false it checks for an unknown
	 * @return true if and only if a variable is defined with the name and it is the type (constant/unknown) that was requested
	 */
	public boolean hasLocalVariable(String name, boolean constant) {
		for (var pair : this.locals)
			if (constant ? pair.constant.equals(name) : pair.name.equals(name))
				return true;
		return false;
	}
	
	/**
	 * The list of element variables, properties which are configured for each instance of this element during creation of the network.
	 * These are constant during the simulation. 
	 * @return the list of element variable names
	 */
	public List<ElementVariable> variables() {
		return this.variables;
	}
	
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
	public void stampMatricies(NodalNetwork.StampingContext ctx, MatrixNd A, MatrixNd E, MatrixNd z, NodalElementState state) throws NodalMatrixStampException {
		
		try {

			state.setParameter("SIMTIME", ctx.simtime());
			if (ctx.stampsA() && this.stampA != null)
				this.stampA.updateParameters(state);
			if (ctx.stampsE() && this.stampE != null)
				this.stampE.updateParameters(state);
			if (ctx.stampsZ() && this.stampZ != null)
				this.stampZ.updateParameters(state);
			
			int s = nodes().size() + locals().size();
			for (int j = 0; j < s; j++) {
				int n = j < nodes().size() ? state.nodes()[j] - 1 : state.localIds()[j - nodes().size()] + ctx.nodeCount();
				 
				for (int i = 0; i < s; i++) {
					int m = i < nodes().size() ? state.nodes()[i] - 1 : state.localIds()[i - nodes().size()] + ctx.nodeCount();
					
					if (n < 0 || m < 0)
						continue;
					
					if (ctx.stampsA() && this.stampA != null)
						A.addM(m, n, stampA.evaluateStampEntry(i, j, state));
					if (ctx.stampsE() && this.stampE != null)
						E.addM(m, n, stampE.evaluateStampEntry(i, j, state));
					
				}
				
				if (n < 0)
					continue;
				
				if (ctx.stampsZ() && this.stampZ != null)
					z.addM(0, n, stampZ.evaluateStampEntry(0, j, state));
			}
			
		} catch (Exception e) {
			throw new NodalMatrixStampException("failed to stamp element matrix: " + state.name(), e);
		}
		
	}
	
	public NodalElementState newInstance(String name) {
		return new NodalElementState(name, this);
	}
	
}
