package tvnlnna.mathematical.term;

import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import tvnlnna.mathematical.MathematicalEvaluationException;
import tvnlnna.mathematical.expression.MathOperator;
import tvnlnna.mathematical.expression.MathTerm;
import tvnlnna.mathematical.term.templates.functional.BaseOperator;
import tvnlnna.mathematical.term.templates.functional.DualFunction;
import tvnlnna.mathematical.term.templates.functional.DualOperator;
import tvnlnna.mathematical.term.templates.functional.SingleFunction;

/**
 * The context in which a infix expression is being parsed.
 * The context provides available functions and operators which can be used, and keeps track of which variables where parsed and are required for evaluating the expression.
 */
public class MathParsingContext {
	
	private final Map<String, MathTerm> mathFunction = new HashMap<String, MathTerm>();
	private final Map<String, MathOperator> mathOperators = new HashMap<String, MathOperator>();
	
	private final Map<Double, MathTerm> constantCache = new HashMap<Double, MathTerm>();
	private final Map<String, MathTerm> variableCache = new HashMap<String, MathTerm>();
	
	private MathParsingContext() {}
	
	public static MathParsingContext empty() {
		return new MathParsingContext();
	}
	
	public static MathParsingContext standard() {
		return empty().addStandardOperators().addStandardFunctions();
	}
	
	public MathParsingContext addOperator(MathOperator operator) {
		this.mathOperators.put(operator.str(), operator);
		return this;
	}
	
	public MathParsingContext addFunction(MathTerm function) {
		this.mathFunction.put(function.str(), function);
		return this;
	}
	
