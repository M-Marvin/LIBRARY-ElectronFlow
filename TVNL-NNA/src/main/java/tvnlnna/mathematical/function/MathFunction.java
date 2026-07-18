package tvnlnna.mathematical.function;

import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import tvnlnna.mathematical.MathematicalEvaluationException;
import tvnlnna.mathematical.MathematicalExpressionException;
import tvnlnna.mathematical.expression.MathExpression;
import tvnlnna.mathematical.expression.MathTerm;
import tvnlnna.mathematical.expression.MathExpression.ValueAndDerivative;
import tvnlnna.mathematical.term.MathParsingContext;

public class MathFunction implements MathTerm {
	
	private final String name;
	private final List<String> variables;
	private final MathExpression expression;
	
	public MathFunction(String name, MathExpression expression, List<String> variables) {
		this.name = name;
		this.variables = Collections.unmodifiableList(variables);
		this.expression = expression;
	}
	
	private static final Pattern FUNCTION_PATTERN = Pattern.compile("(\\w+)\\s*\\(\\s*(\\w+(?:\\s*,\\s*\\w+)*)\\s*\\)\\s*:=\\s*(.*)");
	
	/**
	 * Parses an single function defined as infix notation with the supplied context and constructs a new function object which can be evaluated.
	 * The created function instance is also added to the context so it can be used in further function that are being parsed with the same context.
	 * The variable list in the context will be cleared before parsing the function, and will contain the variables only of this function after completion.
	 * @param s The infix notation of the function
	 * @param ctx The parsing context.
	 * @return The newly created function instance
	 * @throws MathematicalExpressionException If the function is malformed, note that not all cases of ill-formed function are captured, some will only show up during evaluation
	 */
	public static MathFunction parseInfix(String s, MathParsingContext ctx) {
		
		Matcher m = FUNCTION_PATTERN.matcher(s);
		if (!m.find())
			throw new MathematicalExpressionException("not a valid function syntax: " + s);
		
		try {

			ctx.clearVariables();
			
			String name = m.group(1);
			List<String> variables = Stream.of(m.group(2).split(",")).map(String::strip).toList();
			MathExpression expression = MathExpression.parseInfix(m.group(3), ctx);
			
			if (!variables.containsAll(ctx.listVariables()))
				throw new MathematicalExpressionException("function expression contains undefined variables (defined/contained): " + ctx.listVariables() + "/" + variables);
			
			MathFunction f = new MathFunction(name, expression, variables);
			ctx.addFunction(f);
			return f;
			
		} catch (MathematicalExpressionException e) {
			throw new MathematicalExpressionException("unable to parse function expression: " + s, e);
		}
		
	}
	
	/**
	 * Parses an list of functions defined as infix notation separated trough an semicolon with the supplied context and constructs a new function object which can be evaluated.
	 * The created function instances are also added to the context so they can be used in further functions that are being parsed with the same context.
	 * The variable list in the context will be cleared before parsing each function, and will contain the variables only of the last function after completion.
	 * Only the last parsed function will be returned, tough all functions will be registered in the context.
	 * @param s The infix notation of the functions
	 * @param ctx The parsing context.
	 * @return The newly created function instance
	 * @throws MathematicalExpressionException If a function is malformed, note that not all cases of ill-formed functions are captured, some will only show up during evaluation
	 */
	public static MathFunction parseInfixList(String s, MathParsingContext ctx) {
		MathFunction f = null;
		for (String s1 : s.split(";")) {
			if (s1.isBlank())
				continue;
			
			f = parseInfix(s1.strip(), ctx);
		}
		return f;
	}

	/**
	 * The name of this function that was defined in the parsed string.
	 * @return The name of this function
	 */
	public String name() {
		return this.name;
	}
	
	/**
	 * Returns the list of variables defined for this function, in the order in which they where defined and are accepted by the evaluation methods.
	 * @return A ordered list of the variables of this function.
	 */
	public List<String> variables() {
		return this.variables;
	}
	
	/**
	 * Returns the expression which computes the results of this function.
	 * @return The expression instance of this function
	 */
	public MathExpression expression() {
		return this.expression;
	}

