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
import tvnlnna.mathematical.expression.MathExpression.ValueAndDerivative;
import tvnlnna.mathematical.function.MathFunction;
import tvnlnna.mathematical.term.MathParsingContext;
import tvnlnna.nodal.NodalElement.StampPattern.StampPatternAdapter;

/**
 * This is an {@link INodalElement} instance parsed from an XML file.
 * @see INodalElement
 */
@XMLRootType("nodalElement")
@XMLType
public class NodalElement implements INodalElement {
	
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
		
		@Override
		public String toString() {
			return "SystemVariable{ name = " + this.name + ", constant = " + this.constant + "}";
		}
		
	}
	
	@XMLField(value = FieldType.ELEMENT_COLLECTION, name = "node", type = SystemVariablePair.class)
	private List<SystemVariablePair> nodes = new ArrayList<NodalElement.SystemVariablePair>();
	@XMLField(value = FieldType.ELEMENT_COLLECTION, name = "local", type = SystemVariablePair.class)
	private List<SystemVariablePair> locals = new ArrayList<NodalElement.SystemVariablePair>();
	
	@XMLType
	public class ElementVariable {
		
		@XMLField(FieldType.ATTRIBUTE)
		public String name;
		@XMLField(value = FieldType.ATTRIBUTE, name = "default")
		public double defaultValue;
		
		@Override
		public String toString() {
			return "ElementVariable{ name = " + this.name + " }";
		}
		
	}
	
	@XMLField(value = FieldType.ELEMENT_COLLECTION, name = "variable", type = ElementVariable.class)
	private List<ElementVariable> variables = new ArrayList<NodalElement.ElementVariable>();
	
	/* XML - element function set */
	
	@XMLType
	public class ElementFunctionSet {
		
		public static class ElementFunctionSetAdapter implements XMLClassFieldAdapter<MathParsingContext, NodalElement> {

			@Override
			public MathParsingContext adaptType(String str, NodalElement parentObject) throws XMLException {
				try {
					MathParsingContext ctx = MathParsingContext.standard();
					MathFunction.parseInfixList(str, ctx);
					return ctx;
				} catch (MathematicalExpressionException e) {
					throw new XMLException("unable to parse element function: " + str, e);
				}
			}

			@Override
			public String typeString(MathParsingContext value) {
				throw new UnsupportedOperationException();
			}
			
		}
		
		@XMLField(FieldType.TEXT)
		@XMLTypeAdapter(value = ElementFunctionSetAdapter.class, parent = NodalElement.class)
		public MathParsingContext context;
		
		@Override
		public String toString() {
			return "FunctionSet{}";
		}
		
	}

	@XMLField(value = FieldType.ELEMENT)
	private ElementFunctionSet functions;
	
	/* XML - element matrix stamps */
	
	@XMLType
	public class StampComputation {

		public static class StampFunctionAdapter implements XMLClassFieldAdapter<MathExpression, NodalElement> {

			@Override
			public MathExpression adaptType(String str, NodalElement parentObject) throws XMLException {
				try {
					return MathExpression.parseInfix(str, parentObject.mathContext());
				} catch (MathematicalExpressionException e) {
					throw new XMLException("unable to parse stamp computation: " + str, e);
				}
			}

			@Override
			public String typeString(MathExpression value) {
				throw new UnsupportedOperationException();
			}
			
		}
		
		@XMLField(value = FieldType.REMAINING_ATTRIBUTE_MAP, type = MathExpression.class)
		@XMLTypeAdapter(value = StampFunctionAdapter.class, parent = NodalElement.class)
		public Map<String, MathExpression> computations = new HashMap<String, MathExpression>();

		@XMLField(value = FieldType.ATTRIBUTE, name = "derive")
		public String derive;
		
		@Override
		public String toString() {
			return "StampComputation{ derive = " + this.derive + ", " + this.computations + "}";
		}
		
	}
	
	@XMLField(value = FieldType.ELEMENT_COLLECTION, name = "compute", type = StampComputation.class)
	public List<StampComputation> computations = new ArrayList<NodalElement.StampComputation>();

	public <N, E> void updateParameters(NodalElementState<N, E> state) throws NodalMatrixStampException {
		for (var compute : this.computations) {
			if (compute.derive != null) {
				for (var entry : compute.computations.entrySet()) {
					try {
						ValueAndDerivative result = entry.getValue().evaluateAndDerive(state.parameters(), compute.derive);
						state.setParameter(entry.getKey(), result.value());
						state.setParameter(entry.getKey() + "d", result.derivative());
					} catch (MathematicalEvaluationException e) {
						throw new NodalMatrixStampException("unable to evalueate stamp derivation: " + entry.getKey() + " := " + entry.getValue().str(), e);
					}
				}
			} else {
				for (var entry : compute.computations.entrySet()) {
					try {
						state.setParameter(entry.getKey(), entry.getValue().evaluate(state.parameters()));
					} catch (MathematicalEvaluationException e) {
						throw new NodalMatrixStampException("unable to evalueate stamp computation: " + entry.getKey() + " := " + entry.getValue().str(), e);
					}
				}
			}
		}
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
				boolean singleColumn = table[table.length - 1].length == 2 && table[0].length == 2;
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
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("StampPattern\n");
			if (this.stamp != null && this.stamp.length > 0) {
				for (int j = 0; j < this.stamp[0].length; j++) {
					for (int i = 0; i < this.stamp.length; i++) {
						sb.append(this.stamp[i][j].str() + "\t");
					}
					sb.append("\n");
				}
			}
			return sb.toString();
		}
		
	}
	
	@XMLType
	public class ElementStamp {
		
		@XMLField(value = FieldType.ELEMENT_COLLECTION, name = "compute", type = StampComputation.class)
		public List<StampComputation> computations = new ArrayList<NodalElement.StampComputation>();
		
		@XMLField(FieldType.TEXT)
		@XMLTypeAdapter(StampPatternAdapter.class)
		public StampPattern pattern;
		
		public <N, E> void updateParameters(NodalElementState<N, E> state) throws NodalMatrixStampException {
			for (var compute : this.computations) {
				if (compute.derive != null) {
					for (var entry : compute.computations.entrySet()) {
						try {
							ValueAndDerivative result = entry.getValue().evaluateAndDerive(state.parameters(), compute.derive);
							state.setParameter(entry.getKey(), result.value());
							state.setParameter(entry.getKey() + "d", result.derivative());
						} catch (MathematicalEvaluationException e) {
							throw new NodalMatrixStampException("unable to evalueate stamp derivation: " + entry.getKey() + " := " + entry.getValue().str(), e);
						}
					}
				} else {
					for (var entry : compute.computations.entrySet()) {
						try {
							state.setParameter(entry.getKey(), entry.getValue().evaluate(state.parameters()));
						} catch (MathematicalEvaluationException e) {
							throw new NodalMatrixStampException("unable to evalueate stamp computation: " + entry.getKey() + " := " + entry.getValue().str(), e);
						}
					}
				}
			}
		}
		
		public <N, E> double evaluateStampEntry(int i, int j, NodalElementState<N, E> state) throws NodalMatrixStampException {
			return this.pattern.evaluateStampEntry(i, j, state.parameters());
		}
		
		@Override
		public String toString() {
			return "ElementStamp{ computes = " + this.computations + " }\n" + this.pattern;
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
	
	@Override
	public String name() {
		return this.name;
	}

	@Override
	public boolean isNonLinear() {
		return this.nonLinear;
	}

	@Override
	public boolean isTimeVariant() {
		return this.timeVariant;
	}

	@Override
	public MathParsingContext mathContext() {
		return this.functions != null ? this.functions.context : MathParsingContext.standard();
	}

	@Override
	public List<SystemVariablePair> nodes() {
		return this.nodes;
	}

	@Override
	public int getNodeVariableIndex(String name) {
		for (int i = 0; i < this.nodes.size(); i++) {
			if (this.nodes.get(i).name.equals(name) || this.nodes.get(i).constant.equals(name))
				return i;
		}
		throw new IllegalArgumentException("node not defined for element type: " + name() + "/" +  name);
	}

	@Override
	public boolean hasNodeVariable(String name, boolean constant) {
		for (var pair : this.nodes)
			if (constant ? pair.constant.equals(name) : pair.name.equals(name))
				return true;
		return false;
	}

	@Override
	public List<SystemVariablePair> locals() {
		return this.locals;
	}

	@Override
	public int getLocalVariableIndex(String name) {
		for (int i = 0; i < this.locals.size(); i++) {
			if (this.locals.get(i).name.equals(name) || this.locals.get(i).constant.equals(name))
				return i;
		}
		throw new IllegalArgumentException("local not defined for element type: " + name() + "/" +  name);
	}

	@Override
	public boolean hasLocalVariable(String name, boolean constant) {
		for (var pair : this.locals)
			if (constant ? pair.constant.equals(name) : pair.name.equals(name))
				return true;
		return false;
	}

	@Override
	public List<ElementVariable> variables() {
		return this.variables;
	}

	@Override
	public <N, E> void stampMatricies(NodalNetwork<N, E>.StampingContext ctx, MatrixNd A, MatrixNd E, MatrixNd z, MatrixNd x, NodalElementState<N, E> state) throws NodalMatrixStampException {
		
		try {
			
			// update SIMTIME variable
			state.setParameter("SIMTIME", ctx.simtime());
			// execute stamp computations and update parameters with results
			updateParameters(state);
			if (ctx.stampsA() && this.stampA != null)
				this.stampA.updateParameters(state);
			if (ctx.stampsE() && this.stampE != null)
				this.stampE.updateParameters(state);
			if (ctx.stampsZ() && this.stampZ != null)
				this.stampZ.updateParameters(state);
			
			// iterate over all possible stamp entries and insert in corresponding matrices if requested
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
				
				if (ctx.stampsX()) {
					// we assume that everything has been reset to zero before stamping the X vector
					// we also expect that all components agree on the node values, but just in case an element was not
					// initialized, we use the absolute maximum as the final value.
					double v = state.getParamter(j < nodes().size() ? nodes().get(j).name : locals().get(j - nodes().size()).name);
					if (Math.abs(v) > Math.abs(x.m(0, n)))
						x.set(0, n, v);
				}
					
			}
			
		} catch (Exception e) {
			throw new NodalMatrixStampException("failed to stamp element matrix: " + state.name(), e);
		}
		
	}
	
}