	/**
	 * Adds all the standard operators to this context.
	 */
	public MathParsingContext addStandardOperators() {
		
		addOperator(new DualOperator("+", 4, true, (B, A) -> A + B, (B, Bd, A, Ad) -> Ad + Bd));
		addOperator(new DualOperator("-", 4, true, (B, A) -> A - B, (B, Bd, A, Ad) -> Ad - Bd));
		addOperator(new DualOperator("*", 5, true, (B, A) -> A * B, (B, Bd, A, Ad) -> Ad * B + A * Bd));
		addOperator(new DualOperator("/", 5, true, (B, A) -> A / B, (B, Bd, A, Ad) -> (Ad * B + A * Bd) / (B * B)));
		addOperator(new DualOperator("^", 6, false, (B, A) -> Math.pow(A, B), (B, Bd, A, Ad) -> B * Math.pow(A, B - 1)));
		addOperator(new DualOperator("%", 5, false, (B, A) -> A % B, (B, Bd, A, Ad) -> { throw new MathematicalEvaluationException("modulo not differentiable"); }));
		
		addOperator(new DualOperator("=", 1, true, (B, A) -> Double.compare(A, B) == 0 ? 1.0 : 0.0, null));
		addOperator(new DualOperator(">", 1, true, (B, A) -> A > B ? 1.0 : 0.0, null));
		addOperator(new DualOperator("<", 1, true, (B, A) -> A < B ? 1.0 : 0.0, null));
		addOperator(new DualOperator(">=", 1, true, (B, A) -> A >= B ? 1.0 : 0.0, null));
		addOperator(new DualOperator("<=", 1, true, (B, A) -> A <= B ? 1.0 : 0.0, null));
		addOperator(new DualOperator("!=", 1, true, (B, A) -> Double.compare(A, B) != 0 ? 1.0 : 0.0, null));
		
		addOperator(new BaseOperator("!", 6, false) {
			
			@Override
			public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
				try {
					double A = evalstack.pop();
					evalstack.add(A != 0.0 ? 0.0 : 1.0);
				} catch (EmptyStackException e) {
					throw new MathematicalEvaluationException("lacking evalutation stack entries for operation: " + str());
				}
			}
			
			@Override
			public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
				evaluate(evalstack, parameters);
				diffstack.add(evalstack.peek());
			}
			
		});
		
		addOperator(new DualOperator("&", 3, true, (B, A) -> (A != 0.0) && (B  != 0.0) ? 1.0 : 0.0, null));
		addOperator(new DualOperator("|", 2, true, (B, A) -> (A != 0.0) || (B != 0.0) ? 1.0 : 0.0, null));
		addOperator(new DualOperator("°", 2, true, (B, A) -> (A != 0.0) ^  (B != 0.0) ? 1.0 : 0.0, null));
		
		addOperator(new BaseOperator("?", 0, true) {
			
			@Override
			public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
				try {
					double A = evalstack.pop();
					double B = evalstack.pop();
					double C = evalstack.pop();
					evalstack.add(C != 0.0 ? B : A);
				} catch (EmptyStackException e) {
					throw new MathematicalEvaluationException("lacking evalutation stack entries for operation: " + str());
				}
			}
			
			@Override
			public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
				evaluate(evalstack, parameters);
				try {
					double Ad = diffstack.pop();
					double Bd = diffstack.pop();
					double Cd = diffstack.pop();
					diffstack.add(Cd != 0.0 ? Bd : Ad);
				} catch (EmptyStackException e) {
					throw new MathematicalEvaluationException("lacking evalutation stack entries for operation: " + str());
				}
			}
			
			@Override
			public boolean implicitBracket() {
				return true;
			}
			
		});
		
		return this;
	}
	
	/**
	 * Adds all the standard functions to this context.
	 */
	public MathParsingContext addStandardFunctions() {
		
		addFunction(new SingleFunction("sin", (x) -> Math.sin(x), (x, xd) -> Math.cos(x) * xd));
		addFunction(new SingleFunction("cos", (x) -> Math.cos(x), (x, xd) -> -Math.sin(x) * xd));
		addFunction(new SingleFunction("tan", (x) -> Math.tan(x), (x, xd) -> (1 + Math.pow(Math.tan(x), 2)) * xd));
		addFunction(new SingleFunction("asin", (x) -> Math.asin(x), (x, xd) -> (1 / Math.sqrt(1 - Math.pow(x, 2))) * xd));
		addFunction(new SingleFunction("acos", (x) -> Math.acos(x), (x, xd) -> (-1 / Math.sqrt(1 - Math.pow(x, 2))) * xd));
		addFunction(new SingleFunction("atan", (x) -> Math.atan(x), (x, xd) -> (1 / (1 + Math.pow(x, 2))) * xd));
		
		addFunction(new SingleFunction("log", (x) -> Math.log(x), (x, xd) -> (1 / x) * xd));
		addFunction(new SingleFunction("log10", (x) -> Math.log10(x), (x, xd) -> (1 / (x * Math.log(10))) * xd));
		addFunction(new SingleFunction("exp", (x) -> Math.exp(x), (x, xd) -> Math.exp(x) * xd));
		addFunction(new SingleFunction("sqrt", (x) -> Math.sqrt(x), (x, xd) -> (0.5 * Math.pow(x, -1.5)) * xd));
		addFunction(new DualFunction("pow", (B, A) -> Math.pow(A, B), (B, Bd, A, Ad) -> (B * Math.pow(A, B - 1) * Ad) + (Math.pow(A, B) * Math.log(A) * Bd)));
		
		addFunction(new SingleFunction("abs", (X) -> Math.abs(X), (X, Xd) -> X < 0 ? -Xd : Xd));
		addFunction(new SingleFunction("ceil", (x) -> Math.ceil(x), (x, xd) -> 0.0));
		addFunction(new SingleFunction("floor", (x) -> Math.floor(x), (x, xd) -> 0.0));
		addFunction(new SingleFunction("round", (x) -> Math.round(x), (x, xd) -> 0.0));
		addFunction(new DualFunction("min", (B,  A) -> Math.min(A, B), (B, Bd, A, Ad) -> A <= B ? Ad : Bd));
		addFunction(new DualFunction("max", (B,  A) -> Math.max(A, B), (B, Bd, A, Ad) -> A >= B ? Ad : Bd));
		
		return this;
	}
	
	/**
	 * Lists all the variables which where discovered by parsing with this context.
	 * @return A list of variables that where used in expressions parsed with this context.
	 */
	public Collection<String> listVariables() {
		return this.variableCache.keySet();
	}
	
	/**
	 * Clears the list of variables, allows the context to be reused without having previously discovered variables in the list.
	 */
	public void clearVariables() {
		this.variableCache.clear();
	}
	
	/**
	 * Creates a term for an constant numeric value.
	 * @param value The numeric value of the term
	 * @return The term representing the numeric value
	 */
	public MathTerm constant(double value) {
		MathTerm term = this.constantCache.get(value);
		if (term == null) {
			term = new MathTerm() {
				
				@Override
				public TermType type() {
					return TermType.NUMERIC;
				}
				
				@Override
				public String str() {
					return Double.toString(value);
				}
				
				@Override
				public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
					evalstack.add(value);
				}
				
				@Override
				public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
					evaluate(evalstack, parameters);
					diffstack.add(0.0);
				}

				@Override
				public String toString() {
					return "Constant{ " + str() + " }";
				}
				
			};
			this.constantCache.put(value, term);
		};
		return term;
	}
	
	/**
	 * Creates a term for an variable.
	 * @param str The name of the variable
	 * @return The term representing the variable
	 */
	public MathTerm variable(String str) {
		MathTerm term = this.variableCache.get(str);
		if (term == null) {
			term = new MathTerm() {
				
				@Override
				public TermType type() {
					return TermType.LABEL;
				}
				
				@Override
				public String str() {
					return str;
				}
				
				@Override
				public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
					Double d = parameters.get(str);
					if (d == null)
						throw new MathematicalEvaluationException("parameter not defined: " + str);
					evalstack.add(d);
				}
				
				@Override
				public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
					evaluate(evalstack, parameters);
					diffstack.add(str.equals(variable) ? 1.0 : 0.0);
				}
				
				@Override
				public String toString() {
					return "Variable{ " + str() + " }";
				}
				
			};
			this.variableCache.put(str, term);
		};
		return term;
	}
	
	/**
	 * Returns the term for the function.
	 * @param str The name of the function
	 * @return The term representing the function or null if the function is not found within this context
	 */
	public MathTerm function(String str) {
		return this.mathFunction.get(str);
	}
	
	/**
	 * Returns the term for the operator.
	 * @param str The name of the operator
	 * @return The term representing the operator or null if the operator is not found within this context
	 */
	public MathOperator operator(String str) {
		return this.mathOperators.get(str);
	}
	
}