	/**
	 * Evaluates the results of this function.
	 * @param parameters The list of parameters (variable values) for this evaluation
	 */
	public double evaluat(double... parameters) {
		if (parameters.length != this.variables.size())
			throw new IllegalArgumentException("parameter count does not match variable count");
		
		Map<String, Double> parameterMap = new HashMap<String, Double>();
		for (int i = 0; i < parameters.length; i++)
			parameterMap.put(this.variables.get(i), parameters[i]);
		
		return evaluate(parameterMap);
	}

	/**
	 * Evaluates the results of this function and its partial derivative in respect to the given variable.
	 * @param variable The variable for which the partial derivative should be computed
	 * @param parameters The list of parameters (variable values) for this evaluation
	 */
	public ValueAndDerivative evaluateAndDerive(String variable, double... parameters) {
		if (parameters.length != this.variables.size())
			throw new IllegalArgumentException("parameter count does not match variable count");
		
		Map<String, Double> parameterMap = new HashMap<String, Double>();
		for (int i = 0; i < parameters.length; i++)
			parameterMap.put(this.variables.get(i), parameters[i]);
		
		return evaluateAndDerive(variable, parameterMap);
	}

	/**
	 * Evaluates the results of this function and its partial derivative in respect to the given variable.
	 * @param variable The variable index for which the partial derivative should be computed
	 * @param parameters The list of parameters (variable values) for this evaluation
	 */
	public ValueAndDerivative evaluateAndDerive(int variable, double... parameters) {
		if (parameters.length != this.variables.size())
			throw new IllegalArgumentException("parameter count does not match variable count");
		
		if (variable >= this.variables.size() || variable < 0)
			throw new IndexOutOfBoundsException("variable index out of range [0-" + this.variables.size() + ")");
		
		Map<String, Double> parameterMap = new HashMap<String, Double>();
		for (int i = 0; i < parameters.length; i++)
			parameterMap.put(this.variables.get(i), parameters[i]);
		
		return evaluateAndDerive(this.variables.get(variable), parameterMap);
	}

	/**
	 * Evaluates the results of this function.
	 * @param parameters The parameters (variable values) for this evaluation
	 */
	public double evaluate(Map<String, Double> parameters) {
		return this.expression.evaluate(parameters);	
	}
	
	/**
	 * Evaluates the results of this function and its partial derivative in respect to the given variable.
	 * @param variable The variable for which the partial derivative should be computed
	 * @param parameters The parameters (variable values) for this evaluation
	 */
	public ValueAndDerivative evaluateAndDerive(String variable, Map<String, Double> parameters) {
		return this.expression.evaluateAndDerive(parameters, variable);
	}
	
	@Override
	public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
		try {
			double[] parameterList = new double[this.variables.size()];
			for (int i = 0; i < parameterList.length; i++)
				parameterList[parameterList.length - i - 1] = evalstack.pop();
			evalstack.add(evaluat(parameterList));
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for function: " + str());
		}
	}

	@Override
	public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
		try {
			double[] parameterList = new double[this.variables.size()];
			double[] parameterListDiff = new double[this.variables.size()];
			for (int i = parameterList.length - 1; i >= 0; i--) {
				parameterList[i] = evalstack.pop();
				parameterListDiff[i] = diffstack.pop();
			}
			
			double derivative = 0.0;
			Double value = null;
			for (int i = 0; i < parameterList.length; i++) {
				if (parameterListDiff[i] == 0.0)
					continue;
				ValueAndDerivative result = evaluateAndDerive(i, parameterList);
				derivative += result.derivative() * parameterListDiff[i];
				if (value == null)
					value = result.value();
			}
			
			if (value == null)
				value = evaluat(parameterList);
			
			evalstack.add(value);
			diffstack.add(derivative);
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for function: " + str());
		}
	}

	@Override
	public String str() {
		return this.name;
	}

	@Override
	public TermType type() {
		return TermType.LABEL;
	}
	
	@Override
	public String toString() {
		return "MathFunction{ " + str() + " := " + this.expression.str() + " }";
	}
	
}
