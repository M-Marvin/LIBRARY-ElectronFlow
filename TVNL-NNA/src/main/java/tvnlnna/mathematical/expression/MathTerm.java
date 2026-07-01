package tvnlnna.mathematical.expression;

import java.util.Map;
import java.util.Stack;
import java.util.function.Predicate;

import tvnlnna.mathematical.MathematicalEvaluationException;

/**
 * A term represents the most primitive token of an mathematical expression. Example would be numerical constants, operators, brackets, variables and functions.
 */
public interface MathTerm {

	/**
	 * Only relevant for parsing, there exist four types of terms (or tokens):<br>
	 * - numeric: A numerical constant<br>
	 * - label: A text label naming a variable or function<br>
	 * - operator: A mathematical operator, excluding brackets and separators<br>
	 * - structure: Brackets and argument separators<br>
	 */
	public static enum TermType {
		NUMERIC(c -> c >= '0' && c <= '9', c -> c == 'E' || c == 'e' || c == '.'),
		LABEL(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z', c -> c >= '0' && c <= '9' || c == '_'),
		STRUCTURE(c -> c == '(' || c == ')' || c == '[' || c == ']' || c == ',' || c == ':', c -> false),
		OPERATOR(c -> !(NUMERIC.test(c, true) || LABEL.test(c, true) || STRUCTURE.test(c, true)), c -> false);
		
		private final Predicate<Character> firstCharPredicate;
		private final Predicate<Character> additonalCharPredicate;
		
		private TermType(Predicate<Character> firstCharPredicate, Predicate<Character> additonalCharPredicate) {
			this.firstCharPredicate = firstCharPredicate;
			this.additonalCharPredicate = additonalCharPredicate;
		}
		
		/**
		 * Tests weather the provided char is valid for this term type.
		 * @param c The character to test
		 * @param first Weather it is the first character of this term
		 * @return true if and only if the character is valid
		 */
		public boolean test(char c, boolean first) {
			return this.firstCharPredicate.test(c) || (!first && this.additonalCharPredicate.test(c));
		}
		
		/**
		 * Finds the first term type which the character is valid for, searched is in this order: numeric > label > structure > operator
		 * @param c The character to search for
		 * @param first Weather it is the first character of this term
		 * @return The first term type the character is valid for
		 */
		public static TermType ofChar(char c, boolean first) {
			for (var t : TermType.values())
				if (t.test(c, first)) return t;
			return OPERATOR;
		}
		
	}
	
	/**
	 * Evaluates the results (effects to the evaluation stack) of this term/operator.
	 * @param evalstack The evaluation stack, containing the results of previous terms
	 * @param parameters The parameters (variable values) for this evaluation
	 * @throws MathematicalEvaluationException If an exception during evaluation occurs, an example would be that a required parameters is missing
	 */
	public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters);
	
	/**
	 * Evaluates the results (effects to the evaluation stack) of this term/operator and its partial derivative in respect to the given variable.
	 * @param evalstack The evaluation stack, containing the results of previous terms
	 * @param diffstack The evaluation stack for the derivative operations
	 * @param parameters The parameters (variable values) for this evaluation
	 * @param variable The variable for which the partial derivative should be computed
	 * @throws MathematicalEvaluationException If an exception during evaluation occurs, an example would be that a required parameters is missing
	 */
	public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable);
	
	/**
	 * Returns a string representation of this term (such as the value of a constant, name of an variable or symbol for an operator)
	 * @return The string representation of this term
	 */
	public String str();
	
	/**
	 * Returns the {@link TermType} of this term
	 * @return The type of this term
	 */
	public TermType type();
	
}
